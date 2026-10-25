package com.example.loginsignup;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // 🔹 For Real-time cleanup

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class home_staff extends AppCompatActivity {

    private TextView tvStaffName, tvStatMyDocs, tvStatPending, tvStatApproved;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // 🔹 Track listeners to stop them when the app closes
    private ListenerRegistration repoListener, pendingListener, userApprovedListener;

    private static final String CLOUD_NAME = "YOUR_CLOUDINARY_CLOUD_NAME";
    private static final String UPLOAD_PRESET = "YOUR_CLOUDINARY_UPLOAD_PRESET";
    private static final String DB_URL = "https://YOUR_PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) uploadToCloudinary(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_staff);

        initFirebase();
        initCloudinary();
        initViews();
        setListeners();
        loadUserProfile();

        // 🔹 Start the real-time counters
        startRealTimeStats();
    }

    private void initViews() {
        tvStaffName = findViewById(R.id.tvStaffName);
        tvStatMyDocs = findViewById(R.id.tvStatMyDocs);     // Total Approved in Repo
        tvStatPending = findViewById(R.id.tvStatPending);   // User's Pending files
        tvStatApproved = findViewById(R.id.tvStatApproved); // User's Approved files
    }

    // 🔹 REAL-TIME STATS LOGIC
    private void startRealTimeStats() {
        String uid = auth.getUid();
        if (uid == null) return;

        // 1. Total Approved Documents (Global Repository)
        repoListener = db.collection("documents")
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvStatMyDocs.setText(String.valueOf(value.size()));
                    }
                });

        // 2. Specific User's Pending Documents
        pendingListener = db.collection("documents")
                .whereEqualTo("uploadedBy", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvStatPending.setText(String.valueOf(value.size()));
                    }
                });

        // 3. Specific User's Approved Documents
        userApprovedListener = db.collection("documents")
                .whereEqualTo("uploadedBy", uid)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvStatApproved.setText(String.valueOf(value.size()));
                    }
                });
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Users")
                .child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String displayName = (firstName != null && !firstName.isEmpty())
                            ? String.format("Hello, %s", firstName)
                            : "Hello, Staff";
                    tvStaffName.setText(displayName);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setListeners() {
        findViewById(R.id.btnActivityLogs).setOnClickListener(v ->  startActivity(new Intent(this, ActivityLogsActivity.class)));
        findViewById(R.id.btnUploadDocument).setOnClickListener(v -> openFilePicker());
        findViewById(R.id.btnBrowseRepository).setOnClickListener(v ->
                startActivity(new Intent(this, BrowseRepositoryActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnGoUpdateProfile).setOnClickListener(v ->
                startActivity(new Intent(this, UpdateProfileActivity.class)));

        findViewById(R.id.btnMySubmissions).setOnClickListener(v ->
                startActivity(new Intent(this, userSubmissions.class)));
    }

    // ... (Your existing Cloudinary and Firestore Save methods stay the same) ...

    private void openFilePicker() {
        filePicker.launch(new String[]{
                "application/pdf", "image/*", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
    }

    private void uploadToCloudinary(Uri fileUri) {
        String uid = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : "unknown";
        String fileName = resolveFileName(fileUri);
        toast("Uploading...");

        MediaManager.get().upload(fileUri)
                .unsigned(UPLOAD_PRESET)
                .option("resource_type", "auto")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String fileUrl = Objects.requireNonNull(resultData.get("secure_url")).toString();
                        saveToFirestore(fileName, fileUrl, uid);
                    }
                    @Override
                    public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                        toast("Upload failed: " + error.getDescription());
                    }
                    @Override public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirestore(String fileName, String fileUrl, String uid) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("fileName", fileName);
        doc.put("fileUrl", fileUrl);
        doc.put("uploadedBy", uid);
        doc.put("status", "pending");
        doc.put("timestamp", System.currentTimeMillis());

        db.collection("documents").add(doc)
                .addOnSuccessListener(ref -> toast("Upload successful"))
                .addOnFailureListener(e -> toast("Firestore error: " + e.getMessage()));
    }

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", CLOUD_NAME);
        try { MediaManager.init(this, config); } catch (Exception ignored) {}
    }

    /** Reads the original display name so submissions keep a meaningful title. */
    private String resolveFileName(Uri uri) {
        String displayName = null;

        if (uri != null && "content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        displayName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception ignored) {
                // Fall through to the URI/timestamp fallback below.
            }
        }

        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = uri != null ? uri.getLastPathSegment() : null;
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "DOC_" + System.currentTimeMillis();
        }
        return displayName;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 🔹 Stop listeners to save data and battery
        if (repoListener != null) repoListener.remove();
        if (pendingListener != null) pendingListener.remove();
        if (userApprovedListener != null) userApprovedListener.remove();
    }
}