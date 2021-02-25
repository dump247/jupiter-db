package com.mostlycertain.jupiter.db;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.opentest4j.AssertionFailedError;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mostlycertain.jupiter.db.ExtensionStoreUtils.addToList;
import static com.mostlycertain.jupiter.db.ExtensionStoreUtils.get;
import static com.mostlycertain.jupiter.db.ExtensionStoreUtils.getList;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class DatabaseTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(DatabaseTestExtension.class);
    private static final String SYSTEM_PROPERTY_CONNECTION_CONFIG_KEY = "systemPropertyConnectionConfig";
    private static final String CLASS_CONNECTION_CONFIG_KEY = "classConnectionConfig";
    private static final String CLASS_SQL_KEY = "classSql";
    private static final String METHOD_SQL_KEY = "methodSql";
    private static final String CONNECTIONS_KEY = "connections";

    private static final ServiceLoader<DatabaseConnectionAdapter> ADAPTERS = ServiceLoader.load(DatabaseConnectionAdapter.class);

    @Override
    public void beforeAll(final ExtensionContext context) {
        final ExtensionContext.Store store = context.getStore(NAMESPACE);

        store.put(
                SYSTEM_PROPERTY_CONNECTION_CONFIG_KEY,
                DatabaseConnectionConfig.readSystemProperties());

        context.getTestClass()
                .map(DatabaseConnectionConfig::readAnnotation)
                .ifPresent(c -> store.put(CLASS_CONNECTION_CONFIG_KEY, c));

        context.getTestClass()
                .map(SqlRunner::readAnnotations)
                .ifPresent(c -> store.put(CLASS_SQL_KEY, c));
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        final ExtensionContext.Store store = context.getStore(NAMESPACE);

        context.getTestMethod()
                .map(SqlRunner::readAnnotations)
                .ifPresent(c -> store.put(METHOD_SQL_KEY, c));
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        final ExtensionContext.Store store = context.getStore(NAMESPACE);
        final List<ManagedDatabaseConnection> connections = getList(store, CONNECTIONS_KEY);
        final List<ManagedDatabaseConnection> failedToClose = connections.stream()
                .filter(c -> !c.close())
                .collect(Collectors.toList());

        if (failedToClose.size() > 0) {
            final AssertionFailedError error = new AssertionFailedError(format(
                    "Error rolling back and closing database connections: %s",
                    failedToClose.stream().map(c -> c.name).collect(joining(", "))));

            failedToClose.forEach(c -> error.addSuppressed(c.closeError));

            throw error;
        }
    }

    @Override
    public boolean supportsParameter(
            final ParameterContext parameterContext,
            final ExtensionContext extensionContext
    ) throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();

        return Connection.class.isAssignableFrom(parameterType)
                || adapters().anyMatch(a -> a.supportsParameter(parameterContext, extensionContext));
    }

    @Override
    public Object resolveParameter(
            final ParameterContext parameterContext,
            final ExtensionContext extensionContext
    ) throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();

        if (Connection.class.isAssignableFrom(parameterType)) {
            return getConnection(parameterContext, extensionContext).getConnection();
        } else {
            return adapters()
                    .filter(a -> a.supportsParameter(parameterContext, extensionContext))
                    .map(adapter -> adapter.resolveParameter(
                            getConnection(parameterContext, extensionContext),
                            parameterContext,
                            extensionContext))
                    .findFirst()
                    .orElseThrow(() -> new ParameterResolutionException(
                            "Unsupported parameter: " + parameterContext));
        }
    }

    private static Stream<DatabaseConnectionAdapter> adapters() {
        return StreamSupport.stream(ADAPTERS.spliterator(), false);
    }

    private ManagedDatabaseConnection getConnection(
            final ParameterContext parameterContext,
            final ExtensionContext extensionContext
    ) throws ParameterResolutionException {
        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        final String connectionName = parameterContext.getParameter().getName();
        final DatabaseConnectionConfig connectionConfig = getConnectionConfig(store);

        try {
            final ManagedDatabaseConnection connection = new ManagedDatabaseConnection(connectionName, connectionConfig);

            final Optional<SqlRunner> classSql = get(store, CLASS_SQL_KEY, SqlRunner.class);
            final Optional<SqlRunner> methodSql = get(store, METHOD_SQL_KEY, SqlRunner.class);

            if (classSql.isPresent()) {
                classSql.get().executeInitializeSql(connection.getConnection());
            }

            if (methodSql.isPresent()) {
                methodSql.get().executeInitializeSql(connection.getConnection());
            }

            if (classSql.isPresent()) {
                classSql.get().executeFinalizeSql(connection.getConnection());
            }

            if (methodSql.isPresent()) {
                methodSql.get().executeFinalizeSql(connection.getConnection());
            }

            addToList(store, CONNECTIONS_KEY, connection);

            return connection;
        } catch (final SQLException ex) {
            throw new ParameterResolutionException(
                    format("Error establishing connection to database : name=%s %s",
                            connectionName,
                            connectionConfig),
                    ex);
        }
    }

    private DatabaseConnectionConfig getConnectionConfig(final ExtensionContext.Store store) {
        final DatabaseConnectionConfig classConfig = store.getOrDefault(
                CLASS_CONNECTION_CONFIG_KEY,
                DatabaseConnectionConfig.class,
                DatabaseConnectionConfig.getDefault());
        final DatabaseConnectionConfig systemPropertyConfig = store.getOrDefault(
                SYSTEM_PROPERTY_CONNECTION_CONFIG_KEY,
                DatabaseConnectionConfig.class,
                DatabaseConnectionConfig.getDefault());

        return classConfig.merge(systemPropertyConfig);
    }

    private static class ManagedDatabaseConnection implements DatabaseTestConnection {
        final String name;
        final DatabaseConnectionConfig configuration;
        final Connection connection;
        final Savepoint savePoint;
        SQLException closeError;

        ManagedDatabaseConnection(
                final String name,
                final DatabaseConnectionConfig configuration
        ) throws SQLException {
            this.name = name;
            this.configuration = configuration;
            this.connection = configuration.createConnection();

            connection.setAutoCommit(false);
            this.savePoint = connection.setSavepoint("test" + UUID.randomUUID().toString().replace("-", ""));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public DatabaseConnectionConfig getConfig() {
            return configuration;
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        boolean close() {
            try {
                try {
                    connection.rollback(savePoint);
                    connection.releaseSavepoint(savePoint);
                } finally {
                    connection.close();
                }
            } catch (final SQLException ex) {
                this.closeError = ex;
            }

            return this.closeError == null;
        }
    }
}
