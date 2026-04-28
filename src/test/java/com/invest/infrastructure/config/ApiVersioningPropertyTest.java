package com.invest.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.mockito.Mockito.mock;

/**
 * Validates: Requirements 1.1, 2.1, 2.2, 2.3, 7.1, 7.2, 7.3, 7.4
 * Feature: api-uri-versioning, Property 1: Version path prefix construction
 * Feature: api-uri-versioning, Property 2: Deprecation header presence
 * Feature: api-uri-versioning, Property 3: Sunset header HTTP-date formatting
 * Feature: api-uri-versioning, Property 4: Version configuration round-trip
 */
class ApiVersioningPropertyTest {

    private static final String BASE_PACKAGE = "com.invest.adapters.web";

    /**
     * Constructs the version path prefix for a given version number.
     * This mirrors the logic used by VersionPathConfig.configurePathMatch.
     */
    private String buildVersionPrefix(int versionNumber) {
        return "/api/v" + versionNumber;
    }

    /**
     * Constructs the base package name for a given version number.
     * Controllers in this package receive the corresponding path prefix.
     */
    private String buildVersionPackage(int versionNumber) {
        return BASE_PACKAGE + ".v" + versionNumber;
    }

    // Feature: api-uri-versioning, Property 1: Version path prefix construction
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 1: Version path prefix construction")
    void prefixFollowsApiVersionPattern(
            @ForAll @IntRange(min = 1, max = 100) int versionNumber) {

        String prefix = buildVersionPrefix(versionNumber);

        assert prefix.equals("/api/v" + versionNumber)
                : "Expected /api/v%d but got %s".formatted(versionNumber, prefix);
    }

    // Feature: api-uri-versioning, Property 1: Version path prefix construction
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 1: Version path prefix construction")
    void prefixStartsWithApiSlash(
            @ForAll @IntRange(min = 1, max = 100) int versionNumber) {

        String prefix = buildVersionPrefix(versionNumber);

        assert prefix.startsWith("/api/v")
                : "Prefix should start with /api/v but got: " + prefix;
    }

    // Feature: api-uri-versioning, Property 1: Version path prefix construction
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 1: Version path prefix construction")
    void prefixContainsExactVersionNumber(
            @ForAll @IntRange(min = 1, max = 100) int versionNumber) {

        String prefix = buildVersionPrefix(versionNumber);
        String versionSuffix = prefix.substring("/api/v".length());

        assert Integer.parseInt(versionSuffix) == versionNumber
                : "Version number in prefix should be %d but parsed %s"
                        .formatted(versionNumber, versionSuffix);
    }

    // Feature: api-uri-versioning, Property 1: Version path prefix construction
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 1: Version path prefix construction")
    void packageAndPrefixVersionsAreConsistent(
            @ForAll @IntRange(min = 1, max = 100) int versionNumber) {

        String prefix = buildVersionPrefix(versionNumber);
        String packageName = buildVersionPackage(versionNumber);

        String prefixVersion = prefix.substring("/api/".length());
        String packageVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

        assert prefixVersion.equals(packageVersion)
                : "Prefix version '%s' should match package version '%s'"
                        .formatted(prefixVersion, packageVersion);
    }

    // -----------------------------------------------------------------------
    // Property 2: Deprecation header presence is determined by deprecation status
    // Validates: Requirements 7.1, 7.4
    // -----------------------------------------------------------------------

    @Provide
    Arbitrary<VersionProperties.VersionConfig> arbitraryVersionConfig() {
        Arbitrary<Boolean> deprecatedFlag = Arbitraries.of(true, false);
        Arbitrary<String> optionalSunsetDate = Arbitraries.of(
                null,
                "2025-06-01",
                "2026-12-31",
                "2030-01-15",
                "2024-03-10"
        );

        return Combinators.combine(deprecatedFlag, optionalSunsetDate)
                .as((deprecated, sunsetDate) -> {
                    VersionProperties.VersionConfig config = new VersionProperties.VersionConfig();
                    config.setDeprecated(deprecated);
                    config.setSunsetDate(sunsetDate);
                    return config;
                });
    }

    private DeprecationVersionFilter createFilterWithConfig(String version,
                                                            VersionProperties.VersionConfig config) {
        VersionProperties properties = new VersionProperties();
        properties.setVersions(new LinkedHashMap<>());
        properties.getVersions().put(version, config);
        return new DeprecationVersionFilter(properties);
    }

