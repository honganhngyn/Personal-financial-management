package com.example.cashly;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.cashly.CaiDat.Fragment_Settings;
import com.example.cashly.ThongKe.Fragment_Calendar;
import com.example.cashly.ThongKe.Fragment_Chart;
import com.example.cashly.ThuChi.Fragment_Home;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // Biến dùng để lưu thời điểm lần click cuối (chống click quá nhanh)
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Đọc cài đặt giao diện (sáng/tối) từ SharedPreferences
        SharedPreferences themePrefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDark = themePrefs.getBoolean("dark_mode", false);

        // Thiết lập chế độ nền sáng/tối cho toàn bộ app
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        // Gọi phương thức cha
        super.onCreate(savedInstanceState);

        // Gán layout cho Activity
        setContentView(R.layout.activity_main);

        // Khởi tạo BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        // Khởi tạo các Fragment của từng màn hình
        Fragment fragmentHome = new Fragment_Home();
        Fragment fragmentCalendar = new Fragment_Calendar();
        Fragment fragmentChart = new Fragment_Chart();
        Fragment fragmentSettings = new Fragment_Settings();

        // Thiết lập listener cho sự kiện chọn mục trên thanh điều hướng dưới (BottomNavigationView)
        bottomNav.setOnItemSelectedListener(item -> {

            // Lấy thời điểm hiện tại
            long currentTime = System.currentTimeMillis();

            // Nếu người dùng click quá nhanh (< 300 ms) thì bỏ qua sự kiện này
            if (currentTime - lastClickTime < 300) {
                return false;
            }

            // Cập nhật thời điểm click cuối cùng
            lastClickTime = currentTime;

            // Khai báo biến lưu fragment được chọn
            Fragment selectedFragment = null;

            // Lấy ID của mục menu được chọn và gán fragment tương ứng
            int id = item.getItemId();

            if (id == R.id.menu_input) {
                selectedFragment = fragmentHome; // Màn hình nhập thu/chi
            } else if (id == R.id.menu_calendar) {
                selectedFragment = fragmentCalendar; // Màn hình lịch
            } else if (id == R.id.menu_chart) {
                selectedFragment = fragmentChart; // Màn hình biểu đồ
            } else if (id == R.id.menu_setting) {
                selectedFragment = fragmentSettings; // Màn hình cài đặt
            }

            // Nếu fragment hợp lệ, thực hiện chuyển fragment
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment) // Thay thế fragment hiện tại
                        .commit(); // Thực thi giao dịch
            }

            return true; // Báo sự kiện đã xử lý
        });

        // Nếu Activity mới được tạo (không phải khôi phục sau khi bị huỷ)
        // => Thiết lập fragment mặc định là màn hình nhập thu/chi
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.menu_input);
        }
    }
}
