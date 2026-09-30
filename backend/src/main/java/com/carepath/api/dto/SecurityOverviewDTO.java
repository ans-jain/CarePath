package com.carepath.api.dto;

import java.util.List;
import java.util.Map;

public class SecurityOverviewDTO {

    private boolean rateLimitingEnabled;
    private int authRateLimitPerMinute;
    private int mlRateLimitPerMinute;
    private List<String> corsAllowedOrigins;
    private List<String> securityHeadersEnabled;
    private Map<String, Long> userCountByRole;
    private long totalAuditLogsCount;

    public SecurityOverviewDTO() {
    }

    public SecurityOverviewDTO(boolean rateLimitingEnabled, int authRateLimitPerMinute,
                               int mlRateLimitPerMinute, List<String> corsAllowedOrigins,
                               List<String> securityHeadersEnabled, Map<String, Long> userCountByRole,
                               long totalAuditLogsCount) {
        this.rateLimitingEnabled = rateLimitingEnabled;
        this.authRateLimitPerMinute = authRateLimitPerMinute;
        this.mlRateLimitPerMinute = mlRateLimitPerMinute;
        this.corsAllowedOrigins = corsAllowedOrigins;
        this.securityHeadersEnabled = securityHeadersEnabled;
        this.userCountByRole = userCountByRole;
        this.totalAuditLogsCount = totalAuditLogsCount;
    }

    public boolean isRateLimitingEnabled() {
        return rateLimitingEnabled;
    }

    public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
        this.rateLimitingEnabled = rateLimitingEnabled;
    }

    public int getAuthRateLimitPerMinute() {
        return authRateLimitPerMinute;
    }

    public void setAuthRateLimitPerMinute(int authRateLimitPerMinute) {
        this.authRateLimitPerMinute = authRateLimitPerMinute;
    }

    public int getMlRateLimitPerMinute() {
        return mlRateLimitPerMinute;
    }

    public void setMlRateLimitPerMinute(int mlRateLimitPerMinute) {
        this.mlRateLimitPerMinute = mlRateLimitPerMinute;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public List<String> getSecurityHeadersEnabled() {
        return securityHeadersEnabled;
    }

    public void setSecurityHeadersEnabled(List<String> securityHeadersEnabled) {
        this.securityHeadersEnabled = securityHeadersEnabled;
    }

    public Map<String, Long> getUserCountByRole() {
        return userCountByRole;
    }

    public void setUserCountByRole(Map<String, Long> userCountByRole) {
        this.userCountByRole = userCountByRole;
    }

    public long getTotalAuditLogsCount() {
        return totalAuditLogsCount;
    }

    public void setTotalAuditLogsCount(long totalAuditLogsCount) {
        this.totalAuditLogsCount = totalAuditLogsCount;
    }
}
