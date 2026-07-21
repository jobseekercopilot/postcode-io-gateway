package com.jobseekercopilot.postcodeiogateway.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlaceSearchValidatorTest {
    private final PlaceSearchValidator validator = new PlaceSearchValidator();

    @Test
    void normalisesBoundedUnicodePlaceNames() {
        assertEquals("St. Albans", validator.validateAndNormalise("  St.   Albans ", 10));
        assertEquals("King's Lynn", validator.validateAndNormalise("King's Lynn", 1));
        assertEquals("Ynys Môn", validator.validateAndNormalise("Ynys Môn", 5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A", "Leeds?limit=100", "../London", "London/Westminster", "Line\nBreak"})
    void rejectsMissingShortOrUnsafeQueries(String query) {
        assertThrows(InvalidPlaceSearchException.class,
                () -> validator.validateAndNormalise(query, 10));
    }

    @Test
    void rejectsExcessiveQueriesAndLimits() {
        assertThrows(InvalidPlaceSearchException.class,
                () -> validator.validateAndNormalise("L".repeat(81), 10));
        assertThrows(InvalidPlaceSearchException.class,
                () -> validator.validateAndNormalise("Leeds", 0));
        assertThrows(InvalidPlaceSearchException.class,
                () -> validator.validateAndNormalise("Leeds", 11));
    }
}
