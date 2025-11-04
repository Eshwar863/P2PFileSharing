package peerlinkfilesharingsystem.Controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import peerlinkfilesharingsystem.Dto.FileUploadResponse;
import peerlinkfilesharingsystem.Service.FileUploadService.FileUploadService;

import java.util.UUID;

@RestController
@RequestMapping("/files")
@Slf4j
public class UploadController {

    private FileUploadService fileUploadService;
    public UploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse uploadFile(
            @RequestPart(value = "file", required = true) MultipartFile file,
            @RequestHeader(value = "X-Network-Speed", defaultValue = "10.0") Double networkSpeedMbps,
            @RequestHeader(value = "X-Latency-Ms", defaultValue = "50") Integer latencyMs,
            @RequestHeader(value = "X-Device-Type", defaultValue = "DESKTOP") String deviceType,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        String correlationId = UUID.randomUUID().toString();

        log.info("[{}] Upload started - File: {}, Size: {}MB, Speed: {}Mbps, Latency: {}ms, IP: {}",
                correlationId, file.getOriginalFilename(),
                file.getSize() / 1024 / 1024, networkSpeedMbps, latencyMs, clientIp);

        try {
            if (file.isEmpty() || file.getSize() == 0L || clientIp.isEmpty()) {
                return ResponseEntity.status(404).body(
                        FileUploadResponse.builder()
                                .success(false)
                                .message("Upload failed: File Cant be Empty,Client IP Can't be Empty")
                                .build()
                ).getBody();
            }

            System.out.println(file.toString());
            if (file.getSize() > 10 * 1024 * 1024 * 1024L) {  // 5GB
                return ResponseEntity.status(404).body(
                        FileUploadResponse.builder()
                                .success(false)
                                .message("Upload failed: File Size Exceeded")
                                .build()
                ).getBody();
            }

            return fileUploadService.handleFile(file, latencyMs, networkSpeedMbps, deviceType,clientIp);

        }catch (Exception e){
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    FileUploadResponse.builder()
                            .success(false)
                            .message("Upload failed: " + e.getMessage())
                            .build()
            ).getBody();
        }

    }



//    /**
//     * GET TRANSFER STATUS
//     * GET /api/files/transfer/{transferId}/status
//     * Check status of uploaded file
//     */
//    @GetMapping("/transfer/{transferId}/status")
//    public ResponseEntity<?> getTransferStatus(@PathVariable String transferId) {
//        try {
//            FileTransferEntity transfer = fileService.getTransferById(transferId);
//            if (transfer == null) {
//                return ResponseEntity.notFound().build();
//            }
//
//            return ResponseEntity.ok(Map.of(
//                    "transferId", transfer.getTransferId(),
//                    "fileName", transfer.getFileName(),
//                    "status", transfer.getSuccess() ? "SUCCESS" : "FAILED",
//                    "fileSizeBytes", transfer.getFileSizeBytes(),
//                    "compressedSizeBytes", transfer.getBytesCompressed(),
//                    "compressionRatio", calculateCompressionRatio(transfer),
//                    "uploadedAt", transfer.getCreatedAt(),
//                    "completedAt", transfer.getCompletedAt()
//            ));
//        } catch (Exception e) {
//            log.error("Error getting transfer status - TransferId: {}", transferId, e);
//            return ResponseEntity.status(500)
//                    .body(buildErrorResponse("Error: " + e.getMessage(), "STATUS_ERROR"));
//        }
//    }
//
//    /**
//     * GET ANALYTICS
//     * GET /api/files/analytics
//     * System-wide statistics and performance metrics
//     */
//    @GetMapping("/analytics")
//    public ResponseEntity<?> getAnalytics() {
//        try {
//            AnalyticsDto analytics = fileService.generateAnalytics();
//            return ResponseEntity.ok(analytics);
//        } catch (Exception e) {
//            log.error("Error generating analytics", e);
//            return ResponseEntity.status(500)
//                    .body(buildErrorResponse("Analytics error: " + e.getMessage(), "ANALYTICS_ERROR"));
//        }
//    }
//
//    /**
//     * GET TRANSFER HISTORY
//     * GET /api/files/history
//     * Recent transfers and performance
//     */
//    @GetMapping("/history")
//    public ResponseEntity<?> getTransferHistory(
//            @RequestParam(defaultValue = "10") Integer limit) {
//        try {
//            return ResponseEntity.ok(fileService.getRecentTransfers(limit));
//        } catch (Exception e) {
//            log.error("Error getting history", e);
//            return ResponseEntity.status(500)
//                    .body(buildErrorResponse("History error: " + e.getMessage(), "HISTORY_ERROR"));
//        }
//    }
//
//    /**
//     * HEALTH CHECK
//     * GET /api/files/health
//     */
//    @GetMapping("/health")
//    public ResponseEntity<?> healthCheck() {
//        return ResponseEntity.ok(Map.of(
//                "status", "UP",
//                "timestamp", System.currentTimeMillis()
//        ));
//    }
//
//    // ============ HELPER METHODS ============
//
//    private String getClientIp(HttpServletRequest request) {
//        String xForwardedFor = request.getHeader("X-Forwarded-For");
//        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
//            return xForwardedFor.split(",").trim();
//        }
//        String xRealIp = request.getHeader("X-Real-IP");
//        if (xRealIp != null && !xRealIp.isEmpty()) {
//            return xRealIp;
//        }
//        return request.getRemoteAddr();
//    }
//
//    private String extractFileType(String fileName) {
//        if (fileName == null || !fileName.contains(".")) {
//            return "unknown";
//        }
//        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
//    }
//
//    private String calculateCompressionRatio(FileTransferEntity transfer) {
//        if (transfer.getFileSizeBytes() == 0) {
//            return "0%";
//        }
//        double ratio = (1.0 - (double) transfer.getBytesCompressed() / transfer.getFileSizeBytes()) * 100;
//        return String.format("%.1f%%", ratio);
//    }
//
//    private Map<String, Object> buildErrorResponse(String message, String errorCode) {
//        Map<String, Object> error = new HashMap<>();
//        error.put("success", false);
//        error.put("message", message);
//        error.put("errorCode", errorCode);
//        error.put("timestamp", System.currentTimeMillis());
//        return error;
//    }
}

