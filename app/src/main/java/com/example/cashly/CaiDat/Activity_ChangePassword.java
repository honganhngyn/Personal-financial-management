package com.example.cashly.CaiDat;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cashly.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

/**
 * Activity_ChangePassword:
 * - Màn hình Đổi mật khẩu.
 * - Yêu cầu người dùng nhập:
 *      + Mật khẩu hiện tại.
 *      + Mật khẩu mới.
 *      + Xác nhận lại mật khẩu mới.
 * - Thực hiện các bước:
 *      1️ Xác thực lại người dùng bằng mật khẩu hiện tại (reauthenticate).
 *      2 Nếu thành công → gọi updatePassword(newPassword).
 *      3️ Thông báo thành công / lỗi.
 */
public class Activity_ChangePassword extends AppCompatActivity {

    // View
    private EditText edtCurrentPassword, edtNewPassword, edtConfirmNewPassword;
    private Button btnChangePassword;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseUser user;


     // Ghi đè attachBaseContext để cố định ngôn ngữ (vi).
    @Override
    protected void attachBaseContext(Context base) {
        Locale locale = new Locale("vi"); // cố định Tiếng Việt
        Locale.setDefault(locale);

        Configuration config = base.getResources().getConfiguration();
        config.setLocale(locale);

        Context context = base.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    // Gán layout.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Ánh xạ view
        edtCurrentPassword = findViewById(R.id.edtCurrentPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Lấy FirebaseAuth và user hiện tại
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Xử lý khi click nút Đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    // Xử lý đổi mật khẩu:
    private void changePassword() {
        // Lấy dữ liệu từ EditText
        String currentPassword = edtCurrentPassword.getText().toString().trim();
        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmNewPassword = edtConfirmNewPassword.getText().toString().trim();

        // Kiểm tra rỗng
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_incomplete_info), Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra mật khẩu mới nhập lại có khớp không
        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo Credential từ email + mật khẩu hiện tại
        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), currentPassword);

        // Xác thực lại
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Nếu xác thực thành công → update mật khẩu
                user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, getString(R.string.password_change_success), Toast.LENGTH_SHORT).show();
                        finish(); // Quay về màn hình trước
                    } else {
                        // Mật khẩu mới không đạt yêu cầu (quá ngắn chẳng hạn)
                        Toast.makeText(this, getString(R.string.error_short_password), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Mật khẩu hiện tại nhập sai
                Toast.makeText(this, getString(R.string.error_incorrect_current_password), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
