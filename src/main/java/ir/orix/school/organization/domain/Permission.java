package ir.orix.school.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permission", schema = "school")
public class Permission {

    @Id
    @Column(length = 100)
    private String code;

    @Column(nullable = false, length = 60)
    private String module;

    @Column(nullable = false, length = 250)
    private String description;

    protected Permission() {
    }

    public Permission(String code, String module, String description) {
        this.code = code;
        this.module = module;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getModule() { return module; }
    public String getDescription() { return description; }
}
