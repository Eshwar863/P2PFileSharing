package peerlinkfilesharingsystem.Dto;

public class ShareFileResponse {
    private String fileName;
    private String fileDownloadUri;

    public ShareFileResponse(String fileName, String fileDownloadUri) {
        this.fileName = fileName;
        this.fileDownloadUri = fileDownloadUri;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileDownloadUri() {
        return fileDownloadUri;
    }
}