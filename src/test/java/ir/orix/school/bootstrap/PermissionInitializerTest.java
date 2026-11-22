package ir.orix.school.bootstrap;

import static org.mockito.ArgumentMatchers.any; import static org.mockito.Mockito.*;
import java.util.*; import org.junit.jupiter.api.Test;
import ir.orix.school.organization.domain.*; import ir.orix.school.organization.persistence.*;

class PermissionInitializerTest {
 @Test void createsMissingPermissionsAndAssignsCompleteSetToSystemAdmin() throws Exception {
  PermissionRepository permissions=mock(PermissionRepository.class);AccessRoleRepository roles=mock(AccessRoleRepository.class);AccessRole admin=mock(AccessRole.class);
  List<Permission> complete=List.of(new Permission("ORGANIZATION_READ","organization","read"),new Permission("SCHEDULE_WRITE","scheduling","write"));
  when(permissions.existsById(any())).thenReturn(false);when(permissions.findAll()).thenReturn(complete);when(admin.getCode()).thenReturn("ADMIN");when(roles.findAllBySystemRoleTrue()).thenReturn(List.of(admin));
  TenantRepository tenants=mock(TenantRepository.class);when(tenants.findAll()).thenReturn(List.of());
  new PermissionInitializer(permissions,roles,tenants).run(null);
  verify(permissions,times(52)).save(any(Permission.class));verify(admin).replacePermissions(new LinkedHashSet<>(complete));verify(roles).save(admin);
 }
}
