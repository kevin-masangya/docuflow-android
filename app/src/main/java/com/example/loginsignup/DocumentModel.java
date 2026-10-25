package com.example.loginsignup;

public class DocumentModel {
    private String documentId; // 🔹 Firestore document ID
    private String fileName;
    private String fileUrl;
    private String status;
    private long timestamp;
    private String uploadedBy;

    public DocumentModel() {} // 🔹 Required for Firestore

    public DocumentModel(String fileName, String fileUrl, String status, long timestamp, String uploadedBy) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.status = status;
        this.timestamp = timestamp;
        this.uploadedBy = uploadedBy;
    }

    // 🔹 Getter & Setter for documentId
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}