package com.example.cashly.ThongKe;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.cashly.R;
import com.example.cashly.ThuChi.Fragment_AddDay;
import com.example.cashly.ThuChi.Fragment_EditDay;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Fragment_Calendar:
 * - Hiển thị giao dịch trên lưới lịch (GridView).
 * - Cho phép click vào từng ngày để xem chi tiết thu/chi.
 * - Cho phép thêm/sửa giao dịch.
 * - Dữ liệu lấy từ Firestore.
 */
public class Fragment_Calendar extends Fragment {

    // --- UI ---
    private GridView calendarGrid;
    private TextView tvMonth, tvThu, tvChi, tvTong, tvSelectedDate;
    private ImageButton btnPrevMonth, btnNextMonth;
    private ListView listDetails;

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // --- Trạng thái tháng hiện tại ---
    private Calendar currentCalendar;
    private List<String> days = new ArrayList<>();
    private Map<String, Float> thuMap = new HashMap<>();
    private Map<String, Float> chiMap = new HashMap<>();

    // --- List chi tiết ngày ---
    private ArrayAdapter<CharSequence> detailsAdapter;
    private List<CharSequence> detailsList = new ArrayList<>();
    private List<Item_Transaction> itemTransactions = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate layout fragment_calendar.xml
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentCalendar = Calendar.getInstance();

        // Liên kết các View
        linkViews(view);

        // Khởi tạo ListView chi tiết
        initListView();

        // Thiết lập chuyển tháng
        initMonthNavigation();

        // Thiết lập sự kiện click GridView
        initCalendarGridClick();

        // Đăng ký lắng nghe cập nhật từ Fragment_EditDay hoặc Fragment_AddDay
        registerUpdateListener();

        // Tải dữ liệu ban đầu
        refreshCalendar();

