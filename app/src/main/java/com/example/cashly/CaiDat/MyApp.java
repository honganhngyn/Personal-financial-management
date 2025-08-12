package com.example.cashly.CaiDat;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.yariksoffice.lingver.Lingver;

/**
 * MyApp:
 * - Custom Application class của app.
 * - Dùng để:
 *      + Thiết lập ngôn ngữ mặc định khi app khởi động.
 *      + Cài đặt Lingver (thư viện hỗ trợ thay đổi ngôn ngữ runtime).
 * - SharedPreferences:
 *      + "settings" → key "language" → lưu mã ngôn ngữ (VD: "vi", "en", "fr"...).
 * - Mặc định nếu chưa có → sẽ dùng "vi" (Tiếng Việt).
 */
public class MyApp extends Application {

    /**
     * Hàm attachBaseContext (gọi trước khi Application tạo).
     * → Có thể dùng để patch multi-dex, hoặc các thư viện như Lingver nếu cần.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    /**
     * Hàm onCreate():
     * - Gọi khi app khởi động.
     * - Tại đây:
     *      → Đọc SharedPreferences ("settings").
     *      → Lấy mã ngôn ngữ đã lưu ("language").
     *      → Khởi tạo Lingver với ngôn ngữ đó.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Đọc SharedPreferences: "settings"
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Lấy mã ngôn ngữ (nếu chưa có → mặc định "vi")
        String langCode = prefs.getString("language", "vi");

        // Khởi tạo Lingver
        Lingver.init(this, langCode);
    }
}
