package com.example.signme;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private ConnectionClass connectionClass;
    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText, confirmPasswordEditText, dobEditText;
    private Button registerButton;
    private CheckBox learnersPermitCheckBox;
    private RadioGroup licenseRadioGroup;
    private RadioButton learnersPermitRadioButton, driversLicenseRadioButton;
    private TextView obtainTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        connectionClass = new ConnectionClass();
        // Initialize views
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        dobEditText = findViewById(R.id.dobEditText);
        registerButton = findViewById(R.id.registerButton);
        learnersPermitCheckBox = findViewById(R.id.learnersPermitCheckBox);
        licenseRadioGroup = findViewById(R.id.licenseRadioGroup);
        learnersPermitRadioButton = findViewById(R.id.learnersPermitRadioButton);
        driversLicenseRadioButton = findViewById(R.id.driversLicenseRadioButton);
        obtainTextView = findViewById(R.id.obtainTextView);

        TextView loginTextView = findViewById(R.id.loginTextView);
        loginTextView.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Set styled text for logo
        TextView logoText = findViewById(R.id.logoText);
        SpannableString spannableString = new SpannableString("SignMe");
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#ADD8E6")), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Light blue for "Sign"
        spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), 4, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // White for "Me"
        logoText.setText(spannableString);

        // Set click listener for register button
        registerButton.setOnClickListener(v -> registerUser());

        // Add text change listener to confirm password field
        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                validatePasswords();
            }
        });

        // Add text change listener to email field for real-time validation
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateEmail(s.toString());
            }
        });

        // Add click listener to dobEditText for date picker
        dobEditText.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a new DatePickerDialog with custom theme
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.CustomDatePickerDialog, // Apply custom dialog theme here
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = (monthOfYear + 1) + "/" + dayOfMonth + "/" + year1;
                    dobEditText.setText(selectedDate);
                    handleAgeSpecificOptions(year1, monthOfYear, dayOfMonth);
                },
                year, month, day);

        // Show the DatePickerDialog
        datePickerDialog.show();
    }


    private void handleAgeSpecificOptions(int year, int month, int day) {
        Calendar dobCalendar = Calendar.getInstance();
        dobCalendar.set(year, month, day);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        learnersPermitCheckBox.setVisibility(View.GONE);
        licenseRadioGroup.setVisibility(View.GONE);
        obtainTextView.setVisibility(View.GONE);

        if (age >= 16 && age <= 18) {
            learnersPermitCheckBox.setVisibility(View.VISIBLE);
        } else if (age > 18) {
            obtainTextView.setVisibility(View.VISIBLE);
            licenseRadioGroup.setVisibility(View.VISIBLE);
        } else if (age < 16) {
            dobEditText.setError("You must be at least 16 years old to register");
        }
    }

    private void registerUser() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();

        boolean isValid = true;
        StringBuilder errorMessages = new StringBuilder();

        // Clear previous errors
        clearErrors();

        // Validate each field
        if (firstName.isEmpty()) {
            firstNameEditText.setError("First name is required");
            errorMessages.append("First name is required\n");
            isValid = false;
        }
        if (lastName.isEmpty()) {
            lastNameEditText.setError("Last name is required");
            errorMessages.append("Last name is required\n");
            isValid = false;
        }
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            errorMessages.append("Email is required\n");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Invalid email address");
            errorMessages.append("Invalid email address\n");
            isValid = false;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            errorMessages.append("Password is required\n");
            isValid = false;
        } else if (!validatePassword(password)) {
            passwordEditText.setError("Password must contain at least 8 characters, one uppercase letter, one lowercase letter, and one special character");
            errorMessages.append("Password must contain at least 8 characters, one uppercase letter, one lowercase letter, and one special character\n");
            isValid = false;
        }
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.setError("Confirm password is required");
            errorMessages.append("Confirm password is required\n");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            errorMessages.append("Passwords do not match\n");
            isValid = false;
        }
        if (dob.isEmpty()) {
            dobEditText.setError("Date of birth is required");
            errorMessages.append("Date of birth is required\n");
            isValid = false;
        } else {
            try {
                Date dateOfBirth = new SimpleDateFormat("MM/dd/yyyy").parse(dob);
                Calendar dobCalendar = Calendar.getInstance();
                dobCalendar.setTime(dateOfBirth);
                Calendar today = Calendar.getInstance();
                int age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);

                if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
                    age--;
                }

                if (age >= 16 && age <= 18) {
                    if (!learnersPermitCheckBox.isChecked()) {
                        learnersPermitCheckBox.setError("Learner's permit is required for your age to drive!");
                        errorMessages.append("Learner's permit is required for your age to drive!\n");
                        isValid = false;
                    }
                } else if (age > 18) {
                    if (licenseRadioGroup.getCheckedRadioButtonId() == -1) {
                        learnersPermitRadioButton.setError("Driver's License or Learner's permit is required for your age to drive!");
                        errorMessages.append("Driver's License or Learner's permit is required for your age to drive!\n");
                        isValid = false;
                    }
                } else {
                    dobEditText.setError("You must be at least 16 years old to register");
                    errorMessages.append("You must be at least 16 years old to register\n");
                    isValid = false;
                }
            } catch (ParseException e) {
                dobEditText.setError("Invalid date format");
                errorMessages.append("Invalid date format\n");
                isValid = false;
            }
        }

        if (!isValid) {
            Toast.makeText(this, errorMessages.toString().trim(), Toast.LENGTH_LONG).show();
            return;
        }

        // Proceed with registration if all validations pass
        // Check if the email already exists and then save user data
        checkEmailExists(email);
    }

    // Method to clear all errors from EditText fields
    private void clearErrors() {
        firstNameEditText.setError(null);
        lastNameEditText.setError(null);
        emailEditText.setError(null);
        passwordEditText.setError(null);
        confirmPasswordEditText.setError(null);
        dobEditText.setError(null);
        learnersPermitCheckBox.setError(null);
        learnersPermitRadioButton.setError(null);
    }

    // Method to check if email already exists in database
    private void checkEmailExists(String email) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                Connection con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "SELECT EMAIL FROM driver WHERE EMAIL = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    runOnUiThread(() -> {
                        emailEditText.setError("Email address already exists");
                        Toast.makeText(this, "Email address already exists", Toast.LENGTH_LONG).show();
                    });
                    rs.close();
                    stmt.close();
                    con.close();
                    return;
                }

                rs.close();
                stmt.close();

                // Encrypt the password using bcrypt before saving
                String hashedPassword = BCrypt.hashpw(passwordEditText.getText().toString(), BCrypt.gensalt());

                // Insert user data into the database
                saveUserData(firstNameEditText.getText().toString(), lastNameEditText.getText().toString(), email, hashedPassword, dobEditText.getText().toString(), con);
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }




    private void saveUserData(String firstName, String lastName, String email, String password, String dob, Connection con) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                String query = "INSERT INTO driver (FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, DATE_OF_BIRTH) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setString(3, email);
                stmt.setString(4, password);
                stmt.setString(5, dob);

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Registration successful! Please Log In!", Toast.LENGTH_SHORT).show();
                        // Navigate to HomeActivity
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        startActivity(intent);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show());
                }

                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void validatePasswords() {
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (!confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
        } else {
            confirmPasswordEditText.setError(null);
        }
    }

    private void validateEmail(String email) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Invalid email address");
        } else {
            emailEditText.setError(null);
        }
    }

    private boolean validatePassword(String password) {
        Pattern passwordPattern = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*[!@#$&*]).{8,}$");
        return passwordPattern.matcher(password).matches();
    }
}

