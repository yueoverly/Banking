package com.example.myapplication.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.utils.FirebaseHelper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

import java.io.InputStream;

/**
 * QRScannerActivity - Quét mã QR để chuyển tiền
 * - Hỗ trợ quét bằng camera
 * - Hỗ trợ chọn ảnh QR từ thư viện
 */
public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private DecoratedBarcodeView barcodeView;
    private Button btnPickImage, btnMyQR, btnFlash;
    private TextView tvInstruction;
    private View layoutButtons;

    private FirebaseHelper firebaseHelper;
    private boolean isFlashOn = false;
    private boolean isProcessing = false;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        decodeQRFromImage(imageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        initViews();
        firebaseHelper = FirebaseHelper.getInstance();
        
        setupToolbar();
        checkCameraPermission();
    }

    private void initViews() {
        barcodeView = findViewById(R.id.barcodeView);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnMyQR = findViewById(R.id.btnMyQR);
        btnFlash = findViewById(R.id.btnFlash);
        tvInstruction = findViewById(R.id.tvInstruction);
        layoutButtons = findViewById(R.id.layoutButtons);

        btnPickImage.setOnClickListener(v -> openImagePicker());
        btnMyQR.setOnClickListener(v -> openMyQRCode());
        btnFlash.setOnClickListener(v -> toggleFlash());

        // Setup barcode scanner
        barcodeView.setStatusText("");
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && result.getText() != null && !isProcessing) {
                    isProcessing = true;
                    barcodeView.pause();
                    processQRResult(result.getText());
                }
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quét mã QR");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Cần quyền camera để quét mã QR", Toast.LENGTH_SHORT).show();
                tvInstruction.setText("Vui lòng cấp quyền camera hoặc chọn ảnh từ thư viện");
            }
        }
    }

    private void startScanning() {
        barcodeView.resume();
        tvInstruction.setText("Đưa mã QR vào khung hình để quét");
    }

    private void toggleFlash() {
        if (isFlashOn) {
            barcodeView.setTorchOff();
            btnFlash.setText("🔦 Bật đèn");
            isFlashOn = false;
        } else {
            barcodeView.setTorchOn();
            btnFlash.setText("🔦 Tắt đèn");
            isFlashOn = true;
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void openMyQRCode() {
        startActivity(new Intent(this, QRCodeActivity.class));
    }

    /**
     * Decode QR code from selected image
     */
    private void decodeQRFromImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) {
                Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
                return;
            }

            // Decode QR from bitmap
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            MultiFormatReader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);

            if (result != null && result.getText() != null) {
                processQRResult(result.getText());
            } else {
                Toast.makeText(this, "Không tìm thấy mã QR trong ảnh", Toast.LENGTH_SHORT).show();
            }

        } catch (com.google.zxing.NotFoundException e) {
            Toast.makeText(this, "Không tìm thấy mã QR trong ảnh", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Process scanned QR result
     */
    private void processQRResult(String qrContent) {
        try {
            // Try to parse as JSON (TDT Bank format)
            if (qrContent.startsWith("{")) {
                JSONObject json = new JSONObject(qrContent);
                
                String bank = json.optString("bank", "");
                String accountNumber = json.optString("accountNumber", "");
                String accountName = json.optString("accountName", "");
                String accountId = json.optString("accountId", "");

                if ("TDT_BANK".equals(bank) && !accountNumber.isEmpty()) {
                    showTransferConfirmation(accountNumber, accountName, accountId);
                    return;
                }
            }

            // Try to parse as simple account number
            if (qrContent.matches("\\d{10,16}")) {
                lookupAccountByNumber(qrContent);
                return;
            }

            // Unknown format
            showUnknownQRDialog(qrContent);

        } catch (Exception e) {
            showUnknownQRDialog(qrContent);
        }
    }

    /**
     * Show transfer confirmation dialog for TDT Bank QR
     */
    private void showTransferConfirmation(String accountNumber, String accountName, String accountId) {
        new AlertDialog.Builder(this)
                .setTitle("✅ Mã QR TDT Bank")
                .setMessage("Tên: " + accountName + "\n" +
                           "Số TK: " + accountNumber + "\n\n" +
                           "Bạn có muốn chuyển tiền đến tài khoản này?")
                .setPositiveButton("Chuyển tiền", (dialog, which) -> {
                    Intent intent = new Intent(this, TransferActivity.class);
                    intent.putExtra("recipient_account_number", accountNumber);
                    intent.putExtra("recipient_name", accountName);
                    intent.putExtra("recipient_account_id", accountId);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Quét lại", (dialog, which) -> resumeScanning())
                .setOnCancelListener(dialog -> resumeScanning())
                .show();
    }

    /**
     * Lookup account by number (for simple QR codes)
     */
    private void lookupAccountByNumber(String accountNumber) {
        firebaseHelper.searchAccountByNumber(accountNumber,
            querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    Account account = querySnapshot.getDocuments().get(0).toObject(Account.class);
                    if (account != null) {
                        account.setId(querySnapshot.getDocuments().get(0).getId());
                        
                        // Get user name
                        firebaseHelper.getUser(account.getUserId(),
                            userDoc -> {
                                String name = "Khách hàng TDT Bank";
                                if (userDoc.exists()) {
                                    name = userDoc.getString("fullName");
                                    if (name == null) name = "Khách hàng TDT Bank";
                                }
                                showTransferConfirmation(accountNumber, name, account.getId());
                            },
                            e -> showTransferConfirmation(accountNumber, "Khách hàng TDT Bank", account.getId())
                        );
                        return;
                    }
                }
                
                new AlertDialog.Builder(this)
                        .setTitle("Không tìm thấy")
                        .setMessage("Không tìm thấy tài khoản với số: " + accountNumber)
                        .setPositiveButton("Quét lại", (d, w) -> resumeScanning())
                        .setOnCancelListener(d -> resumeScanning())
                        .show();
            },
            e -> {
                Toast.makeText(this, "Lỗi tìm kiếm tài khoản", Toast.LENGTH_SHORT).show();
                resumeScanning();
            }
        );
    }

    /**
     * Show dialog for unknown QR format
     */
    private void showUnknownQRDialog(String content) {
        String displayContent = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        
        new AlertDialog.Builder(this)
                .setTitle("Mã QR không hỗ trợ")
                .setMessage("Nội dung:\n" + displayContent + "\n\n" +
                           "Mã QR này không phải của TDT Bank.")
                .setPositiveButton("Quét lại", (dialog, which) -> resumeScanning())
                .setNegativeButton("Đóng", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> resumeScanning())
                .show();
    }

    private void resumeScanning() {
        isProcessing = false;
        barcodeView.resume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}
