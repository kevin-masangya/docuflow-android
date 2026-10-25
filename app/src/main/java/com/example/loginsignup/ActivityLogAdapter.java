package com.example.loginsignup;

import android.view.*;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.ViewHolder> {

    private List<ActivityLogModel> list;

    public ActivityLogAdapter(List<ActivityLogModel> list) {
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_logs, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int i) {
        ActivityLogModel log = list.get(i);

        h.action.setText(formatAction(log.getAction()));
        h.file.setText("File: " + log.getFileName());

        // 🔥 FIXED: show full name instead of UID
        String userDisplay = log.getUserName();

        if (userDisplay == null || userDisplay.isEmpty()) {
            userDisplay = log.getUid(); // fallback
        }

        h.uid.setText("User: " + userDisplay);

        h.time.setText(formatTime(log.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView action, file, uid, time;

        ViewHolder(View v) {
            super(v);
            action = v.findViewById(R.id.logAction);
            file = v.findViewById(R.id.logFile);
            uid = v.findViewById(R.id.logUid);
            time = v.findViewById(R.id.logTime);
        }
    }

    private String formatTime(long time) {
        return new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                .format(new Date(time));
    }

    private String formatAction(String action) {
        switch (action) {
            case "DELETE_DOCUMENT": return "🗑 Deleted Document";
            case "REPLACE_DOCUMENT": return "♻ Replaced Document";
            case "VIEW_HISTORY": return "📜 Viewed History";
            case "SEARCH": return "🔍 Searched";
            case "OPEN_REPOSITORY_PAGE": return "📂 Opened Repository";
            default: return action;
        }
    }
}