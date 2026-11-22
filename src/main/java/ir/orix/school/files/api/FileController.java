package ir.orix.school.files.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ir.orix.school.files.application.FileStorageService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileStorageService files;
    public FileController(FileStorageService files) { this.files = files; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileStorageService.View upload(@RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "ATTACHMENT") String purpose) {
        return files.store(CurrentUser.tenantId(), CurrentUser.membershipId(), file, purpose);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        FileStorageService.Loaded loaded = files.load(CurrentUser.tenantId(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + loaded.meta().originalName() + "\"")
                .contentType(MediaType.parseMediaType(loaded.meta().contentType()))
                .body(loaded.bytes());
    }
}