        return view;
    }

    // Liên kết các View
    private void linkViews(View view) {
        calendarGrid = view.findViewById(R.id.calendarGrid);
        tvMonth = view.findViewById(R.id.tvMonth);
        tvThu = view.findViewById(R.id.tvThu);
        tvChi = view.findViewById(R.id.tvChi);
        tvTong = view.findViewById(R.id.tvTong);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        listDetails = view.findViewById(R.id.listDetails);
    }

    // Khởi tạo ListView chi tiết ngày
    private void initListView() {
        detailsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, detailsList);
        listDetails.setAdapter(detailsAdapter);

        // Sự kiện click item → mở Fragment_EditDay
        listDetails.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= itemTransactions.size()) return;

            Item_Transaction item = itemTransactions.get(position);

            Bundle bundle = new Bundle();
            bundle.putString("documentId", item.getDocumentId());
            bundle.putString("date", item.getDate());
            bundle.putString("category", item.getCategory());
            bundle.putFloat("amount", item.getAmount());
            bundle.putString("note", item.getNote());
            bundle.putBoolean("isIncome", item.isIncome());

            Fragment_EditDay fragment = new Fragment_EditDay();
            fragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    // Thiết lập sự kiện chuyển tháng
    private void initMonthNavigation() {
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            refreshCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            refreshCalendar();
        });

        tvMonth.setOnClickListener(v -> showMonthYearPicker());
    }

    // Thiết lập sự kiện click/long click GridView
    private void initCalendarGridClick() {
        // Click 1 lần → xem chi tiết ngày
        calendarGrid.setOnItemClickListener((parent, view, position, id) -> {
            String day = days.get(position);
            if (!day.isEmpty()) {
                String selectedDate = formatSelectedDate(day);
                loadDetailsForDate(selectedDate);
            }
        });

        // Click giữ → thêm giao dịch mới cho ngày đó
        calendarGrid.setOnItemLongClickListener((parent, view, position, id) -> {
            String day = days.get(position);
            if (!day.isEmpty()) {
                String selectedDate = formatSelectedDate(day);

                Bundle bundle = new Bundle();
                bundle.putString("selectedDate", selectedDate);

                Fragment_AddDay fragmentAddDay = new Fragment_AddDay();
                fragmentAddDay.setArguments(bundle);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragmentAddDay)
                        .addToBackStack(null)
                        .commit();
            }
            return true;
        });
    }

    // Đăng ký lắng nghe cập nhật khi chỉnh sửa/ thêm mới giao dịch
    private void registerUpdateListener() {
        getParentFragmentManager().setFragmentResultListener("update_day", this, (requestKey, result) -> {
            String updatedDate = result.getString("updatedDate", "");
            if (!updatedDate.isEmpty()) {
                loadDetailsForDate(updatedDate);
            }
        });
    }

    // Làm mới lịch + dữ liệu tháng hiện tại
    private void refreshCalendar() {
        updateMonthText();
        generateDaysInMonth();
        loadDataFromFirestore();
    }

    // Cập nhật TextView tháng
    private void updateMonthText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        tvMonth.setText(sdf.format(currentCalendar.getTime()));
    }

    // Sinh danh sách ngày trong tháng (cho GridView)
    private void generateDaysInMonth() {
        days.clear();
        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) days.add("");
        for (int i = 1; i <= maxDays; i++) days.add(String.valueOf(i));
    }

    // Tải dữ liệu thu/chi từ Firestore
    private void loadDataFromFirestore() {
        thuMap.clear();
        chiMap.clear();

        float[] totalThu = {0f};
        float[] totalChi = {0f};

        String uid = auth.getCurrentUser().getUid();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

        int selectedMonth = currentCalendar.get(Calendar.MONTH);
        int selectedYear = currentCalendar.get(Calendar.YEAR);

        // --- Load incomes ---
        db.collection("users").document(uid).collection("incomes")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        String dateStr = doc.getString("date");
                        Number amount = doc.getDouble("amount");
                        processTransaction(dateStr, amount, sdf, selectedMonth, selectedYear, thuMap, totalThu);
                    }

                    // --- Load expenses ---
                    db.collection("users").document(uid).collection("expenses")
                            .get()
                            .addOnSuccessListener(querySnapshots2 -> {
                                for (QueryDocumentSnapshot doc : querySnapshots2) {
                                    String dateStr = doc.getString("date");
                                    Number amount = doc.getDouble("amount");
                                    processTransaction(dateStr, amount, sdf, selectedMonth, selectedYear, chiMap, totalChi);
                                }

                                // --- Cập nhật UI ---
                                tvThu.setText(getString(R.string.income) + "\n" + currencyFormat.format(totalThu[0]) + " đ");
                                tvChi.setText(getString(R.string.expense) + "\n" + currencyFormat.format(totalChi[0]) + " đ");
                                tvTong.setText(getString(R.string.total) + "\n" + currencyFormat.format(totalThu[0] - totalChi[0]) + " đ");

                                // Cập nhật GridView
                                Adapter_Calendar adapter = new Adapter_Calendar(requireContext(), days, thuMap, chiMap, selectedMonth + 1, selectedYear);
                                calendarGrid.setAdapter(adapter);
                            });
                });
    }

    // Xử lý từng transaction (thu/chi)
    private void processTransaction(String dateStr, Number amount, SimpleDateFormat sdf, int selectedMonth, int selectedYear, Map<String, Float> map, float[] total) {
        if (dateStr != null && amount != null) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(dateStr));

                if (cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear) {
                    total[0] += amount.floatValue();
                    String key = sdf.format(cal.getTime());
                    map.merge(key, amount.floatValue(), Float::sum);
                }
            } catch (Exception ignored) {}
        }
    }

    // Hiển thị hộp thoại chọn tháng/năm
    private void showMonthYearPicker() {
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
                    currentCalendar.set(Calendar.YEAR, npYear.getValue());
                    currentCalendar.set(Calendar.MONTH, npMonth.getValue() - 1);
                    refreshCalendar();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    // Định dạng ngày đã chọn (vd: "05/06/2025")
    private String formatSelectedDate(String day) {
        return String.format(Locale.getDefault(), "%02d/%02d/%04d",
                Integer.parseInt(day), currentCalendar.get(Calendar.MONTH) + 1, currentCalendar.get(Calendar.YEAR));
    }

    // Tải chi tiết giao dịch của ngày đã chọn
    private void loadDetailsForDate(String selectedDate) {
        detailsList.clear();
        itemTransactions.clear();

        String uid = auth.getCurrentUser().getUid();
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

        tvSelectedDate.setText(getString(R.string.label_ngay) + " " + selectedDate);

        // --- Load incomes ---
        db.collection("users").document(uid).collection("incomes")
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        addTransactionItem(doc, selectedDate, true, currencyFormat);
                    }

                    // --- Load expenses ---
                    db.collection("users").document(uid).collection("expenses")
                            .whereEqualTo("date", selectedDate)
                            .get()
                            .addOnSuccessListener(querySnapshots2 -> {
                                for (QueryDocumentSnapshot doc : querySnapshots2) {
                                    addTransactionItem(doc, selectedDate, false, currencyFormat);
                                }

                                detailsAdapter.notifyDataSetChanged();
                            });
                });
    }

    // Thêm 1 item giao dịch vào ListView
    private void addTransactionItem(QueryDocumentSnapshot doc, String date, boolean isIncome, NumberFormat currencyFormat) {
        String documentId = doc.getId();
        String category = doc.getString("category");
        Number amount = doc.getDouble("amount");
        String note = doc.getString("note");

        if (category != null && amount != null) {
            Item_Transaction item = new Item_Transaction(documentId, date, category, amount.floatValue(), isIncome, note == null ? "" : note);
            itemTransactions.add(item);

            String amountText = (isIncome ? "+ " : "- ") + currencyFormat.format(amount.doubleValue()) + " đ";
            String fullText = category + ": " + amountText;

            SpannableString spannable = new SpannableString(fullText);
            int start = fullText.indexOf(amountText);
            int end = start + amountText.length();

            int colorRes = isIncome ? android.R.color.holo_green_dark : android.R.color.holo_red_dark;
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(getResources().getColor(colorRes));
            spannable.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            detailsList.add(spannable);
        }
    }
}
