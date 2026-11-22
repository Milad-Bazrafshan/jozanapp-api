package ir.orix.school.organization.domain;

import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "tenant", schema = "school")
public class Tenant extends AuditedEntity {

    public enum Status {ACTIVE, SUSPENDED, CLOSED}

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(nullable = false, length = 80)
    private String timezone;

    @Column(name = "course_selection_enabled", nullable = false)
    private boolean courseSelectionEnabled;

    @Column(name = "virtual_class_enabled", nullable = false)
    private boolean virtualClassEnabled;

    @Column(name = "sms_provider", nullable = false, length = 40)
    private String smsProvider = "DEMO";

    @Column(name = "meeting_provider", nullable = false, length = 40)
    private String meetingProvider = "GENERIC_LINK";

    @Column(name = "meeting_base_url", length = 400)
    private String meetingBaseUrl;

    @Column(name = "meeting_secret", length = 400)
    private String meetingSecret;

    protected Tenant() {
    }

    public Tenant(String slug, String name, ZoneId timezone) {
        this.slug = slug.strip().toLowerCase();
        this.name = name.strip();
        this.timezone = timezone.getId();
        this.status = Status.ACTIVE;
        this.smsProvider = "DEMO";
        this.meetingProvider = "GENERIC_LINK";
    }

    public void updatePlatformSettings(boolean courseSelectionEnabled, boolean virtualClassEnabled,
                                       String smsProvider, String meetingProvider, String meetingBaseUrl, String meetingSecret) {
        this.courseSelectionEnabled = courseSelectionEnabled;
        this.virtualClassEnabled = virtualClassEnabled;
        if (smsProvider != null && !smsProvider.isBlank()) this.smsProvider = smsProvider.strip().toUpperCase();
        if (meetingProvider != null && !meetingProvider.isBlank())
            this.meetingProvider = meetingProvider.strip().toUpperCase();
        this.meetingBaseUrl = meetingBaseUrl == null || meetingBaseUrl.isBlank() ? null : meetingBaseUrl.strip();
        if (meetingSecret != null && !meetingSecret.isBlank()) this.meetingSecret = meetingSecret.strip();
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public String getTimezone() {
        return timezone;
    }

    public boolean isCourseSelectionEnabled() {
        return courseSelectionEnabled;
    }

    public boolean isVirtualClassEnabled() {
        return virtualClassEnabled;
    }

    public String getSmsProvider() {
        return smsProvider;
    }

    public String getMeetingProvider() {
        return meetingProvider;
    }

    public String getMeetingBaseUrl() {
        return meetingBaseUrl;
    }

    public String getMeetingSecret() {
        return meetingSecret;
    }
}
