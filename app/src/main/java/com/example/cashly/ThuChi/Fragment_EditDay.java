package com.example.cashly.ThuChi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.cashly.MoneyTextWatcher;
import com.example.cashly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Fragment_EditDay extends Fragment {

    private EditText textDate, textNote, textAmount;
    private GridLayout gridCategories;
    private Button btnSave, btnDelete;

    private String selectedCategory = "";
    private String selectedDate = "";
    private double selectedAmount = 0;
    private String selectedNote = "";
    private boolean isIncome = true;
    private String documentId = "";

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_detail_calendar, container, false);

        linkViews(view);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        getArgumentsData();

        setDataToForm();

        loadDanhMucFromFirestore(isIncome ? "thu" : "chi");

        btnSave.setOnClickListener(v -> updateGiaoDich());

        btnDelete.setOnClickListener(v -> deleteGiaoDich());

        return view;
    }

    private void linkViews(View view) {
        textDate = view.findViewById(R.id.textDate);
        textNote = view.findViewById(R.id.textNote);
        textAmount = view.findViewById(R.id.textAmount);
        gridCategories = view.findViewById(R.id.gridCategories);
        btnSave = view.findViewById(R.id.btnSave);
        btnDelete = view.findViewById(R.id.btnDelete);

        textAmount.addTextChangedListener(new MoneyTextWatcher(textAmount));
    }

    private void getArgumentsData() {
        if (getArguments() != null) {
            documentId = getArguments().getString("documentId", "");
            selectedDate = getArguments().getString("date", "");
            selectedCategory = getArguments().getString("category", "");
            selectedAmount = getArguments().getFloat("amount", 0);
            selectedNote = getArguments().getString("note", "");
            isIncome = getArguments().getBoolean("isIncome", true);
        }
    }

    private void setDataToForm() {
        textDate.setText(selectedDate);
        textDate.setEnabled(false);
        textNote.setText(selectedNote);

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedAmount = formatter.format(selectedAmount);
        textAmount.setText(formattedAmount);
    }

    private void updateGiaoDich() {
        String note = textNote.getText().toString().trim();
        String amountStr = textAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_empty_amount), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCategory.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.select_category), Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanAmountStr = amountStr.replace(".", "").replace(",", "").trim();
        double amount = Double.parseDouble(cleanAmountStr);

        String uid = auth.getCurrentUser().getUid();
        String collection = isIncome ? "incomes" : "expenses";

        try {
            // ✅ FIX: Lấy month từ selectedDate
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
            Date date = inputFormat.parse(selectedDate);
            String month = monthFormat.format(date);

            Map<String, Object> data = new HashMap<>();
            data.put("date", selectedDate);
            data.put("note", note);
            data.put("amount", amount);
            data.put("category", selectedCategory);
            data.put("month", month); // ✅ CHUẨN rồi!

            if (!documentId.isEmpty()) {
                db.collection("users").document(uid).collection(collection)
                        .document(documentId)
                        .update(data)
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), getString(R.string.updated), Toast.LENGTH_SHORT).show();

                                if (!isIncome) {
                                    // Nếu là chi → check ngân sách → trong dialog sẽ popBackStack sau
                                    checkBudgetStatus();
                                } else {
                                    // Nếu là thu → không cần check ngân sách → popBackStack luôn
                                    notifyUpdateToCalendar();
                                }
                            }
                        });
            } else {
                Toast.makeText(getContext(), getString(R.string.document_id_not_found_update), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.processing_day) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteGiaoDich() {
        String uid = auth.getCurrentUser().getUid();
        String collection = isIncome ? "incomes" : "expenses";

        if (!documentId.isEmpty()) {
            db.collection("users").document(uid).collection(collection)
                    .document(documentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), getString(R.string.deleted), Toast.LENGTH_SHORT).show();
                            notifyUpdateToCalendar();
                        }
                    });
        } else {
            Toast.makeText(getContext(), getString(R.string.document_id_not_found_delete), Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyUpdateToCalendar() {
        Bundle result = new Bundle();
        result.putString("updatedDate", selectedDate);
        getParentFragmentManager().setFragmentResult("update_day", result);

        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void loadDanhMucFromFirestore(String type) {
        gridCategories.removeAllViews();
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("categories")
                .whereEqualTo("type", type)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    List<String> danhMuc = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        String name = doc.getString("name");
                        if (name != null) {
                            danhMuc.add(name);
                        }
                    }

                    if (!selectedCategory.isEmpty() && !danhMuc.contains(selectedCategory)) {
                        danhMuc.add(0, selectedCategory);
                    }

                    for (String name : danhMuc) {
                        TextView item = createCategoryItem(name);
                        gridCategories.addView(item);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), getString(R.string.error_loading_category) + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private TextView createCategoryItem(String name) {
        TextView item = new TextView(getContext());
        item.setText(name);
        item.setPadding(24, 16, 24, 16);
        item.setBackgroundResource(name.equals(selectedCategory) ? R.drawable.category_item_selected : R.drawable.category_item_bg);
        item.setTextColor(getResources().getColor(android.R.color.black));

        item.setOnClickListener(v -> {
            selectedCategory = name;

            for (int i = 0; i < gridCategories.getChildCount(); i++) {
                TextView child = (TextView) gridCategories.getChildAt(i);
                if (child.getText().toString().equals(name)) {
                    child.setBackgroundResource(R.drawable.category_item_selected);
                } else {
                    child.setBackgroundResource(R.drawable.category_item_bg);
                }
            }

            Toast.makeText(getContext(), getString(R.string.selected) + " " + name, Toast.LENGTH_SHORT).show();
        });

        return item;
    }

    private void checkBudgetStatus() {
        try {
            // Chuyển selectedDate "dd/MM/yyyy" → "MM-yyyy"
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());

            Date date = inputFormat.parse(selectedDate);
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
                                        .whereEqualTo("month", month) // đúng tháng của selectedDate
                                        .get()
                                        .addOnSuccessListener(expenseSnapshot -> {
                                            double totalExpense = 0;
                                            for (DocumentSnapshot doc : expenseSnapshot) {
                                                Double amt = doc.getDouble("amount");
                                                if (amt != null) totalExpense += amt;
                                            }

                                            double ratio = (totalExpense / budgetValue) * 100;

                                            if (totalExpense > budgetValue) {
                                                showBudgetAlertDialog(getString(R.string.over_budget), budgetValue, month);
                                            } else if (ratio >= 90 && ratio < 100) {
                                                showBudgetAlertDialog(getString(R.string.near_budget), budgetValue, month);
                                            } else if (totalExpense == budgetValue) {
                                                showBudgetAlertDialog(getString(R.string.fit_budget), budgetValue, month);
                                            } else {
                                                // Không có cảnh báo → pop luôn
                                                if (isAdded()) {
                                                    notifyUpdateToCalendar();
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), getString(R.string.error_check_total) + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                // Không có ngân sách → pop luôn
                                if (isAdded()) {
                                    notifyUpdateToCalendar();
                                }
                            }
                        } else {
                            // Không có ngân sách → pop luôn
                            if (isAdded()) {
                                notifyUpdateToCalendar();
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

                    // ✅ PopBackStack tại đây
                    if (isAdded()) {
                        notifyUpdateToCalendar();
                    }
                })
                .show();
    }


}
