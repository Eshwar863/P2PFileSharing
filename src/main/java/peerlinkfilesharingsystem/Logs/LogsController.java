package peerlinkfilesharingsystem.Logs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.nio.file.*;

@RestController
@RequestMapping("/logs")
@Slf4j
public class LogsController {

    private static final String LOG_FILE = "logs/peerlink-system.log"; // change path if needed


    @GetMapping(value = "/latest", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getLatestLog() {
        try {
            String content = Files.readString(Paths.get(LOG_FILE));
            log.info("[{}] Reading the latest log file...", LOG_FILE);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body("Error reading log file: " + e.getMessage());
        }
    }

    @GetMapping(value = "/download", produces = "application/octet-stream")
    public ResponseEntity<byte[]> downloadLog() {
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(LOG_FILE));
            log.info("[{}] Downloading the latest log file...", LOG_FILE);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=peerlink-system.log")
                    .body(fileBytes);
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(null);
        }
    }
}