package com.example.loginsignup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ActivityLogger {

    public static void log(String action, String documentId, String fileName) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        String uid = (auth.getCurrentUser() != null)
                ? auth.getCurrentUser().getUid()
                : "anonymous";

        Map<String, Object> log = new HashMap<>();
        log.put("uid", uid);
        log.put("action", action);
        log.put("documentId", documentId);
        log.put("fileName", fileName);
        log.put("timestamp", System.currentTimeMillis());

        db.collection("activity_logs").add(log);
    }
}