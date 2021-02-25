package com.mostlycertain.jupiter.db.jooq;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Scope;
import org.jooq.Transaction;
import org.jooq.TransactionContext;
import org.jooq.conf.Settings;

import java.util.HashMap;
import java.util.Map;

/**
 * Hack around jOOQ's transaction management in the DSLContext.
 *
 * The issue is that the context begins a transaction for the first transaction and then uses
 * save points for further inner transactions. Without this hack, when the first transaction
 * commits, it would commit the transaction containing the test. Using this hack, we force jOOQ to
 * use save points for every transaction. That way the outer transaction is only rolled back by
 * the database test extension once the test completes.
 */
class DefaultTransactionContext implements TransactionContext, Scope {
    private final Map<Object, Object> data = new HashMap<>();
    private final DSLContext dsl;
    private Throwable cause;
    private Transaction transaction;

    DefaultTransactionContext(final DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Transaction transaction() {
        return transaction;
    }

    @Override
    public TransactionContext transaction(final Transaction transaction) {
        this.transaction = transaction;
        return this;
    }

    @Override
    public Exception cause() {
        return cause instanceof Exception ? (Exception) cause : null;
    }

    @Override
    public Throwable causeThrowable() {
        return cause;
    }

    @Override
    public TransactionContext cause(final Exception cause) {
        return causeThrowable(cause);
    }

    @Override
    public TransactionContext causeThrowable(final Throwable cause) {
        this.cause = cause;
        return this;
    }

    @Override
    public Configuration configuration() {
        return dsl.configuration();
    }

    @Override
    public DSLContext dsl() {
        return dsl;
    }

    @Override
    public Settings settings() {
        return dsl.settings();
    }

    @Override
    public SQLDialect dialect() {
        return dsl.dialect();
    }

    @Override
    public SQLDialect family() {
        return dsl.family();
    }

    @Override
    public Map<Object, Object> data() {
        return data;
    }

    @Override
    public Object data(final Object key) {
        return data.get(key);
    }

    @Override
    public Object data(final Object key, final Object value) {
        return data.put(key, value);
    }
}
