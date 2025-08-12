package com.example.cashly.ThongKe;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashly.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Fragment_Chart:
 * - Hiển thị báo cáo thống kê thu/chi dạng PieChart + danh sách danh mục.
 * - Cho phép chuyển tháng, chọn tab "Chi" hoặc "Thu".
 * - Dữ liệu lấy từ Firestore.
 */
public class Fragment_Chart extends Fragment {

    // --- UI ---
    private PieChart pieChart;
    private TextView textChi, textThu, textTong, tvMonth;
    private ImageButton btnPrev, btnNext;
    private TextView tabChi, tabThu;
    private RecyclerView recyclerCategories;

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // --- Trạng thái tháng được chọn ---
    private Calendar currentCalendar;
    public static boolean isChiSelectedStatic = true;  // Lưu trạng thái chọn giữa các lần vào Fragment
    private boolean isChiSelected = true;              // Cờ kiểm tra đang chọn "Chi" hay "Thu"

    // --- Dữ liệu tổng ---
    private float totalChi = 0f;
    private float totalThu = 0f;

    // --- Adapter + List ---
    private Adapter_Category adapter;
    private ArrayList<Item_Category> itemCategoryList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate giao diện fragment_chart.xml
        View view = inflater.inflate(R.layout.fragment_chart, container, false);

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Lấy thời gian hiện tại
        currentCalendar = Calendar.getInstance();

        // Liên kết các View
        linkViews(view);

        // Khởi tạo RecyclerView
        initRecyclerView();

        // Thiết lập chuyển tháng
        initMonthNavigation();

        // Thiết lập tab "Chi"/"Thu"
        initTab();

        // Cập nhật UI ban đầu
        updateTabUI();
        refreshCalendar();

