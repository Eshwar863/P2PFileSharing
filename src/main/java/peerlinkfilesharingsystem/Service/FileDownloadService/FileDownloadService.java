package peerlinkfilesharingsystem.Service.FileDownloadService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import peerlinkfilesharingsystem.Model.ChunkedDownloadResource;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;
import peerlinkfilesharingsystem.Service.IntelligencePredictionService.IntelligencePredictionService;

import java.io.*;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

@Service
@Slf4j
public class FileDownloadService {

    private FileTransferRepo fileTransferRepo;
    private IntelligencePredictionService intelligencePredictionService;

    private static final int GZIP_MAGIC_BYTE_1 = 0x1f;
    private static final int GZIP_MAGIC_BYTE_2 = 0x8b;

    public FileDownloadService(
            FileTransferRepo fileTransferRepo,
            IntelligencePredictionService intelligencePredictionService) {
        this.fileTransferRepo = fileTransferRepo;
        this.intelligencePredictionService = intelligencePredictionService;
    }


    public FileTransferEntity getTransferById(String transferId) {
        log.info("Querying database for transferId: {}", transferId);

        try {
            Optional<FileTransferEntity> transferOpt = fileTransferRepo.findByTransferId(transferId);

            if (transferOpt.isPresent()) {
                FileTransferEntity transfer = transferOpt.get();
                log.info("Transfer found in database");
                log.debug("  ID: {}, File: {}, Path: {}",
                        transfer.getTransferId(), transfer.getFileName(), transfer.getStoragePath());
                return transfer;
            } else {
                log.warn("Transfer not found in database - TransferId: {}", transferId);
                return null;
            }

        } catch (Exception e) {
            log.error("Error querying database for transferId: {}", transferId, e);
            return null;
        }
    }


