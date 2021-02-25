package com.mostlycertain.jupiter.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Execute a SQL script from a resource file before each test runs.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ExecuteSqlResources.class)
public @interface ExecuteSqlResource {
    /**
     * Name of the resource to load.
     *
     * The resource name can be relative to the test class.
     */
    String value();

    /**
     * Name of the database connection to execute the SQL on.
     */
    String connectionName() default DatabaseConnection.DEFAULT_NAME;
}
