package com.example.signme;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class ResetPasswordActivity extends AppCompatActivity {
    ConnectionClass connectionClass;
    Connection con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        connectionClass = new ConnectionClass();

        EditText emailEditText = findViewById(R.id.etResetEmail);
        EditText newPasswordEditText = findViewById(R.id.etNewPassword);
        EditText confirmNewPasswordEditText = findViewById(R.id.etConfirmNewPassword);
        Button resetPasswordButton = findViewById(R.id.btnResetPassword);

        resetPasswordButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmNewPassword = confirmNewPasswordEditText.getText().toString().trim();

            if (email.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(ResetPasswordActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmNewPassword)) {
                Toast.makeText(ResetPasswordActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!validatePassword(newPassword)) {
                Toast.makeText(ResetPasswordActivity.this, "Password must be at least 8 characters long, contain at least one symbol, one uppercase letter, and one number", Toast.LENGTH_LONG).show();
                return;
            }

            resetPassword(email, newPassword);
        });
    }

    private boolean validatePassword(String password) {
        // Updated pattern to allow any symbol including ~`!@#$%^&*()-_+={}[]|\;:"<>,./?
        Pattern pattern = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[~`!@#$%^&*()\\-_=+{}\\[\\]|;:\'\",.<>/?]).{8,}$");
        return pattern.matcher(password).matches();
    }

    private void resetPassword(String email, String newPassword) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN(); // Establishing database connection
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(ResetPasswordActivity.this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

                // Update the user's password in the database
                String query = "UPDATE driver SET PASSWORD = ? WHERE EMAIL = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, hashedPassword);
                stmt.setString(2, email);

                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(ResetPasswordActivity.this, "Password reset successful", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity and return to the login screen
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ResetPasswordActivity.this, "Invalid email address", Toast.LENGTH_SHORT).show());
                }

                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ResetPasswordActivity.this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ResetPasswordActivity.this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}
