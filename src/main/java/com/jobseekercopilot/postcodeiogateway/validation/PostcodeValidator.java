package com.jobseekercopilot.postcodeiogateway.validation;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PostcodeValidator {
    private static final int MAXIMUM_INPUT_LENGTH = 16;
    private static final Pattern FULL_POSTCODE = Pattern.compile(
            "^(?:GIR 0AA|(?:[A-PR-UWYZ][0-9][0-9A-HJKSTUW]?|"
                    + "[A-PR-UWYZ][A-HK-Y][0-9][0-9ABEHMNPRV-Y]?) "
                    + "[0-9][ABD-HJLNP-UW-Z]{2})$");
    private static final Pattern OUTCODE = Pattern.compile(
            "^(?:GIR|[A-PR-UWYZ][0-9][0-9A-HJKSTUW]?|"
                    + "[A-PR-UWYZ][A-HK-Y][0-9][0-9ABEHMNPRV-Y]?)$");

    public String validateAndCanonicalise(String input) {
        if (input == null || input.isBlank() || input.length() > MAXIMUM_INPUT_LENGTH) {
            throw new InvalidPostcodeException();
        }

        String compact = input.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        String candidate = compact.length() > 4
                ? compact.substring(0, compact.length() - 3) + " " + compact.substring(compact.length() - 3)
                : compact;
        if (!FULL_POSTCODE.matcher(candidate).matches() && !OUTCODE.matcher(candidate).matches()) {
            throw new InvalidPostcodeException();
        }
        return candidate;
    }
}
