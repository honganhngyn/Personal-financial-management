package com.example.cashly;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * MoneyTextWatcher
 * Dùng để format số tiền trong EditText theo định dạng VNĐ (### ### ### đ)
 */
public class MoneyTextWatcher implements TextWatcher {

    private final EditText editText;
    private String current = "";

    public MoneyTextWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Nếu text thay đổi và khác với current
        if (!s.toString().equals(current)) {

            // Ngăn TextWatcher tự kích hoạt khi setText
            editText.removeTextChangedListener(this);

            try {
                // Xóa các ký tự không phải số
                String cleanString = s.toString().replaceAll("[,.\\sđ]", "");

                // Parse thành số
                double parsed = Double.parseDouble(cleanString);

                // Định dạng theo kiểu VNĐ
                NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
                formatter.setMaximumFractionDigits(0);

                String formatted = formatter.format(parsed);

                // Cập nhật text
                current = formatted;
                editText.setText(formatted);

                // Đặt con trỏ ở cuối
                editText.setSelection(formatted.length());

            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            // Thêm lại TextWatcher
            editText.addTextChangedListener(this);
        }
    }
}
