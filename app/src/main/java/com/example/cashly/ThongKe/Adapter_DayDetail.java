package com.example.cashly.ThongKe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.cashly.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter_DayDetail:
 * - Adapter cho ListView hiển thị danh sách giao dịch trong 1 ngày.
 * - Mỗi item: hiển thị category + số tiền (+/-).
 * - Dùng cho ListView trong Fragment_Calendar.
 */
public class Adapter_DayDetail extends BaseAdapter {

    private final Context context; // Context của ứng dụng (Activity/Fragment)
    private final List<Item_Transaction> transactionList; // Danh sách giao dịch

    // Constructor
    public Adapter_DayDetail(Context context, List<Item_Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
    }

    // Trả về số lượng item
    @Override
    public int getCount() {
        return transactionList.size();
    }

    // Trả về item tại vị trí position
    @Override
    public Object getItem(int position) {
        return transactionList.get(position);
    }

    // Trả về ID của item (ở đây dùng position làm ID)
    @Override
    public long getItemId(int position) {
        return position;
    }

    // Hàm render 1 dòng trong ListView
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Nếu convertView chưa được tạo → inflate layout item_day.xml
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_day, parent, false);
        }

        // Liên kết các TextView
        TextView tvCategory = convertView.findViewById(R.id.tvCategory);
        TextView tvAmount = convertView.findViewById(R.id.tvAmount);

        // Lấy transaction tại vị trí position
        Item_Transaction item = transactionList.get(position);

        // Set tên category
        tvCategory.setText(item.getCategory());

        // Định dạng số tiền
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        String amountStr = (item.isIncome() ? "+" : "-") + formatter.format(item.getAmount()) + " đ";

        // Set text số tiền
        tvAmount.setText(amountStr);

        // Set màu chữ: xanh (thu) / đỏ (chi)
        tvAmount.setTextColor(item.isIncome() ? 0xFF4CAF50 : 0xFFF44336); // Xanh (#4CAF50) / Đỏ (#F44336)

        return convertView;
    }
}
