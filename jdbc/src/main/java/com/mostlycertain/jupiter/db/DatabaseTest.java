package com.mostlycertain.jupiter.db;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds the database test extension to a test class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DatabaseTestExtension.class)
public @interface DatabaseTest {
    /**
     * JDBC connection string.
     */
    String url() default "";

    /**
     * Database username.
     */
    String user() default "";

    /**
     * Database password.
     */
    String password() default "";
}
