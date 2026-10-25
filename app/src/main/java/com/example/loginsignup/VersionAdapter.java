package com.example.loginsignup;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VersionAdapter extends RecyclerView.Adapter<VersionAdapter.ViewHolder> {

    private final Context context;
    private final List<VersionModel> versions;

    public VersionAdapter(Context context, List<VersionModel> versions) {
        this.context = context;
        this.versions = versions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use R.layout.item_version
        View view = LayoutInflater.from(context).inflate(R.layout.item_version, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VersionModel model = versions.get(position);

        // Date formatting
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
        String dateString = sdf.format(new Date(model.getTimestamp()));

        holder.tvDate.setText("Revised: " + dateString);
        holder.tvUploader.setText("Modified by ID: " + model.getUploadedBy());

        // Button to open the specific file version
        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.getFileUrl()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return versions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvUploader;
        Button btnView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // These MUST match the XML IDs exactly
            tvDate = itemView.findViewById(R.id.tvVersionDate);
            tvUploader = itemView.findViewById(R.id.tvVersionUploader);
            btnView = itemView.findViewById(R.id.btnViewVersion);
        }
    }
}