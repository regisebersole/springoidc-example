package com.example.springbootrestapi.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Instant;

/**
 * Custom health indicator for overall application health
 */
@Component
public class ApplicationHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            // Consider application unhealthy if memory usage is above 90%
            if (memoryUsagePercent > 90) {
                return Health.down()
                        .withDetail("application", "High memory usage")
                        .withDetail("memory-usage-percent", String.format("%.2f", memoryUsagePercent))
                        .withDetail("used-memory-mb", usedMemory / (1024 * 1024))
                        .withDetail("max-memory-mb", maxMemory / (1024 * 1024))
                        .withDetail("checked-at", Instant.now().toString())
                        .build();
            }
            
            return Health.up()
                    .withDetail("application", "Running normally")
                    .withDetail("memory-usage-percent", String.format("%.2f", memoryUsagePercent))
                    .withDetail("used-memory-mb", usedMemory / (1024 * 1024))
                    .withDetail("max-memory-mb", maxMemory / (1024 * 1024))
                    .withDetail("uptime-ms", ManagementFactory.getRuntimeMXBean().getUptime())
                    .withDetail("checked-at", Instant.now().toString())
                    .build();
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("application", "Error checking health")
                    .withDetail("error", e.getMessage())
                    .withDetail("checked-at", Instant.now().toString())
                    .build();
        }
    }
}