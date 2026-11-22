package ir.orix.school.files.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.files.domain.StoredFile;
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByIdAndTenantId(Long id, Long tenantId);
}
