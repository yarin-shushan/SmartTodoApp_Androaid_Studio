package com.afeka.smarttodoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        // Load saved credentials if exist
        loadSavedCredentials()

        // Login button click
        btnLogin.setOnClickListener {
            val email = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                performLogin(email, password)
            }
        }

        // Register link click
        findViewById<android.widget.TextView>(R.id.tvRegisterLink).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loadSavedCredentials() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedEmail = prefs.getString("saved_email", "")
        val savedPassword = prefs.getString("saved_password", "")

        if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            etUsername.setText(savedEmail)
            etPassword.setText(savedPassword)
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            etUsername.error = "Email required"
            return false
        }
        if (password.isEmpty()) {
            etPassword.error = "Password required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.error = "Invalid email format"
            return false
        }
        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    private fun performLogin(email: String, password: String) {
        // Show loading
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true
                btnLogin.text = "Login"

                if (task.isSuccessful) {
                    // Login success - save credentials
                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("saved_email", email)
                        .putString("saved_password", password)
                        .putBoolean("is_logged_in", true)
                        .apply()

                    // Go to MainActivity
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    // Login failed
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}