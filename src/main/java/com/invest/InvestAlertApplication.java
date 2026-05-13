package com.invest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.invest.infrastructure.config.version.VersionProperties;

@SpringBootApplication
@EnableConfigurationProperties(VersionProperties.class)
public class InvestAlertApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestAlertApplication.class, args);
    }
}
