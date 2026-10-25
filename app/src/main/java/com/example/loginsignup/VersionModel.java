package com.example.loginsignup;

public class VersionModel {
    private String fileUrl;
    private long timestamp;
    private String uploadedBy;

    public VersionModel() {} // 🔹 Required for Firestore

    public VersionModel(String fileUrl, long timestamp, String uploadedBy) {
        this.fileUrl = fileUrl;
        this.timestamp = timestamp;
        this.uploadedBy = uploadedBy;
    }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}