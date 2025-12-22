package com.example.myapplication.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.example.myapplication.R;

import java.util.Collections;
import java.util.List;

/**
 * Activity for scanning QR codes for payment
 */
public class QRScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRScanner";
    private static final int CAMERA_PERMISSION_CODE = 102;

    private DecoratedBarcodeView barcodeView;
    private TextView tvInstruction, tvResult;
    private ImageButton btnFlash, btnClose;
    
    private boolean isFlashOn = false;
    private boolean isScanning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        initViews();

        if (checkCameraPermission()) {
            initScanner();
        } else {
            requestCameraPermission();
        }
    }

    private void initViews() {
        barcodeView = findViewById(R.id.barcode_view);
        tvInstruction = findViewById(R.id.tv_instruction);
        tvResult = findViewById(R.id.tv_result);
        btnFlash = findViewById(R.id.btn_flash);
        btnClose = findViewById(R.id.btn_close);

        btnClose.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnFlash.setOnClickListener(v -> toggleFlash());
    }

    private void initScanner() {
        // Set decoder for QR codes only
        barcodeView.getBarcodeView().setDecoderFactory(
                new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));

        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && isScanning) {
                    isScanning = false;
                    handleQRResult(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Not used
            }
        });

        tvInstruction.setText("Đưa mã QR vào khung hình để quét");
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initScanner();
            } else {
                Toast.makeText(this, "Cần cấp quyền camera để quét mã QR",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void toggleFlash() {
        if (isFlashOn) {
            barcodeView.setTorchOff();
            btnFlash.setImageResource(R.drawable.ic_flash_off);
            isFlashOn = false;
        } else {
            barcodeView.setTorchOn();
            btnFlash.setImageResource(R.drawable.ic_flash_on);
            isFlashOn = true;
        }
    }

    private void handleQRResult(String result) {
        Log.d(TAG, "QR Result: " + result);

        // Parse QR content
        // Expected format: ACCOUNT_NUMBER|AMOUNT|DESCRIPTION
        // Or VietQR format: Bank transfer QR

        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText("Đang xử lý...");

        try {
            QRData qrData = parseQRCode(result);
            
            if (qrData != null) {
                // Return result to calling activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("account_number", qrData.accountNumber);
                resultIntent.putExtra("bank_code", qrData.bankCode);
                resultIntent.putExtra("amount", qrData.amount);
                resultIntent.putExtra("description", qrData.description);
                resultIntent.putExtra("raw_data", result);
                
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                // Invalid QR code
                showError("Mã QR không hợp lệ");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing QR", e);
            showError("Lỗi xử lý mã QR");
        }
    }

    private QRData parseQRCode(String content) {
        QRData data = new QRData();

        // Try to parse VietQR format
        if (content.contains("|")) {
            // Simple format: ACCOUNT|AMOUNT|DESCRIPTION
            String[] parts = content.split("\\|");
            if (parts.length >= 1) {
                data.accountNumber = parts[0];
            }
            if (parts.length >= 2) {
                try {
                    data.amount = Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {
                    data.amount = 0;
                }
            }
            if (parts.length >= 3) {
                data.description = parts[2];
            }
            return data;
        }

        // Try to parse VietQR EMV format
        if (content.startsWith("00020101")) {
            // This is EMV QR format - parse it
            return parseVietQR(content);
        }

        // Simple account number only
        if (content.matches("^[0-9]{10,20}$")) {
            data.accountNumber = content;
            return data;
        }

        // Try to parse as JSON
        try {
            org.json.JSONObject json = new org.json.JSONObject(content);
            data.accountNumber = json.optString("account", json.optString("accountNumber", ""));
            data.bankCode = json.optString("bank", json.optString("bankCode", ""));
            data.amount = json.optDouble("amount", 0);
            data.description = json.optString("description", json.optString("memo", ""));
            
            if (!data.accountNumber.isEmpty()) {
                return data;
            }
        } catch (Exception e) {
            // Not JSON
        }

        return null;
    }

    private QRData parseVietQR(String content) {
        QRData data = new QRData();
        
        try {
            // VietQR EMV format parsing (simplified)
            // Real implementation would need full EMV QR parsing
            
            // Look for account number pattern
            int idx = content.indexOf("0208");
            if (idx > 0) {
                int len = Integer.parseInt(content.substring(idx + 4, idx + 6));
                data.bankCode = content.substring(idx + 6, idx + 6 + len);
            }

            // Look for account number (field 01)
            idx = content.indexOf("01");
            if (idx > 0) {
                int start = idx + 4;
                if (start + 2 < content.length()) {
                    int len = Integer.parseInt(content.substring(idx + 2, idx + 4));
                    if (start + len <= content.length()) {
                        data.accountNumber = content.substring(start, start + len);
                    }
                }
            }

            // Look for amount (field 54)
            idx = content.indexOf("54");
            if (idx > 0 && idx + 4 < content.length()) {
                try {
                    int len = Integer.parseInt(content.substring(idx + 2, idx + 4));
                    if (idx + 4 + len <= content.length()) {
                        data.amount = Double.parseDouble(content.substring(idx + 4, idx + 4 + len));
                    }
                } catch (NumberFormatException e) {
                    // No amount
                }
            }

            return data.accountNumber != null && !data.accountNumber.isEmpty() ? data : null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing VietQR", e);
            return null;
        }
    }

    private void showError(String message) {
        tvResult.setText(message);
        tvResult.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
        
        // Allow re-scan after delay
        new android.os.Handler().postDelayed(() -> {
            isScanning = true;
            tvResult.setVisibility(View.GONE);
        }, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }

    // Data class for QR content
    private static class QRData {
        String accountNumber;
        String bankCode;
        double amount;
        String description;
    }
}
