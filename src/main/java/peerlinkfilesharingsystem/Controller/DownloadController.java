package peerlinkfilesharingsystem.Controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Service.FileDownloadService.FileDownloadService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
@Slf4j
public class DownloadController {

    private FileDownloadService fileDownloadService;

    public DownloadController(FileDownloadService fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }


    @GetMapping("/download/{transferId}")
    public ResponseEntity<?> downloadFile(
            @PathVariable String transferId,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        log.info("========== DOWNLOAD START ==========");
        log.info("TransferId: {}, Client IP: {}", transferId, clientIp);

        try {
            // Step 1: Fetch transfer metadata from database
            log.info("Fetching transfer metadata...");
            FileTransferEntity transfer = fileDownloadService.getTransferById(transferId);

            if (transfer == null) {
                log.warn("Transfer not found - TransferId: {}", transferId);
                return ResponseEntity.notFound().build();
            }

            log.info("Transfer found:");
            log.info("  Filename: {}", transfer.getFileName());
            log.info("  Original Size: {} bytes", transfer.getFileSize());
            log.info("  Compressed Size: {} bytes", transfer.getBytesTransferred());
            log.info("  Storage Path: {}", transfer.getStoragePath());

            log.info("Reading file from disk...");
            InputStreamResource resource = fileDownloadService.downloadFile(transferId);

            if (resource == null) {
                log.error("Failed to read file from disk");
                return ResponseEntity.status(500)
                        .body(buildErrorResponse("File not found on disk", "FILE_NOT_FOUND"));
            }

            log.info("File read successfully");

            log.info("Building download response...");
            String downloadFileName = transfer.getFileName();

            log.info("========== DOWNLOAD SUCCESS ==========");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadFileName + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .body(resource);

        } catch (Exception e) {
            log.error("========== DOWNLOAD FAILED ==========", e);
            return ResponseEntity.status(500)
                    .body(buildErrorResponse("Download failed: " + e.getMessage(), "DOWNLOAD_ERROR"));
        }
    }

    @GetMapping("/info/{transferId}")
    public ResponseEntity<?> getTransferInfo(@PathVariable String transferId) {
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

    private Map<String, Object> buildErrorResponse(String message, String errorCode) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", errorCode);
        errorResponse.put("message", message);
        return errorResponse;
    }
}
