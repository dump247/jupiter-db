package com.mostlycertain.jupiter.db.jooq;

import com.mostlycertain.jupiter.db.DatabaseConnectionAdapter;
import com.mostlycertain.jupiter.db.DatabaseTestConnection;
import org.jooq.DSLContext;
import org.jooq.TransactionProvider;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/**
 * Handles test parameters of type {@link DSLContext}.
 */
public class JooqConnectionAdapter implements DatabaseConnectionAdapter {
    @Override
    public boolean supportsParameter(
            final ParameterContext parameterContext,
            final ExtensionContext extensionContext
    ) throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();

        return DSLContext.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolveParameter(
            final DatabaseTestConnection connection,
            final ParameterContext parameterContext,
            final ExtensionContext extensionContext
    ) throws ParameterResolutionException {
        final Class<?> parameterType = parameterContext.getParameter().getType();

        if (DSLContext.class.isAssignableFrom(parameterType)) {
            return buildDslContext(connection);
        } else {
            throw new ParameterResolutionException("Unsupported parameter: " + parameterContext);
        }
    }

    private DSLContext buildDslContext(final DatabaseTestConnection connection) {
        final DefaultTransactionContext transactionContext = new DefaultTransactionContext(
                DSL.using(connection.getConnection()));
        final TransactionProvider transactionProvider = transactionContext
                .configuration()
                .transactionProvider();

        transactionProvider.begin(transactionContext);

        return transactionContext.dsl();
    }
}
