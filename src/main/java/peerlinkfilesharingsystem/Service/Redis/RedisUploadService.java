package peerlinkfilesharingsystem.Service.Redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class RedisUploadService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String UPLOAD_SESSION_PREFIX = "upload:session:";
    private static final String UPLOAD_CHUNKS_PREFIX = "upload:session:";

    @Value("${upload.session.ttl:86400}")  // 24 hours default
    private long sessionTTL;

    public RedisUploadService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    /**
     * Create a new upload session in Redis
     * Called when user starts uploading
     */
    public void createSession(String uploadId, String fileName, long totalChunks, String userId) {
        String sessionKey = "upload:session:" + uploadId;

        log.info("Creating Redis session - UploadId: {}, FileName: {}, TotalChunks: {}, UserId: {}",
                uploadId, fileName, totalChunks, userId);

        redisTemplate.opsForHash().put(sessionKey, "fileName", fileName);
        redisTemplate.opsForHash().put(sessionKey, "totalChunks", totalChunks);
        redisTemplate.opsForHash().put(sessionKey, "uploadedChunks", 0);
        redisTemplate.opsForHash().put(sessionKey, "status", "IN_PROGRESS");
        redisTemplate.opsForHash().put(sessionKey, "userId", userId);
        redisTemplate.opsForHash().put(sessionKey, "createdAt", System.currentTimeMillis());
        redisTemplate.opsForHash().put(sessionKey, "lastActivity", System.currentTimeMillis());

        redisTemplate.expire(sessionKey, Duration.ofSeconds(sessionTTL));

        log.info("Redis session created: {}", sessionKey);
    }


    public void markChunksUploadedBatch(String uploadId, List<Integer> chunkNumbers) {
        if (chunkNumbers == null || chunkNumbers.isEmpty()) {
            return;
        }

        try {
            String chunksKey = UPLOAD_CHUNKS_PREFIX + uploadId + ":chunks";
            String sessionKey = UPLOAD_SESSION_PREFIX + uploadId;

            log.info("Batch marking {} chunks for upload {}", chunkNumbers.size(), uploadId);

            // Use pipeline for efficient batch operations
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                // Add all chunks to set
                for (Integer chunkNumber : chunkNumbers) {
                    connection.sAdd(chunksKey.getBytes(),
                            String.valueOf(chunkNumber).getBytes());
                }

                // Update uploaded count
                connection.hSet(sessionKey.getBytes(),
                        "uploadedChunks".getBytes(),
                        String.valueOf(chunkNumbers.size()).getBytes());

                return null;
            });

            log.info("Successfully batch marked {} chunks", chunkNumbers.size());

        } catch (Exception e) {
            log.error("Error batch marking chunks", e);
        }
    }

    public void markChunkFailed(String uploadId, int chunkNumber) {
        String failedChunksKey = "upload:session:" + uploadId + ":failed_chunks";

        redisTemplate.opsForSet().add(failedChunksKey, chunkNumber);
        redisTemplate.expire(failedChunksKey, Duration.ofSeconds(sessionTTL));

        log.warn("Marked chunk {} as failed for upload {}", chunkNumber, uploadId);
    }

    /**
     * Get all successfully uploaded chunks
     * Returns: Set of chunk numbers [0, 1, 2, 5, 7, ...] (not necessarily sequential)
     */
    public Set<Object> getUploadedChunks(String uploadId) {
        String chunkSetKey = "upload:session:" + uploadId + ":chunks";

        Set<Object> chunks = redisTemplate.opsForSet().members(chunkSetKey);
        log.info("Retrieved {} uploaded chunks for {}", chunks != null ? chunks.size() : 0, uploadId);

        return chunks != null ? chunks : new HashSet<>();
    }


    public long getUploadedChunkCount(String uploadId) {
        String chunkSetKey = "upload:session:" + uploadId + ":chunks";
        Long count = redisTemplate.opsForSet().size(chunkSetKey);
        return count != null ? count : 0;
    }

    public Set<Object> getFailedChunks(String uploadId) {
        String failedChunksKey = "upload:session:" + uploadId + ":failed_chunks";
        Set<Object> chunks = redisTemplate.opsForSet().members(failedChunksKey);
        return chunks != null ? chunks : new HashSet<>();
    }

    /**
     * Get missing chunks that still need to be uploaded
     * Returns: List of chunk numbers to upload
     */
    public List<Integer> getMissingChunks(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;

        // Get total chunks
        Object totalChunksObj = redisTemplate.opsForHash().get(sessionKey, "totalChunks");
        if (totalChunksObj == null) {
            log.warn("Session not found: {}", uploadId);
            return new ArrayList<>();
        }

        long totalChunks = Long.parseLong(totalChunksObj.toString());

        // Get uploaded chunks
        Set<Object> uploadedChunksSet = getUploadedChunks(uploadId);

        // Convert to integers
        Set<Integer> uploadedChunks = uploadedChunksSet.stream()
                .map(chunk -> Integer.parseInt(chunk.toString()))
                .collect(Collectors.toSet());

        // Find missing chunks
        List<Integer> missingChunks = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!uploadedChunks.contains(i)) {
                missingChunks.add(i);
            }
        }

        log.info("Missing {} chunks out of {} for upload {}",
                missingChunks.size(), totalChunks, uploadId);

        return missingChunks;
    }

    /**
     * Get percentage of upload complete
     */
    public double getUploadProgress(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;

        Object totalChunksObj = redisTemplate.opsForHash().get(sessionKey, "totalChunks");
        if (totalChunksObj == null) {
            return 0.0;
        }

        long totalChunks = Long.parseLong(totalChunksObj.toString());
        long uploadedCount = getUploadedChunkCount(uploadId);

        return (uploadedCount * 100.0) / totalChunks;
    }


    /**
     * Pause an active upload
     * User can resume later
     */
    public void pauseUpload(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;

        redisTemplate.opsForHash().put(sessionKey, "status", "PAUSED");
        redisTemplate.opsForHash().put(sessionKey, "lastActivity", System.currentTimeMillis());

        log.info("Upload paused: {}", uploadId);
    }

    /**
     * Resume a paused upload
     */
    public void resumeUpload(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;

        redisTemplate.opsForHash().put(sessionKey, "status", "IN_PROGRESS");
        redisTemplate.opsForHash().put(sessionKey, "lastActivity", System.currentTimeMillis());

        log.info("Upload resumed: {}", uploadId);
    }

    /**
     * Mark upload as completed
     */
    public void markUploadComplete(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;

        redisTemplate.opsForHash().put(sessionKey, "status", "COMPLETED");
        redisTemplate.opsForHash().put(sessionKey, "completedAt", System.currentTimeMillis());

        log.info("Upload marked as completed: {}", uploadId);
    }

    /**
     * Get current status of upload
     */
    public String getUploadStatus(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;
        Object status = redisTemplate.opsForHash().get(sessionKey, "status");

        return status != null ? status.toString() : "NOT_FOUND";
    }

    /**
     * Get full session info
     */
    public Map<Object, Object> getSessionInfo(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;
        return redisTemplate.opsForHash().entries(sessionKey);
    }

    // ==================== CLEANUP ====================

    /**
     * Clear session when upload is fully completed or abandoned
     */
    public void clearSession(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;
        String chunkSetKey = "upload:session:" + uploadId + ":chunks";
        String failedChunksKey = "upload:session:" + uploadId + ":failed_chunks";

        redisTemplate.delete(sessionKey);
        redisTemplate.delete(chunkSetKey);
        redisTemplate.delete(failedChunksKey);

        log.info("Cleared Redis session for upload: {}", uploadId);
    }

    /**
     * Clear abandoned sessions (e.g., user never resumed)
     * Manual cleanup job
     */
    public void clearAbandonedSessions(long timeoutSeconds) {
        log.info("Clearing abandoned sessions (timeout: {}s)", timeoutSeconds);

        // This would require scanning keys - for production, use Redis Streams or a cleanup job
        // For now, TTL auto-expiration handles this

        log.info("Abandoned sessions cleaned up");
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if session exists
     */
    public boolean sessionExists(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
    }

    /**
     * Get time since last activity
     */
    public long getTimeSinceLastActivity(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;
        Object lastActivityObj = redisTemplate.opsForHash().get(sessionKey, "lastActivity");

        if (lastActivityObj == null) {
            return -1;
        }

        long lastActivity = Long.parseLong(lastActivityObj.toString());
        return System.currentTimeMillis() - lastActivity;
    }

    /**
     * Verify all chunks uploaded
     */
    public boolean isUploadComplete(String uploadId) {
        String sessionKey = "upload:session:" + uploadId;

        Object totalChunksObj = redisTemplate.opsForHash().get(sessionKey, "totalChunks");
        if (totalChunksObj == null) {
            return false;
        }

        long totalChunks = Long.parseLong(totalChunksObj.toString());
        long uploadedCount = getUploadedChunkCount(uploadId);

        return totalChunks == uploadedCount;
    }
    public void markUploadCompleteWithAllChunks(String uploadId, long totalChunks) {
        try {
            log.info("Marking upload {} complete with {} total chunks", uploadId, totalChunks);

            // Generate list of all chunk numbers
            List<Integer> allChunks = IntStream.range(0, (int) totalChunks)
                    .boxed()
                    .toList();

            // Mark all chunks in batch
            markChunksUploadedBatch(uploadId, allChunks);

            // Mark session as complete
            markUploadComplete(uploadId);

            log.info("Upload {} marked complete successfully", uploadId);

        } catch (Exception e) {
            log.error("Error marking upload complete", e);
        }
    }
}
