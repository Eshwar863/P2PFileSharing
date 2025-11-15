package peerlinkfilesharingsystem.Controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import peerlinkfilesharingsystem.Dto.FileUploadResponse;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;
import peerlinkfilesharingsystem.Service.FileUploadService.FileUploadService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@Slf4j
public class UploadController {

    private FileUploadService fileUploadService;
    private FileTransferRepo fileTransferRepo;


    public UploadController(FileUploadService fileUploadService,
                            FileTransferRepo fileTransferRepo) {
        this.fileUploadService = fileUploadService;
        this.fileTransferRepo = fileTransferRepo;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestPart(value = "file", required = true) MultipartFile file,
            @RequestHeader(value = "X-Network-Speed", defaultValue = "10.0") Double networkSpeedMbps,
            @RequestHeader(value = "X-Latency-Ms", defaultValue = "50") Integer latencyMs,
            @RequestHeader(value = "X-Device-Type", defaultValue = "DESKTOP") String deviceType,
            @RequestHeader(value = "Authorization", required = false) String authToken,
            HttpServletRequest request) {

        String correlationId = UUID.randomUUID().toString();
        String clientIp = request.getRemoteAddr();

        log.info("[{}] ========== UPLOAD START ==========", correlationId);
        log.info("[{}] File: {}, Size: {} MB", correlationId,
                file.getOriginalFilename(), file.getSize() / (1024.0 * 1024.0));
        log.info("[{}] Network: {} Mbps, Latency: {} ms, Device: {}",
                correlationId, networkSpeedMbps, latencyMs, deviceType);

        try {
            if (file.isEmpty() || file.getSize() == 0L) {
                log.warn("[{}] Empty file rejected", correlationId);
                return new ResponseEntity<>("File cannot be empty", HttpStatus.BAD_REQUEST);
            }

            if (file.getSize() > 10L * 1024 * 1024 * 1024) {
                log.warn("[{}] File too large: {} bytes", correlationId, file.getSize());
                return new ResponseEntity<>("File exceeds maximum size of 10 GB", HttpStatus.PAYLOAD_TOO_LARGE);
            }

            String filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) {
                log.warn("[{}] Invalid filename", correlationId);
                return new ResponseEntity<>("Invalid filename", HttpStatus.NOT_ACCEPTABLE);
            }


            if (clientIp == null || clientIp.isEmpty()) {
                log.warn("[{}] Client IP missing", correlationId);
                clientIp = "0.0.0.0"; // Fallback
            }


            Long userId = 15151L; //extractUserIdFromToken(authToken); // Implement JWT parsing


            FileUploadResponse response = fileUploadService.handleFile(
                    file, latencyMs, networkSpeedMbps, deviceType, clientIp);

            if (response.getSuccess()) {
                log.info("[{}] Upload SUCCESS - TransferID: {}", correlationId, response.getTransferId());
                return ResponseEntity.ok(response);
            } else {
                log.error("[{}] Upload FAILED - {}", correlationId, response.getMessage());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            log.error("[{}] Upload exception", correlationId, e);
            return new  ResponseEntity<>(
                    "Upload failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/history")
    public ResponseEntity<?> getTransferHistory(
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            log.info("Fetching transfer history - limit: {}", limit);
            var transfers = fileUploadService.getRecentTransfers(limit);
            return ResponseEntity.ok(transfers.stream().toList());

        } catch (Exception e) {
            log.error("Error getting history", e);
            return new ResponseEntity<>(Map.of(
                    "success", false,
                    "message", "History error: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
