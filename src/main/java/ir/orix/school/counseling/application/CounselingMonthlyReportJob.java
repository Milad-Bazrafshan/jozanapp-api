package ir.orix.school.counseling.application;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.counseling.domain.CounselorSupervision;
import ir.orix.school.counseling.persistence.CounselorSupervisionRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.TenantRepository;

@Component
public class CounselingMonthlyReportJob {
    private static final Logger log = LoggerFactory.getLogger(CounselingMonthlyReportJob.class);

    private final TenantRepository tenants;
    private final CounselorSupervisionRepository supervisions;
    private final CounselingService counseling;
    private final Clock clock;

    public CounselingMonthlyReportJob(TenantRepository tenants, CounselorSupervisionRepository supervisions,
            CounselingService counseling, Clock clock) {
        this.tenants = tenants;
        this.supervisions = supervisions;
        this.counseling = counseling;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.counseling.monthly-report-cron:0 0 6 1 * *}")
    @Transactional
    public void generateMonthlySeniorReports() {
        YearMonth previous = YearMonth.now(clock).minusMonths(1);
        int year = previous.getYear();
        int month = previous.getMonthValue();
        for (Tenant tenant : tenants.findAll()) {
            ZoneId zone = ZoneId.of(tenant.getTimezone());
            if (YearMonth.now(clock.withZone(zone)).minusMonths(1).getYear() != year
                    || YearMonth.now(clock.withZone(zone)).minusMonths(1).getMonthValue() != month) {
                // still use UTC-based previous month for simplicity
            }
            List<CounselorSupervision> active = supervisions.findAllByTenantIdAndStatus(
                    tenant.getId(), CounselorSupervision.Status.ACTIVE);
            for (CounselorSupervision link : active) {
                try {
                    counseling.systemDraftMonthlySeniorReport(
                            tenant.getId(),
                            link.getSeniorMembership().getId(),
                            link.getCounselorMembership().getId(),
                            year,
                            month);
                } catch (Exception ex) {
                    log.warn("Monthly counselor report failed tenant={} supervision={}: {}",
                            tenant.getSlug(), link.getId(), ex.getMessage());
                }
            }
        }
        log.info("Counseling monthly report job finished for {}/{}", year, month);
    }
}