        return view;
    }

    // Liên kết các view trong layout
    private void linkViews(View view) {
        pieChart = view.findViewById(R.id.pieChart);
        textChi = view.findViewById(R.id.textChiChart);
        textThu = view.findViewById(R.id.textThuChart);
        textTong = view.findViewById(R.id.textTongThuChi);
        tvMonth = view.findViewById(R.id.tvMonth);
        btnPrev = view.findViewById(R.id.btnPrevMonth);
        btnNext = view.findViewById(R.id.btnNextMonth);
        tabChi = view.findViewById(R.id.tabChi);
        tabThu = view.findViewById(R.id.tabThu);
        recyclerCategories = view.findViewById(R.id.recyclerCategories);
    }

    // Thiết lập RecyclerView
    private void initRecyclerView() {
        recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new Adapter_Category(itemCategoryList, item -> {
            // Khi click vào 1 danh mục → mở Activity_CategoryChart
            Intent intent = new Intent(getContext(), Activity_CategoryChart.class);
            intent.putExtra("category_name", item.getCategory());
            intent.putExtra("type", isChiSelected ? "chi" : "thu");
            startActivity(intent);
        });

        recyclerCategories.setAdapter(adapter);
    }

    // Thiết lập sự kiện chuyển tháng (nút Prev/Next và click vào TextView tháng)
    private void initMonthNavigation() {
        btnPrev.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            refreshCalendar();
        });

        btnNext.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            refreshCalendar();
        });

        tvMonth.setOnClickListener(v -> showMonthYearPicker());
    }

    // Thiết lập sự kiện tab "Chi"/"Thu"
    private void initTab() {
        tabChi.setOnClickListener(v -> {
            isChiSelected = true;
            isChiSelectedStatic = true;
            updateTabUI();
            refreshCalendar();
        });

        tabThu.setOnClickListener(v -> {
            isChiSelected = false;
            isChiSelectedStatic = false;
            updateTabUI();
            refreshCalendar();
        });
    }

    // Cập nhật UI của tab "Chi"/"Thu"
    private void updateTabUI() {
        if (isChiSelected) {
            tabChi.setBackgroundResource(R.drawable.tab_selected);
            tabThu.setBackgroundResource(R.drawable.tab_unselected);
        } else {
            tabChi.setBackgroundResource(R.drawable.tab_unselected);
            tabThu.setBackgroundResource(R.drawable.tab_selected);
        }
    }

    // Cập nhật dữ liệu cho tháng hiện tại
    private void refreshCalendar() {
        updateMonthText();
        loadDataFromFirestore();
    }

    // Hiển thị tháng đang chọn
    private void updateMonthText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        tvMonth.setText(sdf.format(currentCalendar.getTime()));
    }

    // Tải dữ liệu từ Firestore
    private void loadDataFromFirestore() {
        // Reset dữ liệu cũ
        totalChi = 0f;
        totalThu = 0f;
        itemCategoryList.clear();

        String uid = auth.getCurrentUser().getUid();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        int selectedMonth = currentCalendar.get(Calendar.MONTH);
        int selectedYear = currentCalendar.get(Calendar.YEAR);

        // Chọn collection theo tab
        String collection = isChiSelected ? "expenses" : "incomes";

        db.collection("users").document(uid).collection(collection)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    Map<String, Float> categoryMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        String dateStr = doc.getString("date");
                        String category = doc.getString("category");
                        Number amount = doc.getDouble("amount");

                        if (dateStr != null && category != null && amount != null) {
                            try {
                                Calendar docCal = Calendar.getInstance();
                                docCal.setTime(sdf.parse(dateStr));

                                int docMonth = docCal.get(Calendar.MONTH);
                                int docYear = docCal.get(Calendar.YEAR);

                                // Chỉ lấy dữ liệu của tháng đang chọn
                                if (docMonth == selectedMonth && docYear == selectedYear) {
                                    float value = amount.floatValue();
                                    categoryMap.put(category, categoryMap.getOrDefault(category, 0f) + value);

                                    if (isChiSelected) {
                                        totalChi += value;
                                    } else {
                                        totalThu += value;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("ParseError", getString(R.string.error_parse_date) + ": " + dateStr);
                            }
                        }
                    }

                    // Cập nhật PieChart + List
                    updateChartAndList(categoryMap);
                });
    }

    // Cập nhật PieChart + List danh mục
    private void updateChartAndList(Map<String, Float> categoryMap) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        itemCategoryList.clear();

        float total = isChiSelected ? totalChi : totalThu;

        // Màu sắc cho biểu đồ
        List<Integer> chartColors = getColorPalette();
        int colorIndex = 0;

        List<Map.Entry<String, Float>> sortedCategoryList = new ArrayList<>(categoryMap.entrySet());

        for (Map.Entry<String, Float> entry : sortedCategoryList) {
            float value = entry.getValue();
            float percent = total > 0 ? (value / total) * 100f : 0f;

            int color = chartColors.get(colorIndex % chartColors.size());

            // Thêm vào PieChart
            entries.add(new PieEntry(value, entry.getKey()));

            // Thêm vào List hiển thị
            itemCategoryList.add(new Item_Category(entry.getKey(), value, percent, color));

            colorIndex++;
        }

        // --- Cập nhật PieChart ---
        PieDataSet dataSet = new PieDataSet(entries, getString(R.string.danh_muc));
        dataSet.setColors(chartColors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);  // Ẩn chú thích

        pieChart.invalidate();

        // --- Cập nhật tổng tiền ---
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        textChi.setText(getString(R.string.label_chi_tieu, formatter.format(totalChi)));
        textThu.setText(getString(R.string.label_thu_nhap, formatter.format(totalThu)));
        textTong.setText(getString(R.string.label_tong_thu_chi, formatter.format(totalThu - totalChi)));

        // --- Cập nhật RecyclerView ---
        adapter.notifyDataSetChanged();
    }

    // Lấy danh sách màu cho PieChart
    private List<Integer> getColorPalette() {
        List<Integer> chartColors = new ArrayList<>();
        Context context = getContext();

        if (context == null) return chartColors;

        chartColors.add(context.getResources().getColor(R.color.chart_color_1));
        chartColors.add(context.getResources().getColor(R.color.chart_color_2));
        chartColors.add(context.getResources().getColor(R.color.chart_color_3));
        chartColors.add(context.getResources().getColor(R.color.chart_color_4));
        chartColors.add(context.getResources().getColor(R.color.chart_color_5));
        chartColors.add(context.getResources().getColor(R.color.chart_color_6));
        chartColors.add(context.getResources().getColor(R.color.chart_color_7));
        chartColors.add(context.getResources().getColor(R.color.chart_color_8));
        chartColors.add(context.getResources().getColor(R.color.chart_color_9));
        chartColors.add(context.getResources().getColor(R.color.chart_color_10));
        chartColors.add(context.getResources().getColor(R.color.chart_color_11));
        chartColors.add(context.getResources().getColor(R.color.chart_color_12));
        chartColors.add(context.getResources().getColor(R.color.chart_color_13));
        chartColors.add(context.getResources().getColor(R.color.chart_color_14));
        chartColors.add(context.getResources().getColor(R.color.chart_color_15));
        chartColors.add(context.getResources().getColor(R.color.chart_color_16));
        chartColors.add(context.getResources().getColor(R.color.chart_color_17));
        chartColors.add(context.getResources().getColor(R.color.chart_color_18));
        chartColors.add(context.getResources().getColor(R.color.chart_color_19));
        chartColors.add(context.getResources().getColor(R.color.chart_color_20));

        return chartColors;
    }

    // Hiển thị hộp thoại chọn tháng/năm
    private void showMonthYearPicker() {
        final Calendar calendar = Calendar.getInstance();
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH);

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_month_year_picker, null);
        android.widget.NumberPicker npYear = dialogView.findViewById(R.id.npYear);
        android.widget.NumberPicker npMonth = dialogView.findViewById(R.id.npMonth);

        npYear.setMinValue(2000);
        npYear.setMaxValue(2100);
        npYear.setValue(currentYear);

        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setValue(currentMonth + 1);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.select_month_year))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    int selectedYear = npYear.getValue();
                    int selectedMonth = npMonth.getValue() - 1;

                    currentCalendar.set(Calendar.YEAR, selectedYear);
                    currentCalendar.set(Calendar.MONTH, selectedMonth);
                    refreshCalendar();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}
