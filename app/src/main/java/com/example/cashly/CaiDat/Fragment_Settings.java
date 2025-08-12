package com.example.cashly.CaiDat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashly.R;

import java.util.Arrays;
import java.util.List;

/**
 * Fragment_Settings:
 * - Fragment hiển thị danh sách các mục Cài đặt.
 * - Dùng RecyclerView để hiển thị các SettingOption.
 * - Mỗi mục gồm:
 *      + icon
 *      + label (chuỗi String resource)
 *
 * - Các mục mẫu hiện tại:
 *      1. Profile
 *      2. Theme
 *      3. Budget
 *      4. Language
 *      5. Change Password
 *      6. Logout
 */
public class Fragment_Settings extends Fragment {

    private RecyclerView recyclerView;
    private List<SettingOption> settingOptions;

    /**
     * Tạo giao diện của Fragment (inflate layout + khởi tạo RecyclerView).
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate layout fragment_settings.xml
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // --- Khởi tạo danh sách SettingOption ---
        settingOptions = Arrays.asList(
                new SettingOption(R.string.setting_profile, R.drawable.user),
                new SettingOption(R.string.setting_theme, R.drawable.palette),
                new SettingOption(R.string.setting_budget, R.drawable.budget),
                new SettingOption(R.string.setting_language, R.drawable.language),
                new SettingOption(R.string.setting_change_password, R.drawable.lock_reset),
                new SettingOption(R.string.setting_logout, R.drawable.logout)
        );

        // --- Khởi tạo RecyclerView ---
        recyclerView = view.findViewById(R.id.recyclerViewSettings);

        // Dùng LinearLayoutManager (dạng danh sách dọc)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Gán Adapter_Setting (hiển thị danh sách SettingOption)
        recyclerView.setAdapter(new Adapter_Setting(getContext(), settingOptions));

        return view;
    }
}
