package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.TransactionAdapter;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccountDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private LinearLayout cardBackground;
    private TextView tvAccountType, tvAccountNumber, tvBalance, tvBalanceLabel, tvStatus;
    
    // Saving account views
    private CardView cardSavingInfo;
    private TextView tvInterestRate, tvMonthlyProfit, tvYearlyProfit;
    
    // Mortgage account views
    private CardView cardMortgageInfo;
    private TextView tvLoanAmount, tvLoanTerm, tvMortgageInterestRate, tvRemainingDebt;
    private TextView tvPaymentFrequencyLabel, tvPaymentAmount, tvBiWeeklyPayment;
    
    private Button btnDeposit, btnWithdraw;
    private RecyclerView rvTransactions;
    private TextView tvNoTransactions;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private Account account;
    private Account checkingAccount; // TK Thanh toán để liên kết với TK Tiết kiệm
    private String accountId;
    private List<Transaction> transactions = new ArrayList<>();

    // Pending transaction
    private double pendingAmount;
    private String pendingTransactionType;

    // OTP Result Launcher
    private final ActivityResultLauncher<Intent> otpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == OTPVerificationActivity.RESULT_OTP_VERIFIED) {
                    switch (pendingTransactionType) {
                        case "DEPOSIT":
                            processDeposit(pendingAmount);
                            break;
                        case "WITHDRAW":
                            processWithdraw(pendingAmount);
                            break;
                        case "LOAN_PAYMENT":
                            processLoanPayment(pendingAmount);
                            break;
                        case "SAVING_DEPOSIT":
                            processSavingDeposit(pendingAmount);
                            break;
                        case "SAVING_WITHDRAW":
                            processSavingWithdraw(pendingAmount);
                            break;
                    }
                } else {
                    Toast.makeText(this, "Giao dịch đã bị hủy", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);

        accountId = getIntent().getStringExtra("account_id");
        if (accountId == null) {
            finish();
            return;
        }

        initViews();
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
        setupToolbar();
        setupListeners();
        loadAccountData();
        
        // Handle action from MainActivity
        String action = getIntent().getStringExtra("action");
        if (action != null) {
            // Delay to allow data to load
            new android.os.Handler().postDelayed(() -> {
                if (account != null) {
                    if ("deposit".equals(action)) {
                        if (account.getAccountType() == Account.AccountType.MORTGAGE) {
                            showLoanPaymentDialog();
                        } else {
                            showDepositDialog();
                        }
                    } else if ("withdraw".equals(action)) {
                        showWithdrawDialog();
                    }
                }
            }, 500);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        cardBackground = findViewById(R.id.cardBackground);
        tvAccountType = findViewById(R.id.tvAccountType);
        tvAccountNumber = findViewById(R.id.tvAccountNumber);
        tvBalance = findViewById(R.id.tvBalance);
        tvBalanceLabel = findViewById(R.id.tvBalanceLabel);
        tvStatus = findViewById(R.id.tvStatus);
        
        // Saving
        cardSavingInfo = findViewById(R.id.cardSavingInfo);
        tvInterestRate = findViewById(R.id.tvInterestRate);
        tvMonthlyProfit = findViewById(R.id.tvMonthlyProfit);
        tvYearlyProfit = findViewById(R.id.tvYearlyProfit);
        
        // Mortgage
        cardMortgageInfo = findViewById(R.id.cardMortgageInfo);
        tvLoanAmount = findViewById(R.id.tvLoanAmount);
        tvLoanTerm = findViewById(R.id.tvLoanTerm);
        tvMortgageInterestRate = findViewById(R.id.tvMortgageInterestRate);
        tvRemainingDebt = findViewById(R.id.tvRemainingDebt);
        tvPaymentFrequencyLabel = findViewById(R.id.tvPaymentFrequencyLabel);
        tvPaymentAmount = findViewById(R.id.tvPaymentAmount);
        tvBiWeeklyPayment = findViewById(R.id.tvBiWeeklyPayment);
        
        btnDeposit = findViewById(R.id.btnDeposit);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        rvTransactions = findViewById(R.id.rvTransactions);
        tvNoTransactions = findViewById(R.id.tvNoTransactions);
        progressBar = findViewById(R.id.progressBar);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnDeposit.setOnClickListener(v -> {
            if (account != null && account.getAccountType() == Account.AccountType.MORTGAGE) {
                showLoanPaymentDialog();
            } else {
                showDepositDialog();
            }
        });
        btnWithdraw.setOnClickListener(v -> showWithdrawDialog());
    }

    private void loadAccountData() {
        progressBar.setVisibility(View.VISIBLE);
        firebaseHelper.getAccount(accountId,
            documentSnapshot -> {
                account = documentSnapshot.toObject(Account.class);
                if (account != null) {
                    account.setId(documentSnapshot.getId());
                    
                    // If this is a SAVING account, also load the CHECKING account
                    if (account.getAccountType() == Account.AccountType.SAVING) {
                        loadCheckingAccount();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        displayAccountData();
                        loadTransactions();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        );
    }

    /**
     * Load TK Thanh toán để liên kết với TK Tiết kiệm
     */
    private void loadCheckingAccount() {
        String userId = sessionManager.getUserId();
        firebaseHelper.getUserAccounts(userId,
            querySnapshot -> {
                progressBar.setVisibility(View.GONE);
                checkingAccount = null;
                
                for (var doc : querySnapshot.getDocuments()) {
                    Account acc = doc.toObject(Account.class);
                    if (acc != null && acc.getAccountType() == Account.AccountType.CHECKING) {
                        acc.setId(doc.getId());
                        checkingAccount = acc;
                        break;
                    }
                }
                
                displayAccountData();
                loadTransactions();
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                displayAccountData();
                loadTransactions();
            }
        );
    }

    private void displayAccountData() {
        // Hide all type-specific cards first
        cardSavingInfo.setVisibility(View.GONE);
        cardMortgageInfo.setVisibility(View.GONE);
        
        switch (account.getAccountType()) {
            case CHECKING:
                displayCheckingAccount();
                break;
            case SAVING:
                displaySavingAccount();
                break;
            case MORTGAGE:
                displayMortgageAccount();
                break;
        }

        // Common fields
        tvAccountNumber.setText(account.getFormattedAccountNumber());
        
        String statusText = "Hoạt động";
        if (account.getStatus() != null) {
            switch (account.getStatus()) {
                case ACTIVE: statusText = "Hoạt động"; break;
                case INACTIVE: statusText = "Không hoạt động"; break;
                case FROZEN: statusText = "Đóng băng"; break;
                case CLOSED: statusText = "Đã đóng"; break;
            }
        }
        tvStatus.setText(statusText);
    }

    /**
     * CHECKING ACCOUNT: Chỉ hiển thị số dư
     */
    private void displayCheckingAccount() {
        tvAccountType.setText("Tài khoản Thanh toán");
        cardBackground.setBackgroundResource(R.drawable.bg_gradient_primary);
        tvBalanceLabel.setText("Số dư khả dụng");
        tvBalance.setText(FormatUtils.formatCurrency(account.getBalance()));
        
        // Hiển thị cả 2 nút
        btnDeposit.setText("Nạp tiền");
        btnWithdraw.setVisibility(View.VISIBLE);
    }

    /**
     * SAVING ACCOUNT: Hiển thị số dư, lãi suất, lợi nhuận hàng tháng
     */
    private void displaySavingAccount() {
        tvAccountType.setText("Tài khoản Tiết kiệm");
        cardBackground.setBackgroundResource(R.drawable.bg_gradient_saving);
        tvBalanceLabel.setText("Số dư tiết kiệm");
        tvBalance.setText(FormatUtils.formatCurrency(account.getBalance()));
        
        // Hiển thị thông tin lãi suất
        cardSavingInfo.setVisibility(View.VISIBLE);
        tvInterestRate.setText(FormatUtils.formatInterestRate(account.getInterestRate()));
        tvMonthlyProfit.setText(FormatUtils.formatCurrency(account.getMonthlyProfit()));
        tvYearlyProfit.setText(FormatUtils.formatCurrency(account.getYearlyProfit()));
        
        // Nút nạp/rút từ TK Thanh toán
        btnDeposit.setText("Gửi thêm tiền");
        btnWithdraw.setText("Rút về TK Thanh toán");
        btnWithdraw.setVisibility(View.VISIBLE);
    }

    /**
     * MORTGAGE ACCOUNT: Hiển thị số tiền cần trả hàng tháng/2 tuần
     */
    private void displayMortgageAccount() {
        tvAccountType.setText("Tài khoản Vay thế chấp");
        cardBackground.setBackgroundResource(R.drawable.bg_gradient_mortgage);
        tvBalanceLabel.setText("Dư nợ gốc còn lại");
        
        double remainingDebt = account.getRemainingDebt() > 0 ? 
                account.getRemainingDebt() : account.getLoanAmount();
        tvBalance.setText(FormatUtils.formatCurrency(remainingDebt));
        
        // Hiển thị thông tin khoản vay
        cardMortgageInfo.setVisibility(View.VISIBLE);
        tvLoanAmount.setText(FormatUtils.formatCurrency(account.getLoanAmount()));
        tvLoanTerm.setText(account.getLoanTermMonths() + " tháng");
        tvMortgageInterestRate.setText(FormatUtils.formatInterestRate(account.getInterestRate()));
        tvRemainingDebt.setText(FormatUtils.formatCurrency(remainingDebt));
        
        // Số tiền cần trả
        double monthlyPayment = account.getMonthlyPayment();
        double biWeeklyPayment = account.getBiWeeklyPayment();
        
        tvPaymentFrequencyLabel.setText("💳 Số tiền cần trả hàng tháng");
        tvPaymentAmount.setText(FormatUtils.formatCurrency(monthlyPayment));
        tvBiWeeklyPayment.setText("Hoặc: " + FormatUtils.formatCurrency(biWeeklyPayment) + " / 2 tuần");
        
        // Chỉ hiển thị nút Trả nợ
        btnDeposit.setText("Trả nợ");
        btnWithdraw.setVisibility(View.GONE);
    }

    private void loadTransactions() {
        firebaseHelper.getAccountTransactionsBoth(accountId, 20,
            transactionList -> {
                transactions.clear();
                transactions.addAll(transactionList);

                TransactionAdapter adapter = new TransactionAdapter(this, transactions, accountId);
                rvTransactions.setAdapter(adapter);
                tvNoTransactions.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
            },
            e -> tvNoTransactions.setVisibility(View.VISIBLE)
        );
    }

    private void showDepositDialog() {
        if (account == null) return;

        // SAVING account: tiền nạp từ TK Thanh toán
        if (account.getAccountType() == Account.AccountType.SAVING) {
            showSavingDepositDialog();
            return;
        }

        // CHECKING account: nạp tiền bình thường
        EditText input = new EditText(this);
        input.setHint("Nhập số tiền");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Nạp tiền")
                .setMessage("Số dư hiện tại: " + FormatUtils.formatCurrency(account.getBalance()))
                .setView(input)
                .setPositiveButton("Tiếp tục", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString());
                        if (amount >= 10000) {
                            pendingAmount = amount;
                            pendingTransactionType = "DEPOSIT";
                            launchOTPVerification(amount, "Nạp tiền");
                        } else {
                            Toast.makeText(this, "Số tiền tối thiểu 10,000 đ", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Dialog nạp tiền vào TK Tiết kiệm - tiền từ TK Thanh toán
     */
    private void showSavingDepositDialog() {
        if (checkingAccount == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Không thể nạp tiền")
                    .setMessage("Bạn cần có tài khoản Thanh toán để nạp tiền vào tài khoản Tiết kiệm.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Nhập số tiền muốn chuyển");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(48, 32, 48, 32);

        String message = "💳 TK Thanh toán: " + FormatUtils.formatCurrency(checkingAccount.getBalance()) + "\n" +
                         "💰 TK Tiết kiệm: " + FormatUtils.formatCurrency(account.getBalance()) + "\n\n" +
                         "Nhập số tiền muốn chuyển từ TK Thanh toán sang TK Tiết kiệm:";

        new AlertDialog.Builder(this)
                .setTitle("Nạp tiền vào TK Tiết kiệm")
                .setMessage(message)
                .setView(input)
                .setPositiveButton("Tiếp tục", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString());
                        if (amount < 10000) {
                            Toast.makeText(this, "Số tiền tối thiểu 10,000 đ", Toast.LENGTH_SHORT).show();
                        } else if (amount > checkingAccount.getBalance()) {
                            Toast.makeText(this, "Số dư TK Thanh toán không đủ!\nSố dư: " + 
                                    FormatUtils.formatCurrency(checkingAccount.getBalance()), Toast.LENGTH_LONG).show();
                        } else {
                            pendingAmount = amount;
                            pendingTransactionType = "SAVING_DEPOSIT";
                            launchOTPVerification(amount, "Nạp tiền TK Tiết kiệm");
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showWithdrawDialog() {
        if (account == null) return;

        // SAVING account: tiền rút về TK Thanh toán
        if (account.getAccountType() == Account.AccountType.SAVING) {
            showSavingWithdrawDialog();
            return;
        }

        // CHECKING account: rút tiền bình thường
        EditText input = new EditText(this);
        input.setHint("Nhập số tiền");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Rút tiền")
                .setMessage("Số dư hiện tại: " + FormatUtils.formatCurrency(account.getBalance()))
                .setView(input)
                .setPositiveButton("Tiếp tục", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString());
                        if (amount < 10000) {
                            Toast.makeText(this, "Số tiền tối thiểu 10,000 đ", Toast.LENGTH_SHORT).show();
                        } else if (amount > account.getBalance()) {
                            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
                        } else {
                            pendingAmount = amount;
                            pendingTransactionType = "WITHDRAW";
                            launchOTPVerification(amount, "Rút tiền");
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Dialog rút tiền từ TK Tiết kiệm - tiền về TK Thanh toán
     */
    private void showSavingWithdrawDialog() {
        if (checkingAccount == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Không thể rút tiền")
                    .setMessage("Bạn cần có tài khoản Thanh toán để nhận tiền rút từ tài khoản Tiết kiệm.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Nhập số tiền muốn rút");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(48, 32, 48, 32);

        String message = "💰 TK Tiết kiệm: " + FormatUtils.formatCurrency(account.getBalance()) + "\n" +
                         "💳 TK Thanh toán: " + FormatUtils.formatCurrency(checkingAccount.getBalance()) + "\n\n" +
                         "Nhập số tiền muốn rút từ TK Tiết kiệm về TK Thanh toán:";

        new AlertDialog.Builder(this)
                .setTitle("Rút tiền từ TK Tiết kiệm")
                .setMessage(message)
                .setView(input)
                .setPositiveButton("Tiếp tục", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString());
                        if (amount < 10000) {
                            Toast.makeText(this, "Số tiền tối thiểu 10,000 đ", Toast.LENGTH_SHORT).show();
                        } else if (amount > account.getBalance()) {
                            Toast.makeText(this, "Số dư TK Tiết kiệm không đủ!\nSố dư: " + 
                                    FormatUtils.formatCurrency(account.getBalance()), Toast.LENGTH_LONG).show();
                        } else {
                            pendingAmount = amount;
                            pendingTransactionType = "SAVING_WITHDRAW";
                            launchOTPVerification(amount, "Rút tiền TK Tiết kiệm");
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showLoanPaymentDialog() {
        if (account == null) return;

        double monthlyPayment = account.getMonthlyPayment();
        double biWeeklyPayment = account.getBiWeeklyPayment();
        double remainingDebt = account.getRemainingDebt() > 0 ? 
                account.getRemainingDebt() : account.getLoanAmount();

        String[] options = {
            "Trả theo tháng: " + FormatUtils.formatCurrency(monthlyPayment),
            "Trả theo 2 tuần: " + FormatUtils.formatCurrency(biWeeklyPayment),
            "Trả số tiền khác"
        };

        new AlertDialog.Builder(this)
                .setTitle("Trả nợ vay")
                .setMessage("Dư nợ còn lại: " + FormatUtils.formatCurrency(remainingDebt))
                .setItems(options, (dialog, which) -> {
                    double amount;
                    String desc;
                    switch (which) {
                        case 0:
                            amount = monthlyPayment;
                            desc = "Trả nợ hàng tháng";
                            break;
                        case 1:
                            amount = biWeeklyPayment;
                            desc = "Trả nợ 2 tuần";
                            break;
                        default:
                            showCustomPaymentDialog(remainingDebt);
                            return;
                    }
                    pendingAmount = amount;
                    pendingTransactionType = "LOAN_PAYMENT";
                    launchOTPVerification(amount, desc);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCustomPaymentDialog(double remainingDebt) {
        EditText input = new EditText(this);
        input.setHint("Nhập số tiền trả nợ");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Trả nợ số tiền khác")
                .setMessage("Dư nợ còn lại: " + FormatUtils.formatCurrency(remainingDebt))
                .setView(input)
                .setPositiveButton("Tiếp tục", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString());
                        if (amount < 100000) {
                            Toast.makeText(this, "Số tiền tối thiểu 100,000 đ", Toast.LENGTH_SHORT).show();
                        } else if (amount > remainingDebt) {
                            Toast.makeText(this, "Số tiền vượt quá dư nợ", Toast.LENGTH_SHORT).show();
                        } else {
                            pendingAmount = amount;
                            pendingTransactionType = "LOAN_PAYMENT";
                            launchOTPVerification(amount, "Trả nợ");
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void launchOTPVerification(double amount, String description) {
        Intent intent = new Intent(this, OTPVerificationActivity.class);
        intent.putExtra(OTPVerificationActivity.EXTRA_TRANSACTION_TYPE, pendingTransactionType);
        intent.putExtra(OTPVerificationActivity.EXTRA_AMOUNT, amount);
        intent.putExtra(OTPVerificationActivity.EXTRA_DESCRIPTION, description);
        otpLauncher.launch(intent);
    }

    private void processDeposit(double amount) {
        progressBar.setVisibility(View.VISIBLE);
        double newBalance = account.getBalance() + amount;
        
        firebaseHelper.updateAccountBalance(accountId, newBalance,
            aVoid -> {
                Transaction transaction = new Transaction();
                transaction.setFromAccountId(accountId);
                transaction.setAmount(amount);
                transaction.setType(Transaction.TransactionType.DEPOSIT);
                transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                transaction.setDescription("Nạp tiền");

                firebaseHelper.createTransaction(transaction, task -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "✅ Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
                    loadAccountData();
                });
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi nạp tiền", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void processWithdraw(double amount) {
        progressBar.setVisibility(View.VISIBLE);
        double newBalance = account.getBalance() - amount;
        
        firebaseHelper.updateAccountBalance(accountId, newBalance,
            aVoid -> {
                Transaction transaction = new Transaction();
                transaction.setFromAccountId(accountId);
                transaction.setAmount(amount);
                transaction.setType(Transaction.TransactionType.WITHDRAWAL);
                transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                transaction.setDescription("Rút tiền");

                firebaseHelper.createTransaction(transaction, task -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "✅ Rút tiền thành công!", Toast.LENGTH_SHORT).show();
                    loadAccountData();
                });
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi rút tiền", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void processLoanPayment(double amount) {
        progressBar.setVisibility(View.VISIBLE);
        
        double remainingDebt = account.getRemainingDebt() > 0 ? 
                account.getRemainingDebt() : account.getLoanAmount();
        double newRemainingDebt = remainingDebt - amount;
        
        firebaseHelper.updateAccountField(accountId, "remainingDebt", newRemainingDebt,
            aVoid -> {
                Transaction transaction = new Transaction();
                transaction.setFromAccountId(accountId);
                transaction.setAmount(amount);
                transaction.setType(Transaction.TransactionType.LOAN_PAYMENT);
                transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                transaction.setDescription("Trả nợ vay");

                firebaseHelper.createTransaction(transaction, task -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "✅ Trả nợ thành công!", Toast.LENGTH_SHORT).show();
                    loadAccountData();
                });
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi trả nợ", Toast.LENGTH_SHORT).show();
            }
        );
    }

    /**
     * Nạp tiền vào TK Tiết kiệm từ TK Thanh toán
     * - Trừ tiền từ TK Thanh toán
     * - Cộng tiền vào TK Tiết kiệm
     * - Tạo giao dịch TRANSFER
     */
    private void processSavingDeposit(double amount) {
        if (checkingAccount == null) {
            Toast.makeText(this, "Không tìm thấy TK Thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        double newCheckingBalance = checkingAccount.getBalance() - amount;
        double newSavingBalance = account.getBalance() + amount;

        // Step 1: Trừ tiền từ TK Thanh toán
        firebaseHelper.updateAccountBalance(checkingAccount.getId(), newCheckingBalance,
            aVoid1 -> {
                // Step 2: Cộng tiền vào TK Tiết kiệm
                firebaseHelper.updateAccountBalance(accountId, newSavingBalance,
                    aVoid2 -> {
                        // Step 3: Tạo giao dịch TRANSFER
                        Transaction transaction = new Transaction();
                        transaction.setFromAccountId(checkingAccount.getId());
                        transaction.setToAccountId(accountId);
                        transaction.setAmount(amount);
                        transaction.setType(Transaction.TransactionType.TRANSFER);
                        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                        transaction.setDescription("Chuyển tiền vào TK Tiết kiệm");
                        transaction.setRecipientName("TK Tiết kiệm");
                        transaction.setCreatedAt(new Date());

                        firebaseHelper.createTransaction(transaction, task -> {
                            progressBar.setVisibility(View.GONE);
                            checkingAccount.setBalance(newCheckingBalance);
                            
                            new AlertDialog.Builder(this)
                                    .setTitle("✅ Thành công!")
                                    .setMessage("Đã chuyển " + FormatUtils.formatCurrency(amount) + 
                                               " từ TK Thanh toán sang TK Tiết kiệm.\n\n" +
                                               "💳 TK Thanh toán còn: " + FormatUtils.formatCurrency(newCheckingBalance) + "\n" +
                                               "💰 TK Tiết kiệm: " + FormatUtils.formatCurrency(newSavingBalance))
                                    .setPositiveButton("OK", null)
                                    .show();
                            
                            loadAccountData();
                        });
                    },
                    e -> {
                        // Rollback: hoàn tiền cho TK Thanh toán
                        firebaseHelper.updateAccountBalance(checkingAccount.getId(), checkingAccount.getBalance(), null, null);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Lỗi cập nhật TK Tiết kiệm", Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi trừ tiền từ TK Thanh toán", Toast.LENGTH_SHORT).show();
            }
        );
    }

    /**
     * Rút tiền từ TK Tiết kiệm về TK Thanh toán
     * - Trừ tiền từ TK Tiết kiệm
     * - Cộng tiền vào TK Thanh toán
     * - Tạo giao dịch TRANSFER
     */
    private void processSavingWithdraw(double amount) {
        if (checkingAccount == null) {
            Toast.makeText(this, "Không tìm thấy TK Thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        double newSavingBalance = account.getBalance() - amount;
        double newCheckingBalance = checkingAccount.getBalance() + amount;

        // Step 1: Trừ tiền từ TK Tiết kiệm
        firebaseHelper.updateAccountBalance(accountId, newSavingBalance,
            aVoid1 -> {
                // Step 2: Cộng tiền vào TK Thanh toán
                firebaseHelper.updateAccountBalance(checkingAccount.getId(), newCheckingBalance,
                    aVoid2 -> {
                        // Step 3: Tạo giao dịch TRANSFER
                        Transaction transaction = new Transaction();
                        transaction.setFromAccountId(accountId);
                        transaction.setToAccountId(checkingAccount.getId());
                        transaction.setAmount(amount);
                        transaction.setType(Transaction.TransactionType.TRANSFER);
                        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                        transaction.setDescription("Rút tiền từ TK Tiết kiệm");
                        transaction.setRecipientName("TK Thanh toán");
                        transaction.setCreatedAt(new Date());

                        firebaseHelper.createTransaction(transaction, task -> {
                            progressBar.setVisibility(View.GONE);
                            checkingAccount.setBalance(newCheckingBalance);
                            
                            new AlertDialog.Builder(this)
                                    .setTitle("✅ Thành công!")
                                    .setMessage("Đã rút " + FormatUtils.formatCurrency(amount) + 
                                               " từ TK Tiết kiệm về TK Thanh toán.\n\n" +
                                               "💰 TK Tiết kiệm còn: " + FormatUtils.formatCurrency(newSavingBalance) + "\n" +
                                               "💳 TK Thanh toán: " + FormatUtils.formatCurrency(newCheckingBalance))
                                    .setPositiveButton("OK", null)
                                    .show();
                            
                            loadAccountData();
                        });
                    },
                    e -> {
                        // Rollback: hoàn tiền cho TK Tiết kiệm
                        firebaseHelper.updateAccountBalance(accountId, account.getBalance(), null, null);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Lỗi cập nhật TK Thanh toán", Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi trừ tiền từ TK Tiết kiệm", Toast.LENGTH_SHORT).show();
            }
        );
    }
}
