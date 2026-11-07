package com.ids.ms_catalog.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Límites máximos del SLA configurables por entorno (app.sla.*).
 */
@ConfigurationProperties(prefix = "app.sla")
public record SlaProperties(int maxResponseHours, int maxResolutionHours) {
}
