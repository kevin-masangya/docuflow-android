package com.example.loginsignup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.*;
import androidx.recyclerview.widget.*;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.Query;

import java.util.*;

public class BrowseRepositoryActivity extends AppCompatActivity {

    private static final String TAG = "APP_DEBUG";
    private static final String DB_URL =
            "https://YOUR_PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app/";

    // UI
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private EditText searchBar;
    private TextView tvEmptyState;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration documentListener;

    // Data
    private final List<DocumentModel> documentList = new ArrayList<>();
    private final List<DocumentModel> fullList = new ArrayList<>();

    private DocumentAdapter adapter;
    private boolean isAdmin = false;

    // 🔥 ONLY ID (no object reference anymore)
    private String docToReplaceId = null;

    // ================= FILE PICKER =================
    private final ActivityResultLauncher<String> filePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {

                Log.d(TAG, "File picker returned: " + uri);

                if (uri == null) {
                    toast("No file selected");
                    return;
                }

                if (docToReplaceId == null) {
                    Log.e(TAG, "No document selected");
                    toast("Error: No document selected");
                    return;
                }

                DocumentModel doc = findDocById(docToReplaceId);

                if (doc == null) {
                    Log.e(TAG, "Document not found");
                    toast("Error: Document not found");
                    return;
                }

                uploadReplacement(uri, doc);
            });

    // ================= LIFECYCLE =================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_repository);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        initRecycler();
        initCloudinary();

        fetchUserRole();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (documentListener != null) documentListener.remove();
    }

    // ================= INIT =================
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        searchBar = findViewById(R.id.searchBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                filterList(s.toString());
            }
        });
    }

    private void initRecycler() {
        adapter = new DocumentAdapter(this, documentList, new DocumentAdapter.OnDocumentActionListener() {
            @Override public void onDeleteClick(DocumentModel doc) { confirmDelete(doc); }
            @Override public void onReplaceClick(DocumentModel doc) { replaceFile(doc); }
            @Override public void onHistoryClick(DocumentModel doc) { openHistory(doc); }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    // ================= ROLE =================
    private void fetchUserRole() {

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid == null) {
            loadDocuments();
            return;
        }

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("Users")
                .child(uid)
                .child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isAdmin = "admin".equalsIgnoreCase(snapshot.getValue(String.class));
                        applyAdminState();
                        loadDocuments();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isAdmin = false;
                        applyAdminState();
                        loadDocuments();
                    }
                });
    }

    private void applyAdminState() {
        if (adapter != null) adapter.setIsAdmin(isAdmin);
    }
    private void confirmDelete(DocumentModel doc) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete " + doc.getFileName() + "?")
                .setPositiveButton("Yes", (d, w) -> {

                    db.collection("documents")
                            .document(doc.getDocumentId())
                            .delete()
                            .addOnSuccessListener(a -> {
                                toast("Deleted");

                                ActivityLogger.log(
                                        "DELETE_DOCUMENT",
                                        doc.getDocumentId(),
                                        doc.getFileName()
                                );
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Delete failed", e);
                                toast("Delete failed ❌");
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }
    public void setUploader(TextView tvUploader, DocumentModel doc) {

        String uid = doc.getUploadedBy();

        if (uid == null || uid.isEmpty()) {
            tvUploader.setText("👤 Unknown");
            return;
        }

        tvUploader.setText("👤 Loading...");

        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Users")
                .child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    tvUploader.setText("👤 Unknown");
                    return;
                }

                String first = snapshot.child("firstName").getValue(String.class);
                String last = snapshot.child("lastName").getValue(String.class);

                if (first != null && last != null) {
                    tvUploader.setText("👤 " + first + " " + last);
                } else {
                    String username = snapshot.child("username").getValue(String.class);
                    tvUploader.setText("👤 " + (username != null ? username : "Unknown"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvUploader.setText("👤 Error");
            }
        });
    }
    // ================= DOCUMENTS =================
    private void loadDocuments() {

        if (documentListener != null) documentListener.remove();

        progressBar.setVisibility(View.VISIBLE);

        documentListener = db.collection("documents")
                .whereEqualTo("status", "approved")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {

                    progressBar.setVisibility(View.GONE);

                    if (e != null || snapshots == null) {
                        Log.e(TAG, "Error loading docs", e);
                        updateEmptyState();
                        return;
                    }

                    documentList.clear();
                    fullList.clear();

                    for (QueryDocumentSnapshot d : snapshots) {
                        DocumentModel model = d.toObject(DocumentModel.class);
                        model.setDocumentId(d.getId());

                        documentList.add(model);
                        fullList.add(model);
                    }

                    updateEmptyState();
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateEmptyState() {
        boolean isEmpty = documentList.isEmpty();
        if (isEmpty) {
            boolean isSearching = !searchBar.getText().toString().trim().isEmpty();
            tvEmptyState.setText(isSearching
                    ? "No documents match your search."
                    : "No approved documents yet.\nUploads appear here after an admin approves them.");
        }
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // ================= ACTIONS =================
    private void replaceFile(DocumentModel doc) {
        docToReplaceId = doc.getDocumentId();
        Log.d(TAG, "Replacing doc: " + docToReplaceId);
        filePicker.launch("*/*");
    }

    private void uploadReplacement(Uri uri, DocumentModel doc) {

        toast("Uploading...");
        Log.d(TAG, "Uploading for: " + doc.getDocumentId());

        MediaManager.get().upload(uri)
                .unsigned("YOUR_CLOUDINARY_UPLOAD_PRESET")
                .callback(new UploadCallback() {

                    @Override public void onStart(String id) {
                        Log.d(TAG, "Upload started");
                    }

                    @Override
                    public void onSuccess(String id, Map data) {

                        Log.d(TAG, "Upload success: " + data);

                        Object url = data.get("secure_url");

                        if (url != null) {
                            saveReplacement(doc, url.toString());
                        } else {
                            toast("Upload failed (no URL)");
                        }
                    }

                    @Override
                    public void onError(String id, com.cloudinary.android.callback.ErrorInfo e) {
                        Log.e(TAG, "Upload error: " + e.getDescription());
                        toast("Upload failed ❌");
                    }

                    @Override public void onProgress(String id, long b, long t) {}
                    @Override public void onReschedule(String id, com.cloudinary.android.callback.ErrorInfo e) {}
                })
                .dispatch();
    }

    private void saveReplacement(DocumentModel doc, String newUrl) {

        String uid = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : "unknown";

        WriteBatch batch = db.batch();

        DocumentReference docRef = db.collection("documents").document(doc.getDocumentId());
        DocumentReference historyRef = docRef.collection("versions").document();

        batch.set(historyRef, new VersionModel(
                doc.getFileUrl(),
                System.currentTimeMillis(),
                uid
        ));

        batch.update(docRef,
                "fileUrl", newUrl,
                "timestamp", System.currentTimeMillis()
        );

        batch.commit()
                .addOnSuccessListener(a -> {
                    toast("File replaced ✅");

                    ActivityLogger.log(
                            "REPLACE_DOCUMENT",
                            doc.getDocumentId(),
                            doc.getFileName()
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error", e);
                    toast("Failed to update ❌");
                });
    }

    // ================= HELPERS =================
    private DocumentModel findDocById(String id) {
        for (DocumentModel d : fullList) {
            if (d.getDocumentId().equals(id)) return d;
        }
        return null;
    }

    private void filterList(String text) {
        documentList.clear();

        if (text.isEmpty()) {
            documentList.addAll(fullList);
        } else {
            for (DocumentModel d : fullList) {
                if (d.getFileName() != null &&
                        d.getFileName().toLowerCase().contains(text.toLowerCase())) {
                    documentList.add(d);
                }
            }
        }

        updateEmptyState();
        adapter.notifyDataSetChanged();
    }

    private void openHistory(DocumentModel doc) {
        Intent intent = new Intent(this, VersionHistoryActivity.class);
        intent.putExtra("docId", doc.getDocumentId());
        startActivity(intent);
    }

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "YOUR_CLOUDINARY_CLOUD_NAME");

        try {
            MediaManager.init(this, config);
        } catch (Exception e) {
            Log.e(TAG, "Cloudinary init error", e);
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}