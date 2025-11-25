package peerlinkfilesharingsystem.Controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import peerlinkfilesharingsystem.Model.ChunkedDownloadResource;
import peerlinkfilesharingsystem.Model.FileDownload;
import peerlinkfilesharingsystem.Model.FileShare;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Repo.FileDownloadRepo;
import peerlinkfilesharingsystem.Repo.FileShareRepo;
import peerlinkfilesharingsystem.Service.FileDownloadService.FileDownloadService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@Slf4j
public class DownloadController {

    private final FileDownloadRepo fileDownloadRepo;
    private final FileShareRepo fileShareRepo;
    private FileDownloadService fileDownloadService;

    public DownloadController(FileDownloadService fileDownloadService, FileDownloadRepo fileDownloadRepo, FileShareRepo fileShareRepo, FileShareRepo fileShareRepo1) {
        this.fileDownloadService = fileDownloadService;
        this.fileDownloadRepo = fileDownloadRepo;
        this.fileShareRepo = fileShareRepo1;
    }


    @GetMapping("/download/{transferId}")
    public ResponseEntity<?> downloadFile(
            @PathVariable String transferId,
            @RequestHeader(value = "X-Network-Speed", defaultValue = "50.0") Double networkSpeedMbps,
            @RequestHeader(value = "X-Latency-Ms", defaultValue = "50") Integer latencyMs,
            @RequestHeader(value = "X-Device-Type", defaultValue = "DESKTOP") String deviceType,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        String downloadId = UUID.randomUUID().toString();

        log.info("========== DOWNLOAD START ==========");
        log.info("DownloadId: {}", downloadId);
        log.info("TransferId: {}", transferId);
        log.info("Client IP: {}", clientIp);
        log.info("Network Speed: {} Mbps", networkSpeedMbps);
        log.info("Latency: {} ms", latencyMs);
        log.info("Device Type: {}", deviceType);

        try {
            log.info("Fetching transfer metadata...");
            FileTransferEntity transfer = fileDownloadService.getTransferById(transferId);

            if (transfer == null) {
                log.warn("[{}] Transfer not found - TransferId: {}", downloadId, transferId);
                return ResponseEntity.notFound().build();
            }

            log.info("[{}] Transfer found:", downloadId);
            log.info("  Filename: {}", transfer.getFileName());
            log.info("  Original Size: {} bytes", transfer.getFileSize());
            log.info("  Compressed Size: {} bytes", transfer.getBytesTransferred());

            log.info("[{}] Calculating optimal download parameters...", downloadId);
            ChunkedDownloadResource resource = fileDownloadService.downloadFileWithAdaptiveChunking(
                    transferId,
                    networkSpeedMbps,
                    latencyMs
            );

            if (resource == null) {
                log.error("[{}] Failed to create download resource", downloadId);
                return ResponseEntity.status(500)
                        .body(buildErrorResponse("File not found on disk", "FILE_NOT_FOUND"));
            }

            log.info("[{}] Download resource created successfully", downloadId);
            log.info("[{}] Adaptive Parameters Applied:", downloadId);
            log.info("    Network Condition: {}", resource.getNetworkCondition());
            log.info("    Chunk Size: {} bytes", resource.getChunkSize());
            log.info("    Is Compressed: {}", resource.getIsCompressed());

            log.info("[{}] Building HTTP response...", downloadId);

            InputStreamResource inputStreamResource = new InputStreamResource(resource.getInputStream());

            log.info("========== DOWNLOAD SUCCESS ==========");
            FileDownload fileDownload = fileDownloadRepo.findByTransferId(transferId);
            fileDownload.setTransferDurationSeconds(LocalDateTime.now());
            fileDownload.setStoragePath(transfer.getStoragePath());
            fileDownloadRepo.save(fileDownload);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header("X-Download-Id", downloadId)
                    .header("X-Chunk-Size", String.valueOf(resource.getChunkSize()))
                    .header("X-Network-Condition", resource.getNetworkCondition())
                    .header("X-Original-Size", String.valueOf(resource.getOriginalSizeBytes()))
                    .header("X-Compressed-Size", String.valueOf(resource.getCompressedSizeBytes()))
                    .body(inputStreamResource);

        } catch (Exception e) {
            log.error("========== DOWNLOAD FAILED ==========", e);
            return ResponseEntity.status(500)
                    .body(buildErrorResponse("Download failed: " + e.getMessage(), "DOWNLOAD_ERROR"));
        }
    }

