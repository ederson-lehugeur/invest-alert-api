package com.invest.infrastructure.config.version;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeprecationVersionFilterTest {

    private VersionProperties versionProperties;
    private DeprecationVersionFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        versionProperties = new VersionProperties();
        versionProperties.setVersions(new LinkedHashMap<>());
        filter = new DeprecationVersionFilter(versionProperties);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldReturnDeprecationAndSunsetHeadersForDeprecatedVersionWithSunsetDate()
            throws ServletException, IOException {
        String sunsetDate = "2026-06-01";
        configureVersion("v1", true, sunsetDate);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("true", response.getHeader("Deprecation"));
        String expectedSunset = LocalDate.parse(sunsetDate)
                .atStartOfDay(ZoneOffset.UTC)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);
        assertEquals(expectedSunset, response.getHeader("Sunset"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReturnOnlyDeprecationHeaderForDeprecatedVersionWithoutSunsetDate()
            throws ServletException, IOException {
        configureVersion("v1", true, null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alerts");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("true", response.getHeader("Deprecation"));
        assertNull(response.getHeader("Sunset"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotReturnDeprecationHeadersForNonDeprecatedVersion()
            throws ServletException, IOException {
        configureVersion("v1", false, null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(response.getHeader("Deprecation"));
        assertNull(response.getHeader("Sunset"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotReturnDeprecationHeadersForUnknownVersion()
            throws ServletException, IOException {
        configureVersion("v1", false, null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v99/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(response.getHeader("Deprecation"));
        assertNull(response.getHeader("Sunset"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldPassThroughWithoutHeadersForNonVersionedUri()
            throws ServletException, IOException {
        configureVersion("v1", true, "2026-06-01");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(response.getHeader("Deprecation"));
        assertNull(response.getHeader("Sunset"));
        verify(filterChain).doFilter(request, response);
    }

    private void configureVersion(String version, boolean deprecated, String sunsetDate) {
        VersionProperties.VersionConfig config = new VersionProperties.VersionConfig();
        config.setDeprecated(deprecated);
        config.setSunsetDate(sunsetDate);
        versionProperties.getVersions().put(version, config);
    }
}
