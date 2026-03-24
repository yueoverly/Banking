package com.example.myapplication.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

/**
 * QRCodeActivity - Hiển thị mã QR của tài khoản để người khác quét và chuyển tiền
 * 
 * QR Data Format (JSON):
 * {
 *   "bank": "TDT_BANK",
 *   "accountNumber": "1234567890",
 *   "accountName": "NGUYEN VAN A",
 *   "accountId": "firebase_doc_id"
 * }
 */
public class QRCodeActivity extends AppCompatActivity {

    private ImageView ivQRCode;
    private TextView tvAccountName, tvAccountNumber, tvBankName, tvInstruction;
    private Button btnSaveQR, btnShareQR;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    
    private Account checkingAccount;
    private User currentUser;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        initViews();
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);

        setupToolbar();
        loadUserData();
    }

    private void initViews() {
        ivQRCode = findViewById(R.id.ivQRCode);
        tvAccountName = findViewById(R.id.tvAccountName);
        tvAccountNumber = findViewById(R.id.tvAccountNumber);
        tvBankName = findViewById(R.id.tvBankName);
        tvInstruction = findViewById(R.id.tvInstruction);
        btnSaveQR = findViewById(R.id.btnSaveQR);
        btnShareQR = findViewById(R.id.btnShareQR);
        progressBar = findViewById(R.id.progressBar);

        btnSaveQR.setOnClickListener(v -> saveQRCode());
        btnShareQR.setOnClickListener(v -> shareQRCode());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mã QR của tôi");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = sessionManager.getUserId();

        // Load user info
        firebaseHelper.getUser(userId,
            userDoc -> {
                currentUser = userDoc.toObject(User.class);
                
                // Load checking account
                firebaseHelper.getUserAccounts(userId,
                    querySnapshot -> {
                        for (var doc : querySnapshot.getDocuments()) {
                            Account acc = doc.toObject(Account.class);
                            if (acc != null && acc.getAccountType() == Account.AccountType.CHECKING) {
                                acc.setId(doc.getId());
                                checkingAccount = acc;
                                break;
                            }
                        }

                        progressBar.setVisibility(View.GONE);
                        
                        if (checkingAccount != null && currentUser != null) {
                            displayQRCode();
                        } else {
                            Toast.makeText(this, "Không tìm thấy tài khoản thanh toán", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    },
                    e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Lỗi tải dữ liệu tài khoản", Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tải thông tin người dùng", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void displayQRCode() {
        // Display account info
        String fullName = currentUser.getFullName() != null ? 
                currentUser.getFullName().toUpperCase() : "KHÁCH HÀNG";
        tvAccountName.setText(fullName);
        tvAccountNumber.setText(FormatUtils.formatAccountNumber(checkingAccount.getAccountNumber()));
        tvBankName.setText("TDT Bank");
        tvInstruction.setText("Quét mã QR để chuyển tiền");

        // Generate QR code
        try {
            JSONObject qrData = new JSONObject();
            qrData.put("bank", "TDT_BANK");
            qrData.put("accountNumber", checkingAccount.getAccountNumber());
            qrData.put("accountName", fullName);
            qrData.put("accountId", checkingAccount.getId());

            String qrContent = qrData.toString();
            
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 800, 800);
            
            BarcodeEncoder encoder = new BarcodeEncoder();
            qrBitmap = encoder.createBitmap(bitMatrix);
            
            ivQRCode.setImageBitmap(qrBitmap);
            
            // Enable buttons
            btnSaveQR.setEnabled(true);
            btnShareQR.setEnabled(true);
            
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveQRCode() {
        if (qrBitmap == null) {
            Toast.makeText(this, "Chưa có mã QR để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save to gallery
            String fileName = "TDT_Bank_QR_" + checkingAccount.getAccountNumber() + ".png";
            
            java.io.OutputStream fos;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, 
                        android.os.Environment.DIRECTORY_PICTURES + "/TDT Bank");
                
                android.net.Uri uri = getContentResolver().insert(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                
                if (uri != null) {
                    fos = getContentResolver().openOutputStream(uri);
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    if (fos != null) fos.close();
                    Toast.makeText(this, "✅ Đã lưu mã QR vào thư viện ảnh", Toast.LENGTH_SHORT).show();
                }
            } else {
                // For older Android versions
                String path = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES).toString();
                java.io.File file = new java.io.File(path, fileName);
                fos = new java.io.FileOutputStream(file);
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                
                // Notify gallery
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, 
                        android.net.Uri.fromFile(file)));
                Toast.makeText(this, "✅ Đã lưu mã QR: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi lưu mã QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void shareQRCode() {
        if (qrBitmap == null) {
            Toast.makeText(this, "Chưa có mã QR để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save bitmap to cache
            java.io.File cachePath = new java.io.File(getCacheDir(), "images");
            cachePath.mkdirs();
            
            java.io.File file = new java.io.File(cachePath, "qr_code.png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // Get URI using FileProvider
            android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file);

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                    "Mã QR tài khoản TDT Bank\n" +
                    "Tên: " + tvAccountName.getText() + "\n" +
                    "STK: " + checkingAccount.getAccountNumber());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Chia sẻ mã QR"));
            
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi chia sẻ mã QR", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
