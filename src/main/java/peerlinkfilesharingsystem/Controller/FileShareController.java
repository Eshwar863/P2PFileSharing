package peerlinkfilesharingsystem.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import peerlinkfilesharingsystem.Service.FileShareService.FileShareService;

import java.io.FileNotFoundException;

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/fileshare")
public class FileShareController {
    private final FileShareService fileShareService;
    public FileShareController(FileShareService fileShareService) {
        this.fileShareService = fileShareService;
    }

    @PostMapping("share/{shareId}/link")
    public ResponseEntity<?> shareFile(@PathVariable(name = "shareId") String shareToken) {
        return fileShareService.getShareUrl(shareToken);
    }

    @PostMapping("markPublic/{transferId}")
    public ResponseEntity<?> markPublic(@PathVariable(name ="transferId" ) String transferId) {
        return fileShareService.markFileAspublic(transferId);
    }
    @PostMapping("markPrivate/{transferId}")
    public ResponseEntity<?> markPrivate(@PathVariable(name ="transferId" ) String transferId) throws FileNotFoundException {
        return fileShareService.markFileAsPrivate(transferId);
    }

}
