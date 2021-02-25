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
import java.util.UUID;
import java.util.stream.Collectors;

import static com.mostlycertain.jupiter.db.ExtensionStoreUtils.addToList;
import static com.mostlycertain.jupiter.db.ExtensionStoreUtils.getList;
import static com.mostlycertain.jupiter.db.ExtensionStoreUtils.getMap;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * test.datasource.url
 * test.datasource.user
 * test.datasource.password
 * test.datasource.superuser.user
 * test.datasource.superuser.password
 */
public class DatabaseTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(DatabaseTestExtension.class);
    private static final String SYSTEM_PROPERTY_CONNECTION_CONFIG_KEY = "systemPropertyConnectionConfig";
    private static final String PACKAGE_CONNECTION_CONFIG_KEY = "packageConnectionConfig";
    private static final String CLASS_CONNECTION_CONFIG_KEY = "classConnectionConfig";
    private static final String METHOD_CONNECTION_CONFIG_KEY = "methodConnectionConfig";
    private static final String CONNECTIONS_KEY = "connections";

    @Override
    public void beforeAll(final ExtensionContext context) {
        final ExtensionContext.Store store = context.getStore(NAMESPACE);

        Optional.of(DatabaseConnectionConfig.readSystemProperties())
                .filter(c -> c.size() > 0)
                .ifPresent(c -> store.put(SYSTEM_PROPERTY_CONNECTION_CONFIG_KEY, c));

        context.getTestClass()
                .map(c -> DatabaseConnectionConfig.readAnnotations(c.getPackage()))
                .filter(c -> c.size() > 0)
                .ifPresent(c -> store.put(PACKAGE_CONNECTION_CONFIG_KEY, c));

        context.getTestClass()
                .map(DatabaseConnectionConfig::readAnnotations)
                .filter(c -> c.size() > 0)
                .ifPresent(c -> store.put(CLASS_CONNECTION_CONFIG_KEY, c));
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        final ExtensionContext.Store store = context.getStore(NAMESPACE);

        context.getTestMethod()
                .map(DatabaseConnectionConfig::readAnnotations)
                .filter(c -> c.size() > 0)
                .ifPresent(c -> store.put(METHOD_CONNECTION_CONFIG_KEY, c));
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        final ExtensionContext.Store store = context.getStore(NAMESPACE);
        final List<TestDatabaseConnection> connections = getList(store, CONNECTIONS_KEY);
        final List<TestDatabaseConnection> failedToClose = connections.stream()
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

        return Connection.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolveParameter(
            final ParameterContext parameterContext,
            final ExtensionContext extensionContext
    ) throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();

        if (Connection.class.isAssignableFrom(parameterType)) {
            return getConnection(parameterContext, extensionContext);
        } else {
            throw new ParameterResolutionException("Unsupported parameter: " + parameterContext);
        }
    }

    private Connection getConnection(
            final ParameterContext parameterContext,
            final ExtensionContext extensionContext
    ) throws ParameterResolutionException {
        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        final String connectionName = parameterContext.findAnnotation(ConnectionName.class)
                .map(ConnectionName::value)
                .orElse(DatabaseConnection.DEFAULT_NAME);
        final DatabaseConnectionConfig connectionConfig = DatabaseConnectionConfig.resolve(
                connectionName,
                getMap(store, PACKAGE_CONNECTION_CONFIG_KEY),
                getMap(store, CLASS_CONNECTION_CONFIG_KEY),
                getMap(store, METHOD_CONNECTION_CONFIG_KEY),
                getMap(store, SYSTEM_PROPERTY_CONNECTION_CONFIG_KEY));

        try {
            final TestDatabaseConnection connection = new TestDatabaseConnection(
                    connectionName,
                    connectionConfig);

            addToList(store, CONNECTIONS_KEY, connection);

            return connection.connection;
        } catch (final SQLException ex) {
            throw new ParameterResolutionException(
                    format("Error establishing connection to database : name=%s %s",
                            connectionName,
                            connectionConfig),
                    ex);
        }
    }

    private static class TestDatabaseConnection {
        final String name;
        final DatabaseConnectionConfig configuration;
        final Connection connection;
        final Savepoint savePoint;
        SQLException closeError;

        TestDatabaseConnection(
                final String name,
                final DatabaseConnectionConfig configuration
        ) throws SQLException {
            this.name = name;
            this.configuration = configuration;
            this.connection = configuration.createConnection();

            connection.setAutoCommit(false);
            this.savePoint = connection.setSavepoint("test" + UUID.randomUUID().toString().replace("-", ""));
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
