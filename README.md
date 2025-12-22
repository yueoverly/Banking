# TDT Bank - Mobile Banking Application

## Mô tả dự án
Ứng dụng Mobile Banking được phát triển cho môn học Mobile Apps Development tại Trường Đại học Tôn Đức Thắng (TDTU). Ứng dụng cho phép khách hàng thực hiện các giao dịch ngân hàng và nhân viên ngân hàng quản lý khách hàng.

## Công nghệ sử dụng
- **Ngôn ngữ**: Java
- **IDE**: Android Studio
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Backend**: Firebase (Authentication, Firestore, Storage, Messaging)
- **Maps**: Google Maps API
- **Xác thực khuôn mặt**: ML Kit Face Detection
- **QR Code**: ZXing Library
- **Camera**: CameraX
- **Biometric**: AndroidX Biometric

## Cấu trúc dự án

```
com.example.myapplication/
├── activities/           # Các Activity chính
│   ├── SplashActivity.java
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   ├── MainActivity.java              # Dashboard khách hàng
│   ├── OfficerMainActivity.java       # Dashboard nhân viên
│   ├── TransferActivity.java          # Chuyển tiền
│   ├── OTPVerificationActivity.java   # Xác thực OTP
│   ├── FaceVerificationActivity.java  # Xác thực khuôn mặt
│   ├── AccountDetailActivity.java     # Chi tiết tài khoản
│   ├── TransactionHistoryActivity.java
│   ├── BillPaymentActivity.java       # Thanh toán hóa đơn
│   ├── PhoneTopUpActivity.java        # Nạp tiền điện thoại
│   ├── MapActivity.java               # Tìm chi nhánh
│   ├── QRScannerActivity.java         # Quét mã QR
│   ├── ProfileActivity.java           # Hồ sơ cá nhân
│   ├── CreateCustomerActivity.java    # Tạo khách hàng (Officer)
│   └── ManageCustomersActivity.java   # Quản lý KH (Officer)
├── adapters/             # RecyclerView Adapters
│   ├── AccountAdapter.java
│   ├── TransactionAdapter.java
│   ├── CustomerAdapter.java
│   └── BranchAdapter.java
├── models/               # Data Models
│   ├── User.java
│   ├── Account.java
│   ├── Transaction.java
│   ├── Bill.java
│   └── BankBranch.java
├── services/             # Background Services
│   └── MyFirebaseMessagingService.java
├── utils/                # Utilities
│   ├── Constants.java
│   ├── FirebaseHelper.java
│   ├── FormatUtils.java
│   └── SessionManager.java
└── BankingApplication.java
```

## Tính năng chính

### 1. Hồ sơ người dùng (User Profiles)
- **Khách hàng**: Xem thông tin tài khoản, thực hiện giao dịch
- **Nhân viên**: Tạo tài khoản khách hàng, quản lý dữ liệu

### 2. Bảo mật (Security)
- Đăng nhập bằng email/mật khẩu
- Xác thực sinh trắc học (vân tay)
- eKYC - Xác thực khuôn mặt (ML Kit Face Detection)
- Xác thực 2 yếu tố (2FA) với OTP
- Mã PIN

### 3. Quản lý tài khoản (Account Management)
- Tài khoản thanh toán (Checking)
- Tài khoản tiết kiệm (Saving) với lãi suất
- Tài khoản vay (Mortgage)
- Xem số dư, lịch sử giao dịch
- Nạp/rút tiền

### 4. Giao dịch (Transaction Management)
- Chuyển tiền nội bộ
- Chuyển tiền liên ngân hàng
- Xác thực OTP cho mọi giao dịch
- Lưu người nhận thường xuyên

### 5. Tiện ích (Utilities)
- Thanh toán hóa đơn (điện, nước, internet, TV, bảo hiểm)
- Nạp tiền điện thoại (Viettel, Vinaphone, Mobifone, Vietnamobile)
- Quét mã QR để thanh toán

### 6. Bản đồ (Navigation)
- Hiển thị vị trí hiện tại
- Tìm chi nhánh ngân hàng gần nhất
- Chỉ đường đến chi nhánh
- Gọi điện đến chi nhánh

## Cài đặt

### Yêu cầu
- Android Studio Arctic Fox trở lên
- JDK 17
- Android SDK 34
- Google Play Services

### Bước 1: Clone project
```bash
git clone <repository-url>
```

### Bước 2: Cấu hình Firebase
1. Tạo project Firebase tại https://console.firebase.google.com
2. Thêm ứng dụng Android với package name: `com.example.myapplication`
3. Tải file `google-services.json` và đặt vào thư mục `app/`
4. Bật các dịch vụ:
   - Authentication (Email/Password)
   - Cloud Firestore
   - Storage
   - Cloud Messaging

### Bước 3: Cấu hình Google Maps
1. Tạo API Key tại Google Cloud Console
2. Bật Maps SDK for Android
3. Thay `YOUR_GOOGLE_MAPS_API_KEY` trong `AndroidManifest.xml`

### Bước 4: Build và chạy
```bash
./gradlew assembleDebug
```

## Cấu trúc Firebase Firestore

```
users/
  └── {userId}/
      ├── email: string
      ├── fullName: string
      ├── phoneNumber: string
      ├── idCardNumber: string
      ├── userType: "CUSTOMER" | "OFFICER"
      ├── isVerified: boolean
      ├── profileImageUrl: string
      └── ...

accounts/
  └── {accountId}/
      ├── userId: string
      ├── accountNumber: string
      ├── accountType: "CHECKING" | "SAVING" | "MORTGAGE"
      ├── balance: number
      └── ...

transactions/
  └── {transactionId}/
      ├── fromAccountId: string
      ├── toAccountId: string
      ├── amount: number
      ├── type: string
      └── ...

branches/
  └── {branchId}/
      ├── name: string
      ├── address: string
      ├── latitude: number
      ├── longitude: number
      └── ...
```

## Screenshots

*Thêm screenshots của ứng dụng ở đây*

## Đóng góp

1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Mở Pull Request

## Giấy phép

Dự án này được phát triển cho mục đích học tập tại TDTU.

## Liên hệ

- **Trường**: Đại học Tôn Đức Thắng
- **Khoa**: Công nghệ Thông tin
- **Môn học**: Mobile Apps Development

---
© 2024 TDT Bank - Mobile Banking Application
