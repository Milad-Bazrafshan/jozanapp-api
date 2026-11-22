package ir.orix.school.organization.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.organization.domain.Permission;

public interface PermissionRepository extends JpaRepository<Permission, String> {
}
