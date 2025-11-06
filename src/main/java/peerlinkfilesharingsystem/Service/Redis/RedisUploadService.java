package peerlinkfilesharingsystem.Service.Redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Set;

@Slf4j
@Service
public class RedisUploadService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${upload.session.ttl:86400}")
    private long sessionTTL;

    public RedisUploadService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public void createSession(String uploadId, String fileName, long totalChunks, String userId) {
        String key = "upload:session:" + uploadId;
        redisTemplate.opsForHash().put(key, "fileName", fileName);
        redisTemplate.opsForHash().put(key, "totalChunks", totalChunks);
        redisTemplate.opsForHash().put(key, "uploadedChunks", 0);
        redisTemplate.opsForHash().put(key, "status", "IN_PROGRESS");
        redisTemplate.opsForHash().put(key, "userId", userId);

        redisTemplate.expire(key, Duration.ofSeconds(sessionTTL));
        log.info("Redis session created for uploadId: {}", uploadId);
    }

    public void markChunkUploaded(String uploadId, int chunkNumber) {
        String chunkKey = "upload:session:" + uploadId + ":chunks";
        redisTemplate.opsForSet().add(chunkKey, chunkNumber);
        redisTemplate.expire(chunkKey, Duration.ofSeconds(sessionTTL));
        log.info("Marked chunk {} as uploaded for {}", chunkNumber, uploadId);
    }

    public long getUploadedChunkCount(String uploadId) {
        String chunkKey = "upload:session:" + uploadId + ":chunks";
        return redisTemplate.opsForSet().size(chunkKey);
    }

    public Set<Object> getUploadedChunks(String uploadId) {
        return redisTemplate.opsForSet().members("upload:session:" + uploadId + ":chunks");
    }

    public void markUploadComplete(String uploadId) {
        String key = "upload:session:" + uploadId;
        redisTemplate.opsForHash().put(key, "status", "COMPLETED");
        log.info("Upload {} marked as completed", uploadId);
    }

    public void clearSession(String uploadId) {
        redisTemplate.delete("upload:session:" + uploadId);
        redisTemplate.delete("upload:session:" + uploadId + ":chunks");
        log.info("Cleared Redis session for {}", uploadId);
    }
}