    private boolean isGzipCompressed(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[2];
            int bytesRead = fis.read(header);

            if (bytesRead < 2) {
                return false;
            }

            // Check GZIP magic number (0x1f8b)
            return (header[0] & 0xFF) == GZIP_MAGIC_BYTE_1 &&
                    (header[1] & 0xFF) == GZIP_MAGIC_BYTE_2;
        } catch (IOException e) {
            log.error("Error checking GZIP magic bytes", e);
            return false;
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Download file with intelligent chunking based on network conditions
     *
     * Features:
     * 1. Calculates optimal chunk size based on network speed & latency
     * 2. Decompresses GZIP files on-the-fly if needed
     * 3. Streams data efficiently to avoid memory issues
     * 4. Logs progress metrics
     */

    public ChunkedDownloadResource downloadFileWithAdaptiveChunking(
            String transferId,
            Double networkSpeedMbps,
            Integer latencyMs) {

        log.info("Starting adaptive chunked download for transferId: {}", transferId);

        try {
            Optional<FileTransferEntity> transferOpt = fileTransferRepo.findByTransferId(transferId);

            if (transferOpt.isEmpty()) {
                log.error("Transfer not found: {}", transferId);
                return null;
            }

            FileTransferEntity transfer = transferOpt.get();
            String storagePath = transfer.getStoragePath();

            File file = new File(storagePath);

            if (!file.exists() || !file.isFile()) {
                log.error("File not found or invalid path: {}", storagePath);
                return null;
            }

            log.info("File found on disk - Size: {} bytes", file.length());

            // Check if file is GZIP compressed
            boolean isCompressed = isGzipCompressed(file);
            log.info("File is {} compressed", isCompressed ? "GZIP" : "NOT");

            // Get file extension
            String extension = getFileExtension(transfer.getFileName());

            // Predict optimal parameters based on network conditions
            IntelligencePredictionService.OptimizationParams optimizationParams =
                    intelligencePredictionService.predictOptimalParameters(
                            transfer.getFileName(),
                            extension,
                            networkSpeedMbps,
                            latencyMs,
                            transfer.getFileSize()
                    );

            log.info("Optimization Parameters:");
            log.info("  Network Condition: {}", optimizationParams.getNetworkCondition());
            log.info("  Chunk Size: {} bytes", optimizationParams.getChunkSize());
            log.info("  Compression Level: {}", optimizationParams.getCompressionLevel());

            // Create chunked input stream
            InputStream baseInputStream = new FileInputStream(file);

            if (isCompressed) {
                baseInputStream = new GZIPInputStream(baseInputStream);
                log.info("GZIPInputStream created - file will be decompressed");
            }

            // Wrap in chunked resource with adaptive chunk size
            ChunkedInputStream chunkedInputStream = new ChunkedInputStream(
                    baseInputStream,
                    optimizationParams.getChunkSize(),
                    transfer.getFileName()
            );

            return ChunkedDownloadResource.builder()
                    .inputStream(chunkedInputStream)
                    .fileName(transfer.getFileName())
                    .originalSizeBytes(transfer.getFileSize())
                    .compressedSizeBytes(transfer.getBytesTransferred())
                    .chunkSize(optimizationParams.getChunkSize())
                    .networkCondition(optimizationParams.getNetworkCondition())
                    .isCompressed(isCompressed)
                    .build();

        } catch (Exception e) {
            log.error("Error in adaptive download for transferId: {}", transferId, e);
            return null;
        }
    }

    public InputStreamResource downloadFile(String transferId) {
        log.info("Downloading file (legacy method) for transferId: {}", transferId);

        try {
            Optional<FileTransferEntity> transferOpt = fileTransferRepo.findByTransferId(transferId);

            if (transferOpt.isEmpty()) {
                log.error("Transfer not found: {}", transferId);
                return null;
            }

            FileTransferEntity transfer = transferOpt.get();
            String storagePath = transfer.getStoragePath();

            File file = new File(storagePath);

            if (!file.exists() || !file.isFile()) {
                log.error("File not found: {}", storagePath);
                return null;
            }

            boolean isCompressed = isGzipCompressed(file);
            log.info("File is {} compressed", isCompressed ? "GZIP" : "NOT");

            FileInputStream fileInputStream = new FileInputStream(file);
            InputStream inputStream;

            if (isCompressed) {
                inputStream = new GZIPInputStream(fileInputStream);
            } else {
                inputStream = fileInputStream;
            }

            return new InputStreamResource(inputStream);

        } catch (Exception e) {
            log.error("Error downloading file", e);
            return null;
        }
    }


    @Slf4j
    public static class ChunkedInputStream extends InputStream {
        private final InputStream delegate;
        private final int chunkSize;
        private final String fileName;
        private final byte[] buffer;
        private long totalBytesRead = 0;
        private long startTime;
        private int chunkCount = 0;

        public ChunkedInputStream(InputStream delegate, int chunkSize, String fileName) {
            this.delegate = delegate;
            this.chunkSize = chunkSize;
            this.fileName = fileName;
            this.buffer = new byte[chunkSize];
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public int read() throws IOException {
            int byte_ = delegate.read();
            if (byte_ != -1) {
                totalBytesRead++;
            }
            return byte_;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = delegate.read(b, off, Math.min(len, chunkSize));
            if (bytesRead > 0) {
                totalBytesRead += bytesRead;
                chunkCount++;

                // Log every 10 chunks
                if (chunkCount % 10 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double speedMbps = (totalBytesRead * 8.0) / (elapsed * 1000.0);
                    log.info("Download Progress - File: {}, Chunk: {}, Total: {} MB, Speed: {:.2f} Mbps",
                            fileName, chunkCount, totalBytesRead / (1024 * 1024), speedMbps);
                }
            }
            return bytesRead;
        }

        @Override
        public void close() throws IOException {
            long totalTime = System.currentTimeMillis() - startTime;
            double avgSpeedMbps = (totalBytesRead * 8.0) / (totalTime * 1000.0);
            log.info("Download Complete - File: {}, Total: {} MB, Time: {}ms, Avg Speed: {:.2f} Mbps, Chunks: {}",
                    fileName, totalBytesRead / (1024 * 1024), totalTime, avgSpeedMbps, chunkCount);
            delegate.close();
        }
    }
}
