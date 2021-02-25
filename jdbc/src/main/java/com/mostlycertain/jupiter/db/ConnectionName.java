package com.mostlycertain.jupiter.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the named database connection to use for a parameter injected into a test.
 *
 * The connection name "default" is used for parameters where no connection name is explicitly set
 * with this annotation.
 *
 * An exception will be thrown if the connection name can not be found or is not properly
 * configured.
 *
 * Example:
 * <pre>
 * void test(@ConnectionName("super") Connection foo) {
 *     // ...
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ConnectionName {
    /**
     * Connection name.
     */
    String value();
}
