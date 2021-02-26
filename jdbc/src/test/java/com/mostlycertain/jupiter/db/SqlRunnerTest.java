package com.mostlycertain.jupiter.db;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SqlRunnerTest {
    public static Stream<Arguments> splitStatementsData() {
        return Stream.of(
                arguments(emptyList(), ""),
                arguments(emptyList(), "     "),
                arguments(emptyList(), ";"),
                arguments(emptyList(), "; ;;;; ; "),
                arguments(singletonList("S V"), "S V"),
                arguments(asList(
                        "S V",
                        " S T   "
                ), "S V; S T   ;"),
                arguments(asList(
                        "S 'V'",
                        " S \"T \"   ",
                        " S `O` "
                ), "S 'V'; S \"T \"   ; S `O` "),
                arguments(singletonList(
                        "S '' \"\" ``"
                ), "S '' \"\" ``"),
                arguments(asList(
                        "S 'V;X'",
                        " S \"T; \"   ",
                        " S `;O` "
                ), "S 'V;X'; S \"T; \"   ; S `;O` "),
                arguments(asList(
                        "S 'V--X'",
                        " S \"T-- \"   ",
                        " S `--O` "
                ), "S 'V--X'; S \"T-- \"   ; S `--O` "),
                arguments(asList(
                        "S 'V/*X'",
                        " S \"T/* \"   ",
                        " S `/*O` "
                ), "S 'V/*X'; S \"T/* \"   ; S `/*O` "),
                arguments(asList(
                        "S 'V''X'",
                        " S \"T\"\" \"   ",
                        " S ```O` "
                ), "S 'V''X'; S \"T\"\" \"   ; S ```O` "),
                arguments(asList(
                        "S V",
                        "\nS -- ; line ; --comment\r\n",
                        "S /* multiline; * / \ncomment\n;*/\n"
                ), "S V;\nS -- ; line ; --comment\r\n;S /* multiline; * / \ncomment\n;*/\n;")
        );
    }

    @ParameterizedTest
    @MethodSource("splitStatementsData")
    void splitStatements(final List<String> expected, final String sqlScript) {
        assertEquals(expected, SqlRunner.splitStatements(sqlScript));
    }
}
