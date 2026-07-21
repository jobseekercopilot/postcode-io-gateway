package com.jobseekercopilot.postcodeiogateway.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostcodeValidatorTest {
    private final PostcodeValidator validator = new PostcodeValidator();

    @Test
    void canonicalisesFullPostcodes() {
        assertEquals("SW1A 1AA", validator.validateAndCanonicalise("sw1a1aa"));
        assertEquals("LS1 1UR", validator.validateAndCanonicalise("  ls1  1ur "));
        assertEquals("GIR 0AA", validator.validateAndCanonicalise("gir0aa"));
    }

    @Test
    void canonicalisesOutcodes() {
        assertEquals("LS1", validator.validateAndCanonicalise(" ls1 "));
        assertEquals("EC1A", validator.validateAndCanonicalise("ec1a"));
        assertEquals("GIR", validator.validateAndCanonicalise("gir"));
    }

    @Test
    void rejectsEmptyPartialAndMalformedValues() {
        assertThrows(InvalidPostcodeException.class, () -> validator.validateAndCanonicalise(null));
        assertThrows(InvalidPostcodeException.class, () -> validator.validateAndCanonicalise("   "));
        assertThrows(InvalidPostcodeException.class, () -> validator.validateAndCanonicalise("L"));
        assertThrows(InvalidPostcodeException.class, () -> validator.validateAndCanonicalise("1UR"));
        assertThrows(InvalidPostcodeException.class, () -> validator.validateAndCanonicalise("LS11U"));
        assertThrows(InvalidPostcodeException.class, () -> validator.validateAndCanonicalise("LS1/1UR"));
        assertThrows(InvalidPostcodeException.class, () -> validator.validateAndCanonicalise("LŁ1 1UR"));
        assertThrows(InvalidPostcodeException.class,
                () -> validator.validateAndCanonicalise("LS1 1UR EXCESSIVE"));
    }
}
