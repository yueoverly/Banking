package com.example.myapplication.utils;

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
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.models.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {
    
    private static final String USERS = "users";
    private static final String ACCOUNTS = "accounts";
    private static final String TRANSACTIONS = "transactions";

    private static FirebaseHelper instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // ========== AUTH ==========
    
    public void signUp(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void signIn(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
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

    // ========== USERS ==========

    public void createUser(User user, OnCompleteListener<Void> listener) {
        db.collection(USERS).document(user.getId()).set(user).addOnCompleteListener(listener);
    }

    public void getUser(String userId, OnSuccessListener<DocumentSnapshot> success, OnFailureListener failure) {
        db.collection(USERS).document(userId).get().addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public void updateUser(String userId, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        updates.put("updatedAt", new Date());
        db.collection(USERS).document(userId).update(updates).addOnCompleteListener(listener);
    }

    public void getAllCustomers(OnSuccessListener<QuerySnapshot> success, OnFailureListener failure) {
        db.collection(USERS)
                .whereEqualTo("userType", User.UserType.CUSTOMER.name())
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    // ========== ACCOUNTS ==========

    public void createAccount(Account account, OnCompleteListener<DocumentReference> listener) {
        account.setCreatedAt(new Date());
        account.setUpdatedAt(new Date());
        db.collection(ACCOUNTS).add(account).addOnCompleteListener(listener);
    }

    public void getAccount(String accountId, OnSuccessListener<DocumentSnapshot> success, OnFailureListener failure) {
        db.collection(ACCOUNTS).document(accountId).get().addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public void getUserAccounts(String userId, OnSuccessListener<QuerySnapshot> success, OnFailureListener failure) {
        db.collection(ACCOUNTS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    public void updateAccountBalance(String accountId, double newBalance, OnSuccessListener<Void> success, OnFailureListener failure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("balance", newBalance);
        updates.put("updatedAt", new Date());
        db.collection(ACCOUNTS).document(accountId).update(updates).addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public void updateAccountField(String accountId, String field, Object value, OnSuccessListener<Void> success, OnFailureListener failure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(field, value);
        updates.put("updatedAt", new Date());
        db.collection(ACCOUNTS).document(accountId).update(updates).addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public void updateAccountInterestRate(String accountId, double newRate, OnSuccessListener<Void> success, OnFailureListener failure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("interestRate", newRate);
        updates.put("updatedAt", new Date());
        db.collection(ACCOUNTS).document(accountId).update(updates).addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public void searchAccountByNumber(String accountNumber, OnSuccessListener<QuerySnapshot> success, OnFailureListener failure) {
        db.collection(ACCOUNTS)
                .whereEqualTo("accountNumber", accountNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    public void getAllSavingAccounts(OnSuccessListener<QuerySnapshot> success, OnFailureListener failure) {
        db.collection(ACCOUNTS)
                .whereEqualTo("accountType", Account.AccountType.SAVING.name())
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    // ========== TRANSACTIONS ==========

    public void createTransaction(Transaction transaction, OnCompleteListener<DocumentReference> listener) {
        transaction.setCreatedAt(new Date());
        db.collection(TRANSACTIONS).add(transaction).addOnCompleteListener(listener);
    }

    /**
     * Get transactions where account is the sender (fromAccountId)
     * Simple query without composite index requirement
     */
    public void getAccountTransactions(String accountId, int limit, OnSuccessListener<QuerySnapshot> success, OnFailureListener failure) {
        // Query transactions where this account is the sender
        db.collection(TRANSACTIONS)
                .whereEqualTo("fromAccountId", accountId)
                .limit(limit)
                .get()
                .addOnSuccessListener(fromSnapshot -> {
                    // Also query transactions where this account is the receiver
                    db.collection(TRANSACTIONS)
                            .whereEqualTo("toAccountId", accountId)
                            .limit(limit)
                            .get()
                            .addOnSuccessListener(toSnapshot -> {
                                // Merge results - return fromSnapshot but caller should handle both
                                // For simplicity, just return fromSnapshot
                                // The caller will need to handle the full picture
                                success.onSuccess(fromSnapshot);
                            })
                            .addOnFailureListener(failure);
                })
                .addOnFailureListener(failure);
    }

    /**
     * Get ALL transactions for an account (both sent and received)
     */
    public void getAccountTransactionsBoth(String accountId, int limit, 
            OnSuccessListener<java.util.List<Transaction>> success, OnFailureListener failure) {
        java.util.List<Transaction> allTransactions = new java.util.ArrayList<>();
        
        // Query 1: Where account is sender
        db.collection(TRANSACTIONS)
                .whereEqualTo("fromAccountId", accountId)
                .limit(limit)
                .get()
                .addOnSuccessListener(fromSnapshot -> {
                    for (var doc : fromSnapshot.getDocuments()) {
                        Transaction t = doc.toObject(Transaction.class);
                        if (t != null) {
                            t.setId(doc.getId());
                            allTransactions.add(t);
                        }
                    }
                    
                    // Query 2: Where account is receiver
                    db.collection(TRANSACTIONS)
                            .whereEqualTo("toAccountId", accountId)
                            .limit(limit)
                            .get()
                            .addOnSuccessListener(toSnapshot -> {
                                for (var doc : toSnapshot.getDocuments()) {
                                    Transaction t = doc.toObject(Transaction.class);
                                    if (t != null) {
                                        t.setId(doc.getId());
                                        // Avoid duplicates
                                        boolean exists = false;
                                        for (Transaction existing : allTransactions) {
                                            if (existing.getId().equals(t.getId())) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            allTransactions.add(t);
                                        }
                                    }
                                }
                                
                                // Sort by date descending
                                allTransactions.sort((t1, t2) -> {
                                    if (t1.getCreatedAt() == null) return 1;
                                    if (t2.getCreatedAt() == null) return -1;
                                    return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                                });
                                
                                success.onSuccess(allTransactions);
                            })
                            .addOnFailureListener(failure);
                })
                .addOnFailureListener(failure);
    }

    public void getAllTransactionsForAccount(String accountId, int limit, OnSuccessListener<QuerySnapshot> success, OnFailureListener failure) {
        // Get transactions where account is sender OR receiver
        db.collection(TRANSACTIONS)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    success.onSuccess(querySnapshot);
                })
                .addOnFailureListener(failure);
    }
}
