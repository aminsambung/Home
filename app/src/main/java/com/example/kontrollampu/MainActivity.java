package com.example.kontrollampu;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

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
    private MaterialButton btnAllOn, btnAllOff, btnTimer, btnAturan, btnRiwayat, btnTema;
    private TextView tvStatusInfo;

    private final ImageButton[] btnPowers = new ImageButton[JUMLAH_LAMPU];
    private final TextView[] tvStatus = new TextView[JUMLAH_LAMPU];
    private final TextView[] tvIkon = new TextView[JUMLAH_LAMPU];
    private final CardView[] cards = new CardView[JUMLAH_LAMPU];
    private final boolean[] statusLampu = new boolean[JUMLAH_LAMPU];

    private SharedPreferences prefs;
    private boolean isUpdatingFromCode = false;
    private final Map<Integer, CountDownTimer> activeTimers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("LampuPrefs", MODE_PRIVATE);
        int modeTema = prefs.getInt("mode_tema", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(modeTema);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridLampu = findViewById(R.id.gridLampu);
        btnAllOn = findViewById(R.id.btnAllOn);
        btnAllOff = findViewById(R.id.btnAllOff);
        btnTimer = findViewById(R.id.btnTimer);
        btnAturan = findViewById(R.id.btnAturan);
        btnRiwayat = findViewById(R.id.btnRiwayat);
        btnTema = findViewById(R.id.btnTema);
        tvStatusInfo = findViewById(R.id.tvStatusInfo);

        updateTombolTemaUI(modeTema);
        btnTema.setOnClickListener(v -> {
            int currentMode = AppCompatDelegate.getDefaultNightMode();
            int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES) 
                          ? AppCompatDelegate.MODE_NIGHT_NO 
                          : AppCompatDelegate.MODE_NIGHT_YES;
            
            prefs.edit().putInt("mode_tema", newMode).apply();
            AppCompatDelegate.setDefaultNightMode(newMode);
            recreate(); 
        });

        buatItemLampu();

        btnAllOn.setOnClickListener(v -> setSemuaLampu(true));
        btnAllOff.setOnClickListener(v -> setSemuaLampu(false));
        btnTimer.setOnClickListener(v -> showMenuTimer());
        btnAturan.setOnClickListener(v -> showAturanDialog(-1));
        btnRiwayat.setOnClickListener(v -> showMenuRiwayat());

        updateStatusInfo();
    }

    private void updateTombolTemaUI(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            btnTema.setText("☀️");
        } else {
            btnTema.setText("🌙");
        }
    }

    private void buatItemLampu() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < JUMLAH_LAMPU; i++) {
            View itemView = inflater.inflate(R.layout.item_lampu, gridLampu, false);
            cards[i] = itemView.findViewById(R.id.cardLampu);
            tvIkon[i] = itemView.findViewById(R.id.tvIkon);
            tvStatus[i] = itemView.findViewById(R.id.tvStatus);
            btnPowers[i] = itemView.findViewById(R.id.btnPower);

            TextView tvNama = itemView.findViewById(R.id.tvNama);
            tvNama.setText("Lampu " + (i + 1));

            final int index = i;
            statusLampu[index] = prefs.getBoolean("lampu_" + index, false);
            updateTampilanLampu(index, statusLampu[index], false);

            btnPowers[i].setOnClickListener(v -> {
                statusLampu[index] = !statusLampu[index]; 
                updateTampilanLampu(index, statusLampu[index], true);
                updateStatusInfo();
                simpanStatus(index, statusLampu[index]);
                
                if (!isUpdatingFromCode) {
                    evaluasiSemuaAturan(index, statusLampu[index]);
                }
            });

            itemView.setOnClickListener(v -> btnPowers[index].performClick());
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
            btnPowers[index].setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.power_on_bg)));
            btnPowers[index].setColorFilter(getColor(R.color.power_on_tint));
        } else {
            tvStatus[index].setText("OFF");
            tvStatus[index].setTextColor(getColor(R.color.text_off));
            tvIkon[index].setAlpha(0.3f);
            btnPowers[index].setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.power_off_bg)));
            btnPowers[index].setColorFilter(getColor(R.color.power_off_tint));
        }

        if (animate) {
            Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            anim.setDuration(300);
            tvIkon[index].startAnimation(anim);
        }
    }

    private void setSemuaLampu(boolean nyala) {
        for (int i = 0; i < JUMLAH_LAMPU; i++) {
            statusLampu[i] = nyala;
            updateTampilanLampu(i, nyala, true);
            simpanStatus(i, nyala);
        }
        updateStatusInfo();
        for (int i = 0; i < JUMLAH_LAMPU; i++) {
            if (!isUpdatingFromCode) evaluasiSemuaAturan(i, nyala);
        }
    }

    private void updateStatusInfo() {
        int count = 0;
        for (boolean status : statusLampu) {
            if (status) count++;
        }
        tvStatusInfo.setText(count + " dari " + JUMLAH_LAMPU + " lampu menyala");
    }

    private void simpanStatus(int index, boolean isNyala) {
        prefs.edit().putBoolean("lampu_" + index, isNyala).apply();
    }

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
                tv.setTextColor(getColor(R.color.text_gray_light));
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
                tv.setTextColor(getColor(R.color.text_dark_light));
                tv.setPadding(0, 16, 0, 16);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                
                Button btnEdit = new Button(this);
                btnEdit.setText("Edit");
                btnEdit.setOnClickListener(v -> {
                    dialog.dismiss();
                    showAturanDialog(indexAturan); 
                });

                Button btnHapus = new Button(this);
                btnHapus.setText("Hapus");
                btnHapus.setOnClickListener(v -> {
                    hapusAturan(indexAturan);
                    renderListAturan(layoutList, dialog);
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

    private void showRiwayatTimer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_riwayat_timer, null);
        builder.setView(view);
        LinearLayout layoutList = view.findViewById(R.id.layoutListTimer);
        
        if (activeTimers.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Tidak ada timer yang aktif.");
            tv.setTextColor(getColor(R.color.text_gray_light));
            layoutList.addView(tv);
        } else {
            for (Map.Entry<Integer, CountDownTimer> entry : activeTimers.entrySet()) {
                final int indexLampu = entry.getKey();
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 16, 0, 16);

                TextView tv = new TextView(this);
                tv.setText("Timer Lampu " + (indexLampu + 1) + " berjalan...");
                tv.setTextColor(getColor(R.color.text_dark_light));

                Button btnBatal = new Button(this);
                btnBatal.setText("Batalkan");
                btnBatal.setOnClickListener(v -> {
                    entry.getValue().cancel();
                    activeTimers.remove(indexLampu);
                    Toast.makeText(this, "Timer Lampu " + (indexLampu+1) + " dibatalkan", Toast.LENGTH_SHORT).show();
                    showRiwayatTimer();
                });

                row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(btnBatal);
                layoutList.addView(row);
            }
        }
        builder.setPositiveButton("Tutup", (d, w) -> d.dismiss());
        builder.create().show();
    }

    private void showAturanDialog(int editIndex) {
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
            
            if (isDarkMode()) checkBoxes[i].setTextColor(Color.WHITE);
            else checkBoxes[i].setTextColor(Color.BLACK);
            
            ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                    getColor(R.color.hijau_primary), 
                    getColor(R.color.text_gray_light) 
                }
            );
            checkBoxes[i].setButtonTintList(colorStateList);

            layoutCheckboxTarget.addView(checkBoxes[i]);
        }

        if (editIndex >= 0) {
            String jsonStr = prefs.getString("aturan_list", "[]");
            try {
                JSONArray jsonArray = new JSONArray(jsonStr);
                JSONObject aturanLama = jsonArray.getJSONObject(editIndex);
                spinnerPemicu.setSelection(aturanLama.getInt("pemicu"));
                spinnerStatusPemicu.setSelection(aturanLama.getBoolean("status_pemicu") ? 0 : 1);
                spinnerAksiTarget.setSelection(aturanLama.getString("aksi").equals("ON") ? 0 : 1);
                JSONArray targets = aturanLama.getJSONArray("targets");
                for (int j = 0; j < targets.length(); j++) {
                    checkBoxes[targets.getInt(j)].setChecked(true);
                }
                btnSimpanAturan.setText("Update Aturan");
            } catch (JSONException e) { e.printStackTrace(); }
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
            
            if (editIndex >= 0) hapusAturan(editIndex);
            simpanAturanKeJSON(pemicuIndex, pemicuStatus, aksi, targetIndices);
            Toast.makeText(this, (editIndex >= 0 ? "Aturan Diperbarui!" : "Aturan Tersimpan!"), Toast.LENGTH_SHORT).show();
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
                        if (statusLampu[targetIdx] != setKeNyala) {
                            statusLampu[targetIdx] = setKeNyala;
                            updateTampilanLampu(targetIdx, setKeNyala, true);
                            simpanStatus(targetIdx, setKeNyala);
                        }
                    }
                    isUpdatingFromCode = false;
                    updateStatusInfo();
                }
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

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
                statusLampu[indexLampu] = true;
                updateTampilanLampu(indexLampu, true, true);
                simpanStatus(indexLampu, true);
                activeTimers.remove(indexLampu);
                updateStatusInfo();
                Toast.makeText(MainActivity.this, "Waktu habis! Lampu " + (indexLampu+1) + " menyala.", Toast.LENGTH_LONG).show();
            }
        }.start();
        activeTimers.put(indexLampu, timer);
    }

    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
                                                }
