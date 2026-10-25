package com.example.loginsignup;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {

    private final Context context;
    private final List<DocumentModel> documents;
    private final OnDocumentActionListener listener;
    private boolean isAdmin = false;

    public DocumentAdapter(Context context, List<DocumentModel> documents, OnDocumentActionListener listener) {
        this.context = context;
        this.documents = documents;
        this.listener = listener;
    }

    // ✅ Proper setter for admin role
    public void setIsAdmin(boolean admin) {
        this.isAdmin = admin;
        notifyDataSetChanged(); // refresh all items
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentModel doc = documents.get(position);
        if (doc != null) {
            holder.bind(doc, isAdmin);
        }
        ((BrowseRepositoryActivity) context).setUploader(holder.tvUploader, doc);
    }

    @Override
    public int getItemCount() {
        return documents != null ? documents.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFileName, tvUploader;
        private final Button btnReplace, btnDelete, btnHistory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUploader = itemView.findViewById(R.id.tvUploader);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            btnReplace = itemView.findViewById(R.id.btnReplace);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnHistory = itemView.findViewById(R.id.btnHistory);
        }

        public void bind(DocumentModel doc, boolean isAdmin) {
            tvFileName.setText(doc.getFileName() != null ? doc.getFileName() : "Unnamed Document");

            // 🔹 Key fix: update Delete visibility every bind
            btnDelete.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

            btnReplace.setVisibility(View.VISIBLE);
            btnHistory.setVisibility(View.VISIBLE);

            // --- Click listeners ---
            itemView.setOnClickListener(v -> openInAppBrowser(doc.getFileUrl()));

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(doc);
            });

            btnReplace.setOnClickListener(v -> {
                if (listener != null) listener.onReplaceClick(doc);
            });

            btnHistory.setOnClickListener(v -> {
                if (listener != null) listener.onHistoryClick(doc);
            });
        }
    }

    private void openInAppBrowser(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(context, "Link missing", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(ContextCompat.getColor(context, R.color.black));
            builder.setShowTitle(true);
            builder.build().launchUrl(context, Uri.parse(url));
        } catch (Exception e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    public interface OnDocumentActionListener {
        void onDeleteClick(DocumentModel doc);
        void onReplaceClick(DocumentModel doc);
        void onHistoryClick(DocumentModel doc);
    }
}