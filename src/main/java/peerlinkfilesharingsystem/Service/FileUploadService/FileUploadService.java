package peerlinkfilesharingsystem.Service.FileUploadService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import peerlinkfilesharingsystem.Dto.FileUploadResponse;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Model.IntelligentModelParametersEntity;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;
import peerlinkfilesharingsystem.Repo.IntelligentModelParametersRepo;
import peerlinkfilesharingsystem.Service.CompressionService.FileCompressionService;
import peerlinkfilesharingsystem.Service.FileStorageService;
import peerlinkfilesharingsystem.Service.IntelligencePredictionService.IntelligencePredictionService;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    private final FileStorageService fileStorageService;
    @Value("${file.storage.path:./uploads}")
    private String uploadDirectory;

    private FileTransferRepo fileTransferRepo;
    private IntelligencePredictionService intelligencePredictionService;
    private FileCompressionService compressionService;
    private IntelligentModelParametersRepo intelligentModelParametersRepo;


    public FileUploadService(FileTransferRepo fileTransferRepo,
                             IntelligencePredictionService intelligencePredictionService,
                             FileCompressionService fileCompressionService,
                             IntelligentModelParametersRepo intelligentModelParametersRepo, FileStorageService fileStorageService
                             ) {
        this.fileTransferRepo = fileTransferRepo;
        this.intelligencePredictionService = intelligencePredictionService;
        this.compressionService = fileCompressionService;
        this.intelligentModelParametersRepo = intelligentModelParametersRepo;
        this.fileStorageService = fileStorageService;
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
            fileTransferEntity.setUserId(13131L); /// replace it with Actual JWT userid
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
        String id = "13131"; /// TODO : userId from JWT
        return fileTransferRepo.findLastUploads(id,limit);
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

}