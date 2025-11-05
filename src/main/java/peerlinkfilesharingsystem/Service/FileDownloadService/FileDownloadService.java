package peerlinkfilesharingsystem.Service.FileDownloadService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;

import java.io.*;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

@Service
@Slf4j
public class FileDownloadService {

    private FileTransferRepo fileTransferRepo;

    // GZIP magic number: 0x1f8b
    /// loading diff diff chunks and on concatinating them failed to construct the file
    /// File is not readable
    private static final int GZIP_MAGIC_BYTE_1 = 0x1f;
    private static final int GZIP_MAGIC_BYTE_2 = 0x8b;

    public FileDownloadService(FileTransferRepo fileTransferRepo) {
        this.fileTransferRepo = fileTransferRepo;

    }


///     Get transfer record by transferId from database

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


///     Check if file is GZIP compressed by reading magic bytes

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

    /**
     * Download file and decompress it on-the-fly if it's compressed
     *
     * WHAT THIS DOES:
     * 1. Check if file is GZIP compressed
     * 2. If compressed: create GZIPInputStream to decompress as we read
     * 3. If not compressed: stream file as-is
     * 4. Stream data to user
     *
     * WHY:
     * - User receives correct file (decompressed if needed)
     * - Memory efficient (streams, doesn't load entire file)
     * - Handles both compressed and uncompressed files
     */
    public InputStreamResource downloadFile(String transferId) {
        log.info("Downloading file for transferId: {}", transferId);

        try {
            Optional<FileTransferEntity> transferOpt = fileTransferRepo.findByTransferId(transferId);

            if (transferOpt.isEmpty()) {
                log.error("Transfer not found: {}", transferId);
                return null;
            }

            FileTransferEntity transfer = transferOpt.get();
            String storagePath = transfer.getStoragePath();

            log.info("Storage path: {}", storagePath);

            File file = new File(storagePath);

            if (!file.exists()) {
                log.error("File not found on disk at path: {}", storagePath);
                return null;
            }

            if (!file.isFile()) {
                log.error("Path is not a file: {}", storagePath);
                return null;
            }

            log.info("File found on disk:");
            log.info("  File Size: {} bytes", file.length());
            log.info("  Original Size: {} bytes", transfer.getFileSize());
            log.info("  Can read: {}", file.canRead());

            // Step 3: Check if file is GZIP compressed
            boolean isCompressed = isGzipCompressed(file);
            log.info("File is {} compressed", isCompressed ? "GZIP" : "NOT");

            // Step 4: Create appropriate input stream
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStream inputStream;

                if (isCompressed) {
                    // Wrap in GZIPInputStream to decompress
                    inputStream = new GZIPInputStream(fileInputStream);
                    log.info("GZIPInputStream created - file will be decompressed during download");
                } else {
                    // Use file directly without decompression
                    inputStream = fileInputStream;
                    log.info("File is not compressed - streaming as-is");
                }

                // Wrap in InputStreamResource for HTTP response
                InputStreamResource resource = new InputStreamResource(inputStream);
                log.info("InputStreamResource created for streaming data");

                return resource;

            } catch (IOException e) {
                log.error("Error creating input stream", e);
                return null;
            }

        } catch (Exception e) {
            log.error("Error downloading file for transferId: {}", transferId, e);
            return null;
        }
    }
}