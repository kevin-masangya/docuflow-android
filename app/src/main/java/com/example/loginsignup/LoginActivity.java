package com.example.loginsignup;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.*;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnGoogle, btnFacebook;
    private TextView tvSignup;
    private ImageView ivTogglePassword;
    private ProgressBar progressBar;

    private boolean passwordVisible = false;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    // ── Replaces deprecated GoogleSignInClient ──
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    // ── Replaces deprecated startActivityForResult ──
    private final ActivityResultLauncher<IntentSenderRequest> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    (ActivityResult result) -> {
                        try {
                            SignInCredential credential =
                                    oneTapClient.getSignInCredentialFromIntent(result.getData());
                            String idToken = credential.getGoogleIdToken();
                            if (idToken != null) {
                                firebaseAuthWithGoogle(idToken);
                            } else {
                                showLoading(false);
                                toast("Google sign in failed: no token received");
                            }
                        } catch (ApiException e) {
                            showLoading(false);
                            toast("Google sign in failed: " + e.getLocalizedMessage());
                        }
                    });

    private CallbackManager fbCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initFirebase();
        initViews();
        configureGoogle();
        configureFacebook();
        setListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) checkProfileAndRedirect(user.getUid());
    }

    /* -------------------------
       Initialization
    --------------------------*/

    private void initFirebase() {
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase
                .getInstance("https://YOUR_PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");
    }

    private void initViews() {
        etUsername       = findViewById(R.id.etLoginUsername);
        etPassword       = findViewById(R.id.etLoginPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        btnGoogle        = findViewById(R.id.btnGoogleSignIn);
        btnFacebook      = findViewById(R.id.btnFacebookSignin);
        tvSignup         = findViewById(R.id.tvGoToSignup);
        ivTogglePassword = findViewById(R.id.ivToggleLoginPassword);
        progressBar      = findViewById(R.id.loginProgressBar);
    }

    private void setListeners() {
        tvSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
        btnLogin.setOnClickListener(v         -> loginWithUsername());
        btnGoogle.setOnClickListener(v        -> signInWithGoogle());
        btnFacebook.setOnClickListener(v      -> signInWithFacebook());
        ivTogglePassword.setOnClickListener(v -> togglePassword());
    }

    /* -------------------------
       Password Visibility
    --------------------------*/

    private void togglePassword() {
        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
        etPassword.setSelection(etPassword.length());
        passwordVisible = !passwordVisible;
    }

    /* -------------------------
       Username Login
    --------------------------*/

    private void loginWithUsername() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty()) { etUsername.setError("Username required"); return; }
        if (password.isEmpty()) { etPassword.setError("Password required"); return; }

        showLoading(true);

        usersRef.orderByChild("username")
                .equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            showLoading(false);
                            toast("Username not found");
                            return;
                        }
                        for (DataSnapshot user : snapshot.getChildren()) {
                            String email = user.child("email").getValue(String.class);
                            signInWithEmail(email, password);
                            break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        toast(error.getMessage());
                    }
                });
    }

    private void signInWithEmail(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) checkProfileAndRedirect(user.getUid());
                    } else {
                        // Fixed: null-safe to prevent NullPointerException
                        Exception ex = task.getException();
                        toast(ex != null ? ex.getMessage() : "Login failed");
                    }
                });
    }

    /* -------------------------
       Google Login
       Fixed: replaced deprecated GoogleSignIn + GoogleSignInClient
              with Identity One Tap + ActivityResultLauncher
    --------------------------*/

    private void configureGoogle() {
        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                .setAutoSelectEnabled(false)
                .build();
    }

    private void signInWithGoogle() {
        showLoading(true);
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        IntentSenderRequest intentSenderRequest =
                                new IntentSenderRequest.Builder(
                                        result.getPendingIntent().getIntentSender()).build();
                        googleLauncher.launch(intentSenderRequest);
                    } catch (Exception e) {
                        showLoading(false);
                        toast("Could not open Google sign in: " + e.getLocalizedMessage());
                    }
                })
                .addOnFailureListener(this, e -> {
                    showLoading(false);
                    toast("Google sign in unavailable: " + e.getLocalizedMessage());
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        handleFirebaseUser(auth.getCurrentUser(), "google");
                    } else {
                        // Fixed: null-safe getMessage
                        Exception ex = task.getException();
                        toast(ex != null ? ex.getMessage() : "Google authentication failed");
                    }
                });
    }

    /* -------------------------
       Facebook Login
    --------------------------*/

    private void configureFacebook() {
        fbCallbackManager = CallbackManager.Factory.create();

        // Fixed: replaced explicit <LoginResult> type arg with <>
        LoginManager.getInstance().registerCallback(
                fbCallbackManager,
                new FacebookCallback<>() {

                    @Override
                    public void onSuccess(LoginResult result) {
                        firebaseAuthWithFacebook(result.getAccessToken().getToken());
                    }

                    @Override
                    public void onCancel() {
                        toast("Facebook login cancelled");
                    }

                    @Override
                    // Fixed: added @NonNull to match the @NotNull contract of FacebookCallback
                    public void onError(@NonNull FacebookException error) {
                        toast(error.getMessage());
                    }
                });
    }

    private void signInWithFacebook() {
        LoginManager.getInstance()
                .logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    private void firebaseAuthWithFacebook(String token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handleFirebaseUser(auth.getCurrentUser(), "facebook");
                    } else {
                        // Fixed: null-safe getMessage
                        Exception ex = task.getException();
                        toast(ex != null ? ex.getMessage() : "Facebook authentication failed");
                    }
                });
    }

    /* -------------------------
       Firebase User Handling
    --------------------------*/

    private void handleFirebaseUser(FirebaseUser user, String provider) {
        if (user == null) return;

        String uid = user.getUid();

        usersRef.child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            usersRef.child(uid).child("email").setValue(user.getEmail());
                            usersRef.child(uid).child("authProvider").setValue(provider);
                            usersRef.child(uid).child("profileCompleted").setValue(false);
                            usersRef.child(uid).child("role").setValue("staff");
                            openUpdateProfile();
                        } else {
                            checkProfileAndRedirect(uid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast(error.getMessage());
                    }
                });
    }

    /* -------------------------
       Profile & Role Navigation
    --------------------------*/

    private void checkProfileAndRedirect(String uid) {
        usersRef.child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Boolean completed =
                                snapshot.child("profileCompleted").getValue(Boolean.class);

                        if (completed == null || !completed) {
                            openUpdateProfile();
                            return;
                        }

                        String role = snapshot.child("role").getValue(String.class);

                        if (role == null) {
                            toast("No role assigned. Contact your administrator.");
                            auth.signOut();
                            return;
                        }

                        switch (role) {
                            case "admin":
                                openActivity(home_admin.class);
                                break;
                            case "staff":
                                openActivity(home_staff.class);
                                break;
                            default:
                                toast("Unknown role: " + role + ". Contact your administrator.");
                                auth.signOut();
                                break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast(error.getMessage());
                    }
                });
    }

    /* -------------------------
       Navigation Helpers
       Fixed: openActivity() was missing — caused "Cannot resolve method" errors
              Removed unused openHome() method
    --------------------------*/

    private void openActivity(Class<?> target) {
        Intent i = new Intent(this, target);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private void openUpdateProfile() {
        openActivity(UpdateProfileActivity.class);
    }

    /* -------------------------
       Activity Result
       Fixed: Google now uses ActivityResultLauncher above
              onActivityResult kept only for Facebook
    --------------------------*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fbCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /* -------------------------
       Utilities
    --------------------------*/

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}