package peerlinkfilesharingsystem.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;
import peerlinkfilesharingsystem.Service.FileStorageService;

@RestController
@Slf4j
public class FileCleanUp {

    private final FileStorageService fileDownloadService;

    public FileCleanUp(FileStorageService fileStorageService) {
        this.fileDownloadService = fileStorageService;
    }

//        @Scheduled(cron = "0 * * * * *")
        @Scheduled(cron = "0 1 * * * *")
//    @Scheduled(cron = "0 0 * * * *")
    public void deleteExpiredFile(){
        log.debug("Deleting expired files");
        fileDownloadService.deleteExpiredFiles();
    }
}
