package com.afeka.smarttodoapp

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afeka.smarttodoapp.data.User
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)

        // Register button click
        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInput(fullName, username, email, password, confirmPassword)) {
                performRegister(fullName, username, email, password)
            }
        }

        // Login link click
        findViewById<android.widget.TextView>(R.id.tvLoginLink).setOnClickListener {
            finish()
        }
    }

    private fun validateInput(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {

        if (fullName.isEmpty()) {
            etFullName.error = "Full name required"
            return false
        }

        if (username.isEmpty()) {
            etUsername.error = "Username required"
            return false
        }

        if (username.length < 3) {
            etUsername.error = "Username must be at least 3 characters"
            return false
        }

        if (email.isEmpty()) {
            etEmail.error = "Email required"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid email format"
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Password required"
            return false
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return false
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            return false
        }

        return true
    }

    private fun performRegister(fullName: String, username: String, email: String, password: String) {
        // Show loading
        btnRegister.isEnabled = false
        btnRegister.text = "Registering..."

        // Create user with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration success - create user document in Firestore
                    val firebaseUser = auth.currentUser
                    val user = User(
                        uid = firebaseUser?.uid ?: "",
                        email = email,
                        username = username,
                        fullName = fullName,
                        createdAt = System.currentTimeMillis()
                    )

                    // Save to Firestore
                    firestore.collection("users")
                        .document(user.uid)
                        .set(user)
                        .addOnSuccessListener {
                            btnRegister.isEnabled = true
                            btnRegister.text = "Register"

                            Toast.makeText(
                                this@RegisterActivity,
                                "Registration successful! Please login",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            btnRegister.isEnabled = true
                            btnRegister.text = "Register"

                            Toast.makeText(
                                this@RegisterActivity,
                                "Failed to save user data: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    // Registration failed
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register"

                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "Email already exists"
                        task.exception?.message?.contains("badly formatted") == true ->
                            "Invalid email format"
                        else -> "Registration failed: ${task.exception?.message}"
                    }

                    Toast.makeText(
                        this@RegisterActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}