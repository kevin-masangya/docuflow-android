package com.example.loginsignup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private ArrayList<User> list;
    private String DB_URL = "https://YOUR_PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public UserAdapter(Context context, ArrayList<User> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = list.get(position);

        // Uses the firstname + lastname logic from your User model
        String staffFullName = user.getFullName();

        holder.userName.setText(staffFullName);
        holder.userEmail.setText(user.getEmail());
        holder.userRole.setText(user.getRole());

        // PROMOTE / DEMOTE BUTTON
        holder.btnEditRole.setOnClickListener(v -> {
            boolean isAdmin = user.getRole().equalsIgnoreCase("Admin");
            String newRole = isAdmin ? "staff" : "admin";

            // Explicitly asks to upgrade/downgrade
            String title = isAdmin ? "Downgrade to Staff" : "Upgrade to Admin";
            String message = "Do you want to " + (isAdmin ? "remove Admin privileges from " : "upgrade ")
                    + staffFullName + " to " + newRole + "?";

            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Yes, Update", (dialog, which) -> {
                        if (user.getUserId() != null) {
                            updateUserInFirebase(user.getUserId(), "role", newRole, staffFullName + " is now " + newRole);
                        } else {
                            Toast.makeText(context, "Error: User ID missing", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // DEACTIVATE BUTTON
        holder.btnDeactivate.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Deactivate Account")
                    .setMessage("Are you sure you want to deactivate " + staffFullName + "? This will restrict their access.")
                    .setPositiveButton("Deactivate", (dialog, which) -> {
                        if (user.getUserId() != null) {
                            updateUserInFirebase(user.getUserId(), "status", "Inactive", "Account Deactivated");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // Refactored helper function to handle all updates
    private void updateUserInFirebase(String userId, String field, String value, String successMessage) {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(userId);
        ref.child(field).setValue(value).addOnSuccessListener(aVoid -> {
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userEmail, userRole;
        Button btnEditRole, btnDeactivate;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.tvUserName);
            userEmail = itemView.findViewById(R.id.tvUserEmail);
            userRole = itemView.findViewById(R.id.tvUserRole);
            btnEditRole = itemView.findViewById(R.id.btnEditRole);
            btnDeactivate = itemView.findViewById(R.id.btnDeactivate);
        }
    }
}