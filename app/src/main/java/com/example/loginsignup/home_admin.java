package com.example.loginsignup;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;

public class home_admin extends AppCompatActivity {

    private TextView tvWelcome, statDocuments, statPending, statActivityToday;
    private Button btnGoUpdateProfile, btnLogout, btnPendingApprovals, btnBrowseRepository, btnActivityLogs,
            btnUploadDocument, btnManageUsers;

    private static final String CLOUD_NAME = "YOUR_CLOUDINARY_CLOUD_NAME";
    private static final String UPLOAD_PRESET = "YOUR_CLOUDINARY_UPLOAD_PRESET";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<String> filePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String fileName = resolveFileName(uri);
                    ActivityLogger.log("SELECT_FILE_FOR_UPLOAD", "N/A", fileName);
                    toast("Uploading...");
                    uploadToCloudinary(uri, fileName);
                } else {
                    toast("No file selected");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupGoogleSignInClient();
        loadUserInfo();
        loadStatsRealtime();
        initCloudinary();
    }

    private void initViews() {
        btnManageUsers = findViewById(R.id.btnManageUsers);
        tvWelcome = findViewById(R.id.tvWelcome);
        statDocuments = findViewById(R.id.statDocuments);
        statPending = findViewById(R.id.statPending);
        statActivityToday = findViewById(R.id.statActivityToday);
        btnGoUpdateProfile = findViewById(R.id.btnGoUpdateProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnPendingApprovals = findViewById(R.id.btnPendingApprovals);
        btnBrowseRepository = findViewById(R.id.btnBrowseRepository);
        btnActivityLogs = findViewById(R.id.btnActivityLogs);
        btnUploadDocument = findViewById(R.id.btnUploadDocument);

        btnManageUsers.setOnClickListener(v ->
                startActivity(new Intent(home_admin.this, Browse_User.class)));

        btnActivityLogs.setOnClickListener(v ->
                startActivity(new Intent(home_admin.this, ActivityLogsActivity.class)));

        btnGoUpdateProfile.setOnClickListener(v ->
                startActivity(new Intent(home_admin.this, UpdateProfileActivity.class)));

        btnLogout.setOnClickListener(v -> logout());

        btnPendingApprovals.setOnClickListener(v ->
                startActivity(new Intent(home_admin.this, PendingApproval.class)));

        btnBrowseRepository.setOnClickListener(v ->
                startActivity(new Intent(home_admin.this, BrowseRepositoryActivity.class)));

        btnUploadDocument.setOnClickListener(v -> filePicker.launch("*/*"));
    }

    private void setupGoogleSignInClient() {
        googleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build());
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(home_admin.this, LoginActivity.class));
            finish();
            return;
        }

        String uid = user.getUid();

        // User profiles live in Realtime Database, while documents and audit logs
        // live in Firestore. Read the admin's name from the correct store.
        DatabaseReference usersRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                String username = snapshot.child("username").getValue(String.class);

                String displayName;
                if (firstName != null && !firstName.isEmpty()) {
                    displayName = firstName;
                    if (lastName != null && !lastName.isEmpty()) {
                        displayName += " " + lastName;
                    }
                } else if (username != null && !username.isEmpty()) {
                    displayName = username;
                } else {
                    displayName = "Admin";
                }

                tvWelcome.setText("Welcome, " + displayName + "!");
                ActivityLogger.log("LOGIN", "N/A", "N/A");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvWelcome.setText("Welcome, Admin!");
                toast("Failed to load user info: " + error.getMessage());
            }
        });
    }

    private void loadStatsRealtime() {
        // Approved documents
        db.collection("documents")
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> statDocuments.setText(value != null ? String.valueOf(value.size()) : "0"));

        // Pending documents
        db.collection("documents")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> statPending.setText(value != null ? String.valueOf(value.size()) : "0"));

        // Today's activity
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        db.collection("activity_logs")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .addSnapshotListener((value, error) -> statActivityToday.setText(value != null ? String.valueOf(value.size()) : "0"));
    }

    private void logout() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) ActivityLogger.log("LOGOUT", "N/A", "N/A");

        mAuth.signOut();
        googleSignInClient.signOut();

        Intent intent = new Intent(home_admin.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // ================= CLOUDINARY UPLOAD =================

    private void uploadToCloudinary(Uri fileUri, String fileName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            toast("User not logged in");
            ActivityLogger.log("UPLOAD_FAILED", "N/A", fileName + " - User not logged in");
            return;
        }

        String uid = user.getUid();
        logDebug("UPLOAD", "Starting upload for: " + fileName);

        MediaManager.get().upload(fileUri)
                .unsigned(UPLOAD_PRESET)
                .option("resource_type", "auto")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) { logDebug("UPLOAD", "Upload started"); }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        Object urlObj = resultData.get("secure_url");
                        if (urlObj != null) {
                            String fileUrl = urlObj.toString();
                            saveToFirestore(fileName, fileUrl, uid);
                            ActivityLogger.log("UPLOAD_DOCUMENT", "N/A", fileName);
                        } else {
                            handleUploadError(fileName, "URL null after upload");
                        }
                    }

                    @Override
                    public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                        handleUploadError(fileName, error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                        logDebug("UPLOAD", "Rescheduled: " + fileName);
                    }
                })
                .dispatch();
    }

    private void saveToFirestore(String fileName, String fileUrl, String uid) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("fileName", fileName);
        doc.put("fileUrl", fileUrl);
        doc.put("uploadedBy", uid);

        doc.put("status", "pending");
        doc.put("timestamp", System.currentTimeMillis());

        db.collection("documents")
                .add(doc)
                .addOnSuccessListener(ref -> toast("Upload successful"))
                .addOnFailureListener(e -> handleUploadError(fileName, e.getMessage()));
    }

    private void handleUploadError(String fileName, String message) {
        logError("UPLOAD", message);
        toast("Upload failed");
        ActivityLogger.log("UPLOAD_FAILED", "N/A", fileName + " - " + message);
    }

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", CLOUD_NAME);

        try {
            MediaManager.init(this, config);
            logDebug("CLOUDINARY", "Initialized");
        } catch (Exception e) {
            logError("CLOUDINARY", "Init failed: " + e.getMessage());
        }
    }

    /**
     * Keeps the original filename instead of storing an opaque DOC_timestamp
     * value. Falls back to a timestamped name for providers that do not expose
     * a display name.
     */
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

    private void logDebug(String tag, String msg) { Log.d(tag, msg); }

    private void logError(String tag, String msg) { Log.e(tag, msg); }
}