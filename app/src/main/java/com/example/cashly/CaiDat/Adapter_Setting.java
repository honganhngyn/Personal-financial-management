package com.example.cashly.CaiDat;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashly.DangNhapDangKy.DangNhap;
import com.example.cashly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter_Setting:
 * - Adapter cho RecyclerView hiển thị các mục Cài đặt (SettingOption).
 * - Xử lý các hành động khi người dùng click từng mục.
 * - Các mục hỗ trợ hiện tại:
 *      + Profile → mở Activity_UpdateUser
 *      + Theme → chuyển Dark/Light mode
 *      + Budget → nhập ngân sách (hiển thị dialog)
 *      + Language → mở Activity_LanguageSelection
 *      + Change Password → mở Activity_ChangePassword
 *      + Logout → xác nhận đăng xuất
 */
public class Adapter_Setting extends RecyclerView.Adapter<Adapter_Setting.ViewHolder> {

    private final List<SettingOption> options;
    private final Context context;

    // Constructor
    public Adapter_Setting(Context context, List<SettingOption> options) {
        this.context = context;
        this.options = options;
    }

    // ViewHolder
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSettingOption;
        ImageView imgSettingIcon;

        public ViewHolder(View itemView) {
            super(itemView);

            // Ánh xạ view trong item_setting_option.xml
            txtSettingOption = itemView.findViewById(R.id.txtSettingOption);
            imgSettingIcon = itemView.findViewById(R.id.imgSettingIcon);
        }

        // Xử lý click từng item
        private void handleClick(int position) {
            int labelResId = options.get(position).labelResId;

            if (labelResId == R.string.setting_budget) {
                // Nhập ngân sách
                showBudgetDialog();

            } else if (labelResId == R.string.setting_profile) {
                // Mở màn hình cập nhật thông tin người dùng
                context.startActivity(new Intent(context, Activity_UpdateUser.class));

            } else if (labelResId == R.string.setting_theme) {
                // Đổi chế độ Dark/Light
                SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                boolean isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
                boolean newDarkMode = !isDark;

                prefs.edit().putBoolean("dark_mode", newDarkMode).apply();
                AppCompatDelegate.setDefaultNightMode(
                        newDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );

            } else if (labelResId == R.string.setting_change_password) {
                // Mở màn hình Đổi mật khẩu
                context.startActivity(new Intent(context, Activity_ChangePassword.class));

            } else if (labelResId == R.string.setting_language) {
                // Chuyển ngôn ngữ
                switchLanguage();

            } else if (labelResId == R.string.setting_logout) {
                // Đăng xuất
                new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.logout_confirm_title))
                        .setMessage(context.getString(R.string.logout_confirm_message))
                        .setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(context.getString(R.string.yes), (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(context, context.getString(R.string.logout_success), Toast.LENGTH_SHORT).show();

                            // Chuyển về màn hình Đăng nhập
                            Intent intent = new Intent(context, DangNhap.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(intent);
                            ((Activity) context).finish();
                        })
                        .show();
            }
        }

        /**
         * Hiển thị dialog nhập ngân sách (budget) cho tháng hiện tại.
         * Lưu ngân sách vào Firestore:
         * /users/{uid}/budgets/{MM-yyyy}
         */
        @SuppressLint("MissingInflatedId")
        private void showBudgetDialog() {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.dialog_set_budget, null);

            EditText edtBudget = view.findViewById(R.id.edtBudget);
            Button btnSave = view.findViewById(R.id.btnSave);

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(view)
                    .create();

            // Lấy ngân sách hiện tại từ Firestore
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String month = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("budgets")
                    .document(month)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long budget = documentSnapshot.getLong("budget");
                            if (budget != null) {
                                String formatted = NumberFormat.getInstance(Locale.US).format(budget);
                                edtBudget.setText(formatted);
                                edtBudget.setSelection(formatted.length());
                            }
                        }
                    });

            // Định dạng số khi người dùng nhập
            edtBudget.addTextChangedListener(new TextWatcher() {
                private String current = "";

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (!s.toString().equals(current)) {
                        edtBudget.removeTextChangedListener(this);

                        String cleanString = s.toString().replaceAll("[,]", "");
                        try {
                            long parsed = Long.parseLong(cleanString);
                            String formatted = NumberFormat.getInstance(Locale.US).format(parsed);
                            current = formatted;
                            edtBudget.setText(formatted);
                            edtBudget.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            current = "";
                            edtBudget.setText("");
                        }

                        edtBudget.addTextChangedListener(this);
                    }
                }
            });

            // Xử lý nút Lưu
            btnSave.setOnClickListener(v -> {
                String budgetStr = edtBudget.getText().toString().trim();
                if (budgetStr.isEmpty()) {
                    Toast.makeText(context, R.string.error_empty_amount, Toast.LENGTH_SHORT).show();
                    return;
                }

                long budget = Long.parseLong(budgetStr.replaceAll(",", ""));

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("budgets")
                        .document(month)
                        .set(Collections.singletonMap("budget", budget))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, context.getString(R.string.budget_saved) + " " + month, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, context.getString(R.string.budget_save_error), Toast.LENGTH_SHORT).show();
                        });
            });

            dialog.show();
        }

        /**
         * Chuyển sang Activity chọn ngôn ngữ.
         */
        private void switchLanguage() {
            context.startActivity(new Intent(context, Activity_LanguageSelection.class));
        }
    }

    // Inflate item_setting_option.xml
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_setting_option, parent, false);
        return new ViewHolder(view);
    }

    // Bind data
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SettingOption option = options.get(position);

        // Gán text và icon
        holder.txtSettingOption.setText(context.getString(option.labelResId));
        holder.imgSettingIcon.setImageResource(option.iconResId);

        // Xử lý click
        holder.itemView.setOnClickListener(v -> holder.handleClick(position));
    }

    @Override
    public int getItemCount() {
        return options.size();
    }
}
