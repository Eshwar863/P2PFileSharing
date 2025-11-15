package peerlinkfilesharingsystem.Enums;

public enum UploadStatus {
    PENDING,      // Just created, not started
    IN_PROGRESS,  // Currently uploading
    COMPLETED,    // Successfully finished
    FAILED,       // Failed and marked for failure
    PAUSED,       // Paused, can resume
    CANCELLED     // User cancelled
}
