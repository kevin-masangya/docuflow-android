package com.example.loginsignup;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {

    private static final String DB_URL = "https://YOUR_PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final String DATE_FORMAT = "MM/dd/yyyy";

    private EditText etUsername, etFirstName, etLastName, etAge, etBirthdate, etAddress;
    private TextView tvUsernameError;
    private Button btnSave;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    private boolean isUsernameValid = false;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users");

        initViews();
        setupListeners();
        loadCurrentProfile();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUpdateUsername);
        etFirstName = findViewById(R.id.etUpdateFirstName);
        etLastName = findViewById(R.id.etUpdateLastName);
        etAge = findViewById(R.id.etUpdateAge);
        etBirthdate = findViewById(R.id.etUpdateBirthdate);
        etAddress = findViewById(R.id.etUpdateAddress);
        tvUsernameError = findViewById(R.id.tvUpdateUsernameError);
        btnSave = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.updateProgressBar);

        // Lock age field
        etAge.setFocusable(false);
        etAge.setInputType(InputType.TYPE_NULL);
    }

    private void setupListeners() {
        etBirthdate.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> validateAndSave());

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validateUsername(s.toString().trim());
            }
        });
    }

    /* -------------------------
       Validation Logic
    --------------------------*/

    private void validateUsername(String username) {
        if (username.length() < 4) {
            updateUsernameStatus("Minimum 4 characters required", false);
        } else if (!username.matches("^[A-Za-z]+$")) {
            updateUsernameStatus("Letters only", false);
        } else {
            updateUsernameStatus(null, true);
        }
    }

    private void updateUsernameStatus(String error, boolean isValid) {
        isUsernameValid = isValid;
        if (isValid) {
            tvUsernameError.setVisibility(View.GONE);
        } else {
            tvUsernameError.setText(error);
            tvUsernameError.setVisibility(View.VISIBLE);
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            etBirthdate.setText(sdf.format(calendar.getTime()));
            calculateAge(calendar);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void calculateAge(Calendar dob) {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--;
        etAge.setText(String.valueOf(age));
    }

    /* -------------------------
       Firebase Operations
    --------------------------*/

    private void loadCurrentProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        setLoading(true);
        usersRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                if (!snapshot.exists()) return;

                setField(snapshot, "username", etUsername);
                setField(snapshot, "firstName", etFirstName);
                setField(snapshot, "lastName", etLastName);
                setField(snapshot, "address", etAddress);

                String dob = snapshot.child("birthdate").getValue(String.class);
                if (dob != null && !dob.isEmpty()) {
                    etBirthdate.setText(dob);
                    try {
                        Calendar c = Calendar.getInstance();
                        c.setTime(sdf.parse(dob));
                        calculateAge(c);
                    } catch (ParseException ignored) {}
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { setLoading(false); }
        });
    }

    private void validateAndSave() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String username = etUsername.getText().toString().trim();
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String bDate = etBirthdate.getText().toString().trim();
        String addr = etAddress.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();

        if (!isUsernameValid || fName.isEmpty() || lName.isEmpty() || bDate.isEmpty() || addr.isEmpty()) {
            toast("Please correct all fields");
            return;
        }

        int age = ageStr.isEmpty() ? 0 : Integer.parseInt(ageStr);
        if (age < 18 || age > 60) {
            toast("Age must be between 18 and 60");
            return;
        }

        setLoading(true);

        // Check if username is taken by someone else
        usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isTaken = false;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (!child.getKey().equals(user.getUid())) {
                                isTaken = true;
                                break;
                            }
                        }

                        if (isTaken) {
                            setLoading(false);
                            etUsername.setError("Username taken");
                        } else {
                            saveToFirebase(user.getUid(), username, fName, lName, age, addr, bDate);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { setLoading(false); }
                });
    }

    private void saveToFirebase(String uid, String userN, String fName, String lName, int age, String addr, String bDate) {
        DatabaseReference ref = usersRef.child(uid);

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", userN);
        updates.put("firstName", fName);
        updates.put("lastName", lName);
        updates.put("age", age);
        updates.put("address", addr);
        updates.put("birthdate", bDate);
        updates.put("profileCompleted", true);

        ref.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Fetch role to redirect correctly
                ref.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        setLoading(false);
                        String role = snapshot.getValue(String.class);
                        redirectByRole(role);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { setLoading(false); }
                });
            } else {
                setLoading(false);
                toast("Failed to update profile");
            }
        });
    }

    private void redirectByRole(String role) {
        Class<?> target = ("admin".equalsIgnoreCase(role)) ? home_admin.class : home_staff.class;
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /* -------------------------
       Helpers
    --------------------------*/

    private void setField(DataSnapshot snap, String key, EditText et) {
        String val = snap.child(key).getValue(String.class);
        if (val != null) et.setText(val);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}