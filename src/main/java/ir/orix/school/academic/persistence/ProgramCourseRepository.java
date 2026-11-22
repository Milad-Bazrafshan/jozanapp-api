package ir.orix.school.academic.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.academic.domain.ProgramCourse;
public interface ProgramCourseRepository extends JpaRepository<ProgramCourse, Long>{
 @EntityGraph(attributePaths={"program","course"}) List<ProgramCourse> findAllByTenantIdOrderByProgramNameAscSequenceNumberAsc(Long tenantId);
 boolean existsByProgramIdAndCourseId(Long programId,Long courseId);
}
