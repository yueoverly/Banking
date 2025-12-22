package com.example.myapplication.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.Constants;
import com.example.myapplication.utils.FirebaseHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Activity for bank officers to create new customer accounts
 */
public class CreateCustomerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 104;
    private static final int CAPTURE_FACE_REQUEST = 1;

    private Toolbar toolbar;
    private EditText etFullName, etEmail, etPhone, etIdCard, etAddress, etDateOfBirth;
    private RadioGroup radioGroupGender;
    private RadioButton rbMale, rbFemale;
    private Spinner spinnerAccountType;
    private EditText etInitialDeposit, etInterestRate, etLoanAmount, etLoanTerm;
    private View layoutSavingOptions, layoutMortgageOptions;
    private MaterialCardView cardFaceCapture;
    private ImageView ivFacePreview;
    private TextView tvFaceStatus;
    private MaterialButton btnCaptureFace, btnCreateCustomer;
    private CheckBox cbVerified;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private Uri faceImageUri;
    private String currentPhotoPath;

    private String[] accountTypes = {"Tài khoản Thanh toán", "Tài khoản Tiết kiệm", "Tài khoản Vay"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_customer);

        initViews();
        setupToolbar();
        setupSpinner();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etIdCard = findViewById(R.id.et_id_card);
        etAddress = findViewById(R.id.et_address);
        etDateOfBirth = findViewById(R.id.et_date_of_birth);
        radioGroupGender = findViewById(R.id.radio_group_gender);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        spinnerAccountType = findViewById(R.id.spinner_account_type);
        etInitialDeposit = findViewById(R.id.et_initial_deposit);
        etInterestRate = findViewById(R.id.et_interest_rate);
        etLoanAmount = findViewById(R.id.et_loan_amount);
        etLoanTerm = findViewById(R.id.et_loan_term);
        layoutSavingOptions = findViewById(R.id.layout_saving_options);
        layoutMortgageOptions = findViewById(R.id.layout_mortgage_options);
        cardFaceCapture = findViewById(R.id.card_face_capture);
        ivFacePreview = findViewById(R.id.iv_face_preview);
        tvFaceStatus = findViewById(R.id.tv_face_status);
        btnCaptureFace = findViewById(R.id.btn_capture_face);
        btnCreateCustomer = findViewById(R.id.btn_create_customer);
        cbVerified = findViewById(R.id.cb_verified);
        progressBar = findViewById(R.id.progress_bar);

        firebaseHelper = FirebaseHelper.getInstance();

        btnCaptureFace.setOnClickListener(v -> captureFaceImage());
        btnCreateCustomer.setOnClickListener(v -> createCustomer());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tạo tài khoản khách hàng");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, accountTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccountType.setAdapter(adapter);

        spinnerAccountType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateAccountTypeOptions(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupListeners() {
        // Set default values
        etInterestRate.setText("3.5");
        etLoanTerm.setText("12");
    }

    private void updateAccountTypeOptions(int position) {
        switch (position) {
            case 0: // Checking
                layoutSavingOptions.setVisibility(View.GONE);
                layoutMortgageOptions.setVisibility(View.GONE);
                break;
            case 1: // Saving
                layoutSavingOptions.setVisibility(View.VISIBLE);
                layoutMortgageOptions.setVisibility(View.GONE);
                break;
            case 2: // Mortgage
                layoutSavingOptions.setVisibility(View.GONE);
                layoutMortgageOptions.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void captureFaceImage() {
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
                faceImageUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, faceImageUri);
                startActivityForResult(intent, CAPTURE_FACE_REQUEST);
            }
        }
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "FACE_" + timeStamp;
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
                captureFaceImage();
            } else {
                Toast.makeText(this, "Cần cấp quyền camera để chụp ảnh khuôn mặt",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == CAPTURE_FACE_REQUEST && resultCode == RESULT_OK) {
            // Display captured face image
            Glide.with(this)
                    .load(faceImageUri)
                    .centerCrop()
                    .into(ivFacePreview);
            
            ivFacePreview.setVisibility(View.VISIBLE);
            tvFaceStatus.setText("Đã chụp ảnh khuôn mặt");
            tvFaceStatus.setTextColor(ContextCompat.getColor(this, R.color.status_active));
        }
    }

    private void createCustomer() {
        // Validate inputs
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String idCard = etIdCard.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String dateOfBirth = etDateOfBirth.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ tên");
            return;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            return;
        }

        if (phone.isEmpty() || !phone.matches("^(0|\\+84)[0-9]{9,10}$")) {
            etPhone.setError("Số điện thoại không hợp lệ");
            return;
        }

        if (idCard.isEmpty() || !idCard.matches("^[0-9]{9,12}$")) {
            etIdCard.setError("Số CCCD không hợp lệ");
            return;
        }

        String initialDepositStr = etInitialDeposit.getText().toString().trim();
        double initialDeposit = 0;
        if (!initialDepositStr.isEmpty()) {
            try {
                initialDeposit = Double.parseDouble(initialDepositStr.replace(",", ""));
            } catch (NumberFormatException e) {
                etInitialDeposit.setError("Số tiền không hợp lệ");
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        btnCreateCustomer.setEnabled(false);

        // Create temp password for customer
        String tempPassword = generateTempPassword();

        // Create Firebase Auth account
        FirebaseAuth auth = FirebaseAuth.getInstance();
        double finalInitialDeposit = initialDeposit;
        
        auth.createUserWithEmailAndPassword(email, tempPassword)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();

                    // Upload face image if captured
                    if (faceImageUri != null) {
                        uploadFaceAndCreateUser(userId, fullName, email, phone, idCard, 
                                address, dateOfBirth, finalInitialDeposit, tempPassword);
                    } else {
                        createUserInFirestore(userId, fullName, email, phone, idCard, 
                                address, dateOfBirth, null, finalInitialDeposit, tempPassword);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreateCustomer.setEnabled(true);
                    Toast.makeText(this, "Lỗi tạo tài khoản: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void uploadFaceAndCreateUser(String userId, String fullName, String email, 
            String phone, String idCard, String address, String dateOfBirth, 
            double initialDeposit, String tempPassword) {
        
        firebaseHelper.uploadFaceImage(userId, faceImageUri,
                downloadUrl -> createUserInFirestore(userId, fullName, email, phone, idCard,
                        address, dateOfBirth, downloadUrl, initialDeposit, tempPassword),
                e -> {
                    // Continue without face image
                    createUserInFirestore(userId, fullName, email, phone, idCard,
                            address, dateOfBirth, null, initialDeposit, tempPassword);
                }
        );
    }

    private void createUserInFirestore(String userId, String fullName, String email, 
            String phone, String idCard, String address, String dateOfBirth,
            String faceImageUrl, double initialDeposit, String tempPassword) {

        // Create user object
        User user = new User();
        user.setId(userId);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setIdCardNumber(idCard);
        user.setAddress(address);
        user.setDateOfBirth(dateOfBirth);
        user.setGender(rbMale.isChecked() ? "Nam" : "Nữ");
        user.setFaceImageUrl(faceImageUrl);
        user.setUserType(User.UserType.CUSTOMER);
        user.setVerified(cbVerified.isChecked());
        user.setBiometricEnabled(faceImageUrl != null);

        firebaseHelper.createUser(user,
                aVoid -> {
                    // Create account based on selected type
                    createAccount(userId, initialDeposit, tempPassword);
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreateCustomer.setEnabled(true);
                    Toast.makeText(this, "Lỗi tạo người dùng: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void createAccount(String userId, double initialDeposit, String tempPassword) {
        int accountTypeIndex = spinnerAccountType.getSelectedItemPosition();
        
        Account account = new Account();
        account.setUserId(userId);
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(initialDeposit);
        account.setCurrency("VND");
        account.setStatus(Account.AccountStatus.ACTIVE);

        switch (accountTypeIndex) {
            case 0: // Checking
                account.setAccountType(Account.AccountType.CHECKING);
                break;
            case 1: // Saving
                account.setAccountType(Account.AccountType.SAVING);
                String rateStr = etInterestRate.getText().toString().trim();
                if (!rateStr.isEmpty()) {
                    account.setInterestRate(Double.parseDouble(rateStr));
                }
                break;
            case 2: // Mortgage
                account.setAccountType(Account.AccountType.MORTGAGE);
                String loanAmountStr = etLoanAmount.getText().toString().trim();
                String loanTermStr = etLoanTerm.getText().toString().trim();
                if (!loanAmountStr.isEmpty()) {
                    account.setLoanAmount(Double.parseDouble(loanAmountStr.replace(",", "")));
                }
                if (!loanTermStr.isEmpty()) {
                    account.setTermMonths(Integer.parseInt(loanTermStr));
                }
                // Calculate monthly payment
                if (account.getLoanAmount() > 0 && account.getTermMonths() > 0) {
                    double monthlyPayment = account.getLoanAmount() / account.getTermMonths();
                    account.setMonthlyPayment(monthlyPayment);
                }
                break;
        }

        firebaseHelper.createAccount(account,
                docRef -> {
                    progressBar.setVisibility(View.GONE);
                    showSuccessDialog(tempPassword);
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreateCustomer.setEnabled(true);
                    Toast.makeText(this, "Lỗi tạo tài khoản: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private String generateAccountNumber() {
        // Generate 12-digit account number
        long timestamp = System.currentTimeMillis() % 1000000000L;
        int random = (int) (Math.random() * 1000);
        return String.format("%09d%03d", timestamp, random);
    }

    private String generateTempPassword() {
        // Generate 8-character temporary password
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void showSuccessDialog(String tempPassword) {
        String email = etEmail.getText().toString().trim();
        String message = String.format(
                "Tạo tài khoản khách hàng thành công!\n\n" +
                "Email: %s\n" +
                "Mật khẩu tạm: %s\n\n" +
                "Vui lòng thông báo cho khách hàng đổi mật khẩu khi đăng nhập lần đầu.",
                email, tempPassword
        );

        new android.app.AlertDialog.Builder(this)
                .setTitle("Thành công")
                .setMessage(message)
                .setPositiveButton("Đóng", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}
