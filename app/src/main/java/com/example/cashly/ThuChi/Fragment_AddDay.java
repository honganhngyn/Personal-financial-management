package com.example.cashly.ThuChi;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.cashly.MoneyTextWatcher;
import com.example.cashly.PhanLoaiThuChi.Activity_EditDanhMuc;
import com.example.cashly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Fragment_AddDay extends Fragment {

    private TextView tabChi, tabThu;
    private View fragmentChi, fragmentThu;
    private EditText textNgaychi, textNgaythu;
    private EditText textNoteChi, textTienChi, textNoteThu, textTienThu;
    private GridLayout gridCategoriesChi, gridCategoriesThu;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String selectedCategoryChi = "";
    private String selectedCategoryThu = "";
    private String selectedDate;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        linkViews(view);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        textTienChi.addTextChangedListener(new MoneyTextWatcher(textTienChi));
        textTienThu.addTextChangedListener(new MoneyTextWatcher(textTienThu));

        if (getArguments() != null) {
            selectedDate = getArguments().getString("selectedDate");
            textNgaychi.setText(selectedDate);
            textNgaythu.setText(selectedDate);
        }

        initTab();

        view.findViewById(R.id.btnThemTienChi).setOnClickListener(v -> saveExpense());
        view.findViewById(R.id.btnThemTienThu).setOnClickListener(v -> saveIncome());

        view.findViewById(R.id.btnChinhSuaChi).setOnClickListener(v -> openEditCategory("chi"));
        view.findViewById(R.id.btnChinhSuaThu).setOnClickListener(v -> openEditCategory("thu"));

        tabChi.performClick();

        return view;
    }

    private void linkViews(View view) {
        tabChi = view.findViewById(R.id.tabChi);
        tabThu = view.findViewById(R.id.tabThu);
        fragmentChi = view.findViewById(R.id.fragmentChi);
        fragmentThu = view.findViewById(R.id.fragmentThu);

        textNgaychi = view.findViewById(R.id.textNgaychi);
        textNgaythu = view.findViewById(R.id.textNgaythu);

        textNoteChi = view.findViewById(R.id.textNoteChi);
        textTienChi = view.findViewById(R.id.textTienChi);
        textNoteThu = view.findViewById(R.id.textNoteThu);
        textTienThu = view.findViewById(R.id.textTienThu);

        gridCategoriesChi = view.findViewById(R.id.gridCategoriesChi);
        gridCategoriesThu = view.findViewById(R.id.gridCategoriesThu);
    }

    private void initTab() {
        tabChi.setOnClickListener(v -> {
            tabChi.setBackgroundResource(R.drawable.tab_selected);
            tabThu.setBackgroundResource(R.drawable.tab_unselected);
            fragmentChi.setVisibility(View.VISIBLE);
            fragmentThu.setVisibility(View.GONE);
            loadDanhMucChi();
        });

        tabThu.setOnClickListener(v -> {
            tabChi.setBackgroundResource(R.drawable.tab_unselected);
            tabThu.setBackgroundResource(R.drawable.tab_selected);
            fragmentChi.setVisibility(View.GONE);
            fragmentThu.setVisibility(View.VISIBLE);
            loadDanhMucThu();
        });
    }

    private void saveExpense() {
        String date = textNgaychi.getText().toString().trim();
        String note = textNoteChi.getText().toString().trim();
        String amountStr = textTienChi.getText().toString().trim();

        if (!validateInput(date, amountStr, selectedCategoryChi)) return;

        double amount = parseAmount(amountStr);
        String uid = auth.getCurrentUser().getUid();

        try {
            // ✅ FIX CHUẨN → Lấy month từ NGÀY user chọn!
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            String month = monthFormat.format(parsedDate);

            Map<String, Object> data = new HashMap<>();
            data.put("date", date);
            data.put("note", note);
            data.put("amount", amount);
            data.put("category", selectedCategoryChi);
            data.put("month", month); // ✅ CHUẨN rồi!

            db.collection("users").document(uid).collection("expenses")
                    .add(data)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(getContext(), getString(R.string.expense_saved), Toast.LENGTH_SHORT).show();
                        textNoteChi.setText("");
                        textTienChi.setText("");

                        // ✅ Sau khi lưu xong → checkBudgetStatus
                        checkBudgetStatus(date);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), getString(R.string.error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.processing_day) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private void saveIncome() {
        String date = textNgaythu.getText().toString().trim();
        String note = textNoteThu.getText().toString().trim();
        String amountStr = textTienThu.getText().toString().trim();

        if (!validateInput(date, amountStr, selectedCategoryThu)) return;

        double amount = parseAmount(amountStr);
        String uid = auth.getCurrentUser().getUid();

        try {
            // ✅ FIX CHUẨN → Lấy month từ NGÀY user chọn!
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            String month = monthFormat.format(parsedDate);

            Map<String, Object> data = new HashMap<>();
            data.put("date", date);
            data.put("note", note);
            data.put("amount", amount);
            data.put("category", selectedCategoryThu);
            data.put("month", month); // ✅ THÊM vào!

            db.collection("users").document(uid).collection("incomes")
                    .add(data)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(getContext(), getString(R.string.income_saved), Toast.LENGTH_SHORT).show();
                        textNoteThu.setText("");
                        textTienThu.setText("");

                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), getString(R.string.error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.processing_day) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput(String date, String amountStr, String category) {
        if (date.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.enter_date), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (amountStr.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_empty_amount), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (category.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.select_category), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private double parseAmount(String amountStr) {
        String cleanAmountStr = amountStr.replace(".", "").replace(",", "").trim();
        return Double.parseDouble(cleanAmountStr);
    }

    private void openEditCategory(String type) {
        Intent intent = new Intent(getContext(), Activity_EditDanhMuc.class);
        intent.putExtra("tab", type);
        startActivity(intent);
    }

    private void loadDanhMucChi() {
        gridCategoriesChi.removeAllViews();
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("categories")
                .whereEqualTo("type", "chi")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name != null) {
                            TextView item = createCategoryItem(name, true);
                            gridCategoriesChi.addView(item);
                        }
                    }
                });
    }

    private void loadDanhMucThu() {
        gridCategoriesThu.removeAllViews();
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("categories")
                .whereEqualTo("type", "thu")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name != null) {
                            TextView item = createCategoryItem(name, false);
                            gridCategoriesThu.addView(item);
                        }
                    }
                });
    }

    private TextView createCategoryItem(String name, boolean isChi) {
        TextView item = new TextView(getContext());
        item.setText(name);
        item.setPadding(24, 16, 24, 16);
        item.setBackgroundResource(R.drawable.category_item_bg);
        item.setTextColor(getResources().getColor(android.R.color.black));

        item.setOnClickListener(v -> {
            if (isChi) {
                selectedCategoryChi = name;
                updateCategoryHighlight(gridCategoriesChi, name);
            } else {
                selectedCategoryThu = name;
                updateCategoryHighlight(gridCategoriesThu, name);
            }

            Toast.makeText(getContext(), getString(R.string.selected) + " " + name, Toast.LENGTH_SHORT).show();
        });

        return item;
    }

    private void updateCategoryHighlight(GridLayout gridLayout, String selectedName) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            TextView child = (TextView) gridLayout.getChildAt(i);
            if (child.getText().toString().equals(selectedName)) {
                child.setBackgroundResource(R.drawable.category_item_selected);
            } else {
                child.setBackgroundResource(R.drawable.category_item_bg);
            }
        }
    }

    private void checkBudgetStatus(String date) {
        try {
            // Chuyển "dd/MM/yyyy" → "MM-yyyy"
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());

            Date parsedDate = inputFormat.parse(date);
            String month = monthFormat.format(parsedDate);

            String uid = auth.getCurrentUser().getUid();

            db.collection("users").document(uid)
                    .collection("budgets").document(month)
                    .get()
                    .addOnSuccessListener(budgetDoc -> {
                        if (budgetDoc.exists()) {
                            Long budgetValue = budgetDoc.getLong("budget");
                            if (budgetValue != null && budgetValue > 0) {
                                db.collection("users").document(uid)
                                        .collection("expenses")
                                        .whereEqualTo("month", month)
                                        .get()
                                        .addOnSuccessListener(expenseSnapshot -> {
                                            double totalExpense = 0;
                                            for (DocumentSnapshot doc : expenseSnapshot) {
                                                Double amt = doc.getDouble("amount");
                                                if (amt != null) totalExpense += amt;
                                            }

                                            double ratio = (totalExpense / budgetValue) * 100;
                                            String budgetFormatted = NumberFormat.getInstance(new Locale("vi", "VN")).format(budgetValue);

                                            if (totalExpense > budgetValue) {
                                                showBudgetAlertDialog(getString(R.string.over_budget), budgetValue, month);
                                            } else if (ratio >= 90 && ratio < 100) {
                                                showBudgetAlertDialog(getString(R.string.near_budget), budgetValue, month);
                                            } else if (totalExpense == budgetValue) {
                                                showBudgetAlertDialog(getString(R.string.fit_budget), budgetValue, month);
                                            } else {
                                                // Không có cảnh báo → popBackStack luôn
                                                if (isAdded()) {
                                                    requireActivity().getSupportFragmentManager().popBackStack();
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), getString(R.string.error_check_total) + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                // Không có ngân sách → popBackStack luôn
                                if (isAdded()) {
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                }
                            }
                        } else {
                            // Không có ngân sách → popBackStack luôn
                            if (isAdded()) {
                                requireActivity().getSupportFragmentManager().popBackStack();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), getString(R.string.error_loading_budget) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.processing_day) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void showBudgetAlertDialog(String title, double budgetValue, String month) {
        String budgetFormatted = NumberFormat.getInstance(new Locale("vi", "VN")).format(budgetValue);

        String message = getString(R.string.month_title) + ": " + month + "\n" + getString(R.string.budget) + ": " + budgetFormatted + " VND";

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    dialog.dismiss();

                    // ✅ PopBackStack sau khi show dialog xong
                    if (isAdded()) {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .show();
    }


}
