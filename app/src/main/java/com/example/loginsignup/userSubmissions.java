package com.example.loginsignup;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent; // 🔹 Added for In-App Browser
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // 🔹 Added for lifecycle safety
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

public class userSubmissions extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout submissionsContainer;
    private ListenerRegistration submissionsListener; // 🔹 Track the listener

    private String pendingReplaceId;
    private static final int PICK_FILE_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_submissions);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        submissionsContainer = findViewById(R.id.submissionsContainer);

        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadSubmissions();
    }

    private void loadSubmissions() {
        String uid = auth.getUid();
        if (uid == null) return;

        // 🔹 Store as a registration to remove in onDestroy
        submissionsListener = db.collection("documents")
                .whereEqualTo("uploadedBy", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (isFinishing() || isDestroyed()) return; // Safety check

                    if (error != null) {
                        toast("Error loading submissions: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        submissionsContainer.removeAllViews();
                        for (QueryDocumentSnapshot doc : value) {
                            inflateSubmissionItem(doc);
                        }
                    }
                });
    }

    private void inflateSubmissionItem(QueryDocumentSnapshot doc) {
        View itemView = getLayoutInflater().inflate(R.layout.item_submission, null);

        TextView tvName = itemView.findViewById(R.id.tvFileName);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        ProgressBar progressBar = itemView.findViewById(R.id.uploadProgress);
        Button btnView = itemView.findViewById(R.id.btnView);
        Button btnEdit = itemView.findViewById(R.id.btnEdit);
        Button btnDelete = itemView.findViewById(R.id.btnDelete);

        String docId = doc.getId();
        String fileName = doc.getString("fileName");
        String fileUrl = doc.getString("fileUrl");
        String status = doc.getString("status");

        tvName.setText(fileName != null ? fileName : "Unnamed File");
        tvStatus.setText(status != null ? status.toUpperCase() : "PENDING");

        // Progress Bar Logic
        progressBar.setVisibility("uploading".equalsIgnoreCase(status) ? View.VISIBLE : View.GONE);
        if (progressBar.getVisibility() == View.VISIBLE) progressBar.setIndeterminate(true);

        // 1. VIEW - 🔹 Now opens in-app using Custom Tabs
        btnView.setOnClickListener(v -> {
            if (fileUrl != null && !fileUrl.isEmpty()) {
                openInAppBrowser(fileUrl);
            } else {
                toast("URL not available");
            }
        });

        // 2. EDIT
        btnEdit.setOnClickListener(v -> {
            String[] options = {"Rename File", "Replace File"};
            new AlertDialog.Builder(this)
                    .setTitle("Edit Submission")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) showRenameDialog(docId, fileName);
                        else openFilePicker(docId);
                    }).show();
        });

        // 3. DELETE
        btnDelete.setOnClickListener(v -> showDeleteConfirmation(docId));

        submissionsContainer.addView(itemView);
    }

    // 🔹 Helper for In-App Browser
    private void openInAppBrowser(String url) {
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(ContextCompat.getColor(this, R.color.black));
            builder.setShowTitle(true);
            builder.build().launchUrl(this, Uri.parse(url));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    private void showDeleteConfirmation(String docId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete this submission?")
                .setPositiveButton("Delete", (d, w) -> db.collection("documents").document(docId).delete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenameDialog(String docId, String currentName) {
        EditText input = new EditText(this);
        input.setText(currentName);
        input.setPadding(40, 20, 40, 20); // Minor UI padding

        new AlertDialog.Builder(this)
                .setTitle("Rename File")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        db.collection("documents").document(docId).update("fileName", newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openFilePicker(String docId) {
        this.pendingReplaceId = docId;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadReplacement(data.getData());
        }
    }

    private void uploadReplacement(Uri fileUri) {
        if (pendingReplaceId == null) return;

        toast("Replacing file...");
        db.collection("documents").document(pendingReplaceId).update("status", "uploading");

        MediaManager.get().upload(fileUri).callback(new UploadCallback() {
            @Override
            public void onSuccess(String requestId, Map resultData) {
                String newUrl = (String) resultData.get("secure_url");
                db.collection("documents").document(pendingReplaceId)
                        .update("fileUrl", newUrl, "status", "updated");
                toast("Replacement successful");
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                db.collection("documents").document(pendingReplaceId).update("status", "error");
                toast("Upload failed");
            }

            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 🔹 CRITICAL: Clean up listener to prevent memory leaks
        if (submissionsListener != null) {
            submissionsListener.remove();
        }
    }
}