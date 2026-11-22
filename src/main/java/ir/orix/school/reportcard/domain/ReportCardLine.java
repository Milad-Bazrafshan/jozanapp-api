package ir.orix.school.reportcard.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "report_card_line", schema = "school")
public class ReportCardLine extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "report_card_id") private ReportCard reportCard;
    @Column(name = "line_type", nullable = false, length = 40) private String lineType;
    @Column(nullable = false, length = 200) private String title;
    @Column(precision = 8, scale = 2) private BigDecimal score;
    @Column(name = "max_score", precision = 8, scale = 2) private BigDecimal maxScore;
    @Column(length = 400) private String note;
    protected ReportCardLine() {}
    public ReportCardLine(Tenant tenant, ReportCard card, String lineType, String title, BigDecimal score, BigDecimal maxScore, String note) {
        this.tenant = tenant; this.reportCard = card; this.lineType = lineType; this.title = title; this.score = score; this.maxScore = maxScore; this.note = note;
    }
    public String getLineType() { return lineType; } public String getTitle() { return title; }
    public BigDecimal getScore() { return score; } public BigDecimal getMaxScore() { return maxScore; } public String getNote() { return note; }
}
