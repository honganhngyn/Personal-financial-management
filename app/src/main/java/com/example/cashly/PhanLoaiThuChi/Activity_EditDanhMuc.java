package com.example.cashly.PhanLoaiThuChi;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cashly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity_EditDanhMuc:
 * - Màn hình quản lý danh mục thu/chi.
 * - Cho phép Thêm / Sửa / Xóa các danh mục.
 * - Dữ liệu lưu trên Firestore: collection "categories".
 * - Dùng trong phần cấu hình danh mục của app.
 */
public class Activity_EditDanhMuc extends AppCompatActivity {

    // --- View + Adapter danh mục Chi ---
    private EditText etTenDanhMucChi;
    private ListView listDanhMucChi;
    private ImageButton btnClearChi;
    private ArrayAdapter<String> adapterChi;
    private ArrayList<String> danhMucChiList;

    // --- View + Adapter danh mục Thu ---
    private EditText etTenDanhMucThu;
    private ListView listDanhMucThu;
    private ImageButton btnClearThu;
    private ArrayAdapter<String> adapterThu;
    private ArrayList<String> danhMucThuList;

    // --- Firebase ---
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_danhmuc);

        // --- Khởi tạo Firebase ---
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // --- Tabs ---
        TextView tabChi = findViewById(R.id.tabChi);
        TextView tabThu = findViewById(R.id.tabThu);
        LinearLayout fragmentChi = findViewById(R.id.fragmentChi);
        LinearLayout fragmentThu = findViewById(R.id.fragmentThu);

        // --- Chi ---
        etTenDanhMucChi = findViewById(R.id.etTenDanhMucChi);
        listDanhMucChi = findViewById(R.id.listDanhMucChi);
        btnClearChi = findViewById(R.id.btnClearChi);

        danhMucChiList = new ArrayList<>();
        adapterChi = new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, danhMucChiList);
        listDanhMucChi.setAdapter(adapterChi);
        listDanhMucChi.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // --- Thu ---
        etTenDanhMucThu = findViewById(R.id.etTenDanhMucThu);
        listDanhMucThu = findViewById(R.id.listDanhMucThu);
        btnClearThu = findViewById(R.id.btnClearThu);

        danhMucThuList = new ArrayList<>();
        adapterThu = new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, danhMucThuList);
        listDanhMucThu.setAdapter(adapterThu);
        listDanhMucThu.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // --- Load dữ liệu từ Firestore ---
        loadDanhMuc("chi");
        loadDanhMuc("thu");

        // --- Xử lý chuyển Tabs ---
        tabChi.setOnClickListener(v -> {
            tabChi.setBackgroundResource(R.drawable.tab_selected);
            tabThu.setBackgroundResource(R.drawable.tab_unselected);
            fragmentChi.setVisibility(View.VISIBLE);
            fragmentThu.setVisibility(View.GONE);
        });

        tabThu.setOnClickListener(v -> {
            tabChi.setBackgroundResource(R.drawable.tab_unselected);
            tabThu.setBackgroundResource(R.drawable.tab_selected);
            fragmentChi.setVisibility(View.GONE);
            fragmentThu.setVisibility(View.VISIBLE);
        });

        // --- Xử lý khi mở đúng tab theo Intent ---
        String tab = getIntent().getStringExtra("tab");
        if ("thu".equals(tab)) {
            tabThu.performClick();
        } else {
            tabChi.performClick();
        }

        // --- Xử lý click chọn item trong List ---
        listDanhMucChi.setOnItemClickListener((parent, view, position, id) -> {
            String selected = danhMucChiList.get(position);
            etTenDanhMucChi.setText(selected);
            listDanhMucChi.setItemChecked(position, true);
        });

        listDanhMucThu.setOnItemClickListener((parent, view, position, id) -> {
            String selected = danhMucThuList.get(position);
            etTenDanhMucThu.setText(selected);
            listDanhMucThu.setItemChecked(position, true);
        });

        // --- Clear button ---
        etTenDanhMucChi.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearChi.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearChi.setOnClickListener(v -> etTenDanhMucChi.setText(""));

        etTenDanhMucThu.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearThu.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearThu.setOnClickListener(v -> etTenDanhMucThu.setText(""));

        // --- Xử lý các nút chức năng ---
        findViewById(R.id.btnThemChi).setOnClickListener(v -> themDanhMuc("chi"));
        findViewById(R.id.btnSuaChi).setOnClickListener(v -> suaDanhMuc("chi"));
        findViewById(R.id.btnXoaChi).setOnClickListener(v -> xoaDanhMuc("chi"));

        findViewById(R.id.btnThemThu).setOnClickListener(v -> themDanhMuc("thu"));
        findViewById(R.id.btnSuaThu).setOnClickListener(v -> suaDanhMuc("thu"));
        findViewById(R.id.btnXoaThu).setOnClickListener(v -> xoaDanhMuc("thu"));

        // --- Nút Back ---
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            setResult(Activity.RESULT_OK);
            finish();
        });
    }

    // ==========================
    // LOAD DỮ LIỆU DANH MỤC
    // ==========================
    private void loadDanhMuc(String type) {
        db.collection("users").document(uid)
                .collection("categories")
                .whereEqualTo("type", type)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ArrayList<String> list = (type.equals("chi")) ? danhMucChiList : danhMucThuList;
                    list.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name != null) {
                            list.add(name);
                        }
                    }
                    if (type.equals("chi")) adapterChi.notifyDataSetChanged();
                    else adapterThu.notifyDataSetChanged();
                });
    }

    // ==========================
    // THÊM DANH MỤC
    // ==========================
    private void themDanhMuc(String type) {
        EditText et = (type.equals("chi")) ? etTenDanhMucChi : etTenDanhMucThu;
        ArrayList<String> list = (type.equals("chi")) ? danhMucChiList : danhMucThuList;

        String name = et.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_category_name), Toast.LENGTH_SHORT).show();
            return;
        }

        if (list.contains(name)) {
            Toast.makeText(this, getString(R.string.category_exists), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("type", type); // để phân biệt

        db.collection("users").document(uid)
                .collection("categories")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    list.add(name);
                    if (type.equals("chi")) adapterChi.notifyDataSetChanged();
                    else adapterThu.notifyDataSetChanged();

                    et.setText("");
                    Toast.makeText(this, getString(R.string.category_added), Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================
    // SỬA DANH MỤC
    // ==========================
    private void suaDanhMuc(String type) {
        EditText et = (type.equals("chi")) ? etTenDanhMucChi : etTenDanhMucThu;
        ListView lv = (type.equals("chi")) ? listDanhMucChi : listDanhMucThu;
        ArrayList<String> list = (type.equals("chi")) ? danhMucChiList : danhMucThuList;

        int pos = lv.getCheckedItemPosition();
        String newName = et.getText().toString().trim();

        if (pos == ListView.INVALID_POSITION) {
            Toast.makeText(this, getString(R.string.select_category), Toast.LENGTH_SHORT).show();
            return;
        }

        if (newName.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_new_name), Toast.LENGTH_SHORT).show();
            return;
        }

        String oldName = list.get(pos);

        db.collection("users").document(uid)
                .collection("categories")
                .whereEqualTo("type", type)
                .whereEqualTo("name", oldName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update("name", newName);
                    }
                    list.set(pos, newName);
                    if (type.equals("chi")) adapterChi.notifyDataSetChanged();
                    else adapterThu.notifyDataSetChanged();

                    et.setText("");
                    Toast.makeText(this, getString(R.string.category_updated), Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================
    // XÓA DANH MỤC
    // ==========================
    private void xoaDanhMuc(String type) {
        ListView lv = (type.equals("chi")) ? listDanhMucChi : listDanhMucThu;
        ArrayList<String> list = (type.equals("chi")) ? danhMucChiList : danhMucThuList;

        int pos = lv.getCheckedItemPosition();
        if (pos == ListView.INVALID_POSITION) {
            Toast.makeText(this, getString(R.string.select_category), Toast.LENGTH_SHORT).show();
            return;
        }

        String nameToDelete = list.get(pos);

        db.collection("users").document(uid)
                .collection("categories")
                .whereEqualTo("type", type)
                .whereEqualTo("name", nameToDelete)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                    list.remove(pos);
                    if (type.equals("chi")) adapterChi.notifyDataSetChanged();
                    else adapterThu.notifyDataSetChanged();

                    Toast.makeText(this, getString(R.string.category_deleted), Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================
    // XỬ LÝ NÚT BACK
    // ==========================
    @Override
    public void onBackPressed() {
        try {
            listDanhMucChi.clearChoices();
            adapterChi.notifyDataSetChanged();
            listDanhMucThu.clearChoices();
            adapterThu.notifyDataSetChanged();

            setResult(Activity.RESULT_OK);
            super.onBackPressed();
        } catch (Exception e) {
            e.printStackTrace();
            super.onBackPressed();
        }
    }
}