    @GetMapping("/info/{transferId}")
    public ResponseEntity<?> getTransferInfo(
            @PathVariable String transferId) {

        log.info("Fetching transfer info for: {}", transferId);

        try {
            FileTransferEntity transfer = fileDownloadService.getTransferById(transferId);

            if (transfer == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> info = new HashMap<>();
            info.put("transferId", transfer.getTransferId());
            info.put("fileName", transfer.getFileName());
            info.put("originalSizeBytes", transfer.getFileSize());
            info.put("compressedSizeBytes", transfer.getBytesTransferred());
            info.put("compressionRatioPercent",
                    String.format("%.2f%%", (1.0 - (double) transfer.getBytesTransferred() / transfer.getFileSize()) * 100));
            info.put("success", transfer.getSuccess());
            info.put("uploadedAt", transfer.getCreatedAt());
            info.put("completedAt", transfer.getCompletedAt());

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            log.error("Error fetching transfer info", e);
            return ResponseEntity.status(500)
                    .body(buildErrorResponse("Failed to get info: " + e.getMessage(), "INFO_ERROR"));
        }
    }

    @GetMapping("/info/public/{shareId}")
    public ResponseEntity<?> getTransferInfoOfPublicFile(
            @PathVariable String shareId) {

        log.info("Fetching transfer info for: {}", shareId);

        try {
            ResponseEntity<?> transfer = fileDownloadService.getTransferInfoOfPublicFile(shareId);

            if (transfer == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(transfer);

        } catch (Exception e) {
            log.error("Error fetching transfer info", e);
            return ResponseEntity.status(500)
                    .body(buildErrorResponse("Failed to get info: " + e.getMessage(), "INFO_ERROR"));
        }
    }




    @GetMapping ("download/{shareId}/public")
    public ResponseEntity<?> downloadPublicFilewithShareId(
            @PathVariable(name = "shareId") Long shareId,
            @RequestHeader(value = "X-Network-Speed", defaultValue = "50.0") Double networkSpeedMbps,
            @RequestHeader(value = "X-Latency-Ms", defaultValue = "50") Integer latencyMs,
            @RequestHeader(value = "X-Device-Type", defaultValue = "DESKTOP") String deviceType,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();

        log.info("========== DOWNLOAD START ==========");
        log.info("ShareId: {}", shareId);
        log.info("Client IP: {}", clientIp);
        log.info("Network Speed: {} Mbps", networkSpeedMbps);
        log.info("Latency: {} ms", latencyMs);
        log.info("Device Type: {}", deviceType);

        try {
            log.info("Fetching transfer metadata...");
            FileShare  fileShare = fileShareRepo.findByShareId(shareId);
            if (fileShare == null) {
                log.warn("Transfer not found - ShareId: {}", shareId);
                return ResponseEntity.notFound().build();
            }if (fileShare.getShareExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("File Expired - ShareId: {}", shareId);
                return ResponseEntity.notFound().build();
            }
            FileTransferEntity transfer = fileDownloadService.getShareById(fileShare.getShareToken());

            if (transfer == null) {
                log.warn("Transfer not found - ShareId: {}", shareId);
                return ResponseEntity.notFound().build();
            }if (transfer.getDeleted()) {
                log.warn("File Expired - ShareId: {}", shareId);
                return ResponseEntity.notFound().build();
            }

            log.info("[{}] Transfer found:", shareId);
            log.info("  Filename: {}", transfer.getFileName());
            log.info("  Original Size: {} bytes", transfer.getFileSize());
            log.info("  Compressed Size: {} bytes", transfer.getBytesTransferred());

            log.info("[{}] Calculating optimal download parameters...", shareId);
            ChunkedDownloadResource resource = fileDownloadService.downloadPublicFileWithAdaptiveChunking(
                    transfer.getTransferId(),
                    networkSpeedMbps,
                    fileShare.getShareToken(),
                    latencyMs
            );

            if (resource == null) {
                log.error("[{}] Failed to create download resource", shareId);
                return ResponseEntity.status(500)
                        .body(buildErrorResponse("File not found on disk", "FILE_NOT_FOUND"));
            }

            log.info("[{}] Download resource created successfully", shareId);
            log.info("[{}] Adaptive Parameters Applied:", shareId);
            log.info("    Network Condition: {}", resource.getNetworkCondition());
            log.info("    Chunk Size: {} bytes", resource.getChunkSize());
            log.info("    Is Compressed: {}", resource.getIsCompressed());

            log.info("[{}] Building HTTP response...", shareId);

            InputStreamResource inputStreamResource = new InputStreamResource(resource.getInputStream());

            log.info("========== DOWNLOAD SUCCESS ==========");
            FileDownload fileDownload = fileDownloadRepo.findByTransferId(transfer.getTransferId());
            if (fileDownload != null) {
                fileDownload.setTransferDurationSeconds(LocalDateTime.now());
                fileDownload.setStoragePath(transfer.getStoragePath());
                fileDownloadRepo.save(fileDownload);
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header("X-Download-Id", fileShare.getShareToken())
                    .header("X-Chunk-Size", String.valueOf(resource.getChunkSize()))
                    .header("X-Network-Condition", resource.getNetworkCondition())
                    .header("X-Original-Size", String.valueOf(resource.getOriginalSizeBytes()))
                    .header("X-Compressed-Size", String.valueOf(resource.getCompressedSizeBytes()))
                    .body(inputStreamResource);

        } catch (Exception e) {
            log.error("========== DOWNLOAD FAILED ==========", e);
            return ResponseEntity.status(500)
                    .body(buildErrorResponse("Download failed: " + e.getMessage(), "DOWNLOAD_ERROR"));
        }
    }





    @GetMapping ("download/{shareToken}/public")
    public ResponseEntity<?> downloadPublicFile(
            @PathVariable(name = "shareToken") String shareId,
            @RequestHeader(value = "X-Network-Speed", defaultValue = "50.0") Double networkSpeedMbps,
            @RequestHeader(value = "X-Latency-Ms", defaultValue = "50") Integer latencyMs,
            @RequestHeader(value = "X-Device-Type", defaultValue = "DESKTOP") String deviceType,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();

        log.info("========== DOWNLOAD START ==========");
        log.info("ShareId: {}", shareId);
        log.info("Client IP: {}", clientIp);
        log.info("Network Speed: {} Mbps", networkSpeedMbps);
        log.info("Latency: {} ms", latencyMs);
        log.info("Device Type: {}", deviceType);

        try {
            log.info("Fetching transfer metadata...");
            FileTransferEntity transfer = fileDownloadService.getShareById(shareId);

            if (transfer == null) {
                log.warn("Transfer not found - ShareId: {}", shareId);
                return ResponseEntity.notFound().build();
            }if (transfer.getDeleted()) {
                log.warn("File Expired - ShareId: {}", shareId);
                return ResponseEntity.notFound().build();
            }

            log.info("[{}] Transfer found:", shareId);
            log.info("  Filename: {}", transfer.getFileName());
            log.info("  Original Size: {} bytes", transfer.getFileSize());
            log.info("  Compressed Size: {} bytes", transfer.getBytesTransferred());

            log.info("[{}] Calculating optimal download parameters...", shareId);
            ChunkedDownloadResource resource = fileDownloadService.downloadPublicFileWithAdaptiveChunking(
                    transfer.getTransferId(),
                    networkSpeedMbps,
                    shareId,
                    latencyMs
            );

            if (resource == null) {
                log.error("[{}] Failed to create download resource", shareId);
                return ResponseEntity.status(500)
                        .body(buildErrorResponse("File not found on disk", "FILE_NOT_FOUND"));
            }

            log.info("[{}] Download resource created successfully", shareId);
            log.info("[{}] Adaptive Parameters Applied:", shareId);
            log.info("    Network Condition: {}", resource.getNetworkCondition());
            log.info("    Chunk Size: {} bytes", resource.getChunkSize());
            log.info("    Is Compressed: {}", resource.getIsCompressed());

            log.info("[{}] Building HTTP response...", shareId);

            InputStreamResource inputStreamResource = new InputStreamResource(resource.getInputStream());

            log.info("========== DOWNLOAD SUCCESS ==========");
            FileDownload fileDownload = fileDownloadRepo.findByTransferId(transfer.getTransferId());
            if (fileDownload != null) {
                fileDownload.setTransferDurationSeconds(LocalDateTime.now());
                fileDownload.setStoragePath(transfer.getStoragePath());
                fileDownloadRepo.save(fileDownload);
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header("X-Download-Id", shareId)
                    .header("X-Chunk-Size", String.valueOf(resource.getChunkSize()))
                    .header("X-Network-Condition", resource.getNetworkCondition())
                    .header("X-Original-Size", String.valueOf(resource.getOriginalSizeBytes()))
                    .header("X-Compressed-Size", String.valueOf(resource.getCompressedSizeBytes()))
                    .body(inputStreamResource);

        } catch (Exception e) {
            log.error("========== DOWNLOAD FAILED ==========", e);
            return ResponseEntity.status(500)
                    .body(buildErrorResponse("Download failed: " + e.getMessage(), "DOWNLOAD_ERROR"));
        }
    }


    private Map<String, Object> buildErrorResponse(String message, String errorCode) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", errorCode);
        errorResponse.put("message", message);
        return errorResponse;
    }

}