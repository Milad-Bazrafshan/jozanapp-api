package ir.orix.school.files.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ir.orix.school.files.domain.StoredFile;
import ir.orix.school.files.persistence.StoredFileRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class FileStorageService {
    private final StoredFileRepository files;
    private final TenantRepository tenants;
    private final Path root;

    public FileStorageService(StoredFileRepository files, TenantRepository tenants,
            @Value("${app.storage.root:./data/files}") String root) {
        this.files = files;
        this.tenants = tenants;
        this.root = Path.of(root);
    }

    @Transactional
    public View store(Long tenantId, Long membershipId, MultipartFile upload, String purpose) {
        if (upload == null || upload.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED", "A file is required.");
        if (upload.getSize() > 8_000_000) throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "File exceeds 8MB.");
        Tenant tenant = tenants.findById(tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Tenant was not found."));
        String key = tenantId + "/" + UUID.randomUUID();
        try {
            Path target = root.resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(upload.getInputStream(), target);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORE_FAILED", "File could not be stored.");
        }
        String name = upload.getOriginalFilename() == null || upload.getOriginalFilename().isBlank() ? "file" : upload.getOriginalFilename();
        String type = upload.getContentType() == null ? "application/octet-stream" : upload.getContentType();
        StoredFile stored = files.save(new StoredFile(tenant, name, type, upload.getSize(), key, membershipId,
                purpose == null || purpose.isBlank() ? "ATTACHMENT" : purpose.strip().toUpperCase()));
        return view(stored);
    }

    @Transactional(readOnly = true)
    public Loaded load(Long tenantId, Long id) {
        StoredFile stored = files.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File was not found."));
        try {
            byte[] bytes = Files.readAllBytes(root.resolve(stored.getStorageKey()));
            return new Loaded(view(stored), bytes);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_MISSING", "Stored binary was not found.");
        }
    }

    private static View view(StoredFile file) {
        return new View(file.getId(), file.getOriginalName(), file.getContentType(), file.getSizeBytes(), file.getPurpose());
    }

    public record View(Long id, String originalName, String contentType, long sizeBytes, String purpose) {}
    public record Loaded(View meta, byte[] bytes) {}
}
