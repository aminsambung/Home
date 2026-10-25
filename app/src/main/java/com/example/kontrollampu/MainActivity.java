package com.example.kontrollampu;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int JUMLAH_LAMPU = 8;
    private GridLayout gridLampu;
    private Button btnAllOn, btnAllOff, btnTimer, btnAturan, btnRiwayat;
    private TextView tvStatusInfo;

    private final SwitchMaterial[] switches = new SwitchMaterial[JUMLAH_LAMPU];
    private final TextView[] tvStatus = new TextView[JUMLAH_LAMPU];
    private final TextView[] tvIkon = new TextView[JUMLAH_LAMPU];
    private final CardView[] cards = new CardView[JUMLAH_LAMPU];

    private SharedPreferences prefs;
    private boolean isUpdatingFromCode = false;

    // Simpan instance timer yang aktif (Key: Index Lampu, Value: CountDownTimer)
    private final Map<Integer, CountDownTimer> activeTimers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("LampuPrefs", MODE_PRIVATE);

        gridLampu = findViewById(R.id.gridLampu);
        btnAllOn = findViewById(R.id.btnAllOn);
        btnAllOff = findViewById(R.id.btnAllOff);
        btnTimer = findViewById(R.id.btnTimer);
        btnAturan = findViewById(R.id.btnAturan);
        btnRiwayat = findViewById(R.id.btnRiwayat);
        tvStatusInfo = findViewById(R.id.tvStatusInfo);

        buatItemLampu();

        btnAllOn.setOnClickListener(v -> setSemuaLampu(true));
        btnAllOff.setOnClickListener(v -> setSemuaLampu(false));
        btnTimer.setOnClickListener(v -> showMenuTimer());
        btnAturan.setOnClickListener(v -> showAturanDialog());
        btnRiwayat.setOnClickListener(v -> showMenuRiwayat());

        updateStatusInfo();
    }

    // ... (Fungsi buatItemLampu, updateTampilanLampu, setSemuaLampu, updateStatusInfo, simpanStatus SAMA SEPERTI SEBELUMNYA) ...
    private void buatItemLampu() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < JUMLAH_LAMPU; i++) {
            View itemView = inflater.inflate(R.layout.item_lampu, gridLampu, false);
            cards[i] = itemView.findViewById(R.id.cardLampu);
            tvIkon[i] = itemView.findViewById(R.id.tvIkon);
            tvStatus[i] = itemView.findViewById(R.id.tvStatus);
            switches[i] = itemView.findViewById(R.id.switchLampu);

            TextView tvNama = itemView.findViewById(R.id.tvNama);
            tvNama.setText("Lampu " + (i + 1));

            final int index = i;
            boolean isNyala = prefs.getBoolean("lampu_" + index, false);
            switches[index].setChecked(isNyala);
            updateTampilanLampu(index, isNyala, false);

            switches[i].setOnCheckedChangeListener((bv, isChecked) -> {
                updateTampilanLampu(index, isChecked, true);
                updateStatusInfo();
                simpanStatus(index, isChecked);
                if (!isUpdatingFromCode) evaluasiSemuaAturan(index, isChecked);
            });

            itemView.setOnClickListener(v -> switches[index].toggle());
            gridLampu.addView(itemView);

            GridLayout.LayoutParams params = (GridLayout.LayoutParams) itemView.getLayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(6, 6, 6, 6);
            itemView.setLayoutParams(params);
        }
    }

    private void updateTampilanLampu(int index, boolean nyala, boolean animate) {
        if (nyala) {
            tvStatus[index].setText("ON");
            tvStatus[index].setTextColor(getColor(R.color.text_on));
            tvIkon[index].setAlpha(1f);
            cards[index].setCardBackgroundColor(getColor(R.color.card_on));
        } else {
            tvStatus[index].setText("OFF");
            tvStatus[index].setTextColor(getColor(R.color.text_off));
            tvIkon[index].setAlpha(0.3f);
            cards[index].setCardBackgroundColor(getColor(R.color.card_off));
        }
        if (animate) {
            Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            anim.setDuration(300);
            tvIkon[index].startAnimation(anim);
        }
    }

    private void setSemuaLampu(boolean nyala) {
        for (int i = 0; i < JUMLAH_LAMPU; i++) switches[i].setChecked(nyala);
    }

    private void updateStatusInfo() {
        int count = 0;
        for (SwitchMaterial sw : switches) if (sw != null && sw.isChecked()) count++;
        tvStatusInfo.setText(count + " dari " + JUMLAH_LAMPU + " lampu menyala");
    }

    private void simpanStatus(int index, boolean isNyala) {
        prefs.edit().putBoolean("lampu_" + index, isNyala).apply();
    }

    // ==========================================
    // FITUR MENU RIWAYAT (PILIHAN)
    // ==========================================
    private void showMenuRiwayat() {
        String[] pilihan = {"Riwayat Aturan Otomatis", "Riwayat Timer Aktif"};
        new AlertDialog.Builder(this)
                .setTitle("Pilih Riwayat")
                .setItems(pilihan, (dialog, which) -> {
                    if (which == 0) showRiwayatAturan();
                    else showRiwayatTimer();
                })
                .show();
    }

    // ==========================================
    // 1. RIWAYAT ATURAN (EDIT & HAPUS)
    // ==========================================
    private void showRiwayatAturan() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_riwayat_aturan, null);
        builder.setView(view);
        LinearLayout layoutList = view.findViewById(R.id.layoutListAturan);

        AlertDialog dialog = builder.create();
        dialog.show();
        
        renderListAturan(layoutList, dialog);
    }

    private void renderListAturan(LinearLayout layoutList, AlertDialog dialog) {
        layoutList.removeAllViews();
        String jsonStr = prefs.getString("aturan_list", "[]");
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            if (jsonArray.length() == 0) {
                TextView tv = new TextView(this);
                tv.setText("Belum ada aturan.");
                tv.setTextColor(getColor(R.color.text_secondary));
                layoutList.addView(tv);
                return;
            }

            for (int i = 0; i < jsonArray.length(); i++) {
                final int indexAturan = i;
                JSONObject aturan = jsonArray.getJSONObject(i);
                int pemicu = aturan.getInt("pemicu") + 1;
                boolean status = aturan.getBoolean("status_pemicu");
                String aksi = aturan.getString("aksi");
                JSONArray targets = aturan.getJSONArray("targets");
                
                StringBuilder targetStr = new StringBuilder();
                for (int j = 0; j < targets.length(); j++) {
                    targetStr.append("Lampu ").append(targets.getInt(j) + 1).append(", ");
                }

                String text = "Lampu " + pemicu + " " + (status ? "ON" : "OFF") + 
                              " -> " + aksi + " " + targetStr.toString().replaceAll(", $", "");

                TextView tv = new TextView(this);
                tv.setText(text);
                tv.setTextColor(getColor(R.color.text_primary));
                tv.setPadding(0, 16, 0, 16);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                
                Button btnEdit = new Button(this);
                btnEdit.setText("Edit");
                btnEdit.setOnClickListener(v -> {
                    dialog.dismiss();
                    // Buka dialog buat aturan dengan data yang sudah ada (fungsi edit)
                    // (Untuk simplifikasi, edit akan menghapus yang lama dan membuka form baru)
                    hapusAturan(indexAturan);
                    showAturanDialog(); 
                });

                Button btnHapus = new Button(this);
                btnHapus.setText("Hapus");
                btnHapus.setOnClickListener(v -> {
                    hapusAturan(indexAturan);
                    renderListAturan(layoutList, dialog); // Refresh list
                });

                row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(btnEdit);
                row.addView(btnHapus);
                layoutList.addView(row);
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void hapusAturan(int index) {
        String jsonStr = prefs.getString("aturan_list", "[]");
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            List<JSONObject> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                if (i != index) list.add(jsonArray.getJSONObject(i));
            }
            JSONArray newArray = new JSONArray();
            for (JSONObject obj : list) newArray.put(obj);
            prefs.edit().putString("aturan_list", newArray.toString()).apply();
        } catch (JSONException e) { e.printStackTrace(); }
    }

    // ==========================================
    // 2. RIWAYAT TIMER (HAPUS/BATALKAN)
    // ==========================================
    private void showRiwayatTimer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_riwayat_timer, null);
        builder.setView(view);
        LinearLayout layoutList = view.findViewById(R.id.layoutListTimer);
        
        if (activeTimers.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Tidak ada timer yang aktif.");
            tv.setTextColor(getColor(R.color.text_secondary));
            layoutList.addView(tv);
        } else {
            for (Map.Entry<Integer, CountDownTimer> entry : activeTimers.entrySet()) {
                final int indexLampu = entry.getKey();
                
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 16, 0, 16);

                TextView tv = new TextView(this);
                tv.setText("Timer Lampu " + (indexLampu + 1) + " berjalan...");
                tv.setTextColor(getColor(R.color.text_primary));

                Button btnBatal = new Button(this);
                btnBatal.setText("Batalkan");
                btnBatal.setOnClickListener(v -> {
                    entry.getValue().cancel();
                    activeTimers.remove(indexLampu);
                    Toast.makeText(this, "Timer Lampu " + (indexLampu+1) + " dibatalkan", Toast.LENGTH_SHORT).show();
                    showRiwayatTimer(); // Refresh
                });

                row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(btnBatal);
                layoutList.addView(row);
            }
        }

        builder.setPositiveButton("Tutup", (d, w) -> d.dismiss());
        builder.create().show();
    }

    // ==========================================
    // LOGIKA BUAT ATURAN (DENGAN SPINNER & CHECKBOX)
    // ==========================================
    private void showAturanDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_aturan, null);
        builder.setView(view);

        Spinner spinnerPemicu = view.findViewById(R.id.spinnerPemicu);
        Spinner spinnerStatusPemicu = view.findViewById(R.id.spinnerStatusPemicu);
        Spinner spinnerAksiTarget = view.findViewById(R.id.spinnerAksiTarget);
        LinearLayout layoutCheckboxTarget = view.findViewById(R.id.layoutCheckboxTarget);
        Button btnSimpanAturan = view.findViewById(R.id.btnSimpanAturan);

        List<String> listLampu = new ArrayList<>();
        for (int i = 1; i <= JUMLAH_LAMPU; i++) listLampu.add("Lampu " + i);
        
        spinnerPemicu.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listLampu));
        spinnerStatusPemicu.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"HIDUP (ON)", "MATI (OFF)"}));
        spinnerAksiTarget.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"HIDUPKAN", "MATIKAN"}));

        CheckBox[] checkBoxes = new CheckBox[JUMLAH_LAMPU];
        for (int i = 0; i < JUMLAH_LAMPU; i++) {
            checkBoxes[i] = new CheckBox(this);
            checkBoxes[i].setText("Lampu " + (i + 1));
            checkBoxes[i].setTextColor(getColor(R.color.text_primary));
            layoutCheckboxTarget.addView(checkBoxes[i]);
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSimpanAturan.setOnClickListener(v -> {
            int pemicuIndex = spinnerPemicu.getSelectedItemPosition();
            boolean pemicuStatus = spinnerStatusPemicu.getSelectedItemPosition() == 0;
            String aksi = spinnerAksiTarget.getSelectedItemPosition() == 0 ? "ON" : "OFF";

            List<Integer> targetIndices = new ArrayList<>();
            for (int i = 0; i < JUMLAH_LAMPU; i++) {
                if (checkBoxes[i].isChecked() && i != pemicuIndex) targetIndices.add(i);
            }

            if (targetIndices.isEmpty()) {
                Toast.makeText(this, "Pilih minimal 1 lampu target!", Toast.LENGTH_SHORT).show();
                return;
            }
            simpanAturanKeJSON(pemicuIndex, pemicuStatus, aksi, targetIndices);
            Toast.makeText(this, "Aturan Tersimpan!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void simpanAturanKeJSON(int pemicuIndex, boolean pemicuStatus, String aksi, List<Integer> targets) {
        String jsonStr = prefs.getString("aturan_list", "[]");
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            JSONObject aturanBaru = new JSONObject();
            aturanBaru.put("pemicu", pemicuIndex);
            aturanBaru.put("status_pemicu", pemicuStatus);
            aturanBaru.put("aksi", aksi);
            aturanBaru.put("targets", new JSONArray(targets));
            jsonArray.put(aturanBaru);
            prefs.edit().putString("aturan_list", jsonArray.toString()).apply();
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void evaluasiSemuaAturan(int changedIndex, boolean isNyala) {
        String jsonStr = prefs.getString("aturan_list", "[]");
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject aturan = jsonArray.getJSONObject(i);
                if (aturan.getInt("pemicu") == changedIndex && aturan.getBoolean("status_pemicu") == isNyala) {
                    String aksi = aturan.getString("aksi");
                    boolean setKeNyala = aksi.equals("ON");
                    JSONArray targets = aturan.getJSONArray("targets");

                    isUpdatingFromCode = true;
                    for (int j = 0; j < targets.length(); j++) {
                        int targetIdx = targets.getInt(j);
                        if (switches[targetIdx].isChecked() != setKeNyala) switches[targetIdx].setChecked(setKeNyala);
                    }
                    isUpdatingFromCode = false;
                }
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

    // ==========================================
    // LOGIKA TIMER (INDIVIDU & RIWAYAT)
    // ==========================================
    private void showMenuTimer() {
        String[] pilihan = {"Buat Timer Baru", "Lihat Timer Aktif"};
        new AlertDialog.Builder(this)
                .setTitle("Menu Timer")
                .setItems(pilihan, (dialog, which) -> {
                    if (which == 0) showBuatTimerDialog();
                    else showRiwayatTimer();
                })
                .show();
    }

    private void showBuatTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_timer, null);
        builder.setView(view);

        Spinner spinnerLampu = new Spinner(this);
        List<String> listLampu = new ArrayList<>();
        for (int i = 1; i <= JUMLAH_LAMPU; i++) listLampu.add("Lampu " + i);
        spinnerLampu.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listLampu));
        
        // Tambahkan Spinner ke layout dialog secara programatik (di atas EditText)
        LinearLayout root = (LinearLayout) view;
        root.addView(spinnerLampu, 1);

        EditText etMenit = view.findViewById(R.id.etMenit);
        builder.setPositiveButton("Mulai", null);
        builder.setNegativeButton("Batal", (d, w) -> d.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String input = etMenit.getText().toString();
            if (input.isEmpty()) return;
            int menit = Integer.parseInt(input);
            int lampuIndex = spinnerLampu.getSelectedItemPosition();
            
            if (menit < 1 || menit > 1440) {
                Toast.makeText(this, "Minimal 1 menit, Maksimal 1440 menit", Toast.LENGTH_LONG).show();
                return;
            }
            mulaiTimerSpesifik(lampuIndex, menit);
            dialog.dismiss();
        });
    }

    private void mulaiTimerSpesifik(int indexLampu, int menit) {
        if (activeTimers.containsKey(indexLampu)) {
            activeTimers.get(indexLampu).cancel();
        }

        long durasiMs = menit * 60L * 1000L;
        Toast.makeText(this, "Timer Lampu " + (indexLampu+1) + " aktif: " + menit + " menit", Toast.LENGTH_LONG).show();

        CountDownTimer timer = new CountDownTimer(durasiMs, 1000) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override
            public void onFinish() {
                isUpdatingFromCode = true;
                switches[indexLampu].setChecked(true); // Nyala saat timer habis
                isUpdatingFromCode = false;
                activeTimers.remove(indexLampu);
                Toast.makeText(MainActivity.this, "Waktu habis! Lampu " + (indexLampu+1) + " menyala.", Toast.LENGTH_LONG).show();
            }
        }.start();
        activeTimers.put(indexLampu, timer);
    }
}
