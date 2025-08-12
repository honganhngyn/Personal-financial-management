package com.example.cashly.DangNhapDangKy;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cashly.R;
import com.example.cashly.databinding.QuenMkBinding;
import com.google.firebase.auth.FirebaseAuth;

/**
 * QuenMK (Quên mật khẩu):
 * - Màn hình khôi phục mật khẩu.
 * - Người dùng nhập email → hệ thống gửi email reset password qua Firebase.
 */
public class QuenMK extends AppCompatActivity {

    private QuenMkBinding binding;       // ViewBinding cho layout quen_mk.xml (file binding tự động tạo)
    private FirebaseAuth firebaseAuth;   // Firebase Auth để gửi email reset password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Ánh xạ ViewBinding ---
        binding = QuenMkBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // --- Khởi tạo FirebaseAuth ---
        firebaseAuth = FirebaseAuth.getInstance();

        // --- Xử lý nút Quay lại ---
        binding.btnLogin3.setOnClickListener(view1 -> {
            super.finish();
            onBackPressed(); // Quay về màn hình trước
        });

        // --- Xử lý nút Gửi link reset password ---
        binding.btnLogin2.setOnClickListener(view1 -> khoiPhucMk());
    }

    /**
     * Xử lý khôi phục mật khẩu:
     * - Kiểm tra email nhập vào
     * - Nếu hợp lệ → gửi email reset qua FirebaseAuth
     * - Thông báo kết quả cho người dùng qua AlertDialog
     */
    private void khoiPhucMk() {
        String email = binding.email.getText().toString().trim();

        // --- Kiểm tra rỗng ---
        if (email.isEmpty()) {
            new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                    .setMessage(getString(R.string.please_enter_email))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
            return;
        }

        // --- Gửi email reset password ---
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    // Thành công → thông báo đã gửi link reset
                    new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                            .setMessage(getString(R.string.reset_link_sent))
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    // Lỗi → hiển thị lỗi
                    new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                            .setMessage(getString(R.string.error) + e.getMessage())
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                });
    }
}