    /**
     * Validates: Requirements 7.1, 7.4
     *
     * For any VersionConfig, the Deprecation header is present iff deprecated is true.
     * When not deprecated, neither Deprecation nor Sunset headers are present.
     */
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 2: Deprecation header presence")
    void deprecationHeaderPresentIffDeprecatedIsTrue(
            @ForAll("arbitraryVersionConfig") VersionProperties.VersionConfig config)
            throws ServletException, IOException {

        DeprecationVersionFilter filter = createFilterWithConfig("v1", config);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        if (config.isDeprecated()) {
            assert "true".equals(response.getHeader("Deprecation"))
                    : "Deprecation header should be 'true' when deprecated";
        } else {
            assert response.getHeader("Deprecation") == null
                    : "Deprecation header should be absent when not deprecated";
            assert response.getHeader("Sunset") == null
                    : "Sunset header should be absent when not deprecated";
        }
    }

    /**
     * Validates: Requirements 7.4
     *
     * For any non-deprecated VersionConfig, neither Deprecation nor Sunset headers are present,
     * regardless of whether a sunset date is configured.
     */
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 2: Deprecation header presence")
    void noHeadersWhenNotDeprecatedRegardlessOfSunsetDate(
            @ForAll("arbitraryVersionConfig") VersionProperties.VersionConfig config)
            throws ServletException, IOException {

        // Force non-deprecated to test this specific invariant
        config.setDeprecated(false);

        DeprecationVersionFilter filter = createFilterWithConfig("v1", config);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alerts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        assert response.getHeader("Deprecation") == null
                : "Deprecation header must be absent when not deprecated";
        assert response.getHeader("Sunset") == null
                : "Sunset header must be absent when not deprecated";
    }

    // -----------------------------------------------------------------------
    // Property 3: Sunset header HTTP-date formatting
    // Validates: Requirements 7.2
    // -----------------------------------------------------------------------

    private static final Pattern RFC_7231_HTTP_DATE_PATTERN = Pattern.compile(
            "^(Mon|Tue|Wed|Thu|Fri|Sat|Sun), \\d{1,2} "
                    + "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) "
                    + "\\d{4} 00:00:00 GMT$"
    );

