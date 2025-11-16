package peerlinkfilesharingsystem.Controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import peerlinkfilesharingsystem.Service.FileShareService.FileShareService;

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/fileshare")
public class FileShareController {
    private final FileShareService fileShareService;
    public FileShareController(FileShareService fileShareService) {
        this.fileShareService = fileShareService;
    }

    @PostMapping("share/{transferid}")
    public ResponseEntity<?> shareFile(@RequestParam String transferId) {
        return fileShareService.getUrl(transferId);
    }

    @PostMapping("markPublic/{transferid}")
    public ResponseEntity<?> markPublic(@RequestParam String transferId) {
        fileShareService.markFileAspublic(transferId);
    }

}
