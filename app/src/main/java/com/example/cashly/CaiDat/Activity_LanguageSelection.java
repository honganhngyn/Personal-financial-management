package com.example.cashly.CaiDat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cashly.DangNhapDangKy.Splash;
import com.example.cashly.R;
import com.yariksoffice.lingver.Lingver;

/**
 * Activity_LanguageSelection:
 * - Màn hình chọn ngôn ngữ cho ứng dụng.
 * - Hỗ trợ các ngôn ngữ:
 *      + Vietnamese (vi)
 *      + English (en)
 *      + Chinese (zh)
 *      + Korean (ko)
 *      + Thai (th)
 *      + Japanese (ja)
 *      + German (de)
 *
 * - Khi chọn:
 *      → Cập nhật Lingver locale.
 *      → Lưu vào SharedPreferences ("settings" → "language").
 *      → Restart app (chuyển về màn hình Splash để load lại ngôn ngữ).
 */
public class Activity_LanguageSelection extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        // --- Bắt sự kiện click các nút ---
        findViewById(R.id.btnVietnamese).setOnClickListener(v -> changeLanguage("vi"));
        findViewById(R.id.btnEnglish).setOnClickListener(v -> changeLanguage("en"));
        findViewById(R.id.btnChinese).setOnClickListener(v -> changeLanguage("zh"));
        findViewById(R.id.btnKorean).setOnClickListener(v -> changeLanguage("ko"));
        findViewById(R.id.btnThai).setOnClickListener(v -> changeLanguage("th"));
        findViewById(R.id.btnJapanese).setOnClickListener(v -> changeLanguage("ja"));
        findViewById(R.id.btnGerman).setOnClickListener(v -> changeLanguage("de"));
    }
    // Đổi ngôn ngữ ứng dụng.
    private void changeLanguage(String lang) {
        // Đổi ngôn ngữ qua Lingver
        Lingver.getInstance().setLocale(this, lang);

        // Lưu ngôn ngữ đã chọn vào SharedPreferences
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putString("language", lang).apply();

        // Restart app → về Splash (để load lại với ngôn ngữ mới)
        Intent intent = new Intent(this, Splash.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Kết thúc Activity hiện tại
        finish();
    }
}
