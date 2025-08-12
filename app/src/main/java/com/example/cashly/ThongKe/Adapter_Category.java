package com.example.cashly.ThongKe;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashly.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter_Category:
 * - Adapter cho RecyclerView hiển thị danh sách các danh mục (category) trong Fragment_Chart.
 * - Mỗi item hiển thị: Tên danh mục, số tiền, phần trăm.
 * - Có hỗ trợ callback khi click vào item.
 */
public class Adapter_Category extends RecyclerView.Adapter<Adapter_Category.ViewHolder> {

    private final List<Item_Category> itemCategoryList;  // Danh sách các Item_Category
    private final OnItemClickListener listener;          // Callback khi click vào item

    // Interface callback
    public interface OnItemClickListener {
        void onItemClick(Item_Category item);
    }

    // Constructor
    public Adapter_Category(List<Item_Category> itemCategoryList, OnItemClickListener listener) {
        this.itemCategoryList = itemCategoryList;
        this.listener = listener;
    }

    // Tạo ViewHolder (inflate layout cho từng item)
    @NonNull
    @Override
    public Adapter_Category.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_category_summary.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_summary, parent, false);
        return new ViewHolder(view);
    }

    // Gán dữ liệu cho từng ViewHolder
    @Override
    public void onBindViewHolder(@NonNull Adapter_Category.ViewHolder holder, int position) {
        // Lấy item tại vị trí position
        Item_Category item = itemCategoryList.get(position);

        // Định dạng số tiền và phần trăm
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedAmount = formatter.format(item.getAmount()) + " đ";
        String formattedPercent = String.format(Locale.getDefault(), "%.1f%%", item.getPercent());

        // Set dữ liệu lên TextView
        holder.tvCategory.setText(item.getCategory());
        holder.tvAmount.setText(formattedAmount);
        holder.tvPercent.setText(formattedPercent);

        // Set màu chữ (màu của danh mục)
        holder.tvCategory.setTextColor(item.getColor());
        holder.tvAmount.setTextColor(item.getColor());
        holder.tvPercent.setTextColor(item.getColor());

        // Xử lý khi click vào item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    // Trả về số lượng item
    @Override
    public int getItemCount() {
        return itemCategoryList.size();
    }

    // --- ViewHolder ---
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvAmount, tvPercent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Liên kết các TextView trong layout item_category_summary.xml
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPercent = itemView.findViewById(R.id.tvPercent);
        }
    }
}
