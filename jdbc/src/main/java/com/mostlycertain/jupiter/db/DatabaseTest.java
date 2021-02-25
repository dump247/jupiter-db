package com.mostlycertain.jupiter.db;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds the database test extension to a test class.
 *
 * Multiple database connections can be configured by using {@link DatabaseConnection}
 * annotations with different names. This {@code DatabaseTest} annotation is is equivalent to a
 * {@code DatabaseConnection} annotation on the test class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DatabaseTestExtension.class)
public @interface DatabaseTest {
    /**
     * See {@link DatabaseConnection#name()}.
     */
    String name() default DatabaseConnection.DEFAULT_NAME;

    /**
     * See {@link DatabaseConnection#url()}.
     */
    String url() default "";

    /**
     * See {@link DatabaseConnection#username()}.
     */
    String username() default "";

    /**
     * See {@link DatabaseConnection#password()}}.
     */
    String password() default "";
}
