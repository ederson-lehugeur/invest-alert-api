package com.invest.infrastructure.config.version;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.api")
public class VersionProperties {

    private Map<String, VersionConfig> versions = new LinkedHashMap<>();

    public Map<String, VersionConfig> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, VersionConfig> versions) {
        this.versions = versions;
    }

    public static class VersionConfig {

        private boolean deprecated;
        private String sunsetDate;

        public boolean isDeprecated() {
            return deprecated;
        }

        public void setDeprecated(boolean deprecated) {
            this.deprecated = deprecated;
        }

        public String getSunsetDate() {
            return sunsetDate;
        }

        public void setSunsetDate(String sunsetDate) {
            this.sunsetDate = sunsetDate;
        }
    }
}
