package com.example.cashly.ThongKe;

/**
 * Lớp mô hình (model class) đại diện cho một danh mục (category) trong báo cáo thống kê.
 * Mỗi danh mục sẽ có tổng số tiền, phần trăm đóng góp và màu sắc hiển thị (ví dụ trên biểu đồ).
 */
public class Item_Category {

    // Tên danh mục (ví dụ: "Ăn uống", "Giải trí", "Đi lại", "Lương", ...)
    private String category;

    // Tổng số tiền thuộc danh mục này
    private float amount;

    // Phần trăm (%) so với tổng chi hoặc tổng thu
    private float percent;

    // Màu sắc dùng để hiển thị (ví dụ trên PieChart, BarChart), dạng mã màu integer (ARGB hoặc RGB)
    private int color;

    // Constructor: Khởi tạo đối tượng Item_Category
    public Item_Category(String category, float amount, float percent, int color) {
        this.category = category;
        this.amount = amount;
        this.percent = percent;
        this.color = color;
    }

    // Getter: lấy tên danh mục
    public String getCategory() {
        return category;
    }

    // Getter: lấy tổng số tiền
    public float getAmount() {
        return amount;
    }

    // Getter: lấy phần trăm
    public float getPercent() {
        return percent;
    }

    // Getter: lấy màu sắc
    public int getColor() {
        return color;
    }
}
