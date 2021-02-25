package com.mostlycertain.jupiter.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SqlRunner {
    private SqlRunner() {
        // Private so instances can not be created
    }

    /**
     * Extract SQL scripts to execute from {@link ExecuteSql} and {@link ExecuteSqlResource}
     * annotations attached to the given element.
     *
     * @param testMethod Unit test method.
     * @return Map from connection name to SQL scripts.
     */
    public static Map<String, List<String>> readAnnotations(
            final Method testMethod
    ) {
        return readAnnotations(testMethod.getDeclaringClass(), testMethod);
    }

    /**
     * Extract SQL scripts to execute from {@link ExecuteSql} and {@link ExecuteSqlResource}
     * annotations attached to the given element.
     *
     * @param testClass Unit test class.
     * @return Map from connection name to SQL scripts.
     */
    public static Map<String, List<String>> readAnnotations(
            final Class<?> testClass
    ) {
        return readAnnotations(testClass, testClass);
    }

    private static Map<String, List<String>> readAnnotations(
            final Class<?> testClass,
            final AnnotatedElement element
    ) {
        final Map<String, List<String>> result = new HashMap<>();
        final ExecuteSqlResource resourceAnnotation = element.getAnnotation(ExecuteSqlResource.class);
        final ExecuteSqlResources resourcesAnnotations = element.getAnnotation(ExecuteSqlResources.class);
        final ExecuteSql sqlAnnotation = element.getAnnotation(ExecuteSql.class);
        final ExecuteSqls sqlAnnotations = element.getAnnotation(ExecuteSqls.class);

        if (resourceAnnotation != null) {
            result.computeIfAbsent(resourceAnnotation.connectionName(), k -> new ArrayList<>())
                    .add(loadResource(testClass, resourceAnnotation));
        }

        if (resourcesAnnotations != null) {
            for (final ExecuteSqlResource annotation : resourcesAnnotations.value()) {
                result.computeIfAbsent(annotation.connectionName(), k -> new ArrayList<>())
                        .add(loadResource(testClass, annotation));
            }
        }

        if (sqlAnnotation != null) {
            result.computeIfAbsent(sqlAnnotation.connectionName(), k -> new ArrayList<>())
                    .add(sqlAnnotation.value());
        }

        if (sqlAnnotations != null) {
            for (final ExecuteSql annotation : sqlAnnotations.value()) {
                result.computeIfAbsent(annotation.connectionName(), k -> new ArrayList<>())
                        .add(annotation.value());
            }
        }

        return result;
    }

    private static String loadResource(
            final Class<?> testClass,
            final ExecuteSqlResource annotation
    ) {
        final InputStream resource = testClass.getResourceAsStream(annotation.value());

        if (resource == null) {
            throw new RuntimeException("SQL resource file not found: name=" + annotation.value() + " test=" + testClass.getName());
        }

        try (final Reader stream = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            final char[] readBuffer = new char[1024];
            final StringBuilder output = new StringBuilder();
            int readLen;

            while ((readLen = stream.read(readBuffer)) >= 0) {
                output.append(readBuffer, 0, readLen);
            }

            return output.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void executeScript(final Connection connection, final String sql) throws SQLException {
        try {
            executeScript(connection, new StringReader(sql));
        } catch (final IOException ex) {
            throw new RuntimeException("Unexpected I/O exception", ex);
        }
    }

    public static void executeScript(final Connection connection, final Reader reader) throws SQLException, IOException {
        final BufferedReader bufferedReader = new BufferedReader(reader);

        try (final Statement statement = connection.createStatement()) {
            String line;
            StringBuilder sqlStatement = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("--") || line.startsWith("//")) {
                    // Skip full line comments
                    continue;
                }

                sqlStatement.append(line);

                if (line.endsWith(";")) {
                    statement.execute(sqlStatement.substring(0, sqlStatement.length() - 1));
                    sqlStatement.setLength(0);
                }
            }

            final String finalSql = sqlStatement.toString().trim();

            if (finalSql.length() > 0) {
                statement.execute(finalSql);
            }
        }
    }
}
