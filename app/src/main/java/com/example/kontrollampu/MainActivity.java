package com.example.kontrollampu;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    // Broadcast Receiver untuk menerima sinyal Alarm
    private final BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int lampuIndex = intent.getIntExtra("lampuIndex", -1);
            boolean nyala = intent.getBooleanExtra("nyala", false);
            if (lampuIndex != -1) {
                statusLampu[lampuIndex] = nyala;
                updateTampilanLampu(lampuIndex, nyala, true);
                simpanStatus(lampuIndex, nyala);
                updateStatusInfo();
                Toast.makeText(context, "Jadwal berjalan: Lampu " + (lampuIndex+1) + (nyala ? " ON" : " OFF"), Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("LampuPrefs", MODE_PRIVATE);
        int modeTema = prefs.getInt("mode_tema", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(modeTema);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Daftarkan Receiver
        IntentFilter filter = new IntentFilter("com.example.kontrollampu.ALARM_TRIGGER");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(alarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(alarmReceiver, filter);
        }

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
        btnTimer.setOnClickListener(v -> showTimerDialog(-1)); // -1 = Buat Baru
        btnAturan.setOnClickListener(v -> showAturanDialog(-1));
        btnRiwayat.setOnClickListener(v -> showMenuRiwayat());

        updateStatusInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(alarmReceiver);
    }

    // ... (Fungsi buatItemLampu, updateTampilanLampu, setSemuaLampu, updateStatusInfo, simpanStatus TETAP SAMA) ...
    private void updateTombolTemaUI(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) btnTema.setText("☀️");
        else btnTema.setText("🌙");
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
                if (!isUpdatingFromCode) evaluasiSemuaAturan(index, statusLampu[index]);
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
        for (boolean status : statusLampu) if (status) count++;
        tvStatusInfo.setText(count + " dari " + JUMLAH_LAMPU + " lampu menyala");
    }

    private void simpanStatus(int index, boolean isNyala) {
        prefs.edit().putBoolean("lampu_" + index, isNyala).apply();
    }

    // ==========================================
    // MENU RIWAYAT
    // ==========================================
    private void showMenuRiwayat() {
        String[] pilihan = {"Riwayat Aturan Otomatis", "Riwayat Jadwal (Timer)"};
        new AlertDialog.Builder(this).setTitle("Pilih Riwayat").setItems(pilihan, (dialog, which) -> {
            if (which == 0) showRiwayatAturan();
            else showRiwayatTimer();
        }).show();
    }

    // ==========================================
    // 1. RIWAYAT ATURAN (TETAP)
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
        try {
            JSONArray jsonArray = new JSONArray(prefs.getString("aturan_list", "[]"));
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
                for (int j = 0; j < targets.length(); j++) targetStr.append("Lampu ").append(targets.getInt(j) + 1).append(", ");

                String text = "Lampu " + pemicu + " " + (status ? "ON" : "OFF") + " -> " + aksi + " " + targetStr.toString().replaceAll(", $", "");
                TextView tv = new TextView(this);
                tv.setText(text);
                tv.setTextColor(getColor(R.color.text_dark_light));
                tv.setPadding(0, 16, 0, 16);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                
                Button btnEdit = new Button(this);
                btnEdit.setText("Edit");
                btnEdit.setOnClickListener(v -> { dialog.dismiss(); showAturanDialog(indexAturan); });
                Button btnHapus = new Button(this);
                btnHapus.setText("Hapus");
                btnHapus.setOnClickListener(v -> { hapusAturan(indexAturan); renderListAturan(layoutList, dialog); });

                row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(btnEdit); row.addView(btnHapus);
                layoutList.addView(row);
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void hapusAturan(int index) {
        try {
            JSONArray jsonArray = new JSONArray(prefs.getString("aturan_list", "[]"));
            List<JSONObject> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) if (i != index) list.add(jsonArray.getJSONObject(i));
            JSONArray newArray = new JSONArray();
            for (JSONObject obj : list) newArray.put(obj);
            prefs.edit().putString("aturan_list", newArray.toString()).apply();
        } catch (JSONException e) { e.printStackTrace(); }
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
            checkBoxes[i].setTextColor(isDarkMode() ? Color.WHITE : Color.BLACK);
            ColorStateList csl = new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked}}, new int[]{getColor(R.color.hijau_primary), getColor(R.color.text_gray_light)});
            checkBoxes[i].setButtonTintList(csl);
            layoutCheckboxTarget.addView(checkBoxes[i]);
        }

        if (editIndex >= 0) {
            try {
                JSONObject aturanLama = new JSONArray(prefs.getString("aturan_list", "[]")).getJSONObject(editIndex);
                spinnerPemicu.setSelection(aturanLama.getInt("pemicu"));
                spinnerStatusPemicu.setSelection(aturanLama.getBoolean("status_pemicu") ? 0 : 1);
                spinnerAksiTarget.setSelection(aturanLama.getString("aksi").equals("ON") ? 0 : 1);
                JSONArray targets = aturanLama.getJSONArray("targets");
                for (int j = 0; j < targets.length(); j++) checkBoxes[targets.getInt(j)].setChecked(true);
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
            for (int i = 0; i < JUMLAH_LAMPU; i++) if (checkBoxes[i].isChecked() && i != pemicuIndex) targetIndices.add(i);

            if (targetIndices.isEmpty()) { Toast.makeText(this, "Pilih minimal 1 lampu target!", Toast.LENGTH_SHORT).show(); return; }
            if (editIndex >= 0) hapusAturan(editIndex);
            simpanAturanKeJSON(pemicuIndex, pemicuStatus, aksi, targetIndices);
            Toast.makeText(this, (editIndex >= 0 ? "Aturan Diperbarui!" : "Aturan Tersimpan!"), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void simpanAturanKeJSON(int pemicuIndex, boolean pemicuStatus, String aksi, List<Integer> targets) {
        try {
            JSONArray jsonArray = new JSONArray(prefs.getString("aturan_list", "[]"));
            JSONObject aturanBaru = new JSONObject();
            aturanBaru.put("pemicu", pemicuIndex); aturanBaru.put("status_pemicu", pemicuStatus); aturanBaru.put("aksi", aksi); aturanBaru.put("targets", new JSONArray(targets));
            jsonArray.put(aturanBaru);
            prefs.edit().putString("aturan_list", jsonArray.toString()).apply();
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void evaluasiSemuaAturan(int changedIndex, boolean isNyala) {
        try {
            JSONArray jsonArray = new JSONArray(prefs.getString("aturan_list", "[]"));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject aturan = jsonArray.getJSONObject(i);
                if (aturan.getInt("pemicu") == changedIndex && aturan.getBoolean("status_pemicu") == isNyala) {
                    boolean setKeNyala = aturan.getString("aksi").equals("ON");
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

    // ==========================================
    // 2. JADWAL TIMER (BARU: ON & OFF TIME)
    // ==========================================
    private void showTimerDialog(int editIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_timer, null);
        builder.setView(view);

        Spinner spinnerLampu = view.findViewById(R.id.spinnerLampuTimer);
        TimePicker timeStart = view.findViewById(R.id.timePickerStart);
        TimePicker timeEnd = view.findViewById(R.id.timePickerEnd);
        timeStart.setIs24HourView(true);
        timeEnd.setIs24HourView(true);

        List<String> listLampu = new ArrayList<>();
        for (int i = 1; i <= JUMLAH_LAMPU; i++) listLampu.add("Lampu " + i);
        spinnerLampu.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listLampu));

        if (editIndex >= 0) {
            try {
                JSONObject jadwal = new JSONArray(prefs.getString("timer_list", "[]")).getJSONObject(editIndex);
                spinnerLampu.setSelection(jadwal.getInt("lampu"));
                timeStart.setHour(jadwal.getInt("startHour"));
                timeStart.setMinute(jadwal.getInt("startMin"));
                timeEnd.setHour(jadwal.getInt("endHour"));
                timeEnd.setMinute(jadwal.getInt("endMin"));
            } catch (JSONException e) { e.printStackTrace(); }
        }

        builder.setPositiveButton("Simpan Jadwal", null);
        builder.setNegativeButton("Batal", (d, w) -> d.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int lampuIdx = spinnerLampu.getSelectedItemPosition();
            int sHour = timeStart.getHour(); int sMin = timeStart.getMinute();
            int eHour = timeEnd.getHour(); int eMin = timeEnd.getMinute();

            if (editIndex >= 0) hapusTimer(editIndex); // Hapus lama jika edit
            simpanJadwalKeJSON(lampuIdx, sHour, sMin, eHour, eMin);
            setAlarm(lampuIdx, sHour, sMin, true);  // Pasang alarm ON
            setAlarm(lampuIdx, eHour, eMin, false); // Pasang alarm OFF
            
            Toast.makeText(this, "Jadwal Tersimpan & Alarm Aktif!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void simpanJadwalKeJSON(int lampuIdx, int sHour, int sMin, int eHour, int eMin) {
        try {
            JSONArray jsonArray = new JSONArray(prefs.getString("timer_list", "[]"));
            JSONObject jadwal = new JSONObject();
            jadwal.put("lampu", lampuIdx);
            jadwal.put("startHour", sHour); jadwal.put("startMin", sMin);
            jadwal.put("endHour", eHour); jadwal.put("endMin", eMin);
            jsonArray.put(jadwal);
            prefs.edit().putString("timer_list", jsonArray.toString()).apply();
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void setAlarm(int lampuIdx, int hour, int minute, boolean nyala) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("com.example.kontrollampu.ALARM_TRIGGER");
        intent.putExtra("lampuIndex", lampuIdx);
        intent.putExtra("nyala", nyala);
        
        // Menggunakan requestCode unik agar alarm tidak saling menimpa
        int reqCode = (lampuIdx * 10) + (nyala ? 1 : 2); 
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Jika waktu sudah lewat hari ini, set untuk besok
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void showRiwayatTimer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_riwayat_timer, null);
        builder.setView(view);
        LinearLayout layoutList = view.findViewById(R.id.layoutListTimer);
        layoutList.removeAllViews();

        try {
            JSONArray jsonArray = new JSONArray(prefs.getString("timer_list", "[]"));
            if (jsonArray.length() == 0) {
                TextView tv = new TextView(this);
                tv.setText("Belum ada jadwal timer.");
                tv.setTextColor(getColor(R.color.text_gray_light));
                layoutList.addView(tv);
            } else {
                for (int i = 0; i < jsonArray.length(); i++) {
                    final int idx = i;
                    JSONObject jadwal = jsonArray.getJSONObject(i);
                    int lampu = jadwal.getInt("lampu") + 1;
                    String start = String.format("%02d:%02d", jadwal.getInt("startHour"), jadwal.getInt("startMin"));
                    String end = String.format("%02d:%02d", jadwal.getInt("endHour"), jadwal.getInt("endMin"));

                    TextView tv = new TextView(this);
                    tv.setText("Lampu " + lampu + "\nON: " + start + " | OFF: " + end);
                    tv.setTextColor(getColor(R.color.text_dark_light));
                    tv.setPadding(0, 16, 0, 16);

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    
                    Button btnEdit = new Button(this);
                    btnEdit.setText("Edit");
                    btnEdit.setOnClickListener(v -> { builder.create().dismiss(); showTimerDialog(idx); });
                    
                    Button btnHapus = new Button(this);
                    btnHapus.setText("Hapus");
                    btnHapus.setOnClickListener(v -> {
                        hapusTimer(idx);
                        showRiwayatTimer(); // Refresh
                    });

                    row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    row.addView(btnEdit); row.addView(btnHapus);
                    layoutList.addView(row);
                }
            }
        } catch (JSONException e) { e.printStackTrace(); }

        builder.setPositiveButton("Tutup", (d, w) -> d.dismiss());
        builder.create().show();
    }

    private void hapusTimer(int index) {
        try {
            JSONArray jsonArray = new JSONArray(prefs.getString("timer_list", "[]"));
            List<JSONObject> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) if (i != index) list.add(jsonArray.getJSONObject(i));
            JSONArray newArray = new JSONArray();
            for (JSONObject obj : list) newArray.put(obj);
            prefs.edit().putString("timer_list", newArray.toString()).apply();
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
                }
