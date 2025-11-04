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
            // Video formats
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",
            // Image formats
            "jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff", "webp", "psd",
            // Audio formats
            "mp3", "wav", "wma", "flac", "aac", "ogg",
            // Archive formats
            "zip", "rar", "7z", "gz", "tar", "bz2",
            // Fonts
            "ttf", "otf", "woff", "woff2"
    );

    private static final Set<String> TEXT_FORMATS = Set.of(
            "doc", "docx", "pdf", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv",
            "html", "htm", "css", "php", "asp", "xml",
            "sql", "md", "json", "yaml", "yml", "java", "py", "js"
    );

    public OptimizationParams predictOptimalParameters(
            String fileName,
            String extention,
            Double networkSpeedMbps,
            Integer latencyMs,
            Long fileSizeBytes) {

        String networkCondition = classifyNetworkCondition(networkSpeedMbps, latencyMs);

        boolean isAlreadyCompressed = PRECOMPRESSED_FORMATS.contains(extention);
        boolean isTextFile = TEXT_FORMATS.contains(extention);

        Optional<IntelligentModelParametersEntity> learnedParams =
                intelligentModelParametersRepo.findByFileTypeAndNetworkCondition(extention, networkCondition);

        if (learnedParams.isPresent()) {
            return buildFromLearned(learnedParams.get(), isAlreadyCompressed);
        }
        else {
            return predictUsingRules(extention, networkSpeedMbps, latencyMs, isAlreadyCompressed, isTextFile, fileSizeBytes);
        }
    }

    private OptimizationParams predictUsingRules(String fileType, Double networkSpeedMbps, Integer latencyMs, boolean isAlreadyCompressed, boolean isTextFile, Long fileSizeBytes) {
        int compressionLevel;
        int chunkSize;

        if (isAlreadyCompressed) {
            compressionLevel = 1;
        } else {
            if (networkSpeedMbps < 5.0) {
                compressionLevel = 8;
            } else if (networkSpeedMbps < 10.0) {
                compressionLevel = 6;
            } else if (networkSpeedMbps < 50.0) {
                compressionLevel = 5;
            } else {
                compressionLevel = 2;
            }

            if (isTextFile) {
                compressionLevel = Math.min(9, compressionLevel + 2);
            }
        }

        double bandwidthDelayProduct = (networkSpeedMbps * latencyMs) / 8.0;

        if (latencyMs > 200 || networkSpeedMbps < 2.0) {
            chunkSize = 16 * 1024;
        } else if (latencyMs > 150 || networkSpeedMbps < 5.0) {
            chunkSize = 32 * 1024;
        } else if (latencyMs > 100 || networkSpeedMbps < 25.0) {
            chunkSize = (int) Math.min(256 * 1024, bandwidthDelayProduct * 1024);
        } else {
            chunkSize = (int) Math.min(1024 * 1024, bandwidthDelayProduct * 1024 * 2);
        }

        int timeSavingPercent = isAlreadyCompressed ? 10 : (int) (50 * compressionLevel / 9);

        return OptimizationParams.builder()
                .compressionLevel(compressionLevel)
                .chunkSize(chunkSize)
                .fileType(fileType)
                .networkCondition(classifyNetworkCondition(networkSpeedMbps, latencyMs))
                .estimatedTimeSavingPercent(timeSavingPercent)
                .predictedSuccessRate(0.96)
                .build();
    }

    private String classifyNetworkCondition(Double speedMbps, Integer latencyMs) {
        if (speedMbps < 5.0 || latencyMs > 200) return "VERY_SLOW";
        else if (speedMbps < 10.0 || latencyMs > 150) return "SLOW";
        else if (speedMbps < 50.0 || latencyMs > 100) return "MEDIUM";
        else return "FAST";
    }

    private OptimizationParams buildFromLearned(IntelligentModelParametersEntity learned, boolean isAlreadyCompressed) {
        int compression = isAlreadyCompressed ? 1 : learned.getOptimalCompressionLevel();
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
