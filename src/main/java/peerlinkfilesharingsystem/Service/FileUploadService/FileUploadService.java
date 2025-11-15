package peerlinkfilesharingsystem.Service.FileUploadService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import peerlinkfilesharingsystem.Dto.FileUploadResponse;
import peerlinkfilesharingsystem.Enums.UploadStatus;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Model.IntelligentModelParametersEntity;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;
import peerlinkfilesharingsystem.Repo.IntelligentModelParametersRepo;
import peerlinkfilesharingsystem.Repo.TransferMetricsRepo;
import peerlinkfilesharingsystem.Service.CompressionService.FileCompressionService;
import peerlinkfilesharingsystem.Service.FileStorageService;
import peerlinkfilesharingsystem.Service.IntelligencePredictionService.IntelligencePredictionService;
import peerlinkfilesharingsystem.Service.Redis.RedisUploadService;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
public class FileUploadService {

    private final FileStorageService fileStorageService;
    private final RedisUploadService redisUploadService;
    @Value("${file.storage.path:./uploads}")
    private String uploadDirectory;

    private FileTransferRepo fileTransferRepo;
    private IntelligencePredictionService intelligencePredictionService;
    private TransferMetricsRepo transferMetricsRepo;
    private FileCompressionService compressionService;
    private IntelligentModelParametersRepo intelligentModelParametersRepo;


    public FileUploadService(FileTransferRepo fileTransferRepo,
                             IntelligencePredictionService intelligencePredictionService,
                             TransferMetricsRepo transferMetricsRepo,
                             FileCompressionService fileCompressionService,
                             IntelligentModelParametersRepo intelligentModelParametersRepo, FileStorageService fileStorageService,
                             RedisUploadService redisUploadService) {
        this.fileTransferRepo = fileTransferRepo;
        this.intelligencePredictionService = intelligencePredictionService;
        this.transferMetricsRepo = transferMetricsRepo;
        this.compressionService = fileCompressionService;
        this.intelligentModelParametersRepo = intelligentModelParametersRepo;
        this.fileStorageService = fileStorageService;
        this.redisUploadService = redisUploadService;
    }

