package com.example.cashly.CaiDat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cashly.MainActivity;
import com.example.cashly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Activity_UpdateUser:
 * - Màn hình cập nhật thông tin người dùng.
 * - Hiển thị thông tin người dùng hiện tại từ Firebase Realtime Database.
 * - Cho phép người dùng chỉnh sửa:
 *      + Họ tên (hoTen)
 *      + Số điện thoại (sdt)
 *      + Quốc gia (quocGia)
 * - Email không cho chỉnh sửa (hiển thị readonly).
 * - Cập nhật thông tin mới vào Realtime Database.
 */
public class Activity_UpdateUser extends AppCompatActivity {

    // View
    private EditText editEmail, editName, editPhone, editNationality;
    private Button btnUpdate;

    // Firebase
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_user_activity);

        // --- Ánh xạ view ---
        editEmail = findViewById(R.id.edtEmail);
        editName = findViewById(R.id.edtFullName);
        editPhone = findViewById(R.id.edtPhone);
        editNationality = findViewById(R.id.edtQuocTich);
        btnUpdate = findViewById(R.id.btnCapnhat);

        // --- Lấy người dùng hiện tại ---
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.user_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // --- Tham chiếu tới user trong Realtime Database ---
        String uid = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("TaiKhoan").child(uid);

        // --- Load dữ liệu người dùng ---
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String email = currentUser.getEmail(); // Lấy từ FirebaseAuth
                String name = snapshot.child("hoTen").getValue(String.class);
                String phone = snapshot.child("sdt").getValue(String.class);
                String nationality = snapshot.child("quocGia").getValue(String.class);

                // Gán dữ liệu lên EditText
                editEmail.setText(email); // Email chỉ hiển thị, không chỉnh sửa
                editName.setText(name != null ? name : "");
                editPhone.setText(phone != null ? phone : "");
                editNationality.setText(nationality != null ? nationality : "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Activity_UpdateUser.this, getString(R.string.data_load_error), Toast.LENGTH_SHORT).show();
            }
        });

        // --- Xử lý nút Cập nhật ---
        btnUpdate.setOnClickListener(v -> {
            String name = editName.getText().toString();
            String phone = editPhone.getText().toString();
            String nationality = editNationality.getText().toString();

            // Cập nhật thông tin vào Database
            userRef.child("hoTen").setValue(name);
            userRef.child("sdt").setValue(phone);
            userRef.child("quocGia").setValue(nationality)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Thành công
                            Toast.makeText(Activity_UpdateUser.this, getString(R.string.updated), Toast.LENGTH_SHORT).show();

                            // Chuyển về MainActivity
                            Intent intent = new Intent(Activity_UpdateUser.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Thất bại
                            Toast.makeText(Activity_UpdateUser.this, getString(R.string.update_failed_try_again), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
