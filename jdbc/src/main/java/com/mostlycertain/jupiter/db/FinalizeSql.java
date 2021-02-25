package com.mostlycertain.jupiter.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SQL to execute before a test starts but after all {@link InitializeSql} has executed.
 *
 * This is useful for things like switching to the role the tests should execute as.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FinalizeSqls.class)
public @interface FinalizeSql {
    /**
     * Raw SQL statements to execute.
     */
    String value() default "";

    /**
     * Name of a resource file that contains SQL statements to execute.
     *
     * The resource name can be relative to the test class the annotation is attached to.
     */
    String resource() default "";
}
