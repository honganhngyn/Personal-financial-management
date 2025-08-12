package com.example.cashly.CaiDat;

/**
 * SettingOption:
 * - Model (data class) đại diện cho 1 mục cài đặt trong danh sách Setting.
 * - Gồm:
 *      + labelResId: id của String resource (hiển thị tên của mục cài đặt).
 *      + iconResId: id của Drawable resource (hiển thị icon của mục cài đặt).
 *
 * - Dùng trong: RecyclerView / ListView của Fragment_Settings.
 */
public class SettingOption {

    // Id của String resource cho label (VD: R.string.setting_dark_mode)
    public final int labelResId;

    // Id của Drawable resource cho icon (VD: R.drawable.ic_dark_mode)
    public final int iconResId;

    // Constructor
    public SettingOption(int labelResId, int iconResId) {
        this.labelResId = labelResId;
        this.iconResId = iconResId;
    }
}
