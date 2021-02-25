package com.mostlycertain.jupiter.db;

import java.lang.reflect.AnnotatedElement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * Database connection configuration options.
 */
public final class DatabaseConnectionConfig {
    private static final Pattern SYSTEM_PROP_KEY = Pattern.compile("^jupiterdb\\.connection\\.([^.]+?)\\.(url|username|password)$");

    private static final DatabaseConnectionConfig DEFAULT = new DatabaseConnectionConfig(new Builder());

    private final String url;

    private final String username;

    private final String password;

    private DatabaseConnectionConfig(final Builder builder) {
        this.url = builder.url;
        this.username = builder.username;
        this.password = builder.password;
    }

    @Override
    public String toString() {
        return "url=" + getUrl()
                + " username=" + getUsername()
                + " password=" + getPassword();
    }

    /**
     * JDBC url or empty string if not set.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Database username or empty string if not set.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Database password or empty string if not set.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Establish a database connection with the settings from this configuration.
     *
     * @return A connection to the {@link #getUrl() url}.
     * @throws SQLException If a database access error occurs.
     */
    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(this.url, this.username, this.password);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default configuration with no properties set.
     */
    public static DatabaseConnectionConfig getDefault() {
        return DEFAULT;
    }

    /**
     * Read database connection settings from system properties.
     *
     * The system properties are in the format:
     * {@code jupterdb.connection.NAME.url|username|password} where NAME is the connection name
     * the value is for.
     *
     * @return Map from connection name to configuration.
     */
    public static Map<String, DatabaseConnectionConfig> readSystemProperties() {
        final Map<String, DatabaseConnectionConfig.Builder> connections = new HashMap<>();

        for (final Map.Entry<Object, Object> prop : System.getProperties().entrySet()) {
            final String key = Objects.toString(prop.getKey());
            final Matcher matcher = SYSTEM_PROP_KEY.matcher(key);

            if (matcher.matches()) {
                final String value = Objects.toString(prop.getValue()).trim();
                final String name = matcher.group(1);
                final String configName = matcher.group(2);
                final DatabaseConnectionConfig.Builder connection = connections.computeIfAbsent(
                        name,
                        k -> DatabaseConnectionConfig.builder());

                switch (configName) {
                    case "url":
                        connection.url(value);
                        break;

                    case "username":
                        connection.username(value);
                        break;

                    case "password":
                        connection.password(value);
                        break;

                    default:
                        throw new UnsupportedOperationException("Configuration property not handled: " + configName);
                }
            }
        }

        return connections.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().build()));
    }

    /**
     * Read database connection settings from annotations attached to an element.
     *
     * Reads {@link DatabaseConnection} and {@link DatabaseTest} annotations.
     *
     * @param element Element to read the annotations from.
     * @return Map from connection name to configuration.
     * @throws IllegalArgumentException If an invalid annotation is discovered. For example, if
     *                                  duplication connection names are found.
     */
    public static Map<String, DatabaseConnectionConfig> readAnnotations(
            final AnnotatedElement element
    ) {
        final Map<String, DatabaseConnectionConfig> connections = new HashMap<>();
        final DatabaseConnections connectionAnnotationList = element.getAnnotation(DatabaseConnections.class);
        final DatabaseConnection connectionAnnotation = element.getAnnotation(DatabaseConnection.class);
        final DatabaseTest databaseTest = element.getAnnotation(DatabaseTest.class);

        if (connectionAnnotation != null) {
            connections.put(connectionAnnotation.name(), fromAnnotation(connectionAnnotation));
        }

        if (connectionAnnotationList != null) {
            for (final DatabaseConnection annotation : connectionAnnotationList.value()) {
                if (connections.containsKey(annotation.name())) {
                    throw new IllegalArgumentException(format(
                            "@DatabaseConnection annotations with duplicate name found.\nname=%s\nelement=%s",
                            annotation.name(),
                            element));
                }

                connections.put(annotation.name(), fromAnnotation(annotation));
            }
        }

        if (databaseTest != null) {
            if (connections.containsKey(databaseTest.name())) {
                throw new IllegalArgumentException(format(
                        "@DatabaseTest and @DatabaseConnection annotations with duplicate name found.\nname=%s\nelement=%s",
                        databaseTest.name(),
                        element));
            }

            connections.put(
                    databaseTest.name(),
                    DatabaseConnectionConfig.builder()
                            .url(databaseTest.url().trim())
                            .username(databaseTest.username().trim())
                            .password(databaseTest.password().trim())
                            .build());
        }

        return connections;
    }

    private static DatabaseConnectionConfig fromAnnotation(final DatabaseConnection annotation) {
        return DatabaseConnectionConfig.builder()
                .url(annotation.url().trim())
                .username(annotation.username().trim())
                .password(annotation.password().trim())
                .build();
    }

    /**
     * Resolve the database configuration settings with the given name.
     *
     * Searches the provided maps for settings with the given name. Settings in later maps
     * override settings in earlier maps. The resolved settings may be partially or completely
     * configured.
     *
     * @param name              Configuration settings name to resolve.
     * @param configurationMaps Maps from configuration name to settings.
     * @return Resolved configuration settings.
     */
    @SafeVarargs
    public static DatabaseConnectionConfig resolve(
            final String name,
            final Map<String, DatabaseConnectionConfig>... configurationMaps
    ) {
        final DatabaseConnectionConfig.Builder resolved = builder();

        for (final Map<String, DatabaseConnectionConfig> configurationMap : configurationMaps) {
            final DatabaseConnectionConfig configuration = configurationMap.get(name);

            if (configuration != null) {
                if (configuration.url.length() > 0) {
                    resolved.url(configuration.url);
                }

                if (configuration.username.length() > 0) {
                    resolved.username(configuration.username);
                }

                if (configuration.password.length() > 0) {
                    resolved.password(configuration.password);
                }
            }
        }

        return resolved.build();
    }

    public static class Builder {
        private String url = "";
        private String username = "";
        private String password = "";

        /**
         * JDBC url or empty string if not set.
         */
        public Builder url(final String url) {
            this.url = requireNonNull(url);
            return this;
        }

        /**
         * Database username or empty string if not set.
         */
        public Builder username(final String username) {
            this.username = requireNonNull(username);
            return this;
        }

        /**
         * Database password or empty string if not set.
         */
        public Builder password(final String password) {
            this.password = requireNonNull(password);
            return this;
        }

        public DatabaseConnectionConfig build() {
            if (url.isEmpty() && username.isEmpty() && password.isEmpty()) {
                return getDefault();
            }

            return new DatabaseConnectionConfig(this);
        }
    }
}
