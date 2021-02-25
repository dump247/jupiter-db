package com.mostlycertain.jupiter.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Execute SQL before each test runs.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ExecuteSqls.class)
public @interface ExecuteSql {
    /**
     * Sql statements to execute.
     */
    String value();

    /**
     * Name of the database connection to execute the SQL on.
     */
    String connectionName() default DatabaseConnection.DEFAULT_NAME;
}
