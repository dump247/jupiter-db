package com.mostlycertain.jupiter.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mostlycertain.jupiter.db.AnnotationUtil.getAnnotations;
import static com.mostlycertain.jupiter.db.ResourceUtil.loadTextResource;
import static java.lang.String.format;

public final class SqlRunner {
    private final List<String> initializeSql;
    private final List<String> finalizeSql;

    public SqlRunner(final List<String> initializeSql, final List<String> finalizeSql) {
        this.initializeSql = new ArrayList<>(initializeSql);
        this.finalizeSql = new ArrayList<>(finalizeSql);
    }

    public void executeInitializeSql(final Connection connection) throws SQLException {
        executeSql(connection, initializeSql);
    }

    public void executeFinalizeSql(final Connection connection) throws SQLException {
        executeSql(connection, finalizeSql);
    }

    private static void executeSql(final Connection connection, final List<String> sqlScripts) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            for (final String sqlScript : sqlScripts) {
                executeScript(statement, sqlScript);
            }
        }
    }

    private static void executeScript(final Statement statement, final String sqlScript) throws SQLException {
        SqlParseState state = SqlParseState.NONE;
        char quoteChar = 0;
        final StringBuilder statementBuffer = new StringBuilder();

        for (int index = 0; index < sqlScript.length(); index += 1) {
            char ch = sqlScript.charAt(index);
            statementBuffer.append(ch);

            switch (state) {
                case NONE:
                    switch (ch) {
                        case '\'':
                        case '"':
                        case '`':
                            state = SqlParseState.QUOTE;
                            quoteChar = ch;
                            break;
                        case '-':
                            if (isNextChar(sqlScript, index, '-')) {
                                state = SqlParseState.LINE_COMMENT;
                                index += 1;
                                statementBuffer.append(sqlScript.charAt(index));
                            }
                            break;
                        case '/':
                            if (isNextChar(sqlScript, index, '*')) {
                                state = SqlParseState.MULTILINE_COMMENT;
                                index += 1;
                                statementBuffer.append(sqlScript.charAt(index));
                            }
                            break;
                        case ';':
                            // Execute the sql without the delimiter and clear the statement buffer
                            executeStatement(statement, statementBuffer.substring(0, statementBuffer.length() - 1));
                            statementBuffer.setLength(0);
                            break;
                    }
                    break;
                case QUOTE:
                    if (quoteChar == ch && !isNextChar(sqlScript, index, quoteChar)) {
                        state = SqlParseState.NONE;
                    }
                    break;
                case LINE_COMMENT:
                    if (ch == '\n') {
                        state = SqlParseState.NONE;
                    }
                    break;
                case MULTILINE_COMMENT:
                    if (ch == '*' && isNextChar(sqlScript, index, '/')) {
                        state = SqlParseState.NONE;
                        index += 1;
                        statementBuffer.append(sqlScript.charAt(index));
                    }
                    break;
            }
        }

        // Execute any final statement that does not end with a delimiter
        executeStatement(statement, statementBuffer.toString());
    }

    private static boolean isNextChar(final String str, final int index, final char nextChar) {
        return index < str.length() - 1 && str.charAt(index + 1) == nextChar;
    }

    private static void executeStatement(final Statement statement, final String sql) throws SQLException {
        if (sql.trim().length() > 0) {
            statement.execute(sql);
        }
    }

    /**
     * Extract SQL scripts to execute from {@link InitializeSql} and {@link FinalizeSql}
     * annotations attached to the given element.
     *
     * @param testMethod Unit test method.
     * @return Map from connection name to SQL scripts.
     */
    public static SqlRunner readAnnotations(final Method testMethod) {
        return readAnnotations(testMethod.getDeclaringClass(), testMethod);
    }

    /**
     * Extract SQL scripts to execute from {@link InitializeSql} and {@link FinalizeSql}
     * annotations attached to the given element.
     *
     * @param testClass Unit test class.
     * @return Map from connection name to SQL scripts.
     */
    public static SqlRunner readAnnotations(final Class<?> testClass) {
        return readAnnotations(testClass, testClass);
    }

    private static SqlRunner readAnnotations(
            final Class<?> testClass,
            final AnnotatedElement element
    ) {
        final List<String> initializeSql = getAnnotations(element, InitializeSql.class)
                .flatMap(a -> Stream.of(
                        a.value().isEmpty() ? null : a.value(),
                        a.resource().isEmpty() ? null : loadSqlResource(testClass, a, a.resource())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final List<String> finalizeSql = getAnnotations(element, FinalizeSql.class)
                .flatMap(a -> Stream.of(
                        a.value().isEmpty() ? null : a.value(),
                        a.resource().isEmpty() ? null : loadSqlResource(testClass, a, a.resource())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new SqlRunner(initializeSql, finalizeSql);
    }

    private static String loadSqlResource(
            final Class<?> testClass,
            final Annotation annotation,
            final String resourceName
    ) {
        return loadTextResource(testClass, resourceName)
                .orElseThrow(() -> new RuntimeException(format(
                        "@%s resource not found: resource=%s test=%s",
                        annotation.annotationType().getSimpleName(),
                        resourceName,
                        testClass.getName())));
    }

    private enum SqlParseState {
        NONE,
        QUOTE,
        LINE_COMMENT,
        MULTILINE_COMMENT,
    }
}
