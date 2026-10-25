package com.example.loginsignup;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SignupActivity extends AppCompatActivity {

    private EditText etUsername, etFirstName, etLastName, etAge, etEmail, etPassword, etConfirmPassword, etAddress, etBirthdate;
    private TextView tvUsernameError, tvPasswordError, tvReqUpper, tvReqSymbol, tvReqNumber, tvReqLength, tvTermsError, tvGoToLogin;
    private CheckBox cbTerms;
    private ImageView ivTogglePassword, ivToggleConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private boolean isUsernameValid = false;
    private boolean isPasswordValid = false;
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase
                .getInstance("https://YOUR_PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");

        initViews();
        setupUsernameValidation();
        setupPasswordValidation();
        setupPasswordToggle();

        // Setup Birthdate Picker
        etBirthdate.setOnClickListener(v -> showDatePicker());

        tvGoToLogin.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> attemptRegister());

        etAge.setFocusable(false);
        etAge.setClickable(false);
        etAge.setCursorVisible(false);
        etAge.setInputType(InputType.TYPE_NULL);
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etAge = findViewById(R.id.etAge);
        etAddress = findViewById(R.id.etAddress);
        etBirthdate = findViewById(R.id.etBirthdate);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tvUsernameError = findViewById(R.id.tvUsernameError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        tvReqUpper = findViewById(R.id.tvReqUpper);
        tvReqSymbol = findViewById(R.id.tvReqSymbol);
        tvReqNumber = findViewById(R.id.tvReqNumber);
        tvReqLength = findViewById(R.id.tvReqLength);
        tvTermsError = findViewById(R.id.tvTermsError);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        cbTerms = findViewById(R.id.cbTerms);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            // Format date for display
            String myFormat = "MM/dd/yyyy";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
            etBirthdate.setText(sdf.format(calendar.getTime()));

            // Auto-compute Age
            calculateAge(year, month, dayOfMonth);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void calculateAge(int year, int month, int day) {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - year;
        if (today.get(Calendar.DAY_OF_YEAR) < calendar.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        etAge.setText(String.valueOf(age));
    }

    private void setupUsernameValidation() {
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String username = s.toString().trim();
                if (username.length() < 4) {
                    isUsernameValid = false;
                    tvUsernameError.setText("Minimum 4 characters required");
                    tvUsernameError.setVisibility(View.VISIBLE);
                } else if (!username.matches("^[A-Za-z]+$")) {
                    isUsernameValid = false;
                    tvUsernameError.setText("Letters only");
                    tvUsernameError.setVisibility(View.VISIBLE);
                } else {
                    isUsernameValid = true;
                    tvUsernameError.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupPasswordValidation() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                if (password.isEmpty()) {
                    setRequirementVisibility(View.GONE);
                    return;
                }

                setRequirementVisibility(View.VISIBLE);
                boolean hasUpper = password.matches(".*[A-Z].*");
                boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");
                boolean hasNumber = password.matches(".*[0-9].*");
                boolean hasLength = password.length() >= 8;

                updateRequirementColor(tvReqUpper, hasUpper);
                updateRequirementColor(tvReqSymbol, hasSymbol);
                updateRequirementColor(tvReqNumber, hasNumber);
                updateRequirementColor(tvReqLength, hasLength);

                isPasswordValid = hasUpper && hasSymbol && hasNumber && hasLength;
                tvPasswordError.setVisibility(isPasswordValid ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void setRequirementVisibility(int visibility) {
        tvReqUpper.setVisibility(visibility);
        tvReqSymbol.setVisibility(visibility);
        tvReqNumber.setVisibility(visibility);
        tvReqLength.setVisibility(visibility);
    }

    private void updateRequirementColor(TextView tv, boolean valid) {
        tv.setTextColor(getResources().getColor(valid ? android.R.color.holo_green_dark : android.R.color.darker_gray));
    }

    private void setupPasswordToggle() {
        ivTogglePassword.setOnClickListener(v -> togglePassword(etPassword, isPasswordVisible = !isPasswordVisible));
        ivToggleConfirmPassword.setOnClickListener(v -> togglePassword(etConfirmPassword, isConfirmPasswordVisible = !isConfirmPasswordVisible));
    }

    private void togglePassword(EditText et, boolean visible) {
        et.setInputType(visible ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        et.setSelection(et.getText().length());
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String birthdate = etBirthdate.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (!isUsernameValid || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || address.isEmpty() || birthdate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = ageStr.isEmpty() ? 0 : Integer.parseInt(ageStr);
        if (age < 18 || age > 60) {
            etAge.setError("Age must be between 18 and 60");
            return;
        }

        if (!isPasswordValid || !password.equals(confirmPassword)) {
            Toast.makeText(this, "Check your password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbTerms.isChecked()) {
            tvTermsError.setVisibility(View.VISIBLE);
            return;
        }

        registerUser(username, firstName, lastName, age, address, birthdate, email, password);
    }

    private void registerUser(String username, String fName, String lName, int age, String address, String bday, String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Check Username Uniqueness first
        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    etUsername.setError("Username already taken");
                } else {
                    createAuthUser(username, fName, lName, age, address, bday, email, password);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void createAuthUser(String username, String fName, String lName, int age, String address, String bday, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                saveUserData(uid, username, fName, lName, age, address, bday, email);
            } else {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserData(String uid, String username, String fName, String lName, int age, String address, String bday, String email) {
        DatabaseReference userRef = usersRef.child(uid);
        userRef.child("username").setValue(username);
        userRef.child("firstName").setValue(fName);
        userRef.child("lastName").setValue(lName);
        userRef.child("age").setValue(age);
        userRef.child("address").setValue(address);
        userRef.child("birthdate").setValue(bday);
        userRef.child("email").setValue(email);
        userRef.child("role").setValue("staff");

        userRef.child("profileCompleted").setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // New self-registered accounts are staff accounts. Send them to the
                // staff dashboard so they cannot see admin-only screens.
                Intent intent = new Intent(this, home_staff.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}