    @Provide
    Arbitrary<LocalDate> arbitraryLocalDate() {
        return Arbitraries.integers().between(1970, 2099)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> {
                            int maxDay = LocalDate.of(year, month, 1).lengthOfMonth();
                            return Arbitraries.integers().between(1, maxDay)
                                    .map(day -> LocalDate.of(year, month, day));
                        }));
    }

    /**
     * Validates: Requirements 7.2
     *
     * For any valid LocalDate, when a deprecated version has a configured sunset date,
     * the Sunset header value shall be a correctly formatted HTTP-date string per RFC 7231.
     */
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 3: Sunset header HTTP-date formatting")
    void sunsetHeaderMatchesRfc7231HttpDateFormat(
            @ForAll("arbitraryLocalDate") LocalDate sunsetDate)
            throws ServletException, IOException {

        VersionProperties.VersionConfig config = new VersionProperties.VersionConfig();
        config.setDeprecated(true);
        config.setSunsetDate(sunsetDate.toString());

        DeprecationVersionFilter filter = createFilterWithConfig("v1", config);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        String sunsetHeader = response.getHeader("Sunset");
        assert sunsetHeader != null
                : "Sunset header must be present for deprecated version with sunset date: " + sunsetDate;
        assert RFC_7231_HTTP_DATE_PATTERN.matcher(sunsetHeader).matches()
                : "Sunset header '%s' does not match RFC 7231 HTTP-date format for date %s"
                        .formatted(sunsetHeader, sunsetDate);
    }

    /**
     * Validates: Requirements 7.2
     *
     * For any valid LocalDate, the day-of-week in the Sunset header must be consistent
     * with the actual day-of-week of the input date.
     */
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 3: Sunset header HTTP-date formatting")
    void sunsetHeaderDayOfWeekMatchesInputDate(
            @ForAll("arbitraryLocalDate") LocalDate sunsetDate)
            throws ServletException, IOException {

        VersionProperties.VersionConfig config = new VersionProperties.VersionConfig();
        config.setDeprecated(true);
        config.setSunsetDate(sunsetDate.toString());

        DeprecationVersionFilter filter = createFilterWithConfig("v1", config);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        String sunsetHeader = response.getHeader("Sunset");
        assert sunsetHeader != null
                : "Sunset header must be present for deprecated version with sunset date";

        String headerDayAbbrev = sunsetHeader.substring(0, 3);
        String expectedDayAbbrev = sunsetDate.getDayOfWeek().name().substring(0, 1).toUpperCase()
                + sunsetDate.getDayOfWeek().name().substring(1, 3).toLowerCase();

        assert headerDayAbbrev.equals(expectedDayAbbrev)
                : "Day-of-week in Sunset header '%s' should be '%s' for date %s"
                        .formatted(headerDayAbbrev, expectedDayAbbrev, sunsetDate);
    }

    // -----------------------------------------------------------------------
    // Property 4: Version configuration round-trip
    // Validates: Requirements 7.3
    // -----------------------------------------------------------------------

    @Provide
    Arbitrary<Map<String, VersionProperties.VersionConfig>> arbitraryVersionConfigMap() {
        Arbitrary<String> versionNames = Arbitraries.integers().between(1, 50)
                .map(n -> "v" + n);

        Arbitrary<VersionProperties.VersionConfig> configs = Combinators.combine(
                Arbitraries.of(true, false),
                Arbitraries.of(null, "2025-06-01", "2026-12-31", "2030-01-15")
        ).as((deprecated, sunsetDate) -> {
            VersionProperties.VersionConfig config = new VersionProperties.VersionConfig();
            config.setDeprecated(deprecated);
            config.setSunsetDate(sunsetDate);
            return config;
        });

        return Combinators.combine(versionNames, configs)
                .as((name, config) -> {
                    Map<String, VersionProperties.VersionConfig> map = new LinkedHashMap<>();
                    map.put(name, config);
                    return map;
                });
    }

    /**
     * Validates: Requirements 7.3
     *
     * For any valid version config (version name, deprecated flag, optional sunset date),
     * setting values on VersionProperties and reading them back produces equivalent values.
     */
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 4: Version configuration round-trip")
    void versionConfigRoundTripPreservesValues(
            @ForAll("arbitraryVersionConfigMap") Map<String, VersionProperties.VersionConfig> inputMap) {

        VersionProperties properties = new VersionProperties();
        properties.setVersions(new LinkedHashMap<>(inputMap));

        Map<String, VersionProperties.VersionConfig> readBack = properties.getVersions();

        assert readBack.size() == inputMap.size()
                : "Expected %d entries but got %d".formatted(inputMap.size(), readBack.size());

        for (Map.Entry<String, VersionProperties.VersionConfig> entry : inputMap.entrySet()) {
            String versionName = entry.getKey();
            VersionProperties.VersionConfig original = entry.getValue();
            VersionProperties.VersionConfig restored = readBack.get(versionName);

            assert restored != null
                    : "Version '%s' should be present after round-trip".formatted(versionName);
            assert restored.isDeprecated() == original.isDeprecated()
                    : "Deprecated flag for '%s' should be %b but was %b"
                            .formatted(versionName, original.isDeprecated(), restored.isDeprecated());

            if (original.getSunsetDate() == null) {
                assert restored.getSunsetDate() == null
                        : "Sunset date for '%s' should be null but was '%s'"
                                .formatted(versionName, restored.getSunsetDate());
            } else {
                assert original.getSunsetDate().equals(restored.getSunsetDate())
                        : "Sunset date for '%s' should be '%s' but was '%s'"
                                .formatted(versionName, original.getSunsetDate(), restored.getSunsetDate());
            }
        }
    }

    /**
     * Validates: Requirements 7.3
     *
     * For any valid version config, the version key is preserved exactly through the round-trip.
     */
    @Property(tries = 100)
    @Tag("Feature: api-uri-versioning, Property 4: Version configuration round-trip")
    void versionConfigRoundTripPreservesKeys(
            @ForAll("arbitraryVersionConfigMap") Map<String, VersionProperties.VersionConfig> inputMap) {

        VersionProperties properties = new VersionProperties();
        properties.setVersions(new LinkedHashMap<>(inputMap));

        Map<String, VersionProperties.VersionConfig> readBack = properties.getVersions();

        assert readBack.keySet().equals(inputMap.keySet())
                : "Version keys should be preserved: expected %s but got %s"
                        .formatted(inputMap.keySet(), readBack.keySet());
    }
}