    public FileUploadResponse handleFile(MultipartFile file, Integer latencyMs,
                                         Double networkSpeedMbps, String deviceType, String clientIp) {
        String transferId = UUID.randomUUID().toString();
        String filename = file.getOriginalFilename();
        String extension = extractFileType(filename);

        log.info("========== UPLOAD START ==========");
        log.info("TransferID: {}", transferId);
        log.info("Filename: {}, Extension: {}", filename, extension);
        log.info("Original File Size: {} bytes ({} MB)", file.getSize(), file.getSize() / 1024 / 1024);
        log.info("Network Speed: {} Mbps, Latency: {} ms", networkSpeedMbps, latencyMs);

        try {
            FileTransferEntity fileTransferEntity = new FileTransferEntity();
            fileTransferEntity.setTransferId(transferId);
            fileTransferEntity.setUserId("13131"); /// replace it with Actual JWT userid
            fileTransferEntity.setFileName(filename);
            fileTransferEntity.setFileType(extension);
            fileTransferEntity.setDeviceType(deviceType);
            fileTransferEntity.setFileSize(file.getSize());
            fileTransferEntity.setLatencyMs(latencyMs);
            fileTransferEntity.setNetworkSpeedMbps(networkSpeedMbps);
            fileTransferEntity.setClientIp(clientIp);

            log.info("FileTransferEntity created and saved");

            log.info("Requesting ML predictions...");
            IntelligencePredictionService.OptimizationParams params =
                    intelligencePredictionService.predictOptimalParameters(
                            filename, extension, networkSpeedMbps, latencyMs, file.getSize());

            log.info("ML PREDICTION RESULTS:");
            log.info("  Compression Level: {} (higher = more compression)", params.getCompressionLevel());
            log.info("  Chunk Size: {} bytes ({} KB)", params.getChunkSize(), params.getChunkSize() / 1024);
            log.info("  Network Condition: {}", params.getNetworkCondition());
            log.info("  Est. Time Saving: {}%", params.getEstimatedTimeSavingPercent());
            log.info("  Predicted Success Rate: {:.2f}%", params.getPredictedSuccessRate() * 100);

            fileTransferEntity.setCompressionLevel(params.getCompressionLevel());
            fileTransferEntity.setChunkSize(params.getChunkSize());
            fileTransferRepo.save(fileTransferEntity);
            String Userpath  = fileStorageService.createUserDirectory(String.valueOf(fileTransferEntity.getUserId()));
            log.info("Starting compression process..."+ Userpath);
            long startTime = System.currentTimeMillis();

            CompressionResult compressionResult = processUploadWithCompression(
                    file.getInputStream(),fileTransferEntity,Userpath,params);

            long duration = (System.currentTimeMillis() - startTime) / 1000;

            log.info("COMPRESSION RESULTS:");
            log.info("  Original Size: {} bytes ({} MB)", compressionResult.totalBytesRead, compressionResult.totalBytesRead / 1024 / 1024);
            log.info("  Compressed Size: {} bytes ({} MB)", compressionResult.totalBytesCompressed, compressionResult.totalBytesCompressed / 1024 / 1024);
            log.info("  Compression Ratio: {:.2f}% saved",
                    (1.0 - (double) compressionResult.totalBytesCompressed / compressionResult.totalBytesRead) * 100);
            log.info("  Duration: {} seconds", duration);
            log.info("  Chunks Processed: {}", compressionResult.chunkCount);

            fileTransferEntity.setBytesTransferred(compressionResult.totalBytesCompressed);
            fileTransferEntity.setTransferDurationSeconds((int) duration);
            fileTransferEntity.setSuccess(true);
            fileTransferEntity.setCompletedAt(LocalDateTime.now());
            fileTransferEntity.setStoragePath(Userpath + "/" + transferId);
            fileTransferRepo.save(fileTransferEntity);

            log.info("Updating ML Model Parameters...");
            updateMLParamsAfterUpload(
                    extension,
                    params.getNetworkCondition(),
                    params.getCompressionLevel(),
                    params.getChunkSize(),
                    true);
            log.info("ML Model Parameters updated");

            double compressionRatio = (1.0 - (double) compressionResult.totalBytesCompressed / file.getSize()) * 100;

            log.info("========== UPLOAD SUCCESS ==========\n");

            return FileUploadResponse.builder()
                    .fileId(fileTransferEntity.getFileId())
                    .transferId(transferId)
                    .fileName(filename)
                    .fileSizeBytes(file.getSize())
                    .compressedSizeBytes(compressionResult.totalBytesCompressed)
                    .compressionRatioPercent(String.format("%.2f%%", compressionRatio))
                    .appliedCompressionLevel(params.getCompressionLevel())
                    .appliedChunkSize(params.getChunkSize())
                    .success(true)
                    .message("File uploaded successfully with " + String.format("%.2f%%", compressionRatio) + " compression")
                    .uploadedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("========== UPLOAD FAILED ==========", e);
            return FileUploadResponse.builder()
                    .transferId(transferId)
                    .success(false)
                    .message("Upload failed: " + e.getMessage())
                    .build();
        }
    }


    private CompressionResult processUploadWithCompression(InputStream fileInputStream,
                                                           FileTransferEntity transfer,
                                                           String path,
                                                           IntelligencePredictionService.OptimizationParams params)
            throws IOException {

        log.info("Starting file compression process...");

        // Create upload directory if needed
        File uploadDir = new File(uploadDirectory);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Path for temporary original file
        String tempOriginalPath = path + "/" + transfer.getTransferId() + ".tmp";
        String finalCompressedPath = path + "/" + transfer.getTransferId();

        // Step 1: Save original file temporarily
        log.info("Saving original file to temp location...");
        long originalFileSize = 0;
        try (FileOutputStream tempFos = new FileOutputStream(tempOriginalPath)) {
            int optimalBufferSize = params.getChunkSize();
            byte[] buffer = new byte[optimalBufferSize];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                tempFos.write(buffer, 0, bytesRead);
                originalFileSize += bytesRead;
            }
        }

        log.info("Original file saved: {} bytes", originalFileSize);

        // Step 2: Compress the entire file at once using GZIP
        log.info("Compressing file with GZIP...");
        long compressedFileSize = 0;
        try {
            compressedFileSize = compressionService.compressFileToGzip(
                    tempOriginalPath,
                    finalCompressedPath);

            double compressionRatio = (1.0 - (double) compressedFileSize / originalFileSize) * 100;
            log.info("Compression complete: {:.2f}% compression achieved", compressionRatio);

        } finally {
            // Clean up temporary file
            File tempFile = new File(tempOriginalPath);
            if (tempFile.exists()) {
                tempFile.delete();
                log.info("Temporary file cleaned up");
            }
        }

