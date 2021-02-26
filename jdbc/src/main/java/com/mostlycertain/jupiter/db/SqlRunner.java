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
                for (final String statementSql : splitStatements(sqlScript)) {
                    statement.execute(statementSql);
                }
            }
        }
    }

    public static List<String> splitStatements(final String sqlScript) {
        final List<String> statements = new ArrayList<>();
        int statementStartIndex = 0;
        int statementEndIndex = 0;
        SqlParseState state = SqlParseState.NONE;
        char quoteChar = 0;

        for (statementEndIndex = 0; statementEndIndex < sqlScript.length(); statementEndIndex += 1) {
            char ch = sqlScript.charAt(statementEndIndex);

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
                            if (isNextChar(sqlScript, statementEndIndex, '-')) {
                                state = SqlParseState.LINE_COMMENT;
                                statementEndIndex += 1;
                            }
                            break;
                        case '/':
                            if (isNextChar(sqlScript, statementEndIndex, '*')) {
                                state = SqlParseState.MULTILINE_COMMENT;
                                statementEndIndex += 1;
                            }
                            break;
                        case ';':
                            // Strip the delimiter and start a new statement after the delimiter
                            addStatement(sqlScript.substring(statementStartIndex, statementEndIndex), statements);
                            statementStartIndex = statementEndIndex + 1;
                            break;
                    }
                    break;
                case QUOTE:
                    if (quoteChar == ch) {
                        // If the next char is another quote, this is an escaped quote character
                        if (isNextChar(sqlScript, statementEndIndex, quoteChar)) {
                            statementEndIndex += 1;
                        } else {
                            state = SqlParseState.NONE;
                        }
                    }
                    break;
                case LINE_COMMENT:
                    if (ch == '\n') {
                        state = SqlParseState.NONE;
                    }
                    break;
                case MULTILINE_COMMENT:
                    if (ch == '*' && isNextChar(sqlScript, statementEndIndex, '/')) {
                        state = SqlParseState.NONE;
                        statementEndIndex += 1;
                    }
                    break;
            }
        }

        // Add any final statement that does not end with a delimiter
        if (statementStartIndex < statementEndIndex) {
            addStatement(sqlScript.substring(statementStartIndex, statementEndIndex), statements);
        }

        return statements;
    }

    private static boolean isNextChar(final String str, final int index, final char nextChar) {
        return index < str.length() - 1 && str.charAt(index + 1) == nextChar;
    }

    private static void addStatement(final String sqlStatement, final List<String> output) {
        if (sqlStatement.trim().length() > 0) {
            output.add(sqlStatement);
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
