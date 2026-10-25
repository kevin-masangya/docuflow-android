package com.example.loginsignup;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class VersionHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private VersionAdapter adapter;
    private List<VersionModel> versionList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version_history);

        // 1. Initialize UI Elements
        recyclerView = findViewById(R.id.recyclerViewHistory);
        progressBar = findViewById(R.id.historyProgressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // SAFETY CHECK: If XML ID doesn't match, stop here to avoid crash
        if (recyclerView == null) {
            Toast.makeText(this, "Error: RecyclerView not found in layout", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        versionList = new ArrayList<>();
        adapter = new VersionAdapter(this, versionList);
        recyclerView.setAdapter(adapter);

        // 3. Get Data from Intent
        db = FirebaseFirestore.getInstance();
        String docId = getIntent().getStringExtra("docId");

        if (docId != null) {
            loadVersions(docId);
        } else {
            Toast.makeText(this, "Error: Document ID is missing", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no ID provided
        }
    }

    private void loadVersions(String docId) {
        // Show loading spinner
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("documents")
                .document(docId)
                .collection("versions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    // Hide loading spinner
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    if (e != null) {
                        Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        versionList.clear();
                        for (DocumentSnapshot doc : snapshots) {
                            VersionModel vm = doc.toObject(VersionModel.class);
                            if (vm != null) {
                                versionList.add(vm);
                            }
                        }

                        // Toggle Empty State View
                        if (versionList.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }
}