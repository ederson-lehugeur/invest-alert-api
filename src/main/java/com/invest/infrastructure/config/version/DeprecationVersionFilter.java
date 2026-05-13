package com.invest.infrastructure.config.version;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeprecationVersionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DeprecationVersionFilter.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^/api/(v\\d+)/");

    private final VersionProperties versionProperties;

    public DeprecationVersionFilter(VersionProperties versionProperties) {
        this.versionProperties = versionProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        extractVersion(request.getRequestURI()).ifPresent(version -> {
            VersionProperties.VersionConfig config =
                    versionProperties.getVersions().get(version);
            if (config != null && config.isDeprecated()) {
                response.setHeader("Deprecation", "true");
                addSunsetHeaderIfConfigured(response, config);
            }
        });

        filterChain.doFilter(request, response);
    }

    private void addSunsetHeaderIfConfigured(HttpServletResponse response,
                                             VersionProperties.VersionConfig config) {
        String sunsetDate = config.getSunsetDate();
        if (sunsetDate == null || sunsetDate.isBlank()) {
            return;
        }
        try {
            LocalDate date = LocalDate.parse(sunsetDate);
            String httpDate = date.atStartOfDay(ZoneOffset.UTC)
                    .format(DateTimeFormatter.RFC_1123_DATE_TIME);
            response.setHeader("Sunset", httpDate);
        } catch (DateTimeParseException e) {
            log.warn("Invalid sunset date format '{}' for deprecated version: {}",
                    sunsetDate, e.getMessage());
        }
    }

    private Optional<String> extractVersion(String uri) {
        Matcher matcher = VERSION_PATTERN.matcher(uri);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
