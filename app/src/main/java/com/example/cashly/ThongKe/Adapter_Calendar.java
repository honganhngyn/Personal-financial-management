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
import java.util.Map;

/**
 * Adapter_Calendar:
 * - Adapter cho GridView trong Fragment_Calendar.
 * - Mỗi ô hiển thị:
 *    + Số ngày
 *    + Tổng thu (nếu có)
 *    + Tổng chi (nếu có)
 * - Dữ liệu lấy từ Firestore.
 */
public class Adapter_Calendar extends BaseAdapter {

    private final Context context;
    private final List<String> days;          // Danh sách ngày (VD: "1", "2", ... hoặc "" cho padding đầu tháng)
    private final Map<String, Float> thuMap;  // Map ngày → tổng thu
    private final Map<String, Float> chiMap;  // Map ngày → tổng chi
    private final int currentMonth;           // Tháng hiện tại
    private final int currentYear;            // Năm hiện tại

    // Constructor
    public Adapter_Calendar(Context context, List<String> days,
                            Map<String, Float> thuMap, Map<String, Float> chiMap,
                            int currentMonth, int currentYear) {
        this.context = context;
        this.days = days;
        this.thuMap = thuMap;
        this.chiMap = chiMap;
        this.currentMonth = currentMonth;
        this.currentYear = currentYear;
    }

    // Trả về số lượng ô trong GridView
    @Override
    public int getCount() {
        return days.size();
    }

    // Trả về ngày tại vị trí position
    @Override
    public Object getItem(int position) {
        return days.get(position);
    }

    // Trả về ID của item
    @Override
    public long getItemId(int position) {
        return position;
    }

    // Render 1 ô trong GridView
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String day = days.get(position);

        // Inflate layout nếu convertView chưa có
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        }

        // Liên kết các TextView
        TextView tvDay = convertView.findViewById(R.id.tvDay);
        TextView tvThu = convertView.findViewById(R.id.tvThu);
        TextView tvChi = convertView.findViewById(R.id.tvChi);

        // Set ngày
        tvDay.setText(day);

        // Nếu là ô ngày trống (padding đầu tháng) → clear text
        if (day.isEmpty()) {
            clearDayView(tvThu, tvChi);
            return convertView;
        }

        // Format key ngày dạng "dd/MM/yyyy"
        String dateKey = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                Integer.parseInt(day), currentMonth, currentYear);

        // Lấy tổng thu/chi cho ngày này
        Float thu = thuMap.get(dateKey);
        Float chi = chiMap.get(dateKey);

        // Định dạng tiền tệ VN
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

        // --- Xử lý text thu ---
        String thuText = "";
        if (thu != null && thu > 0) {
            String formattedThu = formatter.format(thu);

            // Nếu string sau format > 12 ký tự → cắt bớt + "..."
            if (formattedThu.length() > 12) {
                formattedThu = formattedThu.substring(0, 9) + "...";
            }

            thuText = formattedThu;
        }

        // --- Xử lý text chi ---
        String chiText = "";
        if (chi != null && chi > 0) {
            String formattedChi = formatter.format(chi);

            if (formattedChi.length() > 12) {
                formattedChi = formattedChi.substring(0, 9) + "...";
            }

            chiText = formattedChi;
        }

        // Set text lên các TextView
        tvThu.setText(thuText);
        tvChi.setText(chiText);

        // Làm text nhỏ hơn cho đẹp
        tvThu.setTextSize(10);
        tvChi.setTextSize(10);

        // Bắt buộc 1 dòng, nếu dài → "..." ở cuối
        tvThu.setMaxLines(1);
        tvChi.setMaxLines(1);
        tvThu.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvChi.setEllipsize(android.text.TextUtils.TruncateAt.END);

        return convertView;
    }

    // Hàm clear các TextView cho ô ngày trống
    private void clearDayView(TextView tvThu, TextView tvChi) {
        tvThu.setText("");
        tvChi.setText("");
    }
}