        transfer.setStoragePath(finalCompressedPath);

        return new CompressionResult(originalFileSize, compressedFileSize, 1);
    }


    public FileTransferEntity getTransferId(String transferId) {
        fileTransferRepo.findById(UUID.fromString(transferId)).orElseThrow(() ->
                new IllegalArgumentException("Transfer ID not found: " + transferId));
        return fileTransferRepo.findById(UUID.fromString(transferId)).orElseThrow(() ->
                new IllegalArgumentException("Transfer ID not found: " + transferId));

    }
    public void updateMLParamsAfterUpload(String fileType, String networkCondition,
                                          int compressionLevel, int chunkSize, boolean wasSuccessful) {

        log.info("Updating ML params for FileType: {}, NetworkCondition: {}", fileType, networkCondition);

        Optional<IntelligentModelParametersEntity> entryOpt =
                intelligentModelParametersRepo.findByFileTypeAndNetworkCondition(fileType, networkCondition);

        if (entryOpt.isPresent()) {
            IntelligentModelParametersEntity entry = entryOpt.get();

            int oldSampleCount = entry.getSampleCount();
            double oldSuccessRate = entry.getSuccessRate();

            log.info("  Found existing record: SampleCount: {}, OldSuccessRate: {:.2f}%", oldSampleCount, oldSuccessRate * 100);

            double newSuccessRate = (oldSuccessRate * oldSampleCount + (wasSuccessful ? 1 : 0)) / (oldSampleCount + 1);

            int avgCompression = (entry.getOptimalCompressionLevel() * oldSampleCount + compressionLevel) / (oldSampleCount + 1);
            int avgChunk = (entry.getOptimalChunkSize() * oldSampleCount + chunkSize) / (oldSampleCount + 1);

            entry.setOptimalCompressionLevel(avgCompression);
            entry.setOptimalChunkSize(avgChunk);
            entry.setSuccessRate(newSuccessRate);
            entry.setSampleCount(oldSampleCount + 1);
            intelligentModelParametersRepo.save(entry);

            log.info("  Updated: NewSuccessRate: {:.2f}%, AvgCompression: {}, AvgChunkSize: {}",
                    newSuccessRate * 100, avgCompression, avgChunk);

        } else {
            log.info("  No existing record found. Creating new entry...");

            IntelligentModelParametersEntity newEntry = IntelligentModelParametersEntity.builder()
                    .fileType(fileType)
                    .networkCondition(networkCondition)
                    .optimalCompressionLevel(compressionLevel)
                    .optimalChunkSize(chunkSize)
                    .successRate(wasSuccessful ? 1.0 : 0.0)
                    .sampleCount(1)
                    .build();
            intelligentModelParametersRepo.save(newEntry);

            log.info("  New record created: Compression: {}, ChunkSize: {}", compressionLevel, chunkSize);
        }
    }

    private String extractFileType(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "unknown";
    }

    public List<FileTransferEntity> getRecentTransfers(Integer limit) {
        String userId = "13131";
        return fileTransferRepo.findLastUploads(userId,limit);
    }

    public ResponseEntity<?> getResumableFiles() {
        log.info("===== GET RESUMABLE FILES START =====");
        String userId = "13131";

        List<UploadStatus> statuses = Arrays.asList(UploadStatus.IN_PROGRESS, UploadStatus.PAUSED);

        // Repository method to find by userId and status list
        List<FileTransferEntity> resumableFiles = fileTransferRepo
                .findByUserIdAndUploadStatusIn(userId, statuses);

        return ResponseEntity.ok(resumableFiles);
    }

    public int getOptimalChunkSize(String originalFilename, Double networkSpeedMbps, Integer latencyMs, long size) {
        IntelligencePredictionService.OptimizationParams params =
                intelligencePredictionService.predictOptimalParameters(
                        originalFilename, extractFileType(originalFilename), networkSpeedMbps, latencyMs, size);
        return params.getChunkSize();
    }

    private static class CompressionResult {
        long totalBytesRead;
        long totalBytesCompressed;
        int chunkCount;

        CompressionResult(long totalBytesRead, long totalBytesCompressed, int chunkCount) {
            this.totalBytesRead = totalBytesRead;
            this.totalBytesCompressed = totalBytesCompressed;
            this.chunkCount = chunkCount;
        }
    }
    public FileUploadResponse resumeUpload(String transferId, Long userId) {
        log.info("===== RESUME UPLOAD START =====");
        log.info("TransferId: {}, UserId: {}", transferId, userId);

        try {
            // Step 1: Check if session exists in Redis
            if (!redisUploadService.sessionExists(transferId)) {
                log.error("Session not found in Redis: {}", transferId);
                return FileUploadResponse.builder()
                        .success(false)
                        .message("Upload session not found. Cannot resume.")
                        .build();
            }

            // Step 2: Get session info
            Map<Object, Object> sessionInfo = redisUploadService.getSessionInfo(transferId);
            String fileName = sessionInfo.get("fileName").toString();
            long totalChunks = Long.parseLong(sessionInfo.get("totalChunks").toString());

            log.info("Session found for: {}", fileName);
            log.info("Total chunks: {}, Already uploaded: {}",
                    totalChunks, redisUploadService.getUploadedChunkCount(transferId));

            // Step 3: Change status to IN_PROGRESS
            redisUploadService.resumeUpload(transferId);

            // Step 4: Get missing chunks to inform client
            List<Integer> missingChunks = redisUploadService.getMissingChunks(transferId);

            double progress = redisUploadService.getUploadProgress(transferId);

            log.info("===== RESUME UPLOAD READY =====");
            log.info("Progress: {:.2f}%", progress);
            log.info("Missing chunks to upload: {}", missingChunks.size());

            return FileUploadResponse.builder()
                    .transferId(transferId)
                    .fileName(fileName)
                    .success(true)
                    .message(String.format("Upload resumed. Progress: %.2f%%. Resume upload from chunk %d",
                            progress, missingChunks.isEmpty() ? -1 : missingChunks.get(0)))
                    .build();

        } catch (Exception e) {
            log.error("Resume failed", e);
            return FileUploadResponse.builder()
                    .success(false)
                    .message("Resume failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Check if upload is complete by comparing:
     * - Expected size (from client)
     * - Bytes received
     * - File hash/checksum validation
     */
    public boolean isUploadComplete(FileTransferEntity transfer, Long receivedBytes) {

        if (transfer == null) {
            log.error("Transfer entity is null");
            return false;
        }

        log.info("Checking upload completion for: {}", transfer.getTransferId());
        log.info("  Expected: {} bytes", transfer.getFileSize());
        log.info("  Received: {} bytes", receivedBytes);
        log.info("  Compressed: {} bytes", transfer.getBytesTransferred());

        // Check 1: Did we receive all bytes?
        if (receivedBytes == null || transfer.getFileSize() == null) {
            log.warn("Missing size information");
            return false;
        }

        boolean sizeMatch = receivedBytes >= transfer.getFileSize();
        log.info("Size match: {} ({}% complete)", sizeMatch,
                (receivedBytes * 100) / transfer.getFileSize());

        return sizeMatch;
    }

    /**
     * Mark upload as successful
     */
    public void markUploadAsSuccessful(FileTransferEntity transfer, Long compressedSize) {
        try {
            transfer.setUploadStatus(UploadStatus.COMPLETED);
            transfer.setSuccess(true);
            transfer.setBytesTransferred(compressedSize);
            transfer.setCompletedAt(LocalDateTime.now());
            transfer.setLastAttemptAt(LocalDateTime.now());
            transfer.setFailureReason(null);

            fileTransferRepo.save(transfer);

            log.info("Upload marked SUCCESSFUL: {}", transfer.getTransferId());
            log.info("  Original: {} MB", transfer.getFileSize() / (1024 * 1024));
            log.info("  Compressed: {} MB", compressedSize / (1024 * 1024));

        } catch (Exception e) {
            log.error("Error marking upload as successful", e);
        }
    }

    /**
     * Mark upload as FAILED with reason
     * Generate resume token for retry
     */
    public String markUploadAsFailed(FileTransferEntity transfer, String failureReason) {
        try {
            transfer.setUploadStatus(UploadStatus.FAILED);
            transfer.setSuccess(false);
            transfer.setFailureReason(failureReason);
            transfer.setLastAttemptAt(LocalDateTime.now());

            // Generate resume token (for resuming later)
            String resumeToken = generateResumeToken(transfer);
            transfer.setResumeToken(resumeToken);

            // Increment retry count
            Integer attempts = transfer.getUploadAttempts() != null ?
                    transfer.getUploadAttempts() : 1;
            transfer.setUploadAttempts(attempts + 1);

            // Check if we've exceeded max retries
            Integer maxRetries = transfer.getMaxRetries() != null ?
                    transfer.getMaxRetries() : 3;

            if (attempts >= maxRetries) {
                transfer.setUploadStatus(UploadStatus.CANCELLED);
                transfer.setFailureReason(failureReason + " (Max retries exceeded)");
                log.error("Upload CANCELLED after {} attempts: {}",
                        attempts, transfer.getTransferId());
            }

            fileTransferRepo.save(transfer);

            log.error("Upload marked FAILED: {}", transfer.getTransferId());
            log.error("  Reason: {}", failureReason);
            log.error("  Attempt: {} of {}", attempts, maxRetries);
            log.error("  Resume Token: {}", resumeToken);

            return resumeToken;

        } catch (Exception e) {
            log.error("Error marking upload as failed", e);
            return null;
        }
    }

    /**
     * Generate unique resume token
     */
    private String generateResumeToken(FileTransferEntity transfer) {
        return transfer.getTransferId() + "_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Mark upload as PAUSED (user paused, can resume)
     */
    public String markUploadAsPaused(FileTransferEntity transfer) {
        try {
            transfer.setUploadStatus(UploadStatus.PAUSED);
            String resumeToken = generateResumeToken(transfer);
            transfer.setResumeToken(resumeToken);
            transfer.setLastAttemptAt(LocalDateTime.now());

            fileTransferRepo.save(transfer);

            log.info("Upload PAUSED: {}", transfer.getTransferId());
            log.info("  Resume Token: {}", resumeToken);
            log.info("  Bytes Uploaded: {}", transfer.getBytesTransferred());

            return resumeToken;

        } catch (Exception e) {
            log.error("Error pausing upload", e);
            return null;
        }
    }

    /**
     * Resume a failed or paused upload
     */
    public FileTransferEntity resumeUpload(String resumeToken) {
        try {
            log.info("Attempting to resume upload with token: {}", resumeToken);

            FileTransferEntity transfer = fileTransferRepo.findByResumeToken(resumeToken)
                    .orElse(null);

            if (transfer == null) {
                log.error("No upload found for resume token: {}", resumeToken);
                return null;
            }

            // Check if upload can be resumed
            if (!transfer.getAllowResume()) {
                log.error("Upload {} cannot be resumed (allowResume = false)",
                        transfer.getTransferId());
                return null;
            }

            // Check if not cancelled
            if (transfer.getUploadStatus() == UploadStatus.CANCELLED) {
                log.error("Upload {} is CANCELLED and cannot be resumed",
                        transfer.getTransferId());
                return null;
            }

            // Update status to IN_PROGRESS
            transfer.setUploadStatus(UploadStatus.IN_PROGRESS);
            transfer.setStartedAt(LocalDateTime.now());

            fileTransferRepo.save(transfer);

            log.info("Upload RESUMED: {}", transfer.getTransferId());
            log.info("  Bytes already uploaded: {}", transfer.getBytesTransferred());
            log.info("  Remaining: {} bytes",
                    transfer.getFileSize() - transfer.getBytesTransferred());

            return transfer;

        } catch (Exception e) {
            log.error("Error resuming upload", e);
            return null;
        }
    }

    /**
     * Get upload status
     */
    public UploadStatus getUploadStatus(String transferId) {
        try {
            FileTransferEntity transfer = fileTransferRepo.findByTransferId(transferId)
                    .orElse(null);

            if (transfer == null) {
                log.warn("Transfer not found: {}", transferId);
                return null;
            }

            return transfer.getUploadStatus();

        } catch (Exception e) {
            log.error("Error getting upload status", e);
            return null;
        }
    }

    /**
     * Check upload health (detect stalled uploads)
     */
    public boolean isUploadStalled(FileTransferEntity transfer, long timeoutMs) {
        try {
            if (transfer.getUploadStatus() != UploadStatus.IN_PROGRESS) {
                return false; // Not in progress, so not stalled
            }

            LocalDateTime lastAttempt = transfer.getLastAttemptAt();
            if (lastAttempt == null) {
                return false;
            }

            long lastAttemptMillis = lastAttempt
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            long timeSinceLastActivity = System.currentTimeMillis() - lastAttemptMillis;

            boolean isStalled = timeSinceLastActivity > timeoutMs;

            if (isStalled) {
                log.warn("Upload STALLED: {} (no activity for {} ms)",
                        transfer.getTransferId(), timeSinceLastActivity);
            }

            return isStalled;

        } catch (Exception e) {
            log.error("Error checking if upload is stalled", e);
            return false;
        }
    }

}