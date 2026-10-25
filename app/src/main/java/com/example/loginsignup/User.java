package com.example.loginsignup;

import com.google.firebase.database.PropertyName;

public class User {
    // 1. Database fields - must match your Firebase keys exactly
    @PropertyName("firstName")
    private String firstName;

    @PropertyName("lastName")
    private String lastName;

    @PropertyName("username")
    private String username;

    @PropertyName("email")
    private String email;

    @PropertyName("role")
    private String role;

    private String userId;

    // 2. Required empty constructor for Firebase
    public User() {}

    // 3. Logic to get the display name
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (username != null) {
            return username;
        }
        return "Unknown Staff";
    }

    // 4. Corrected Getters and Setters
    @PropertyName("firstName")
    public String getFirstName() { return firstName; }
    @PropertyName("firstName")
    public void setFirstName(String firstName) { this.firstName = firstName; }

    @PropertyName("lastName")
    public String getLastName() { return lastName; }
    @PropertyName("lastName")
    public void setLastName(String lastName) { this.lastName = lastName; }

    @PropertyName("username")
    public String getUsername() { return username; }
    @PropertyName("username")
    public void setUsername(String username) { this.username = username; }

    @PropertyName("email")
    public String getEmail() { return email; }

    @PropertyName("role")
    public String getRole() { return role; }

    // This one is local only, not in Firebase
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}