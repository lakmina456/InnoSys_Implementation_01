package com.example.signme;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profileImageView;
    private TextView userNameTextView;
    private TextView emailTextView;
    private TextView ageTextView;
    private TextView dobTextView;
    private Button editProfileButton;
    private Button changePasswordButton;

    private ConnectionClass connectionClass;
    private Connection con;
    private String email;
    private Bitmap selectedImageBitmap;

    private static final String TAG = "ProfileActivity";

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        connectionClass = new ConnectionClass();

        profileImageView = findViewById(R.id.profileImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        ageTextView = findViewById(R.id.ageTextView);
        dobTextView = findViewById(R.id.dobTextView);
        editProfileButton = findViewById(R.id.editProfileButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            email = intent.getStringExtra("email");
            loadUserProfile();
        } else {
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
        }

        editProfileButton.setOnClickListener(v -> {
            Intent editIntent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            editIntent.putExtra("email", email);
            startActivity(editIntent);
        });

        changePasswordButton.setOnClickListener(v -> {
            Intent changePasswordIntent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            changePasswordIntent.putExtra("email", email);
            startActivity(changePasswordIntent);
        });

        profileImageView.setOnClickListener(v -> openImageChooser());
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                profileImageView.setImageBitmap(selectedImageBitmap);
                saveImageToDatabase(selectedImageBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveImageToDatabase(Bitmap bitmap) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                String query = "UPDATE driver SET PROFILE_PICTURE = ? WHERE EMAIL = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setBytes(1, imageBytes);
                stmt.setString(2, email);

                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show());
                }

                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_refresh);
        if (menuItem != null) {
            SpannableString s = new SpannableString(menuItem.getTitle());
            s.setSpan(new ForegroundColorSpan(Color.BLACK), 0, s.length(), 0);
            menuItem.setTitle(s);
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refreshPage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshPage() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            Connection con = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "SELECT FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, PROFILE_PICTURE FROM driver WHERE EMAIL = ?";
                stmt = con.prepareStatement(query);
                stmt.setString(1, email);

                rs = stmt.executeQuery();
                if (rs.next()) {
                    String firstName = rs.getString("FIRST_NAME");
                    String lastName = rs.getString("LAST_NAME");
                    String fullName = firstName + " " + lastName;
                    String dob = rs.getString("DATE_OF_BIRTH");
                    byte[] profilePicture = rs.getBytes("PROFILE_PICTURE");

                    runOnUiThread(() -> {
                        userNameTextView.setText(fullName);
                        emailTextView.setText(email);
                        if (dob != null && !dob.isEmpty()) {
                            dobTextView.setText(dob);
                            calculateAndDisplayAge(dob);
                        } else {
                            dobTextView.setText("NO INFO");
                            ageTextView.setText("NO INFO");
                        }

                        // Load the profile image using Glide
                        if (profilePicture != null && profilePicture.length > 0) {
                            Glide.with(ProfileActivity.this)
                                    .load(profilePicture)
                                    .apply(new RequestOptions()
                                            .transform(new CenterCrop(), new RoundedCorners(20)))
                                    .into(profileImageView);
                        } else {
                            Glide.with(ProfileActivity.this)
                                    .load(R.drawable.default_profile_image)
                                    .apply(new RequestOptions()
                                            .transform(new CenterCrop(), new RoundedCorners(20)))
                                    .into(profileImageView);
                        }
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (con != null) con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void calculateAndDisplayAge(String dob) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            Calendar dobCalendar = Calendar.getInstance();
            dobCalendar.setTime(sdf.parse(dob));
            Calendar todayCalendar = Calendar.getInstance();

            AtomicInteger age = new AtomicInteger(todayCalendar.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR));

            // Check if the birthday hasn't occurred yet this year
            if (todayCalendar.get(Calendar.MONTH) < dobCalendar.get(Calendar.MONTH)) {
                age.decrementAndGet();
            } else if (todayCalendar.get(Calendar.MONTH) == dobCalendar.get(Calendar.MONTH) &&
                    todayCalendar.get(Calendar.DAY_OF_MONTH) < dobCalendar.get(Calendar.DAY_OF_MONTH)) {
                age.decrementAndGet();
            }

            // Display the calculated age
            runOnUiThread(() -> ageTextView.setText(String.valueOf(age.get())));
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> ageTextView.setText("ERROR"));
        }
    }
}
