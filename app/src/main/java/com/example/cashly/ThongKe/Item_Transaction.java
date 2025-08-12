package com.example.cashly.ThongKe;

/**
 * Lớp mô hình (model class) đại diện cho một giao dịch (transaction) trong ứng dụng.
 * Dùng để lưu trữ thông tin của một khoản thu/chi.
 */
public class Item_Transaction {

    // ID tài liệu trên Firebase (hoặc ID trong cơ sở dữ liệu)
    private String documentId;

    // Ngày thực hiện giao dịch (định dạng chuỗi, ví dụ: "2025-06-05")
    private String date;

    // Danh mục của giao dịch (ví dụ: "Ăn uống", "Lương", "Giải trí", ...)
    private String category;

    // Số tiền của giao dịch
    private float amount;

    // Biến cờ xác định đây là khoản thu (true) hay khoản chi (false)
    private boolean isIncome;

    // Ghi chú thêm cho giao dịch
    private String note;

    // Hàm khởi tạo (constructor)
    public Item_Transaction(String documentId, String date, String category, float amount, boolean isIncome, String note) {
        this.documentId = documentId;
        this.date = date;
        this.category = category;
        this.amount = amount;
        this.isIncome = isIncome;
        this.note = note;
    }

    // Getter: lấy ID của giao dịch
    public String getDocumentId() {
        return documentId;
    }

    // Getter: lấy ngày giao dịch
    public String getDate() {
        return date;
    }

    // Getter: lấy danh mục giao dịch
    public String getCategory() {
        return category;
    }

    // Getter: lấy số tiền giao dịch
    public float getAmount() {
        return amount;
    }

    // Getter: kiểm tra có phải khoản thu không
    public boolean isIncome() {
        return isIncome;
    }

    // Getter: lấy ghi chú
    public String getNote() {
        return note;
    }
}
