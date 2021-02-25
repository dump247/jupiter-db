package com.mostlycertain.jupiter.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration for a connection to the database.
 *
 * A database connection can be configured on the class or test method. The connection
 * settings are merged when the test is run where the lower values override the upper. Empty
 * string values are considered not set. The connection settings be set or overridden at runtime
 * with system properties.
 *
 * In the following example, the database connection will have url=foo, username=farb,
 * and password=foob.
 * <pre>
 * @DatabaseConnection(username="farb")
 * class TestClass {
 *     @DatabaseConnection(password = "foob")
 *     void test(Connection connection) {
 *         // ...
 *     }
 * }
 * </pre>
 */
@Repeatable(DatabaseConnections.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DatabaseConnection {
    String DEFAULT_NAME = "default";

    /**
     * Name of the connection settings.
     *
     * Multiple sets of connection settings can be used with different names. The connection
     * settings with name "default" are used when no explicit connection name is specified by the
     * test.
     *
     * A use case for this would be a set of credentials for the test to use that have
     * permissions limited to what the application is assigned another set of super user
     * credentials used for test setup.
     */
    String name() default DEFAULT_NAME;

    /**
     * JDBC connection string.
     */
    String url() default "";

    /**
     * Database username.
     */
    String username() default "";

    /**
     * Database password.
     */
    String password() default "";
}
