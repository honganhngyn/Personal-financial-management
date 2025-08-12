package com.example.cashly.ThongKe;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cashly.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Activity_CategoryChart:
 * - Màn hình hiển thị biểu đồ LineChart theo từng tháng của 1 danh mục cụ thể.
 * - Có thể chọn năm để xem.
 * - Dùng trong Fragment_Chart khi click vào 1 danh mục.
 */
public class Activity_CategoryChart extends AppCompatActivity {

    // --- UI ---
    private LineChart lineChart;
    private TextView tvCategoryTitle;
    private TextView tvSelectYear;
    private ImageButton btnBack;

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // --- State ---
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private String selectedCategory = "";
    private String type = ""; // "chi" hoặc "thu"

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_chart);

        // Liên kết các View
        lineChart = findViewById(R.id.lineChartCategory);
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        btnBack = findViewById(R.id.btnBack);
        tvSelectYear = findViewById(R.id.tvSelectYear);

        // Nút back
        btnBack.setOnClickListener(v -> finish());

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Hiển thị năm mặc định
        tvSelectYear.setText(getString(R.string.year) + ": " + selectedYear);

        // Xử lý khi click chọn năm
        tvSelectYear.setOnClickListener(v -> showYearPickerDialog());

        // Nhận category + type từ Intent
        selectedCategory = getIntent().getStringExtra("category_name");
        type = getIntent().getStringExtra("type");

        if (selectedCategory != null && type != null) {
            // Set tiêu đề danh mục
            tvCategoryTitle.setText(getString(R.string.danh_muc) + ": " + selectedCategory);

            // Load dữ liệu LineChart
            loadCategoryChartData(selectedCategory, selectedYear, type);
        }
    }

    // Hiển thị dialog chọn năm
    private void showYearPickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_year_picker, null);

        NumberPicker npYear = dialogView.findViewById(R.id.npYear);
        npYear.setMinValue(2010);
        npYear.setMaxValue(2030);
        npYear.setValue(selectedYear);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_year))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    selectedYear = npYear.getValue();
                    tvSelectYear.setText(getString(R.string.year) + ": " + selectedYear);

                    // Load lại biểu đồ với năm mới
                    loadCategoryChartData(selectedCategory, selectedYear, type);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    // Load dữ liệu LineChart cho danh mục
    private void loadCategoryChartData(String category, int year, String type) {
        ArrayList<Entry> entries = new ArrayList<>();
        float[] monthlyTotals = new float[12]; // Tổng tiền cho từng tháng

        String uid = auth.getCurrentUser().getUid();

        // Chọn collection theo type
        String collection = type.equals("chi") ? "expenses" : "incomes";

        db.collection("users").document(uid).collection(collection)
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(snapshot -> {
                    // Duyệt từng document
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String dateStr = doc.getString("date");
                        Double amount = doc.getDouble("amount");

                        if (dateStr != null && amount != null) {
                            // Parse ngày
                            String[] parts = dateStr.split("/");
                            if (parts.length == 3) {
                                int month = Integer.parseInt(parts[1]) - 1;
                                int y = Integer.parseInt(parts[2]);

                                // Nếu đúng năm đang chọn → cộng vào tháng đó
                                if (y == year) {
                                    monthlyTotals[month] += amount;
                                }
                            }
                        }
                    }

                    // Chuyển dữ liệu sang Entry cho LineChart
                    for (int i = 0; i < 12; i++) {
                        entries.add(new Entry(i + 1, monthlyTotals[i])); // X: tháng (1-12), Y: tổng tiền
                    }

                    int axisTextColor = ContextCompat.getColor(this, R.color.textColor);

                    // Chọn màu line theo type
                    int lineColor;
                    int circleColor;

                    if (type.equals("chi")) {
                        lineColor = Color.RED;
                        circleColor = Color.RED;
                    } else {
                        lineColor = Color.GREEN;
                        circleColor = Color.GREEN;
                    }

                    // Tạo DataSet cho LineChart
                    LineDataSet dataSet = new LineDataSet(entries,
                            (type.equals("chi") ? getString(R.string.expense1) : getString(R.string.income))
                                    + ": " + category);

                    dataSet.setColor(lineColor);
                    dataSet.setValueTextColor(axisTextColor);
                    dataSet.setLineWidth(2f);
                    dataSet.setCircleColor(circleColor);

                    // Gán dữ liệu vào LineChart
                    LineData lineData = new LineData(dataSet);
                    lineChart.setData(lineData);

                    // Thiết lập LineChart
                    lineChart.getDescription().setEnabled(false);
                    lineChart.getLegend().setEnabled(false);

                    lineChart.getAxisLeft().setTextColor(axisTextColor);
                    lineChart.getAxisRight().setTextColor(axisTextColor);

                    XAxis xAxis = lineChart.getXAxis();
                    xAxis.setGranularity(1f);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setTextColor(axisTextColor);

                    xAxis.setAxisMinimum(1f);
                    xAxis.setAxisMaximum(12f);
                    xAxis.setLabelCount(12, true);

                    // Format nhãn trục X → "T1", "T2", ... "T12"
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int month = (int) value;
                            return (month >= 1 && month <= 12) ? "T" + month : "";
                        }
                    });

                    // Cập nhật biểu đồ
                    lineChart.invalidate();
                });
    }
}
