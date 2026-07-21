package com.jobseekercopilot.postcodeiogateway.validation;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PlaceSearchValidator {
    public static final int MINIMUM_QUERY_LENGTH = 2;
    public static final int MAXIMUM_QUERY_LENGTH = 80;
    public static final int MINIMUM_LIMIT = 1;
    public static final int MAXIMUM_LIMIT = 10;

    private static final Pattern SAFE_QUERY = Pattern.compile("[\\p{L}\\p{M} .'-]+");

    public String validateAndNormalise(String query, int limit) {
        if (query != null && query.codePoints().anyMatch(Character::isISOControl)) {
            throw new InvalidPlaceSearchException();
        }
        String clean = query == null ? "" : query.trim().replaceAll("\\s+", " ");
        if (clean.length() < MINIMUM_QUERY_LENGTH
                || clean.length() > MAXIMUM_QUERY_LENGTH
                || !SAFE_QUERY.matcher(clean).matches()
                || limit < MINIMUM_LIMIT
                || limit > MAXIMUM_LIMIT) {
            throw new InvalidPlaceSearchException();
        }
        return clean;
    }
}
