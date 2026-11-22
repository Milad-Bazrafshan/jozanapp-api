package ir.orix.school.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.audit.application.OutboxService;
import ir.orix.school.classroom.domain.ClassInstructor;
import ir.orix.school.classroom.persistence.ClassInstructorRepository;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.enrollment.persistence.EnrollmentRepository;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.instructor.application.InstructorPortalService;
import ir.orix.school.notification.domain.Announcement;
import ir.orix.school.notification.domain.InAppNotification;
import ir.orix.school.notification.domain.SmsDelivery;
import ir.orix.school.notification.persistence.AnnouncementRepository;
import ir.orix.school.notification.persistence.InAppNotificationRepository;
import ir.orix.school.notification.persistence.SmsDeliveryRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.AccessGrantRepository;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.domain.InstructorProfile;
import ir.orix.school.people.domain.Person;
import ir.orix.school.people.domain.StudentGuardian;
import ir.orix.school.people.persistence.InstructorProfileRepository;
import ir.orix.school.people.persistence.StudentGuardianRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class NotificationService {
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final UserAccountRepository accounts;
    private final AnnouncementRepository announcements;
    private final InAppNotificationRepository inbox;
    private final SmsDeliveryRepository deliveries;
    private final EnrollmentRepository enrollments;
    private final ClassInstructorRepository assignments;
    private final StudentGuardianRepository relationships;
    private final InstructorProfileRepository instructors;
    private final AccessGrantRepository grants;
    private final List<SmsProvider> providers;
    private final InstructorPortalService instructorPortal;
    private final OutboxService outbox;
    private final AuditService audit;
    private final Clock clock;

    public NotificationService(TenantRepository tenants, TenantMembershipRepository memberships, UserAccountRepository accounts,
            AnnouncementRepository announcements, InAppNotificationRepository inbox, SmsDeliveryRepository deliveries,
            EnrollmentRepository enrollments, ClassInstructorRepository assignments, StudentGuardianRepository relationships,
            InstructorProfileRepository instructors, AccessGrantRepository grants, List<SmsProvider> providers,
            InstructorPortalService instructorPortal, OutboxService outbox, AuditService audit, Clock clock) {
        this.tenants = tenants; this.memberships = memberships; this.accounts = accounts; this.announcements = announcements;
        this.inbox = inbox; this.deliveries = deliveries; this.enrollments = enrollments; this.assignments = assignments;
        this.relationships = relationships; this.instructors = instructors; this.grants = grants; this.providers = providers;
        this.instructorPortal = instructorPortal; this.outbox = outbox; this.audit = audit; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementView> list(Long tenantId) {
        return announcements.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::view).toList();
    }

    @Transactional
    public AnnouncementView create(Long tenantId, Long actorId, Long membershipId, Command command, boolean instructorOnly) {
        if (instructorOnly) {
            if (command.audienceType() != Announcement.AudienceType.CLASS || command.audienceRef() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CLASS_AUDIENCE_REQUIRED", "Instructors can only announce to their class.");
            }
            instructorPortal.requireSection(tenantId, membershipId, command.audienceRef());
        }
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        UserAccount actor = accounts.findById(actorId).orElseThrow();
        Announcement announcement = announcements.save(new Announcement(tenant, command.title(), command.body(), command.audienceType(),
                command.audienceRef(), command.channel(), command.scheduledAt(), actor, command.audienceType() == Announcement.AudienceType.CLASS ? command.audienceRef() : null));
        List<Recipient> recipients = resolve(tenantId, announcement);
        Instant now = Instant.now(clock);
        for (Recipient recipient : recipients) {
            if (announcement.getChannel() == Announcement.Channel.IN_APP || announcement.getChannel() == Announcement.Channel.BOTH) {
                inbox.save(new InAppNotification(tenant, recipient.membershipId(), announcement, announcement.getTitle(), announcement.getBody()));
            }
            if ((announcement.getChannel() == Announcement.Channel.SMS || announcement.getChannel() == Announcement.Channel.BOTH) && recipient.phone() != null) {
                SmsDelivery delivery = deliveries.save(new SmsDelivery(tenant, announcement, recipient.membershipId(), recipient.phone()));
                sendSms(tenant, delivery, announcement.getTitle() + " — " + announcement.getBody(), now);
            }
        }
        announcement.markSent(now);
        outbox.enqueue(tenantId, "ANNOUNCEMENT_SENT", "ANNOUNCEMENT", announcement.getId(), "{\"count\":" + recipients.size() + "}");
        audit.record(tenantId, actorId, "ANNOUNCEMENT_SENT", "ANNOUNCEMENT", announcement.getId(), Map.of("recipients", recipients.size()));
        return view(announcement);
    }

    @Transactional(readOnly = true)
    public Preview preview(Long tenantId, Long membershipId, Command command, boolean instructorOnly) {
        if (instructorOnly && command.audienceType() == Announcement.AudienceType.CLASS && command.audienceRef() != null) {
            instructorPortal.requireSection(tenantId, membershipId, command.audienceRef());
        }
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        Announcement fake = new Announcement(tenant, command.title() == null ? "x" : command.title(), command.body() == null ? "x" : command.body(),
                command.audienceType(), command.audienceRef(), command.channel(), null, null, command.audienceRef());
        return new Preview(resolve(tenantId, fake).size());
    }

    @Transactional(readOnly = true)
    public InboxView inbox(Long tenantId, Long membershipId) {
        List<InboxItem> items = inbox.findAllByTenantIdAndMembershipIdOrderByCreatedAtDesc(tenantId, membershipId).stream()
                .map(item -> new InboxItem(item.getId(), item.getTitle(), item.getBody(), item.getReadAt(), item.getCreatedAt())).toList();
        return new InboxView(inbox.countByTenantIdAndMembershipIdAndReadAtIsNull(tenantId, membershipId), items);
    }

    @Transactional
    public InboxItem markRead(Long tenantId, Long membershipId, Long id) {
        InAppNotification item = inbox.findByIdAndTenantIdAndMembershipId(id, tenantId, membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification was not found."));
        item.markRead(Instant.now(clock));
        return new InboxItem(item.getId(), item.getTitle(), item.getBody(), item.getReadAt(), item.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<DeliveryView> deliveries(Long tenantId, Long announcementId) {
        announcements.findByIdAndTenantId(announcementId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ANNOUNCEMENT_NOT_FOUND", "Announcement was not found."));
        return deliveries.findAllByAnnouncementIdOrderByCreatedAtDesc(announcementId).stream()
                .map(item -> new DeliveryView(item.getId(), item.getPhone(), item.getStatus().name(), item.getProviderMessageId(), item.getErrorMessage(), item.getSentAt()))
                .toList();
    }

    public void notifyMembership(Long tenantId, Long membershipId, String title, String body) {
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        inbox.save(new InAppNotification(tenant, membershipId, null, title, body));
    }

    private void sendSms(Tenant tenant, SmsDelivery delivery, String text, Instant now) {
        String code = tenant.getSmsProvider() == null ? "DEMO" : tenant.getSmsProvider();
        SmsProvider provider = providers.stream().filter(item -> item.code().equalsIgnoreCase(code)).findFirst()
                .orElseGet(() -> providers.stream().filter(item -> "DEMO".equals(item.code())).findFirst().orElseThrow());
        SmsProvider.Result result = provider.send(delivery.getPhone(), text);
        if (result.ok()) delivery.sent(result.providerMessageId(), now);
        else delivery.failed(result.error());
    }

    private List<Recipient> resolve(Long tenantId, Announcement announcement) {
        List<TenantMembership> all = memberships.findAllByTenantIdOrderByPersonLastNameAscPersonFirstNameAsc(tenantId);
        Set<Long> ids = new LinkedHashSet<>();
        switch (announcement.getAudienceType()) {
            case ALL -> all.forEach(item -> ids.add(item.getId()));
            case BRANCH, ROLE -> all.stream()
                    .filter(item -> announcement.getAudienceRef() == null || grants.findAllByMembershipId(item.getId()).stream()
                            .anyMatch(grant -> announcement.getAudienceType() != Announcement.AudienceType.ROLE
                                    || grant.getRole().getId().equals(announcement.getAudienceRef())
                                    || grant.getRole().getCode().equalsIgnoreCase(String.valueOf(announcement.getAudienceRef()))))
                    .forEach(item -> ids.add(item.getId()));
            case CLASS -> {
                Long sectionId = announcement.getAudienceRef();
                for (Enrollment enrollment : enrollments.findAllByTenantIdAndClassSectionIdOrderByStudentPersonLastNameAsc(tenantId, sectionId)) {
                    memberships.findByTenantIdAndPersonId(tenantId, enrollment.getStudent().getPerson().getId()).ifPresent(m -> ids.add(m.getId()));
                    for (StudentGuardian link : relationships.findAllByTenantIdAndStudentId(tenantId, enrollment.getStudent().getId())) {
                        if (link.getEffectiveTo() == null && link.isReceivesNotifications()) {
                            memberships.findByTenantIdAndPersonId(tenantId, link.getGuardian().getPerson().getId()).ifPresent(m -> ids.add(m.getId()));
                        }
                    }
                }
                for (ClassInstructor assignment : assignments.findAllByTenantIdOrderByCreatedAtDesc(tenantId)) {
                    if (assignment.getClassSection().getId().equals(sectionId) && assignment.getStatus() == ClassInstructor.Status.ACTIVE) {
                        memberships.findByTenantIdAndPersonId(tenantId, assignment.getInstructor().getPerson().getId()).ifPresent(m -> ids.add(m.getId()));
                    }
                }
            }
            case MEMBERSHIP -> { if (announcement.getAudienceRef() != null) ids.add(announcement.getAudienceRef()); }
        }
        Map<Long, TenantMembership> byId = all.stream().collect(Collectors.toMap(TenantMembership::getId, item -> item, (a, b) -> a));
        List<Recipient> recipients = new ArrayList<>();
        for (Long id : ids) {
            TenantMembership membership = byId.get(id);
            if (membership == null) continue;
            Person person = membership.getPerson();
            recipients.add(new Recipient(membership.getId(), person.getMobile()));
        }
        return recipients;
    }

    private AnnouncementView view(Announcement value) {
        long delivered = deliveries.findAllByAnnouncementIdOrderByCreatedAtDesc(value.getId()).size();
        return new AnnouncementView(value.getId(), value.getTitle(), value.getBody(), value.getAudienceType().name(),
                value.getAudienceRef(), value.getChannel().name(), value.getStatus().name(), value.getScheduledAt(),
                value.getSentAt(), value.getClassSectionId(), delivered);
    }

    private record Recipient(Long membershipId, String phone) {}
    public record Command(String title, String body, Announcement.AudienceType audienceType, Long audienceRef,
            Announcement.Channel channel, Instant scheduledAt) {}
    public record AnnouncementView(Long id, String title, String body, String audienceType, Long audienceRef, String channel,
            String status, Instant scheduledAt, Instant sentAt, Long classSectionId, long deliveryCount) {}
    public record Preview(int recipientCount) {}
    public record InboxItem(Long id, String title, String body, Instant readAt, Instant createdAt) {}
    public record InboxView(long unread, List<InboxItem> items) {}
    public record DeliveryView(Long id, String phone, String status, String providerMessageId, String error, Instant sentAt) {}
}
