package com.example.cashly.DangNhapDangKy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cashly.MainActivity;
import com.example.cashly.R;
import com.example.cashly.databinding.DangNhapBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;

/**
 * DangNhap:
 * - Màn hình Đăng nhập.
 * - Đăng nhập bằng email + mật khẩu.
 * - Kiểm tra xác thực email.
 * - Cập nhật trạng thái "online".
 * - Nếu thành công → vào MainActivity.
 */
public class DangNhap extends AppCompatActivity {

    private DangNhapBinding binding; // ViewBinding cho layout DangNhap
    private ProgressDialog dialog;   // ProgressDialog hiển thị khi đang xử lý
    private FirebaseAuth firebaseAuth; // FirebaseAuth để xử lý đăng nhập

    private String email, matKhau;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Ánh xạ ViewBinding ---
        binding = DangNhapBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // --- Khởi tạo FirebaseAuth ---
        firebaseAuth = FirebaseAuth.getInstance();

        // --- Khởi tạo ProgressDialog ---
        dialog = new ProgressDialog(this);
        dialog.setCanceledOnTouchOutside(false);

        // --- Chuyển sang Đăng ký ---
        binding.btnRegister.setOnClickListener(view1 ->
                startActivity(new Intent(getApplicationContext(), DangKy.class)));

        // --- Chuyển sang Quên mật khẩu ---
        binding.quenMK.setOnClickListener(view1 ->
                startActivity(new Intent(getApplicationContext(), QuenMK.class)));

        // --- Xử lý Đăng nhập ---
        binding.btnLogin.setOnClickListener(view1 -> dangNhap());
    }

    /**
     * Xử lý nút Đăng nhập:
     * - Kiểm tra định dạng email.
     * - Kiểm tra mật khẩu không rỗng.
     * - Gọi FirebaseAuth.signInWithEmailAndPassword.
     * - Nếu thành công → kiểm tra xác thực email → update trạng thái online → vào MainActivity.
     */
    private void dangNhap() {
        email = binding.email.getText().toString().trim();
        matKhau = binding.matKhau.getText().toString().trim();

        // --- Kiểm tra định dạng email ---
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                    .setTitle(getString(R.string.login_failed))
                    .setMessage(getString(R.string.error_invalid_email))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
            return;
        }

        // --- Kiểm tra rỗng ---
        if (email.isEmpty()) {
            binding.email.setError(getString(R.string.please_enter_email));
            return;
        }

        if (matKhau.isEmpty()) {
            binding.matKhau.setError(getString(R.string.error_empty_password));
            return;
        }

        // --- Hiển thị ProgressDialog ---
        dialog.setMessage(getString(R.string.logging_in));
        dialog.show();

        // --- Thực hiện đăng nhập Firebase ---
        firebaseAuth.signInWithEmailAndPassword(email, matKhau)
                .addOnSuccessListener(authResult -> {
                    // Nếu muốn kiểm tra xác thực email trước → gọi xacThuc()
                    xacThuc();
                    // Nếu muốn bỏ kiểm tra xác thực → gọi online();
                    // online();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    new AlertDialog.Builder(DangNhap.this, R.style.CustomAlertDialog)
                            .setTitle(getString(R.string.login_failed))
                            .setMessage(getString(R.string.error) + ": " + e.getMessage())
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                });

        // --- Timeout 15 giây nếu bị treo ---
        new Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), getString(R.string.timeout_retry), Toast.LENGTH_SHORT).show();
            }
        }, 15000);
    }

    /**
     * Cập nhật trạng thái online lên Realtime Database.
     * Sau đó gọi kiemTra() để kiểm tra thông tin tài khoản.
     */
    private void online() {
        dialog.setMessage(getString(R.string.checking));

        String uid = firebaseAuth.getUid();
        if (uid == null) {
            dialog.dismiss();
            Toast.makeText(getApplicationContext(), getString(R.string.user_uid_error), Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("online", "true");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("TaiKhoan");
        ref.child(uid).updateChildren(hashMap)
                .addOnSuccessListener(aVoid -> {
                    // Thành công → kiểm tra thông tin người dùng
                    kiemTra();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.network_error) + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });

        // Timeout 10 giây
        new Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), getString(R.string.timeout_retry), Toast.LENGTH_SHORT).show();
            }
        }, 10000);
    }

    /**
     * Kiểm tra xác thực email:
     * - Nếu chưa xác thực → hiển thị thông báo yêu cầu xác thực.
     * - Nếu đã xác thực → tiếp tục update trạng thái online.
     */
    private void xacThuc() {
        if (!Objects.requireNonNull(firebaseAuth.getCurrentUser()).isEmailVerified()) {
            new AlertDialog.Builder(DangNhap.this, R.style.CustomAlertDialog)
                    .setTitle(getString(R.string.login_failed))
                    .setMessage(getString(R.string.email_verification_required))
                    .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> dialog.dismiss())
                    .show();
        } else {
            // Nếu đã verify → tiếp tục
            online();
        }
    }

    /**
     * Kiểm tra dữ liệu tài khoản từ Realtime Database:
     * - Lấy tên người dùng.
     * - Hiển thị lời chào.
     * - Chuyển sang MainActivity.
     */
    private void kiemTra() {
        dialog.setMessage(getString(R.string.loading_user_data));

        String uid = firebaseAuth.getUid();
        if (uid == null) {
            dialog.dismiss();
            Toast.makeText(getApplicationContext(), getString(R.string.user_uid_error), Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("TaiKhoan");
        ref.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dialog.dismiss();

                if (snapshot.exists()) {
                    // Lấy tên người dùng (hoTen)
                    String tenNguoiDung = snapshot.child("hoTen").getValue(String.class);
                    if (tenNguoiDung == null) tenNguoiDung = "";

                    // Hiển thị lời chào
                    Toast.makeText(DangNhap.this,
                            getString(R.string.welcome) + " " + tenNguoiDung + " " + getString(R.string.back),
                            Toast.LENGTH_SHORT).show();

                    // Chuyển sang MainActivity sau 1.5 giây
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(DangNhap.this, MainActivity.class));
                        finish();
                    }, 1500);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.account_not_found), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), getString(R.string.error) + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
