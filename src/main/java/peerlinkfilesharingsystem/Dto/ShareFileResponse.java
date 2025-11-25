package peerlinkfilesharingsystem.Dto;

public class ShareFileResponse {
    private String fileName;
    private Long shareport;
    private String fileDownloadUri;

    public ShareFileResponse(String fileName, String fileDownloadUri,Long port) {
        this.fileName = fileName;
        this.fileDownloadUri = fileDownloadUri;
        this.shareport = port;
    }

    public Long getPort() {
        return shareport;
    }
    public String getFileName() {
        return fileName;
    }

    public String getFileDownloadUri() {
        return fileDownloadUri;
    }
}