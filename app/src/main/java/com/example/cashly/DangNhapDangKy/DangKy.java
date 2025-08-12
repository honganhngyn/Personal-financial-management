package com.example.cashly.DangNhapDangKy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cashly.R;
import com.example.cashly.databinding.DangKyBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * DangKy:
 * - Màn hình Đăng ký tài khoản mới.
 * - Người dùng nhập thông tin:
 *      + Họ tên, SĐT, Quốc gia, Email, Mật khẩu, Nhập lại mật khẩu.
 * - Xử lý:
 *      + Kiểm tra hợp lệ.
 *      + Tạo tài khoản Firebase Auth.
 *      + Lưu thông tin người dùng vào Realtime Database.
 *      + Gửi email xác thực.
 *      + Tạo các danh mục mẫu (Firestore).
 */
public class DangKy extends AppCompatActivity {

    private DangKyBinding binding; // ViewBinding cho layout DangKy
    private ProgressDialog progressDialog; // ProgressDialog hiển thị khi đang xử lý
    private FirebaseAuth firebaseAuth; // FirebaseAuth để tạo tài khoản

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Ánh xạ ViewBinding ---
        binding = DangKyBinding.inflate(getLayoutInflater());
        binding.quocGia.setText("Việt Nam"); // Set mặc định quốc gia
        View view = binding.getRoot();
        setContentView(view);

        // --- Khởi tạo FirebaseAuth ---
        firebaseAuth = FirebaseAuth.getInstance();

        // --- Khởi tạo ProgressDialog ---
        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);

        // --- Xử lý nút Đăng ký ---
        binding.btnRegister2.setOnClickListener(view1 -> dangKy());
    }

    // Biến lưu data nhập vào
    private String hoTen, sDT, quocGia, email, matKhau, nhapLaiMK;

    /**
     * Xử lý Đăng ký:
     * - Kiểm tra hợp lệ các field.
     * - Nếu hợp lệ → gọi taoTaiKhoan().
     */
    private void dangKy() {
        // --- Lấy dữ liệu ---
        hoTen = binding.hoTen.getText().toString().trim();
        sDT = binding.sDT.getText().toString().trim();
        quocGia = binding.quocGia.getText().toString().trim();
        email = binding.email.getText().toString().trim();
        matKhau = binding.matKhau.getText().toString().trim();
        nhapLaiMK = binding.nhapLaiMK.getText().toString().trim();

        // --- Kiểm tra ---
        if (hoTen.isEmpty()) {
            binding.hoTen.setError(getString(R.string.error_empty_name));
            return;
        }
        if (sDT.isEmpty()) {
            binding.sDT.setError(getString(R.string.error_empty_phone));
            return;
        }
        if (quocGia.isEmpty()) {
            binding.quocGia.setError(getString(R.string.error_empty_country));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (matKhau.isEmpty() || matKhau.length() < 6) {
            binding.matKhau.setError(getString(R.string.error_short_password));
            return;
        }
        if (nhapLaiMK.isEmpty() || !nhapLaiMK.equals(matKhau)) {
            binding.nhapLaiMK.setError(getString(R.string.error_password_not_match));
            return;
        }

        // --- Nếu hợp lệ → tạo tài khoản ---
        taoTaiKhoan();
    }

    /**
     * Tạo tài khoản bằng FirebaseAuth.createUserWithEmailAndPassword().
     * Nếu thành công → gọi saveData() → gửi xác thực email → kết thúc.
     */
    private void taoTaiKhoan() {
        progressDialog.setMessage(getString(R.string.progress_creating_account));
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, matKhau)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        progressDialog.dismiss();

                        // Lưu thông tin người dùng
                        saveData();

                        // Hiển thị Dialog thông báo
                        new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                                .setTitle(getString(R.string.register_account))
                                .setMessage(getString(R.string.register_success_message))
                                .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                                    xacThucEmail();
                                    onBackPressed();
                                    finish();
                                })
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                            .setTitle(getString(R.string.register_account))
                            .setMessage(getString(R.string.register_fail_message) + " " + e.getMessage())
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                });
    }

    /**
     * Gửi email xác thực tài khoản.
     */
    private void xacThucEmail() {
        Objects.requireNonNull(firebaseAuth.getCurrentUser()).sendEmailVerification()
                .addOnSuccessListener(unused -> {
                    // Gửi thành công (có thể thêm Toast nếu muốn)
                })
                .addOnFailureListener(e -> {
                    new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                            .setTitle(getString(R.string.verify_account))
                            .setMessage(getString(R.string.verify_email_fail_message) + " " + e.getMessage())
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                });
    }

    /**
     * Lưu thông tin người dùng vào Realtime Database:
     * /TaiKhoan/{UID}
     */
    private void saveData() {
        progressDialog.setMessage(getString(R.string.progress_saving_data));

        HashMap<String, Object> hashMap = new HashMap<>();
        final String timestamp = "" + System.currentTimeMillis();

        // Ghi dữ liệu người dùng
        hashMap.put("uid", "" + firebaseAuth.getUid());
        hashMap.put("email", "" + email);
        hashMap.put("hoTen", "" + hoTen);
        hashMap.put("sdt", "" + sDT);
        hashMap.put("quocGia", "" + quocGia);
        hashMap.put("timestamp", "" + timestamp);
        hashMap.put("taiKhoan", "KhachHang"); // Loại tài khoản
        hashMap.put("online", "true");        // Trạng thái online

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("TaiKhoan");
        reference.child(Objects.requireNonNull(firebaseAuth.getUid()))
                .setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();

                    // Sau khi lưu thành công → tạo danh mục mẫu
                    taoDanhMucMau();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                            .setTitle(getString(R.string.save_account))
                            .setMessage(getString(R.string.save_fail_message) + " " + e.getMessage())
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                });
    }

    /**
     * Tạo các danh mục mẫu (Firestore):
     * /users/{UID}/categories
     */
    private void taoDanhMucMau() {
        String uid = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Tham chiếu tới collection categories
        CollectionReference refCategories = firestore.collection("users").document(uid).collection("categories");

        // --- Tạo danh mục CHI mẫu ---
        String[] chiMau = {
                getString(R.string.category_food),
                getString(R.string.category_transport),
                getString(R.string.category_shopping),
                getString(R.string.category_entertainment),
                getString(R.string.category_health),
                getString(R.string.category_education),
                getString(R.string.category_bills)
        };

        for (String name : chiMau) {
            DocumentReference docRef = refCategories.document();
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("type", "chi");

            docRef.set(data);
        }

        // --- Tạo danh mục THU mẫu ---
        String[] thuMau = {
                getString(R.string.income_salary),
                getString(R.string.income_bonus),
                getString(R.string.income_investment),
                getString(R.string.income_sales),
                getString(R.string.income_other)
        };

        for (String name : thuMau) {
            DocumentReference docRef = refCategories.document();
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("type", "thu");

            docRef.set(data);
        }

        // ✅ Có thể thêm Toast nếu muốn
        // Toast.makeText(this, "Đã tạo danh mục mẫu!", Toast.LENGTH_SHORT).show();
    }
}
