package com.mostlycertain.jupiter.db;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceUtilTest {
    @ParameterizedTest
    @CsvSource({
            ",non_existant_resource.txt",
            "/test_resource.txt,/test_resource.txt",
            "/com/mostlycertain/jupiter/db/test_resource.txt,test_resource.txt",
            "/com/mostlycertain/jupiter/db/test_resource.txt,/com/mostlycertain/jupiter/db/test_resource.txt",
    })
    void loadTextResource(final String expected, final String resourceName) {
        assertEquals(
                Optional.ofNullable(expected),
                ResourceUtil.loadTextResource(ResourceUtilTest.class, resourceName));
    }
}
