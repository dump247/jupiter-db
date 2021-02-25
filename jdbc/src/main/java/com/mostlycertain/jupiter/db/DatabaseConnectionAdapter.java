package com.mostlycertain.jupiter.db;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/**
 * Adapter that can be used to plugin database connection wrappers.
 *
 * The adapters are loaded via {@link java.util.ServiceLoader} by the {@link DatabaseTestExtension}.
 * To create an adapter, implement this interface and then add the full implementation class
 * name to a file named {@code META-INF/services/com.mostlycertain.jupiter.db.DatabaseConnectionAdapter}.
 */
public interface DatabaseConnectionAdapter {
    /**
     * Test if the given parameter is handled by this adpater.
     *
     * @param parameterContext Context for the parameter.
     * @param extensionContext Context for the {@link DatabaseTestExtension}.
     * @return True if this adapter handles the given parameter.
     * @throws ParameterResolutionException If an error occurs.
     */
    boolean supportsParameter(
            ParameterContext parameterContext,
            ExtensionContext extensionContext
    ) throws ParameterResolutionException;

    /**
     * Resolve the value of a parameter handled by this adapter.
     *
     * This method will only be called if {@link #supportsParameter} returns true.
     *
     * @param connection       Database connection to wrap.
     * @param parameterContext Context for the parameter.
     * @param extensionContext Context for the {@link DatabaseTestExtension}.
     * @return Value for the parameter.
     * @throws ParameterResolutionException If an error occurs.
     */
    Object resolveParameter(
            DatabaseTestConnection connection,
            ParameterContext parameterContext,
            ExtensionContext extensionContext
    ) throws ParameterResolutionException;
}
