package com.mostlycertain.jupiter.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * Database connection configuration options.
 */
public final class DatabaseConnectionConfig {
    private static final DatabaseConnectionConfig DEFAULT = new DatabaseConnectionConfig(new Builder());

    private final String url;

    private final String user;

    private final String password;

    private DatabaseConnectionConfig(final Builder builder) {
        this.url = builder.url;
        this.user = builder.user;
        this.password = builder.password;
    }

    @Override
    public String toString() {
        return "url=" + getUrl() + " user=" + getUser() + " password=" + getPassword();
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
    public String getUser() {
        return user;
    }

    /**
     * Database password or empty string if not set.
     */
    public String getPassword() {
        return password;
    }

    /**
     * True if none of the properties are set.
     *
     * @see #getDefault()
     */
    public boolean isEmpty() { return this == DEFAULT; }

    /**
     * Establish a database connection with the settings from this configuration.
     *
     * @return A connection to the {@link #getUrl() url}.
     * @throws SQLException If a database access error occurs.
     */
    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(this.url, this.user, this.password);
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
     * System property names:
     * <ul>
     *     <li>{@code jupterdb.database.url} JDBC URL</li>
     *     <li>{@code jupterdb.database.user} Database user</li>
     *     <li>{@code jupterdb.database.password} Database password</li>
     * </ul>
     *
     * @return Connection configuration from system properties.
     */
    public static DatabaseConnectionConfig readSystemProperties() {
        return DatabaseConnectionConfig.builder()
                .url(System.getProperty("jupterdb.database.url", "").trim())
                .user(System.getProperty("jupterdb.database.user", "").trim())
                .password(System.getProperty("jupterdb.database.password", "").trim())
                .build();
    }

    /**
     * Read database connection configuration from the {@link DatabaseTest} annotation on the
     * test class.
     *
     * @param testClass Test class.
     * @return Database connection configuration.
     */
    public static DatabaseConnectionConfig readAnnotation(final Class<?> testClass) {
        final DatabaseTest annotation = testClass.getAnnotation(DatabaseTest.class);

        if (annotation == null) {
            return DatabaseConnectionConfig.getDefault();
        }

        return DatabaseConnectionConfig.builder()
                .url(annotation.url())
                .user(annotation.user())
                .password(annotation.password())
                .build();
    }

    public DatabaseConnectionConfig merge(final DatabaseConnectionConfig config) {
        if (isEmpty()) {
            return config;
        } else if (config.isEmpty()) {
            return this;
        }

        return builder()
                .url(config.url.isEmpty() ? url : config.url)
                .user(config.user.isEmpty() ? user : config.user)
                .password(config.password.isEmpty() ? password : config.password)
                .build();
    }

    public static class Builder {
        private String url = "";
        private String user = "";
        private String password = "";

        /**
         * JDBC url or empty string if not set.
         */
        public Builder url(final String url) {
            this.url = requireNonNull(url);
            return this;
        }

        /**
         * Database user or empty string if not set.
         */
        public Builder user(final String user) {
            this.user = requireNonNull(user);
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
            if (url.isEmpty() && user.isEmpty() && password.isEmpty()) {
                return getDefault();
            }

            return new DatabaseConnectionConfig(this);
        }
    }
}
