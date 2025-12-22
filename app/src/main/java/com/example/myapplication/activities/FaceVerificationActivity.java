package com.example.myapplication.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.example.myapplication.R;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.FirebaseHelper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Face verification activity using ML Kit Face Detection and CameraX
 */
public class FaceVerificationActivity extends AppCompatActivity {

    private static final String TAG = "FaceVerification";
    private static final int CAMERA_PERMISSION_CODE = 100;

    private PreviewView previewView;
    private ImageView ivFaceFrame, ivFaceStatus;
    private TextView tvInstruction, tvTimer, tvTransactionInfo;
    private Button btnCancel;
    private ProgressBar progressBar;
    private View overlayView;

    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;

    private FirebaseHelper firebaseHelper;

    // Transaction data
    private String fromAccountId;
    private String toAccountId;
    private double amount;
    private double fee;
    private String description;
    private String transferType;
    private String recipientName;

    private boolean isFaceDetected = false;
    private boolean isVerifying = false;
    private int faceDetectionCount = 0;
    private static final int REQUIRED_DETECTIONS = 5;

    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verification);

        initViews();
        getIntentData();
        initFaceDetector();

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.preview_view);
        ivFaceFrame = findViewById(R.id.iv_face_frame);
        ivFaceStatus = findViewById(R.id.iv_face_status);
        tvInstruction = findViewById(R.id.tv_instruction);
        tvTimer = findViewById(R.id.tv_timer);
        tvTransactionInfo = findViewById(R.id.tv_transaction_info);
        btnCancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progress_bar);
        overlayView = findViewById(R.id.overlay_view);

        firebaseHelper = FirebaseHelper.getInstance();
        cameraExecutor = Executors.newSingleThreadExecutor();

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void getIntentData() {
        Intent intent = getIntent();
        fromAccountId = intent.getStringExtra("from_account_id");
        toAccountId = intent.getStringExtra("to_account_id");
        amount = intent.getDoubleExtra("amount", 0);
        fee = intent.getDoubleExtra("fee", 0);
        description = intent.getStringExtra("description");
        transferType = intent.getStringExtra("transfer_type");
        recipientName = intent.getStringExtra("recipient_name");

        // Show transaction info
        String info = String.format("Số tiền: %,.0f VND\nNgười nhận: %s", 
                amount, recipientName != null ? recipientName : "N/A");
        tvTransactionInfo.setText(info);
    }

    private void initFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();

        faceDetector = FaceDetection.getClient(options);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Cần cấp quyền camera để xác thực khuôn mặt", 
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));

        // Start countdown timer (30 seconds)
        startCountdownTimer();
    }

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (isVerifying) {
            imageProxy.close();
            return;
        }

        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    processFaces(faces);
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed", e);
                    imageProxy.close();
                });
    }

    private void processFaces(List<Face> faces) {
        runOnUiThread(() -> {
            if (faces.isEmpty()) {
                // No face detected
                isFaceDetected = false;
                faceDetectionCount = 0;
                tvInstruction.setText("Đưa khuôn mặt vào khung hình");
                ivFaceStatus.setImageResource(R.drawable.ic_face_searching);
            } else if (faces.size() > 1) {
                // Multiple faces
                tvInstruction.setText("Chỉ được có một khuôn mặt trong khung hình");
                ivFaceStatus.setImageResource(R.drawable.ic_face_error);
            } else {
                // Single face detected
                Face face = faces.get(0);
                float rightEyeOpenProb = face.getRightEyeOpenProbability() != null ? 
                        face.getRightEyeOpenProbability() : 0f;
                float leftEyeOpenProb = face.getLeftEyeOpenProbability() != null ? 
                        face.getLeftEyeOpenProbability() : 0f;

                if (rightEyeOpenProb > 0.5f && leftEyeOpenProb > 0.5f) {
                    isFaceDetected = true;
                    faceDetectionCount++;
                    
                    if (faceDetectionCount >= REQUIRED_DETECTIONS && !isVerifying) {
                        // Start verification
                        verifyFace();
                    } else {
                        int progress = (faceDetectionCount * 100) / REQUIRED_DETECTIONS;
                        tvInstruction.setText("Giữ nguyên... " + progress + "%");
                        ivFaceStatus.setImageResource(R.drawable.ic_face_detected);
                    }
                } else {
                    tvInstruction.setText("Vui lòng mở mắt");
                    ivFaceStatus.setImageResource(R.drawable.ic_face_searching);
                }
            }
        });
    }

    private void verifyFace() {
        isVerifying = true;
        
        runOnUiThread(() -> {
            tvInstruction.setText("Đang xác thực...");
            progressBar.setVisibility(View.VISIBLE);
            ivFaceStatus.setImageResource(R.drawable.ic_face_verified);
        });

        // Simulate face verification (in production, compare with stored face)
        // For demo purposes, we'll accept any valid face detection
        new android.os.Handler().postDelayed(() -> {
            processTransaction();
        }, 1500);
    }

    private void processTransaction() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Process the transaction
        if ("internal".equals(transferType)) {
            firebaseHelper.processTransfer(fromAccountId, toAccountId, amount, fee,
                    (fromSuccess, toSuccess) -> {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            if (fromSuccess && toSuccess) {
                                showSuccess();
                            } else {
                                showError("Giao dịch thất bại. Vui lòng thử lại.");
                            }
                        });
                    },
                    e -> runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        showError("Lỗi: " + e.getMessage());
                    })
            );
        } else {
            // External transfer - create transaction record
            Transaction transaction = new Transaction();
            transaction.setFromAccountId(fromAccountId);
            transaction.setToAccountId(toAccountId);
            transaction.setAmount(amount);
            transaction.setFee(fee);
            transaction.setType(Transaction.TransactionType.TRANSFER_EXTERNAL);
            transaction.setDescription(description);
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setFaceVerified(true);

            firebaseHelper.createTransaction(transaction,
                    docRef -> runOnUiThread(this::showSuccess),
                    e -> runOnUiThread(() -> showError("Lỗi: " + e.getMessage()))
            );
        }
    }

    private void showSuccess() {
        tvInstruction.setText("Xác thực thành công!");
        ivFaceStatus.setImageResource(R.drawable.ic_check_circle);
        
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent();
            intent.putExtra("verified", true);
            setResult(RESULT_OK, intent);
            finish();
        }, 1500);
    }

    private void showError(String message) {
        tvInstruction.setText(message);
        ivFaceStatus.setImageResource(R.drawable.ic_face_error);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        
        new android.os.Handler().postDelayed(() -> {
            setResult(RESULT_CANCELED);
            finish();
        }, 2000);
    }

    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                if (!isVerifying) {
                    showError("Hết thời gian xác thực");
                }
            }
        };
        countDownTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
}
