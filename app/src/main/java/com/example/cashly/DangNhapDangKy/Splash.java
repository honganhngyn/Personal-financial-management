package com.example.cashly.DangNhapDangKy;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cashly.MainActivity;
import com.example.cashly.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

/**
 * Splash:
 * - Màn hình Splash khi khởi động app.
 * - Kiểm tra trạng thái đăng nhập:
 *    + Nếu chưa đăng nhập → chuyển về màn hình Đăng nhập.
 *    + Nếu đã đăng nhập → kiểm tra Email Verified → chuyển vào MainActivity.
 */
public class Splash extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Khởi tạo Firebase ---
        FirebaseApp.initializeApp(this);

        // --- Thiết lập full screen ---
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // --- Giao diện Splash ---
        setContentView(R.layout.splash);

        // --- Khởi tạo FirebaseAuth ---
        firebaseAuth = FirebaseAuth.getInstance();

        // --- Delay 1 giây rồi kiểm tra đăng nhập ---
        new Handler().postDelayed(() -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();

            // Nếu chưa đăng nhập → chuyển về màn hình Đăng nhập
            if (user == null) {
                startActivity(new Intent(getApplicationContext(), DangNhap.class));
                finish();
            }
            // Nếu đã đăng nhập → kiểm tra tiếp
            else {
                kiemTra();
            }
        }, 1000); // 1 giây
    }

    /**
     * Hàm kiểm tra thông tin tài khoản:
     * - Nếu email đã verify → vào MainActivity
     * - Nếu chưa verify → quay về màn hình Đăng nhập
     */
    private void kiemTra() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("TaiKhoan");

        // Lấy UID hiện tại và kiểm tra thông tin
        ref.child(Objects.requireNonNull(firebaseAuth.getUid()))
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Lấy giá trị "taiKhoan" (có thể dùng sau)
                        String taiKhoan = "" + dataSnapshot.child("taiKhoan").getValue();

                        // Nếu email đã verify
                        if (Objects.requireNonNull(firebaseAuth.getCurrentUser()).isEmailVerified()) {

                            // Chuyển vào MainActivity
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));

                            // Hiển thị lời chào
                            Toast.makeText(getApplicationContext(), getString(R.string.welcome_to_my_app), Toast.LENGTH_SHORT).show();

                            finish();
                        }
                        // Nếu email chưa verify → quay về Đăng nhập
                        else {
                            startActivity(new Intent(getApplicationContext(), DangNhap.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Nếu có lỗi khi truy vấn Firebase Database
                    }
                });
    }
}
