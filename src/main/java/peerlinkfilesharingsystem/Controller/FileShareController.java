package peerlinkfilesharingsystem.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import peerlinkfilesharingsystem.Dto.EmailFileRequest;
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

        @PostMapping("fileStatus/{transferId}")
    public ResponseEntity<?> fileStatus(@PathVariable(name ="transferId" ) String transferId) throws FileNotFoundException {
        return fileShareService.markedStatus(transferId);
    }

    @PostMapping("share/file/email")
    public ResponseEntity<?> ShareFileToEmail(@RequestBody EmailFileRequest emailFileRequest ) {
        if (emailFileRequest.getEmail() == null) {
            return new ResponseEntity<>("Email is missing or invalid", HttpStatus.BAD_REQUEST);
        }
        return fileShareService.sendLinkToEmail(emailFileRequest);
    }



}
