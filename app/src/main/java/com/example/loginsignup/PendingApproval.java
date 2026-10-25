package com.example.loginsignup;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.app.AlertDialog;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.*;

public class PendingApproval extends AppCompatActivity {

    private LinearLayout container;
    private FirebaseFirestore db;
    private Button btnViewRejected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        container = findViewById(R.id.container);
        db = FirebaseFirestore.getInstance();

        btnViewRejected = findViewById(R.id.btnViewRejected);
        btnViewRejected.setOnClickListener(v -> showRejectedDialog());

        loadPendingDocs();
    }

    /** ==================== PENDING DOCUMENTS ==================== **/
    private void loadPendingDocs() {
        db.collection("documents")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    container.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("No pending documents");
                        container.addView(tv);
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String docId = doc.getId();
                        String fileName = doc.getString("fileName");
                        String fileUrl = doc.getString("fileUrl");
                        String uploadedByUid = doc.getString("uploadedBy");

                        if (uploadedByUid == null || uploadedByUid.isEmpty()) {
                            addDocCard(docId, fileName, fileUrl, "Unknown");
                        } else {
                            getFullName(uploadedByUid, fullName ->
                                    addDocCard(docId, fileName, fileUrl, fullName)
                            );
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void addDocCard(String docId, String fileName, String fileUrl, String uploadedBy) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 24, 24, 24);
        card.setBackground(getDrawable(R.drawable.bg_card_elevated));

        // Image preview
        ImageView image = new ImageView(this);
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400
        ));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(fileUrl).into(image);

        // Text: File Name + Uploaded By
        TextView tv = new TextView(this);
        tv.setText("📄 " + fileName + "\n👤 " + uploadedBy);

        // Buttons
        Button btnApprove = new Button(this);
        btnApprove.setText("Approve");
        btnApprove.setOnClickListener(v -> updateStatus(docId, "approved"));

        Button btnReject = new Button(this);
        btnReject.setText("Reject");
        btnReject.setOnClickListener(v -> updateStatus(docId, "rejected"));

        card.addView(image);
        card.addView(tv);
        card.addView(btnApprove);
        card.addView(btnReject);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);

        container.addView(card, params);
    }

    /** ==================== REJECTED DOCUMENTS ==================== **/
    private void showRejectedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Archived / Rejected Documents");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        scrollView.addView(layout);
        builder.setView(scrollView);

        db.collection("documents")
                .whereEqualTo("status", "rejected")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("No rejected documents");
                        layout.addView(tv);
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String docId = doc.getId();
                        String fileName = doc.getString("fileName");
                        String fileUrl = doc.getString("fileUrl");
                        String uploadedByUid = doc.getString("uploadedBy");

                        if (uploadedByUid == null || uploadedByUid.isEmpty()) {
                            addRejectedCard(layout, docId, fileName, fileUrl, "Unknown");
                        } else {
                            getFullName(uploadedByUid, fullName ->
                                    addRejectedCard(layout, docId, fileName, fileUrl, fullName)
                            );
                        }
                    }
                });

        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void addRejectedCard(LinearLayout layout, String docId, String fileName, String fileUrl, String uploadedBy) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        card.setBackground(getDrawable(R.drawable.bg_card_elevated));

        ImageView image = new ImageView(this);
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 300
        ));
        Glide.with(this).load(fileUrl).into(image);

        TextView tv = new TextView(this);
        tv.setText("📄 " + fileName + "\n👤 " + uploadedBy);

        Button btnRestore = new Button(this);
        btnRestore.setText("Restore");
        btnRestore.setOnClickListener(v -> {
            updateStatus(docId, "approved");
            Toast.makeText(this, "Restored", Toast.LENGTH_SHORT).show();
        });

        Button btnDelete = new Button(this);
        btnDelete.setText("Delete Permanently");
        btnDelete.setOnClickListener(v -> db.collection("documents").document(docId)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                ));

        card.addView(image);
        card.addView(tv);
        card.addView(btnRestore);
        card.addView(btnDelete);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);

        layout.addView(card, params);
    }

    /** ==================== HELPER FUNCTIONS ==================== **/
    private void updateStatus(String docId, String status) {
        db.collection("documents")
                .document(docId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Updated to " + status, Toast.LENGTH_SHORT).show();
                    loadPendingDocs();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    /** ✅ ROBUST FULLNAME FETCH FUNCTION **/
    private void getFullName(String uid, final UsernameCallback callback) {
        // 1. Point to your Realtime Database URL
        String DB_URL = "https://YOUR_PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app/";
        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Users")
                .child(uid);

        // 2. Fetch from Realtime Database instead of Firestore
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fName = snapshot.child("firstName").getValue(String.class);
                    String lName = snapshot.child("lastName").getValue(String.class);

                    if (fName != null && lName != null) {
                        callback.onCallback(fName + " " + lName);
                    } else {
                        String user = snapshot.child("username").getValue(String.class);
                        callback.onCallback(user != null ? user : "Unknown User");
                    }
                } else {
                    // This happens because the UID exists in Auth, but not in the "Users" node of RTDB
                    callback.onCallback("User Not Found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onCallback("Error");
            }
        });
    }

    private interface UsernameCallback {
        void onCallback(String username);
    }
}