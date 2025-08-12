package com.example.cashly.ThuChi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.cashly.MoneyTextWatcher;
import com.example.cashly.PhanLoaiThuChi.Activity_EditDanhMuc;
import com.example.cashly.R;
import com.example.cashly.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Fragment_Home extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String selectedCategoryChi = "";
    private String selectedCategoryThu = "";

    private boolean isTabChiSelected = true;

    private final ActivityResultLauncher<Intent> danhMucLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (isTabChiSelected) {
                        loadDanhMucChi();
                    } else {
                        loadDanhMucThu();
                    }
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        binding.textTienChi.addTextChangedListener(new MoneyTextWatcher(binding.textTienChi));
        binding.textTienThu.addTextChangedListener(new MoneyTextWatcher(binding.textTienThu));

        String ngayHomNay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        binding.textNgaychi.setText(ngayHomNay);
        binding.textNgaythu.setText(ngayHomNay);

        binding.tabChi.setOnClickListener(v -> showChi());
        binding.tabThu.setOnClickListener(v -> showThu());

        binding.btnThemTienChi.setOnClickListener(v -> themKhoanThuChi(
                true,
                binding.textNgaychi.getText().toString().trim(),
                binding.textNoteChi.getText().toString().trim(),
                binding.textTienChi.getText().toString().trim(),
                selectedCategoryChi
        ));

        binding.btnThemTienThu.setOnClickListener(v -> themKhoanThuChi(
                false,
                binding.textNgaythu.getText().toString().trim(),
                binding.textNoteThu.getText().toString().trim(),
                binding.textTienThu.getText().toString().trim(),
                selectedCategoryThu
        ));

        binding.btnChinhSuaChi.setOnClickListener(v -> moEditDanhMuc("chi"));
        binding.btnChinhSuaThu.setOnClickListener(v -> moEditDanhMuc("thu"));

        showChi();

        return binding.getRoot();
    }

    // ✅ themKhoanThuChi CHUẨN
    private void themKhoanThuChi(boolean isChi, String ngay, String ghichu, String sotienStr, String selectedCategory) {
        if (ngay.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.enter_date), Toast.LENGTH_SHORT).show();
            return;
        }
        if (sotienStr.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_empty_amount), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCategory.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.select_category), Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanAmountStr = sotienStr.replace(".", "").replace(",", "").trim();
        double sotien = Double.parseDouble(cleanAmountStr);

        String uid = auth.getCurrentUser().getUid();
        String collection = isChi ? "expenses" : "incomes";

        try {
            // ✅ FIX chuẩn: Lấy month từ ngay
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
            Date date = inputFormat.parse(ngay);
            String month = monthFormat.format(date);

            Map<String, Object> data = new HashMap<>();
            data.put("date", ngay);
            data.put("note", ghichu);
            data.put("amount", sotien);
            data.put("category", selectedCategory);
            data.put("month", month);

            db.collection("users").document(uid).collection(collection)
                    .add(data)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), getString(R.string.saved) + " " + (isChi ? getString(R.string.chi) : getString(R.string.thu)), Toast.LENGTH_SHORT).show();

                        String ngayHomNay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Calendar.getInstance().getTime());

                        if (isChi) {
                            binding.textNgaychi.setText(ngayHomNay);
                            binding.textNoteChi.setText("");
                            binding.textTienChi.setText("");
                            selectedCategoryChi = "";
                            resetCategoryHighlight(binding.gridCategoriesChi);

                            // ✅ Gọi check budget sau khi add xong
                            checkBudgetStatus(ngay);

                        } else {
                            binding.textNgaythu.setText(ngayHomNay);
                            binding.textNoteThu.setText("");
                            binding.textTienThu.setText("");
                            selectedCategoryThu = "";
                            resetCategoryHighlight(binding.gridCategoriesThu);

                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), getString(R.string.error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.processing_day) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showChi() {
        isTabChiSelected = true;
        binding.tabChi.setBackgroundResource(R.drawable.tab_selected);
        binding.tabThu.setBackgroundResource(R.drawable.tab_unselected);
        binding.fragmentChi.setVisibility(View.VISIBLE);
        binding.fragmentThu.setVisibility(View.GONE);
        loadDanhMucChi();
    }

    private void showThu() {
        isTabChiSelected = false;
        binding.tabChi.setBackgroundResource(R.drawable.tab_unselected);
        binding.tabThu.setBackgroundResource(R.drawable.tab_selected);
        binding.fragmentChi.setVisibility(View.GONE);
        binding.fragmentThu.setVisibility(View.VISIBLE);
        loadDanhMucThu();
    }

    private void loadDanhMucChi() {
        loadDanhMuc(binding.gridCategoriesChi, "chi", true);
    }

    private void loadDanhMucThu() {
        loadDanhMuc(binding.gridCategoriesThu, "thu", false);
    }

    private void loadDanhMuc(ViewGroup gridLayout, String type, boolean isChi) {
        gridLayout.removeAllViews();

        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("categories")
                .whereEqualTo("type", type)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name != null) {
                            TextView item = new TextView(getContext());
                            item.setText(name);
                            item.setPadding(24, 16, 24, 16);
                            item.setBackgroundResource(R.drawable.category_item_bg);
                            item.setTextColor(Color.BLACK);

                            item.setOnClickListener(v -> {
                                if (isChi) {
                                    selectedCategoryChi = name;
                                } else {
                                    selectedCategoryThu = name;
                                }

                                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                                    TextView child = (TextView) gridLayout.getChildAt(i);
                                    if (child.getText().toString().equals(name)) {
                                        child.setBackgroundResource(R.drawable.category_item_selected);
                                    } else {
                                        child.setBackgroundResource(R.drawable.category_item_bg);
                                    }
                                }

                                Toast.makeText(getContext(), getString(R.string.selected) + " " + name, Toast.LENGTH_SHORT).show();
                            });

                            gridLayout.addView(item);
                        }
                    }
                });
    }

    private void resetCategoryHighlight(ViewGroup gridLayout) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            TextView child = (TextView) gridLayout.getChildAt(i);
            child.setBackgroundResource(R.drawable.category_item_bg);
        }
    }

    private void moEditDanhMuc(String tabType) {
        Intent intent = new Intent(getContext(), Activity_EditDanhMuc.class);
        intent.putExtra("tab", tabType);
        danhMucLauncher.launch(intent);
    }

    // ✅ checkBudgetStatus CHUẨN
    private void checkBudgetStatus(String ngay) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());

            Date date = inputFormat.parse(ngay);
            String month = monthFormat.format(date);

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
                                                Double amtDouble = doc.getDouble("amount");
                                                if (amtDouble != null) {
                                                    totalExpense += amtDouble;
                                                } else {
                                                    Long amtLong = doc.getLong("amount");
                                                    if (amtLong != null) {
                                                        totalExpense += amtLong.doubleValue();
                                                    }
                                                }
                                            }

                                            double ratio = (totalExpense / budgetValue) * 100;

                                            if (totalExpense > budgetValue) {
                                                showBudgetAlertDialog(getString(R.string.over_budget), budgetValue, month);
                                            } else if (ratio >= 90 && ratio < 100) {
                                                showBudgetAlertDialog(getString(R.string.near_budget), budgetValue, month);
                                            } else if (totalExpense == budgetValue) {
                                                showBudgetAlertDialog(getString(R.string.fit_budget), budgetValue, month);
                                            } else {
                                                if (isAdded()) {
                                                    requireActivity().getSupportFragmentManager().popBackStack();
                                                }
                                            }
                                        })

                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), getString(R.string.error_check_total) + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                if (isAdded()) {
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                }
                            }
                        } else {
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

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    dialog.dismiss();

                    if (isAdded()) {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .show();
    }
}
