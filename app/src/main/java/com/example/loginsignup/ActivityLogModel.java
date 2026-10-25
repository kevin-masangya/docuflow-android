package com.example.loginsignup;

public class ActivityLogModel {

    // 🔥 existing field (DO NOT REMOVE - matches Firestore)
    private String uid;

    private String action;
    private String documentId;
    private String fileName;
    private long timestamp;

    // 🔥 UI/helper fields (not required in Firestore but useful)
    private String userId;
    private String userName;

    public ActivityLogModel() {}

    // ================= ORIGINAL GETTERS =================
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
        this.userId = uid; // keep both synced
    }

    public String getAction() {
        return action;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // ================= USER SUPPORT (NEW) =================
    public String getUserId() {
        return userId != null ? userId : uid;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.uid = userId; // keep synced both ways
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}