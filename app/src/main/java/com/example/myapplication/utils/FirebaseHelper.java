package com.example.myapplication.utils;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Bill;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.models.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Helper class for Firebase operations
 */
public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static FirebaseHelper instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // ==================== Getter Methods ====================

    public FirebaseFirestore getFirestore() {
        return db;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseStorage getStorage() {
        return storage;
    }

    // ==================== Authentication ====================

    public void signUp(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    public void signIn(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    public void signOut() {
        auth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void sendPasswordResetEmail(String email, OnCompleteListener<Void> listener) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener(listener);
    }

    // ==================== User Operations ====================

    public void createUser(User user, OnCompleteListener<Void> listener) {
        if (user.getId() == null) {
            user.setId(getCurrentUserId());
        }
        db.collection(Constants.COLLECTION_USERS)
                .document(user.getId())
                .set(user)
                .addOnCompleteListener(listener);
    }

    public void getUser(String userId, OnSuccessListener<DocumentSnapshot> successListener,
                        OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getUserByEmail(String email, OnSuccessListener<QuerySnapshot> successListener,
                               OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateUser(String userId, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        updates.put("updatedAt", new Date());
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    public void deleteUser(String userId, OnCompleteListener<Void> listener) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .delete()
                .addOnCompleteListener(listener);
    }

    public void getAllCustomers(OnSuccessListener<QuerySnapshot> successListener,
                                OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("userType", "CUSTOMER")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getCustomersByStatus(String status, OnSuccessListener<QuerySnapshot> successListener,
                                     OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("userType", "CUSTOMER")
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateCustomerStatus(String customerId, String status, OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", new Date());

        db.collection(Constants.COLLECTION_USERS)
                .document(customerId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    public void verifyCustomer(String customerId, OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isVerified", true);
        updates.put("verifiedAt", new Date());
        updates.put("updatedAt", new Date());

        db.collection(Constants.COLLECTION_USERS)
                .document(customerId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    public void updateFaceVerification(String userId, boolean verified, String faceImageUrl,
                                       OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isFaceVerified", verified);
        updates.put("faceImageUrl", faceImageUrl);
        updates.put("updatedAt", new Date());

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    public void updateFcmToken(String userId, String token, OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        updates.put("updatedAt", new Date());

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    // ==================== Account Operations ====================

    public void createAccount(Account account, OnCompleteListener<DocumentReference> listener) {
        account.setCreatedAt(new Date());
        account.setUpdatedAt(new Date());
        db.collection(Constants.COLLECTION_ACCOUNTS)
                .add(account)
                .addOnCompleteListener(listener);
    }

    public void getAccount(String accountId, OnSuccessListener<DocumentSnapshot> successListener,
                           OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_ACCOUNTS)
                .document(accountId)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getUserAccounts(String userId, OnSuccessListener<QuerySnapshot> successListener,
                                OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_ACCOUNTS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getAccountByNumber(String accountNumber, OnSuccessListener<QuerySnapshot> successListener,
                                   OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_ACCOUNTS)
                .whereEqualTo("accountNumber", accountNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateAccount(String accountId, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        updates.put("updatedAt", new Date());
        db.collection(Constants.COLLECTION_ACCOUNTS)
                .document(accountId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    public void updateAccountBalance(String accountId, double newBalance, OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("balance", newBalance);
        updates.put("updatedAt", new Date());

        db.collection(Constants.COLLECTION_ACCOUNTS)
                .document(accountId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    public void updateInterestRate(String accountType, double newRate, OnCompleteListener<Void> listener) {
        db.collection(Constants.COLLECTION_ACCOUNTS)
                .whereEqualTo("accountType", accountType)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().update("interestRate", newRate, "updatedAt", new Date());
                    }
                    listener.onComplete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating interest rate", e);
                });
    }

    public String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("1001");
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    // ==================== Transaction Operations ====================

    public void createTransaction(Transaction transaction, OnCompleteListener<DocumentReference> listener) {
        transaction.setCreatedAt(new Date());
        db.collection(Constants.COLLECTION_TRANSACTIONS)
                .add(transaction)
                .addOnCompleteListener(listener);
    }

    public void getTransaction(String transactionId, OnSuccessListener<DocumentSnapshot> successListener,
                               OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_TRANSACTIONS)
                .document(transactionId)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateTransaction(String transactionId, Map<String, Object> updates,
                                  OnCompleteListener<Void> listener) {
        updates.put("updatedAt", new Date());
        db.collection(Constants.COLLECTION_TRANSACTIONS)
                .document(transactionId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    public void getAccountTransactions(String accountId, int limit,
                                       OnSuccessListener<QuerySnapshot> successListener,
                                       OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_TRANSACTIONS)
                .whereEqualTo("fromAccountId", accountId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getUserTransactions(String userId, int limit,
                                    OnSuccessListener<QuerySnapshot> successListener,
                                    OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getTransactionsByDateRange(String accountId, Date startDate, Date endDate,
                                           OnSuccessListener<QuerySnapshot> successListener,
                                           OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_TRANSACTIONS)
                .whereEqualTo("fromAccountId", accountId)
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // ==================== Transfer Processing ====================

    public void processTransfer(Transaction transaction, Account fromAccount, Account toAccount,
                                OnSuccessListener<String> successListener,
                                OnFailureListener failureListener) {
        db.runTransaction(firestoreTransaction -> {
            DocumentReference fromRef = db.collection(Constants.COLLECTION_ACCOUNTS)
                    .document(fromAccount.getId());
            DocumentReference toRef = db.collection(Constants.COLLECTION_ACCOUNTS)
                    .document(toAccount.getId());

            DocumentSnapshot fromSnapshot = firestoreTransaction.get(fromRef);
            DocumentSnapshot toSnapshot = firestoreTransaction.get(toRef);

            Double fromBalanceObj = fromSnapshot.getDouble("balance");
            Double toBalanceObj = toSnapshot.getDouble("balance");
            
            double fromBalance = fromBalanceObj != null ? fromBalanceObj : 0;
            double toBalance = toBalanceObj != null ? toBalanceObj : 0;

            if (fromBalance < transaction.getAmount() + transaction.getFee()) {
                throw new RuntimeException("Insufficient balance");
            }

            double newFromBalance = fromBalance - transaction.getAmount() - transaction.getFee();
            double newToBalance = toBalance + transaction.getAmount();

            firestoreTransaction.update(fromRef, "balance", newFromBalance, "updatedAt", new Date());
            firestoreTransaction.update(toRef, "balance", newToBalance, "updatedAt", new Date());

            return null;
        }).addOnSuccessListener(aVoid -> {
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setCompletedAt(new Date());

            createTransaction(transaction, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    successListener.onSuccess(task.getResult().getId());
                } else {
                    failureListener.onFailure(new Exception("Failed to save transaction"));
                }
            });
        }).addOnFailureListener(failureListener);
    }

    // ==================== OTP Operations ====================

    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public void saveOTP(String transactionId, String otp, OnCompleteListener<Void> listener) {
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("otp", otp);
        otpData.put("transactionId", transactionId);
        otpData.put("createdAt", new Date());
        otpData.put("expiresAt", new Date(System.currentTimeMillis() + Constants.OTP_EXPIRY_MINUTES * 60 * 1000));
        otpData.put("isUsed", false);

        db.collection(Constants.COLLECTION_OTP)
                .document(transactionId)
                .set(otpData)
                .addOnCompleteListener(listener);
    }

    public void verifyOTP(String transactionId, String inputOtp,
                          OnSuccessListener<Boolean> successListener,
                          OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_OTP)
                .document(transactionId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String savedOtp = document.getString("otp");
                        Date expiresAt = document.getDate("expiresAt");
                        Boolean isUsed = document.getBoolean("isUsed");

                        if (isUsed != null && isUsed) {
                            successListener.onSuccess(false);
                        } else if (expiresAt != null && expiresAt.before(new Date())) {
                            successListener.onSuccess(false);
                        } else if (savedOtp != null && savedOtp.equals(inputOtp)) {
                            document.getReference().update("isUsed", true);
                            successListener.onSuccess(true);
                        } else {
                            successListener.onSuccess(false);
                        }
                    } else {
                        successListener.onSuccess(false);
                    }
                })
                .addOnFailureListener(failureListener);
    }

    // ==================== Bill Operations ====================

    public void createBill(Bill bill, OnCompleteListener<DocumentReference> listener) {
        bill.setCreatedAt(new Date());
        db.collection(Constants.COLLECTION_BILLS)
                .add(bill)
                .addOnCompleteListener(listener);
    }

    public void getUserBills(String userId, OnSuccessListener<QuerySnapshot> successListener,
                             OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_BILLS)
                .whereEqualTo("userId", userId)
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getUnpaidBills(String userId, OnSuccessListener<QuerySnapshot> successListener,
                               OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_BILLS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "UNPAID")
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateBillStatus(String billId, String status, String transactionId,
                                 OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("paidDate", new Date());
        updates.put("transactionId", transactionId);
        updates.put("updatedAt", new Date());

        db.collection(Constants.COLLECTION_BILLS)
                .document(billId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    // ==================== Bank Branch Operations ====================

    public void getBankBranches(OnSuccessListener<QuerySnapshot> successListener,
                                OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_BRANCHES)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getNearbyBranches(double latitude, double longitude, double radiusKm,
                                  OnSuccessListener<QuerySnapshot> successListener,
                                  OnFailureListener failureListener) {
        getBankBranches(successListener, failureListener);
    }

    // ==================== Storage Operations ====================

    public void uploadImage(String path, Uri imageUri, OnSuccessListener<Uri> successListener,
                            OnFailureListener failureListener) {
        StorageReference storageRef = storage.getReference().child(path);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(successListener)
                            .addOnFailureListener(failureListener);
                })
                .addOnFailureListener(failureListener);
    }

    public void uploadFaceImage(String userId, Uri imageUri, OnSuccessListener<Uri> successListener,
                                OnFailureListener failureListener) {
        String path = "faces/" + userId + "/" + System.currentTimeMillis() + ".jpg";
        uploadImage(path, imageUri, successListener, failureListener);
    }

    public void uploadProfileImage(String userId, Uri imageUri, OnSuccessListener<Uri> successListener,
                                   OnFailureListener failureListener) {
        String path = "profiles/" + userId + "/" + System.currentTimeMillis() + ".jpg";
        uploadImage(path, imageUri, successListener, failureListener);
    }

    public void uploadIdCardImage(String userId, Uri imageUri, String side,
                                  OnSuccessListener<Uri> successListener,
                                  OnFailureListener failureListener) {
        String path = "id_cards/" + userId + "/" + side + "_" + System.currentTimeMillis() + ".jpg";
        uploadImage(path, imageUri, successListener, failureListener);
    }

    public void deleteImage(String imageUrl, OnCompleteListener<Void> listener) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            storage.getReferenceFromUrl(imageUrl)
                    .delete()
                    .addOnCompleteListener(listener);
        }
    }

    // ==================== Notification Operations ====================

    public void createNotification(String userId, String title, String message, String type,
                                   OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("isRead", false);
        notification.put("createdAt", new Date());

        db.collection(Constants.COLLECTION_NOTIFICATIONS)
                .add(notification)
                .addOnCompleteListener(listener);
    }

    public void getUserNotifications(String userId, int limit,
                                     OnSuccessListener<QuerySnapshot> successListener,
                                     OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void markNotificationAsRead(String notificationId, OnCompleteListener<Void> listener) {
        db.collection(Constants.COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update("isRead", true)
                .addOnCompleteListener(listener);
    }

    // ==================== Statistics for Officers ====================

    public void getTodayTransactionsCount(OnSuccessListener<Integer> successListener,
                                          OnFailureListener failureListener) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();

        db.collection(Constants.COLLECTION_TRANSACTIONS)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    successListener.onSuccess(querySnapshot.size());
                })
                .addOnFailureListener(failureListener);
    }

    public void getTotalCustomersCount(OnSuccessListener<Integer> successListener,
                                       OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("userType", "CUSTOMER")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    successListener.onSuccess(querySnapshot.size());
                })
                .addOnFailureListener(failureListener);
    }

    public void getPendingVerificationsCount(OnSuccessListener<Integer> successListener,
                                             OnFailureListener failureListener) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("userType", "CUSTOMER")
                .whereEqualTo("isVerified", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    successListener.onSuccess(querySnapshot.size());
                })
                .addOnFailureListener(failureListener);
    }

    // ==================== Utility Methods ====================

    public void checkAccountExists(String accountNumber, OnSuccessListener<Boolean> successListener,
                                   OnFailureListener failureListener) {
        getAccountByNumber(accountNumber, querySnapshot -> {
            successListener.onSuccess(!querySnapshot.isEmpty());
        }, failureListener);
    }

    public void checkEmailExists(String email, OnSuccessListener<Boolean> successListener,
                                 OnFailureListener failureListener) {
        getUserByEmail(email, querySnapshot -> {
            successListener.onSuccess(!querySnapshot.isEmpty());
        }, failureListener);
    }

    // ==================== Initialize Sample Data ====================

    public void initializeSampleBranches() {
        String[][] branches = {
                {"TDT Bank - Chi nhánh Tôn Đức Thắng", "19 Nguyễn Hữu Thọ, Quận 7, TP.HCM", "10.7326", "106.6997", "028 3775 5555"},
                {"TDT Bank - Chi nhánh Quận 1", "123 Nguyễn Du, Quận 1, TP.HCM", "10.7769", "106.7009", "028 3822 1111"},
                {"TDT Bank - Chi nhánh Quận 3", "456 Điện Biên Phủ, Quận 3, TP.HCM", "10.7873", "106.6879", "028 3930 2222"},
                {"TDT Bank - Chi nhánh Quận 7", "789 Nguyễn Văn Linh, Quận 7, TP.HCM", "10.7418", "106.7054", "028 5411 3333"},
                {"TDT Bank - Chi nhánh Bình Thạnh", "321 Xô Viết Nghệ Tĩnh, Bình Thạnh, TP.HCM", "10.8012", "106.7139", "028 3512 4444"}
        };

        for (String[] branch : branches) {
            Map<String, Object> branchData = new HashMap<>();
            branchData.put("name", branch[0]);
            branchData.put("address", branch[1]);
            branchData.put("latitude", Double.parseDouble(branch[2]));
            branchData.put("longitude", Double.parseDouble(branch[3]));
            branchData.put("phoneNumber", branch[4]);
            branchData.put("workingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6), 08:00 - 12:00 (Thứ 7)");
            branchData.put("hasATM", true);
            branchData.put("isMainBranch", branch[0].contains("Tôn Đức Thắng"));
            branchData.put("createdAt", new Date());

            db.collection(Constants.COLLECTION_BRANCHES)
                    .add(branchData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Branch added: " + branch[0]);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding branch", e);
                    });
        }
    }

    public void initializeSampleRates() {
        Map<String, Object> rates = new HashMap<>();
        rates.put("saving_1m", 3.0);
        rates.put("saving_3m", 3.5);
        rates.put("saving_6m", 4.5);
        rates.put("saving_12m", 5.5);
        rates.put("mortgage", 8.5);
        rates.put("updatedAt", new Date());

        db.collection("settings")
                .document("interest_rates")
                .set(rates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Interest rates initialized");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error initializing rates", e);
                });
    }

    public void getInterestRates(OnSuccessListener<DocumentSnapshot> successListener,
                                 OnFailureListener failureListener) {
        db.collection("settings")
                .document("interest_rates")
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateInterestRates(Map<String, Object> rates, OnCompleteListener<Void> listener) {
        rates.put("updatedAt", new Date());
        db.collection("settings")
                .document("interest_rates")
                .update(rates)
                .addOnCompleteListener(listener);
    }
}
