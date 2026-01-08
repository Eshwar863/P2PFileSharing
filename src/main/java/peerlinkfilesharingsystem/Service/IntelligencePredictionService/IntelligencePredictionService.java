package peerlinkfilesharingsystem.Service.IntelligencePredictionService;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import peerlinkfilesharingsystem.Model.IntelligentModelParametersEntity;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;
import peerlinkfilesharingsystem.Repo.IntelligentModelParametersRepo;

import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class IntelligencePredictionService {

    @Autowired
    private IntelligentModelParametersRepo intelligentModelParametersRepo;

    @Autowired
    private FileTransferRepo fileTransferRepo;

    private static final Set<String> PRECOMPRESSED_FORMATS = Set.of(
            // Video formats (already highly compressed)
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg",
            // Image formats (lossy compression)
            "jpg", "jpeg", "png", "gif", "webp", "heic", "heif",
            // Audio formats (compressed)
            "mp3", "aac", "ogg", "m4a", "opus", "wma",
            // Archive formats (already compressed)
            "zip", "rar", "7z", "gz", "bz2", "xz", "tar.gz", "tgz",
            // Fonts (optimized)
            "woff", "woff2"
    );

    private static final Set<String> HIGHLY_COMPRESSIBLE_FORMATS = Set.of(
            // Text documents
            "txt", "log", "csv", "json", "xml", "yaml", "yml", "md", "rst",
            // Source code
            "java", "js", "ts", "py", "rb", "go", "c", "cpp", "h", "hpp", "cs", "php",
            // Markup
            "html", "htm", "css", "scss", "sass", "less",
            // Office documents (uncompressed)
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
            // Database
            "sql", "db",
            // Configuration
            "ini", "conf", "config", "properties"
    );

    private static final Set<String> MODERATELY_COMPRESSIBLE_FORMATS = Set.of(
            // Uncompressed images
            "bmp", "tif", "tiff", "psd", "raw", "svg",
            // Uncompressed audio
            "wav", "flac", "aiff", "alac",
            // PDF (depends on content)
            "pdf",
            // Fonts (uncompressed)
            "ttf", "otf"
    );

    public OptimizationParams predictOptimalParameters(
            String fileName,
            String extension,
            Double networkSpeedMbps,
            Integer latencyMs,
            Long fileSizeBytes,
            String deviceType) {

        log.info("=== PREDICTING OPTIMAL PARAMETERS ===");
        log.info("File: {}, Extension: {}, Size: {} MB", fileName, extension, fileSizeBytes / 1024.0 / 1024.0);
        log.info("Network: {} Mbps, Latency: {} ms, Device: {}", networkSpeedMbps, latencyMs, deviceType);

        String networkCondition = classifyNetworkCondition(networkSpeedMbps, latencyMs);
        log.info("Network Condition: {}", networkCondition);

        CompressionStrategy strategy = determineCompressionStrategy(extension);
        log.info("Compression Strategy: {}", strategy);

        Optional<IntelligentModelParametersEntity> learnedParams =
                intelligentModelParametersRepo.findByFileTypeAndNetworkCondition(extension, networkCondition);

        OptimizationParams params;
        if (learnedParams.isPresent()) {
            log.info(" Using learned parameters from database");
            params = buildFromLearned(learnedParams.get(), strategy);
        } else {
            log.info(" Using rule-based prediction");
            params = predictUsingAdvancedRules(
                    extension,
                    networkSpeedMbps,
                    latencyMs,
                    fileSizeBytes,
                    strategy,
                    deviceType
            );
        }

        log.info("=== PREDICTED PARAMETERS ===");
        log.info("Compression Level: {}/9", params.getCompressionLevel());
        log.info("Chunk Size: {} KB", params.getChunkSize() / 1024);
        log.info("Estimated Chunks: {}", (fileSizeBytes / params.getChunkSize()) + 1);
        log.info("Network Condition: {}", params.getNetworkCondition());
        log.info("Time Saving: {}%", params.getEstimatedTimeSavingPercent());

        return params;
    }


    private CompressionStrategy determineCompressionStrategy(String extension) {
        if (PRECOMPRESSED_FORMATS.contains(extension.toLowerCase())) {
            return CompressionStrategy.SKIP; // Already compressed, don't waste CPU
        } else if (HIGHLY_COMPRESSIBLE_FORMATS.contains(extension.toLowerCase())) {
            return CompressionStrategy.HIGH; // Text/code compresses very well
        } else if (MODERATELY_COMPRESSIBLE_FORMATS.contains(extension.toLowerCase())) {
            return CompressionStrategy.MEDIUM; // Some benefit from compression
        } else {
            return CompressionStrategy.LOW; // Unknown, use low compression
        }
    }

    private OptimizationParams predictUsingAdvancedRules(
            String fileType,
            Double networkSpeedMbps,
            Integer latencyMs,
            Long fileSizeBytes,
            CompressionStrategy strategy,
            String deviceType) {


        int compressionLevel = calculateCompressionLevel(
                networkSpeedMbps,
                latencyMs,
                strategy,
                deviceType
        );


        int chunkSize = calculateOptimalChunkSize(
                networkSpeedMbps,
                latencyMs,
                fileSizeBytes
        );


        int timeSavingPercent = estimateTimeSaving(
                compressionLevel,
                strategy,
                networkSpeedMbps
        );


        double predictedSuccessRate = predictSuccessRate(
                networkSpeedMbps,
                latencyMs,
                chunkSize
        );

        return OptimizationParams.builder()
                .compressionLevel(compressionLevel)
                .chunkSize(chunkSize)
                .fileType(fileType)
                .networkCondition(classifyNetworkCondition(networkSpeedMbps, latencyMs))
                .estimatedTimeSavingPercent(timeSavingPercent)
                .predictedSuccessRate(predictedSuccessRate)
                .build();
    }

    private int calculateCompressionLevel(
            Double networkSpeedMbps,
            Integer latencyMs,
            CompressionStrategy strategy,
            String deviceType) {

        int baseLevel;

        // Step 1: Determine base compression level based on strategy
        switch (strategy) {
            case SKIP:
                return 1; // No compression for already compressed files

            case HIGH:
                // Text/code files - high compression is very effective
                if (networkSpeedMbps < 10.0) {
                    baseLevel = 9; // Slow network: max compression
                } else if (networkSpeedMbps < 50.0) {
                    baseLevel = 7; // Medium network: high compression
                } else if (networkSpeedMbps < 100.0) {
                    baseLevel = 6; // Fast network: balanced
                } else {
                    baseLevel = 4; // Very fast: prioritize speed
                }
                break;

            case MEDIUM:
                // Images/audio - moderate compression
                if (networkSpeedMbps < 10.0) {
                    baseLevel = 6;
                } else if (networkSpeedMbps < 50.0) {
                    baseLevel = 5;
                } else {
                    baseLevel = 3;
                }
                break;

            case LOW:
            default:
                // Unknown files - light compression
                if (networkSpeedMbps < 10.0) {
                    baseLevel = 4;
                } else {
                    baseLevel = 2;
                }
                break;
        }

        // Step 2: Adjust based on latency
        if (latencyMs > 200) {
            baseLevel = Math.min(9, baseLevel + 2); // High latency: compress more
        } else if (latencyMs > 150) {
            baseLevel = Math.min(9, baseLevel + 1);
        } else if (latencyMs < 30) {
            baseLevel = Math.max(1, baseLevel - 1); // Low latency: compress less
        }

        // Step 3: Adjust based on device type
        if ("MOBILE".equalsIgnoreCase(deviceType)) {
            baseLevel = Math.max(1, baseLevel - 2); // Mobile: save CPU
        } else if ("TABLET".equalsIgnoreCase(deviceType)) {
            baseLevel = Math.max(1, baseLevel - 1); // Tablet: moderate CPU
        }
        // DESKTOP: no adjustment (full CPU power)

        // Step 4: Ensure within valid range
        return Math.max(1, Math.min(9, baseLevel));
    }

    private int calculateOptimalChunkSize(
            Double networkSpeedMbps,
            Integer latencyMs,
            Long fileSizeBytes) {

        double fileSizeMB = fileSizeBytes / 1024.0 / 1024.0;

        // ========== BANDWIDTH-DELAY PRODUCT ==========
        // Optimal chunk size should fill the network pipe
        // BDP = Bandwidth (bits/sec) × RTT (seconds)
        double bandwidthBitsPerSec = networkSpeedMbps * 1_000_000.0;
        double rttSeconds = latencyMs / 1000.0;
        double bdpBytes = (bandwidthBitsPerSec * rttSeconds) / 8.0;

        log.debug("BDP calculation: {} Mbps × {} ms = {} bytes",
                networkSpeedMbps, latencyMs, bdpBytes);

        int chunkSize;

        // ========== SPEED-BASED CHUNK SIZE ==========
        if (networkSpeedMbps < 1.0) {
            // Very slow (< 1 Mbps) - 2G/Edge
            chunkSize = 16 * 1024; // 16 KB
        } else if (networkSpeedMbps < 5.0) {
            // Slow (1-5 Mbps) - Slow 3G
            chunkSize = 32 * 1024; // 32 KB
        } else if (networkSpeedMbps < 10.0) {
            // Below average (5-10 Mbps) - 3G
            chunkSize = 64 * 1024; // 64 KB
        } else if (networkSpeedMbps < 25.0) {
            // Average (10-25 Mbps) - 4G/DSL
            chunkSize = 128 * 1024; // 128 KB
        } else if (networkSpeedMbps < 50.0) {
            // Good (25-50 Mbps) - Cable/4G+
            chunkSize = 256 * 1024; // 256 KB
        } else if (networkSpeedMbps < 100.0) {
            // Fast (50-100 Mbps) - Fiber/5G
            chunkSize = 512 * 1024; // 512 KB
        } else if (networkSpeedMbps < 250.0) {
            // Very fast (100-250 Mbps)
            chunkSize = 1 * 1024 * 1024; // 1 MB
        } else if (networkSpeedMbps < 500.0) {
            // Ultra fast (250-500 Mbps)
            chunkSize = 2 * 1024 * 1024; // 2 MB
        } else {
            // Gigabit+ (> 500 Mbps)
            chunkSize = 4 * 1024 * 1024; // 4 MB
        }

        // ========== LATENCY ADJUSTMENT ==========
        if (latencyMs > 300) {
            // Very high latency: smaller chunks for faster retransmission
            chunkSize = (int) (chunkSize * 0.5);
        } else if (latencyMs > 200) {
            chunkSize = (int) (chunkSize * 0.75);
        } else if (latencyMs > 150) {
            chunkSize = (int) (chunkSize * 0.9);
        } else if (latencyMs < 20) {
            // Very low latency: can use larger chunks
            chunkSize = (int) (chunkSize * 1.5);
        } else if (latencyMs < 50) {
            chunkSize = (int) (chunkSize * 1.2);
        }

        // ========== FILE SIZE ADJUSTMENT ==========
        if (fileSizeMB < 1.0) {
            // Small file (< 1 MB): use smaller chunks
            chunkSize = Math.min(chunkSize, 64 * 1024);
        } else if (fileSizeMB < 10.0) {
            // Medium file (1-10 MB)
            chunkSize = Math.min(chunkSize, 256 * 1024);
        } else if (fileSizeMB < 100.0) {
            // Large file (10-100 MB)
            chunkSize = Math.min(chunkSize, 1024 * 1024);
        }
        // Files > 100 MB can use any calculated chunk size

        // ========== ENSURE MINIMUM CHUNKS ==========
        // Ensure at least 10 chunks for progress tracking
        int maxChunkSize = (int) (fileSizeBytes / 10);
        if (maxChunkSize > 0 && chunkSize > maxChunkSize) {
            chunkSize = maxChunkSize;
        }

        // ========== BOUNDARY CONSTRAINTS ==========
        // Minimum: 16 KB (for tiny files and very slow networks)
        chunkSize = Math.max(16 * 1024, chunkSize);

        // Maximum: 8 MB (to prevent memory issues and allow progress updates)
        chunkSize = Math.min(8 * 1024 * 1024, chunkSize);

        // Round to nearest power of 2 for efficiency
        chunkSize = roundToPowerOfTwo(chunkSize);

        log.debug("Final chunk size: {} KB for {} MB file", chunkSize / 1024, fileSizeMB);

        return chunkSize;
    }


    private int roundToPowerOfTwo(int value) {
        int power = 1;
        while (power < value) {
            power *= 2;
        }
        // Return closest power of 2
        int lower = power / 2;
        return (value - lower < power - value) ? lower : power;
    }

    private int estimateTimeSaving(
            int compressionLevel,
            CompressionStrategy strategy,
            Double networkSpeedMbps) {

        int baseSaving;

        // Compression reduces file size, saving transfer time
        switch (strategy) {
            case SKIP:
                baseSaving = 5; // Minimal saving (skip compression overhead)
                break;

            case HIGH:
                // Text/code can achieve 70-90% compression
                baseSaving = 50 + (compressionLevel * 4); // 50-86%
                break;

            case MEDIUM:
                // Images/PDFs achieve 20-50% compression
                baseSaving = 20 + (compressionLevel * 3); // 20-47%
                break;

            case LOW:
            default:
                // Unknown formats: 10-30% compression
                baseSaving = 10 + (compressionLevel * 2); // 10-28%
                break;
        }

        // Adjust for network speed
        // Fast networks benefit less from compression (CPU becomes bottleneck)
        if (networkSpeedMbps > 100.0) {
            baseSaving = (int) (baseSaving * 0.7); // Reduce by 30%
        } else if (networkSpeedMbps > 50.0) {
            baseSaving = (int) (baseSaving * 0.85); // Reduce by 15%
        }
        // Slow networks benefit more (network is bottleneck)

        return Math.max(0, Math.min(95, baseSaving));
    }

    private double predictSuccessRate(
            Double networkSpeedMbps,
            Integer latencyMs,
            Integer chunkSize) {

        double baseRate = 0.98; // Start with 98% success rate

        // Reduce success rate for poor network conditions
        if (networkSpeedMbps < 1.0) {
            baseRate -= 0.15; // Very slow network
        } else if (networkSpeedMbps < 5.0) {
            baseRate -= 0.08;
        } else if (networkSpeedMbps < 10.0) {
            baseRate -= 0.03;
        }

        if (latencyMs > 300) {
            baseRate -= 0.10; // Very high latency
        } else if (latencyMs > 200) {
            baseRate -= 0.05;
        } else if (latencyMs > 150) {
            baseRate -= 0.02;
        }

        // Large chunks on poor networks reduce success rate
        if (chunkSize > 1024 * 1024 && networkSpeedMbps < 10.0) {
            baseRate -= 0.05;
        }

        return Math.max(0.75, Math.min(0.99, baseRate));
    }
    private String classifyNetworkCondition(Double speedMbps, Integer latencyMs) {
        // Combined scoring approach
        int score = 0;

        // Speed score (0-50 points)
        if (speedMbps >= 100.0) score += 50;
        else if (speedMbps >= 50.0) score += 40;
        else if (speedMbps >= 25.0) score += 30;
        else if (speedMbps >= 10.0) score += 20;
        else if (speedMbps >= 5.0) score += 10;
        else score += 5;

        // Latency score (0-50 points)
        if (latencyMs <= 20) score += 50;
        else if (latencyMs <= 50) score += 40;
        else if (latencyMs <= 100) score += 30;
        else if (latencyMs <= 150) score += 20;
        else if (latencyMs <= 200) score += 10;
        else score += 5;

        // Classify based on total score
        if (score >= 80) return "EXCELLENT"; // 100+ Mbps, < 20ms
        else if (score >= 60) return "FAST";      // 50+ Mbps, < 50ms
        else if (score >= 40) return "MEDIUM";    // 10-50 Mbps, 50-100ms
        else if (score >= 25) return "SLOW";      // 5-10 Mbps, 100-200ms
        else return "VERY_SLOW";                  // < 5 Mbps or > 200ms
    }

    private OptimizationParams buildFromLearned(
            IntelligentModelParametersEntity learned,
            CompressionStrategy strategy) {

        int compression = (strategy == CompressionStrategy.SKIP)
                ? 1
                : learned.getOptimalCompressionLevel();

        return OptimizationParams.builder()
                .compressionLevel(compression)
                .chunkSize(learned.getOptimalChunkSize())
                .fileType(learned.getFileType())
                .networkCondition(learned.getNetworkCondition())
                .estimatedTimeSavingPercent(
                        (int) ((1.0 - (1.0 / learned.getSuccessRate())) * 100))
                .predictedSuccessRate(learned.getSuccessRate())
                .build();
    }

    private enum CompressionStrategy {
        SKIP,   // Already compressed - level 1
        HIGH,   // Text/code - level 6-9
        MEDIUM, // Images/PDFs - level 3-6
        LOW     // Unknown - level 2-4
    }
    @Data
    @Builder
    public static class OptimizationParams {
        private Integer compressionLevel;
        private Integer chunkSize;
        private String fileType;
        private String networkCondition;
        private Integer estimatedTimeSavingPercent;
        private Double predictedSuccessRate;
    }
}
