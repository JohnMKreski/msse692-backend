package com.arkvalleyevents.msse692_backend.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Additional logging configuration: verbose request logging in dev/local profiles.
 */
@Configuration
@ConditionalOnClass(CommonsRequestLoggingFilter.class)
@Profile({"dev","local"}) // only enable verbose request logging for non-prod profiles
public class LoggingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LoggingConfiguration.class);

    @Bean
    public CommonsRequestLoggingFilter detailedRequestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setIncludeHeaders(false); // avoid logging auth headers
        filter.setMaxPayloadLength(1024); // limit size
        log.info("Initialized DEV CommonsRequestLoggingFilter (bean: detailedRequestLoggingFilter) for detailed request logging.");
        return filter;
    }
}
