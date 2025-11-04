package peerlinkfilesharingsystem.Service.CompressionService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.zip.GZIPOutputStream;

@Service
@Slf4j
public class FileCompressionService {

    /**
     * Compress entire file as a single GZIP stream
     *
     * WHY THIS APPROACH:
     * - Creates ONE valid GZIP file
     * - Decompression works perfectly
     * - File integrity preserved
     * - Compatible with all tools (7zip, gunzip, WinRAR, etc)
     */
    public long compressFileToGzip(String inputFilePath, String outputFilePath, Integer compressionLevel) throws IOException {
        log.info("Starting GZIP compression of entire file");
        log.info("  Input: {}", inputFilePath);
        log.info("  Output: {}", outputFilePath);

        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Input file not found: " + inputFilePath);
        }

        long originalSize = inputFile.length();
        log.info("  Original Size: {} bytes ({} MB)", originalSize, originalSize / 1024 / 1024);

        long compressedSize = 0;

        try (FileInputStream fis = new FileInputStream(inputFilePath);
             FileOutputStream fos = new FileOutputStream(outputFilePath);
             GZIPOutputStream gzipOut = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];  // 8KB buffer
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                gzipOut.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (totalBytesRead % (1024 * 1024) == 0) {  // Log every 1MB
                    log.debug("Compressed {} MB so far...", totalBytesRead / 1024 / 1024);
                }
            }

            // CRITICAL: Finish the GZIP stream properly
            gzipOut.finish();
            gzipOut.flush();

        } catch (IOException e) {
            log.error("Error during GZIP compression", e);
            File outputFile = new File(outputFilePath);
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw e;
        }

        File outputFile = new File(outputFilePath);
        compressedSize = outputFile.length();

        double compressionRatio = (1.0 - (double) compressedSize / originalSize) * 100;
        log.info("GZIP compression complete:");
        log.info("  Original Size: {} bytes ({} MB)", originalSize, originalSize / 1024 / 1024);
        log.info("  Compressed Size: {} bytes ({} MB)", compressedSize, compressedSize / 1024 / 1024);
        log.info("  Compression Ratio: {:.2f}% saved", compressionRatio);

        return compressedSize;
    }


    /**
     * Compress chunk (for backward compatibility)
     * But mark file as uncompressed in DB so we don't double-decompress
     */
    public byte[] compressChunk(byte[] buffer, int bytesRead, Integer compressionLevel) {
        // For this implementation, we'll skip chunk compression
        // Instead, we'll compress the entire file at once
        log.warn("compressChunk called - returning uncompressed data. Use compressFileToGzip instead.");
        return buffer;  // Return uncompressed
    }
}
