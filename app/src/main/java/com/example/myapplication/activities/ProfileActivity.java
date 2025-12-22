package com.example.myapplication.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.example.myapplication.R;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Activity for managing user profile and settings
 */
public class ProfileActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 103;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;

    private Toolbar toolbar;
    private ImageView ivProfileImage;
    private MaterialButton btnChangePhoto;
    private TextView tvUserName, tvUserEmail, tvUserType;
    private EditText etFullName, etPhone, etIdCard, etAddress, etDateOfBirth;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    // Security section
    private MaterialCardView cardSecurity;
    private Switch switchBiometric;
    private LinearLayout layoutChangePin, layoutChangePwd, layoutFaceId;
    private TextView tvBiometricStatus;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private User currentUser;
    private Uri photoUri;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupToolbar();
        loadUserData();
        setupSecurityOptions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivProfileImage = findViewById(R.id.iv_profile_image);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvUserType = findViewById(R.id.tv_user_type);
        etFullName = findViewById(R.id.et_full_name);
        etPhone = findViewById(R.id.et_phone);
        etIdCard = findViewById(R.id.et_id_card);
        etAddress = findViewById(R.id.et_address);
        etDateOfBirth = findViewById(R.id.et_date_of_birth);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);

        cardSecurity = findViewById(R.id.card_security);
        switchBiometric = findViewById(R.id.switch_biometric);
        layoutChangePin = findViewById(R.id.layout_change_pin);
        layoutChangePwd = findViewById(R.id.layout_change_password);
        layoutFaceId = findViewById(R.id.layout_face_id);
        tvBiometricStatus = findViewById(R.id.tv_biometric_status);

        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);

        btnChangePhoto.setOnClickListener(v -> showImagePickerDialog());
        btnSave.setOnClickListener(v -> saveProfile());

        layoutChangePin.setOnClickListener(v -> showChangePinDialog());
        layoutChangePwd.setOnClickListener(v -> showChangePasswordDialog());
        layoutFaceId.setOnClickListener(v -> setupFaceId());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thông tin cá nhân");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadUserData() {
        currentUser = sessionManager.getUser();
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi tải thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Display user info
        tvUserName.setText(currentUser.getFullName());
        tvUserEmail.setText(currentUser.getEmail());
        tvUserType.setText(currentUser.getUserType() == User.UserType.OFFICER ? 
                "Nhân viên ngân hàng" : "Khách hàng");

        etFullName.setText(currentUser.getFullName());
        etPhone.setText(currentUser.getPhone());
        etIdCard.setText(currentUser.getIdCardNumber());
        etAddress.setText(currentUser.getAddress());
        if (currentUser.getDateOfBirth() != null) {
            etDateOfBirth.setText(FormatUtils.formatDate(currentUser.getDateOfBirth()));
        }

        // Load profile image
        if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentUser.getProfileImageUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivProfileImage);
        }
    }

    private void setupSecurityOptions() {
        // Biometric switch
        switchBiometric.setChecked(sessionManager.isBiometricEnabled());
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableBiometric();
            } else {
                sessionManager.setBiometricEnabled(false);
                tvBiometricStatus.setText("Đã tắt");
            }
        });

        tvBiometricStatus.setText(sessionManager.isBiometricEnabled() ? "Đã bật" : "Đã tắt");
    }

    private void enableBiometric() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        sessionManager.setBiometricEnabled(true);
                        tvBiometricStatus.setText("Đã bật");
                        Toast.makeText(ProfileActivity.this, 
                                "Đăng nhập sinh trắc học đã được bật", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        switchBiometric.setChecked(false);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        switchBiometric.setChecked(false);
                        Toast.makeText(ProfileActivity.this, errString, Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực sinh trắc học")
                .setSubtitle("Xác nhận để bật tính năng này")
                .setNegativeButtonText("Hủy")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void showImagePickerDialog() {
        String[] options = {"Chụp ảnh", "Chọn từ thư viện"};
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("Đổi ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        captureImage();
                    } else {
                        pickImage();
                    }
                })
                .show();
    }

    private void captureImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
            }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "PROFILE_" + timeStamp;
            File storageDir = getExternalFilesDir(null);
            File image = File.createTempFile(fileName, ".jpg", storageDir);
            currentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            } else {
                Toast.makeText(this, "Cần cấp quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                photoUri = data.getData();
                uploadProfileImage();
            } else if (requestCode == CAPTURE_IMAGE_REQUEST) {
                uploadProfileImage();
            }
        }
    }

    private void uploadProfileImage() {
        if (photoUri == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnChangePhoto.setEnabled(false);

        firebaseHelper.uploadProfileImage(currentUser.getId(), photoUri,
                downloadUrl -> {
                    // Update user profile with new image URL
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("profileImageUrl", downloadUrl);

                    firebaseHelper.updateUser(currentUser.getId(), updates,
                            aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                btnChangePhoto.setEnabled(true);

                                currentUser.setProfileImageUrl(downloadUrl);
                                sessionManager.updateUser(currentUser);

                                Glide.with(this)
                                        .load(downloadUrl)
                                        .circleCrop()
                                        .into(ivProfileImage);

                                Toast.makeText(this, "Cập nhật ảnh thành công", Toast.LENGTH_SHORT).show();
                            },
                            e -> {
                                progressBar.setVisibility(View.GONE);
                                btnChangePhoto.setEnabled(true);
                                Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    );
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    btnChangePhoto.setEnabled(true);
                    Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String idCard = etIdCard.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String dateOfBirth = etDateOfBirth.getText().toString().trim();

        // Validation
        if (fullName.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ tên");
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phone", phone);
        updates.put("idCardNumber", idCard);
        updates.put("address", address);
        updates.put("dateOfBirth", dateOfBirth);

        firebaseHelper.updateUser(currentUser.getId(), updates,
                aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);

                    // Update local user
                    currentUser.setFullName(fullName);
                    currentUser.setPhone(phone);
                    currentUser.setIdCardNumber(idCard);
                    currentUser.setAddress(address);
                    currentUser.setDateOfBirth(dateOfBirth);
                    sessionManager.updateUser(currentUser);

                    tvUserName.setText(fullName);
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showChangePinDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_change_pin, null);
        EditText etCurrentPin = view.findViewById(R.id.et_current_pin);
        EditText etNewPin = view.findViewById(R.id.et_new_pin);
        EditText etConfirmPin = view.findViewById(R.id.et_confirm_pin);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Đổi mã PIN")
                .setView(view)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String newPin = etNewPin.getText().toString();
                    String confirmPin = etConfirmPin.getText().toString();

                    if (newPin.length() != 6) {
                        Toast.makeText(this, "Mã PIN phải có 6 số", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPin.equals(confirmPin)) {
                        Toast.makeText(this, "Mã PIN không khớp", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update PIN
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("pin", newPin);
                    firebaseHelper.updateUser(currentUser.getId(), updates,
                            aVoid -> {
                                sessionManager.setPin(newPin);
                                Toast.makeText(this, "Đổi mã PIN thành công", Toast.LENGTH_SHORT).show();
                            },
                            e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText etNewPassword = view.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = view.findViewById(R.id.et_confirm_password);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(view)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String newPassword = etNewPassword.getText().toString();
                    String confirmPassword = etConfirmPassword.getText().toString();

                    if (newPassword.length() < 6) {
                        Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    firebaseHelper.updatePassword(newPassword,
                            aVoid -> Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupFaceId() {
        Toast.makeText(this, "Tính năng Face ID đang phát triển", Toast.LENGTH_SHORT).show();
    }
}
