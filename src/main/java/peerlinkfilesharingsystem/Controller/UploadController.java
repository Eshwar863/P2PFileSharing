package peerlinkfilesharingsystem.Controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import peerlinkfilesharingsystem.Dto.FileUploadResponse;
import peerlinkfilesharingsystem.Enums.UploadStatus;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;
import peerlinkfilesharingsystem.Service.FileUploadService.FileUploadService;
import peerlinkfilesharingsystem.Service.Redis.RedisUploadService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@Slf4j
public class UploadController {

    private FileUploadService fileUploadService;
    private FileTransferRepo fileTransferRepo;
    private RedisUploadService redisUploadService;

    public UploadController(FileUploadService fileUploadService,
                            FileTransferRepo fileTransferRepo,
                            RedisUploadService redisUploadService) {
        this.fileUploadService = fileUploadService;
        this.fileTransferRepo = fileTransferRepo;
        this.redisUploadService = redisUploadService;
    }

    /**
     * ENHANCED: Single endpoint handles both upload and resume
     *
     * Usage:
     * - NEW UPLOAD: POST /files/upload (no resumeId)
     * - RESUME: POST /files/upload?resumeId=xyz (with resumeId)
     *
     * Supports:
     * - Full file upload
     * - Resume from partial upload
     * - Redis session tracking
     * - Progress tracking
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestPart(value = "file", required = true) MultipartFile file,
            @RequestHeader(value = "X-Network-Speed", defaultValue = "10.0") Double networkSpeedMbps,
            @RequestHeader(value = "X-Latency-Ms", defaultValue = "50") Integer latencyMs,
            @RequestHeader(value = "X-Device-Type", defaultValue = "DESKTOP") String deviceType,
            @RequestParam(value = "resumeId", required = false) String resumeId,
            @RequestParam(value = "progressOnly", required = false, defaultValue = "false") boolean progressOnly,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        String correlationId = UUID.randomUUID().toString();

        log.info("[{}] Upload request - resumeId: {}, progressOnly: {}, File: {}",
                correlationId, resumeId, progressOnly, file.getOriginalFilename());

        try {
            // ========== CASE 1: Check upload progress only (no file sent) ==========
            if (progressOnly && resumeId != null && !resumeId.isEmpty()) {
                log.info("[{}] Progress check for upload: {}", correlationId, resumeId);

                if (!redisUploadService.sessionExists(resumeId)) {
                    return ResponseEntity.status(400).body(Map.of(
                            "success", false,
                            "message", "Upload session not found"
                    ));
                }

                double progress = redisUploadService.getUploadProgress(resumeId);
                List<Integer> missingChunks = redisUploadService.getMissingChunks(resumeId);
                long uploadedChunks = redisUploadService.getUploadedChunkCount(resumeId);

                Map<Object, Object> sessionInfo = redisUploadService.getSessionInfo(resumeId);
                String status = sessionInfo.getOrDefault("status", "UNKNOWN").toString();

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "uploadId", resumeId,
                        "progress", String.format("%.2f%%", progress),
                        "uploadedChunks", uploadedChunks,
                        "missingChunks", missingChunks,
                        "sessionStatus", status,
                        "progressOnly", true
                ));
            }

            // ========== Validate file ==========
            if (file.isEmpty() || file.getSize() == 0L) {
                log.warn("[{}] File is empty", correlationId);
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "message", "File cannot be empty"
                ));
            }

            if (clientIp.isEmpty()) {
                log.warn("[{}] Client IP missing", correlationId);
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "message", "Client IP cannot be empty"
                ));
            }

            if (file.getSize() > 10 * 1024 * 1024 * 1024L) {
                log.warn("[{}] File exceeds 10 GB limit: {} bytes", correlationId, file.getSize());
                return ResponseEntity.status(413).body(Map.of(
                        "success", false,
                        "message", "File size exceeded (max: 10 GB)"
                ));
            }

            log.info("[{}] Upload parameters - Speed: {} Mbps, Latency: {} ms, Device: {}",
                    correlationId, networkSpeedMbps, latencyMs, deviceType);

            // ========== CASE 2: Resume existing upload ==========
            if (resumeId != null && !resumeId.isEmpty()) {
                log.info("[{}] Resuming upload with ID: {}", correlationId, resumeId);

                // Check if session exists in Redis
                if (!redisUploadService.sessionExists(resumeId)) {
                    log.error("[{}] Resume session not found: {}", correlationId, resumeId);
                    return ResponseEntity.status(404).body(Map.of(
                            "success", false,
                            "message", "Resume session not found. Cannot resume.",
                            "uploadId", resumeId
                    ));
                }

                // Get upload info from Redis
                Map<Object, Object> sessionInfo = redisUploadService.getSessionInfo(resumeId);
                String originalFileName = sessionInfo.getOrDefault("fileName", "").toString();
                long totalChunks = Long.parseLong(
                        sessionInfo.getOrDefault("totalChunks", "0").toString());

                log.info("[{}] Session found - File: {}, Total chunks: {}",
                        correlationId, originalFileName, totalChunks);

                // Mark as IN_PROGRESS in Redis
                redisUploadService.resumeUpload(resumeId);

                // Get missing chunks
                List<Integer> missingChunks = redisUploadService.getMissingChunks(resumeId);
                double progress = redisUploadService.getUploadProgress(resumeId);

                log.info("[{}] Upload resumed - Progress: {:.2f}%, Missing chunks: {}",
                        correlationId, progress, missingChunks.size());

                // Update database status
                FileTransferEntity transfer = fileTransferRepo.findByTransferId(resumeId).orElse(null);
                if (transfer != null) {
                    transfer.setUploadStatus(UploadStatus.IN_PROGRESS);
                    transfer.setLastAttemptAt(java.time.LocalDateTime.now());
                    fileTransferRepo.save(transfer);
                }

                // Perform upload
                FileUploadResponse response = fileUploadService.handleFile(
                        file, latencyMs, networkSpeedMbps, deviceType, clientIp);



                if (response.getSuccess()) {
                    // Calculate total chunks
                    long totalChunksFromDB = (file.getSize() + response.getAppliedChunkSize() - 1)
                            / response.getAppliedChunkSize();

                    // ✅ NEW: Mark ALL chunks at once in BATCH (1 operation instead of 5200!)
                    redisUploadService.markUploadCompleteWithAllChunks(resumeId, totalChunksFromDB);

                    log.info("[{}] Upload RESUMED and COMPLETED: {}", correlationId, resumeId);

                    return ResponseEntity.ok()
                            .body(addRedisMetadata(response, resumeId, "RESUMED"));
                } else {
                    // Resume failed - generate new resume token
                    FileTransferEntity failedTransfer = fileTransferRepo
                            .findByTransferId(resumeId).orElse(null);

                    if (failedTransfer != null) {
                        String newResumeToken = fileUploadService.markUploadAsFailed(failedTransfer,
                                "Resume upload failed: " + response.getMessage());

                        redisUploadService.pauseUpload(resumeId);

                        log.error("[{}] Resume failed: {}", correlationId, response.getMessage());

                        return ResponseEntity.status(400).body(Map.of(
                                "success", false,
                                "message", "Resume failed: " + response.getMessage(),
                                "uploadId", resumeId,
                                "resumeToken", newResumeToken,
                                "retryable", true
                        ));
                    }
                }

            }

            // ========== CASE 3: NEW upload ==========
            else {
                String uploadId = UUID.randomUUID().toString();
                log.info("[{}] NEW upload started - ID: {}, File: {}",
                        correlationId, uploadId, file.getOriginalFilename());

                int chunkSize = fileUploadService.getOptimalChunkSize(
                        file.getOriginalFilename(), networkSpeedMbps, latencyMs, file.getSize());

                long totalChunks = (file.getSize() + chunkSize - 1) / chunkSize;

                log.info("[{}] Upload config - Chunk Size: {} KB, Total chunks: {}",
                        correlationId, chunkSize / 1024, totalChunks);

                // Create Redis session BEFORE upload starts
                redisUploadService.createSession(uploadId, file.getOriginalFilename(),
                        totalChunks, "13131"); // TODO: Get from JWT

                log.info("[{}] Redis session created for upload: {}", correlationId, uploadId);

                try {
                    FileUploadResponse response = fileUploadService.handleFile(
                            file, latencyMs, networkSpeedMbps, deviceType, clientIp);

                    if (response.getSuccess()) {
                        redisUploadService.markUploadCompleteWithAllChunks(uploadId, totalChunks);

                        log.info("[{}] Upload SUCCESS: {}", correlationId, uploadId);

                        return ResponseEntity.ok()
                                .body(addRedisMetadata(response, uploadId, "COMPLETED"));

                    } else {
                        // Upload failed - generate resume token and pause in Redis
                        FileTransferEntity transfer = fileTransferRepo
                                .findByTransferId(uploadId).orElse(null);

                        String resumeToken = fileUploadService.markUploadAsFailed(transfer,
                                "Upload failed: " + response.getMessage());

                        // Pause in Redis
                        redisUploadService.pauseUpload(uploadId);

                        log.error("[{}] Upload FAILED: {}", correlationId, response.getMessage());

                        return ResponseEntity.status(400).body(Map.of(
                                "success", false,
                                "message", "Upload failed: " + response.getMessage(),
                                "uploadId", uploadId,
                                "resumeToken", resumeToken,
                                "retryable", true
                        ));
                    }

                } catch (Exception uploadException) {
                    log.error("[{}] Upload exception: {}", correlationId, uploadException.getMessage());

                    FileTransferEntity transfer = fileTransferRepo
                            .findByTransferId(uploadId).orElse(null);

                    String resumeToken = fileUploadService.markUploadAsFailed(transfer,
                            "Exception: " + uploadException.getMessage());

                    redisUploadService.pauseUpload(uploadId);

                    return ResponseEntity.status(500).body(Map.of(
                            "success", false,
                            "message", "Upload failed: " + uploadException.getMessage(),
                            "uploadId", uploadId,
                            "resumeToken", resumeToken,
                            "retryable", true
                    ));
                }
            }


        } catch (Exception e) {
            log.error("[{}] Unexpected error in upload endpoint", correlationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Upload failed: " + e.getMessage(),
                    "correlationId", correlationId
            ));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Upload failed : Unknown error. Please try again later."
        ));
    }

    /**
     * ENHANCED: Existing /history endpoint returns Redis metadata too
     */
    @GetMapping("/history")
    public ResponseEntity<?> getTransferHistory(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(value = "includeRedisMetadata", required = false, defaultValue = "false")
            boolean includeRedisMetadata) {
        try {
            log.info("Fetching transfer history - limit: {}, includeRedisMetadata: {}",
                    limit, includeRedisMetadata);

            var transfers = fileUploadService.getRecentTransfers(limit);

            if (includeRedisMetadata) {
                return ResponseEntity.ok(transfers.stream().map(transfer -> {
                    Map<String, Object> enhanced = new HashMap<>();
                    enhanced.put("transfer", transfer);

                    if (redisUploadService.sessionExists(transfer.getTransferId())) {
                        Map<Object, Object> sessionInfo =
                                redisUploadService.getSessionInfo(transfer.getTransferId());

                        enhanced.put("redis", Map.of(
                                "status", sessionInfo.getOrDefault("status", "UNKNOWN"),
                                "uploadedChunks", redisUploadService.getUploadedChunkCount(
                                        transfer.getTransferId()),
                                "missingChunks", redisUploadService.getMissingChunks(
                                        transfer.getTransferId()).size(),
                                "progress", redisUploadService.getUploadProgress(
                                        transfer.getTransferId())
                        ));
                    }

                    return enhanced;
                }).toList());
            }

            return ResponseEntity.ok(transfers);

        } catch (Exception e) {
            log.error("Error getting history", e);
            return new ResponseEntity<>(Map.of(
                    "success", false,
                    "message", "History error: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Helper: Add Redis metadata to upload response
     */
    private Map<String, Object> addRedisMetadata(FileUploadResponse response,
                                                 String uploadId, String redisStatus) {
        Map<String, Object> result = new HashMap<>();

        // Add original response fields
        result.put("fileId", response.getFileId());
        result.put("transferId", response.getTransferId());
        result.put("fileName", response.getFileName());
        result.put("fileSizeBytes", response.getFileSizeBytes());
        result.put("compressedSizeBytes", response.getCompressedSizeBytes());
        result.put("compressionRatioPercent", response.getCompressionRatioPercent());
        result.put("appliedCompressionLevel", response.getAppliedCompressionLevel());
        result.put("appliedChunkSize", response.getAppliedChunkSize());
        result.put("success", response.getSuccess());
        result.put("message", response.getMessage());
        result.put("uploadedAt", response.getUploadedAt());

        // Add Redis metadata
        Map<String, Object> redisData = new HashMap<>();
        redisData.put("uploadId", uploadId);
        redisData.put("status", redisStatus);
        redisData.put("uploadedChunks", redisUploadService.getUploadedChunkCount(uploadId));
        redisData.put("totalChunks", redisUploadService.getSessionInfo(uploadId)
                .getOrDefault("totalChunks", 0));
        redisData.put("progress", redisUploadService.getUploadProgress(uploadId));

        result.put("redisMetadata", redisData);

        return result;
    }
}
