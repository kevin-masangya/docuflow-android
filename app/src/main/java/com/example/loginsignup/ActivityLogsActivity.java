package com.example.loginsignup;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ActivityLogsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private ListenerRegistration logListener;

    private final List<ActivityLogModel> logList = new ArrayList<>();
    private ActivityLogAdapter adapter;

    private static final String DB_URL =
            "https://YOUR_PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        recyclerView = findViewById(R.id.recyclerLogs);
        progressBar = findViewById(R.id.progressBarLogs);

        db = FirebaseFirestore.getInstance();

        setupRecycler();
        loadLogs();
    }

    private void setupRecycler() {
        adapter = new ActivityLogAdapter(logList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    // ================= LOAD LOGS =================
    private void loadLogs() {
        progressBar.setVisibility(View.VISIBLE);

        logListener = db.collection("activity_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener((snapshots, e) -> {

                    progressBar.setVisibility(View.GONE);

                    if (e != null || snapshots == null) return;

                    logList.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {

                        ActivityLogModel log = doc.toObject(ActivityLogModel.class);

                        String uid = log.getUserId();

                        if (uid == null || uid.isEmpty()) {
                            log.setUserName("Unknown");
                            logList.add(log);
                        } else {

                            // 🔥 fetch full name like PendingApproval
                            getFullName(uid, fullName -> {
                                log.setUserName(fullName);
                                adapter.notifyDataSetChanged();
                            });

                            logList.add(log);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    // ================= RTDB USER LOOKUP =================
    private void getFullName(String uid, final UsernameCallback callback) {

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Users")
                .child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    String fName = snapshot.child("firstName").getValue(String.class);
                    String lName = snapshot.child("lastName").getValue(String.class);

                    if (fName != null && lName != null) {
                        callback.onCallback(fName + " " + lName);
                    } else {
                        String username = snapshot.child("username").getValue(String.class);
                        callback.onCallback(username != null ? username : "Unknown User");
                    }

                } else {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (logListener != null) logListener.remove();
    }
}