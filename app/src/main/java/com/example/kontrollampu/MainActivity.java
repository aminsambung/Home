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
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int JUMLAH_LAMPU = 8;
    private GridLayout gridLampu;
    private Button btnAllOn, btnAllOff, btnTimer, btnAturan;
    private TextView tvStatusInfo;

    private final SwitchMaterial[] switches = new SwitchMaterial[JUMLAH_LAMPU];
    private final TextView[] tvStatus = new TextView[JUMLAH_LAMPU];
    private final TextView[] tvIkon = new TextView[JUMLAH_LAMPU];
    private final CardView[] cards = new CardView[JUMLAH_LAMPU];

    private SharedPreferences prefs;
    private CountDownTimer countDownTimer;
    private boolean isUpdatingFromCode = false; 

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
        tvStatusInfo = findViewById(R.id.tvStatusInfo);

        buatItemLampu();

        btnAllOn.setOnClickListener(v -> setSemuaLampu(true));
        btnAllOff.setOnClickListener(v -> setSemuaLampu(false));
        btnTimer.setOnClickListener(v -> showTimerDialog());
        btnAturan.setOnClickListener(v -> showAturanDialog());

        updateStatusInfo();
    }

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
                
                // Evaluasi aturan setiap kali switch berubah
                if (!isUpdatingFromCode) {
                    evaluasiSemuaAturan(index, isChecked);
                }
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
        for (int i = 0; i < JUMLAH_LAMPU; i++) {
            switches[i].setChecked(nyala);
        }
    }

    private void updateStatusInfo() {
        int count = 0;
        for (SwitchMaterial sw : switches) {
            if (sw != null && sw.isChecked()) count++;
        }
        tvStatusInfo.setText(count + " dari " + JUMLAH_LAMPU + " lampu menyala");
    }

    private void simpanStatus(int index, boolean isNyala) {
        prefs.edit().putBoolean("lampu_" + index, isNyala).apply();
    }

    // ==========================================
    // LOGIKA ATURAN OTOMATIS (CUSTOM)
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

        // Siapkan Data Spinner
        List<String> listLampu = new ArrayList<>();
        for (int i = 1; i <= JUMLAH_LAMPU; i++) listLampu.add("Lampu " + i);
        
        ArrayAdapter<String> adapterLampu = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listLampu);
        spinnerPemicu.setAdapter(adapterLampu);

        String[] statusPemicu = {"HIDUP (ON)", "MATI (OFF)"};
        spinnerStatusPemicu.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusPemicu));

        String[] aksiTarget = {"HIDUPKAN", "MATIKAN"};
        spinnerAksiTarget.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, aksiTarget));

        // Buat CheckBox dinamis untuk target
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
            boolean pemicuStatus = spinnerStatusPemicu.getSelectedItemPosition() == 0; // true = ON
            String aksi = spinnerAksiTarget.getSelectedItemPosition() == 0 ? "ON" : "OFF";

            // Kumpulkan target yang dicentang
            List<Integer> targetIndices = new ArrayList<>();
            for (int i = 0; i < JUMLAH_LAMPU; i++) {
                if (checkBoxes[i].isChecked() && i != pemicuIndex) { // Cegah target = pemicu
                    targetIndices.add(i);
                }
            }

            if (targetIndices.isEmpty()) {
                Toast.makeText(this, "Pilih minimal 1 lampu target!", Toast.LENGTH_SHORT).show();
                return;
            }

            simpanAturanKeJSON(pemicuIndex, pemicuStatus, aksi, targetIndices);
            Toast.makeText(this, "Aturan Berhasil Disimpan!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    // Simpan aturan dalam bentuk JSON Array agar bisa banyak aturan
    private void simpanAturanKeJSON(int pemicuIndex, boolean pemicuStatus, String aksi, List<Integer> targets) {
        String jsonStr = prefs.getString("aturan_list", "[]");
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            JSONObject aturanBaru = new JSONObject();
            aturanBaru.put("pemicu", pemicuIndex);
            aturanBaru.put("status_pemicu", pemicuStatus);
            aturanBaru.put("aksi", aksi); // "ON" atau "OFF"
            aturanBaru.put("targets", new JSONArray(targets)); // Array of int

            jsonArray.put(aturanBaru);
            prefs.edit().putString("aturan_list", jsonArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Evaluasi aturan saat switch berubah
    private void evaluasiSemuaAturan(int changedIndex, boolean isNyala) {
        String jsonStr = prefs.getString("aturan_list", "[]");
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject aturan = jsonArray.getJSONObject(i);
                
                // Cek apakah lampu yang diubah adalah pemicu dari aturan ini
                if (aturan.getInt("pemicu") == changedIndex && aturan.getBoolean("status_pemicu") == isNyala) {
                    
                    String aksi = aturan.getString("aksi");
                    boolean setKeNyala = aksi.equals("ON");
                    JSONArray targets = aturan.getJSONArray("targets");

                    isUpdatingFromCode = true; // Kunci agar tidak looping
                    for (int j = 0; j < targets.length(); j++) {
                        int targetIdx = targets.getInt(j);
                        // Hanya ubah jika statusnya berbeda
                        if (switches[targetIdx].isChecked() != setKeNyala) {
                            switches[targetIdx].setChecked(setKeNyala);
                        }
                    }
                    isUpdatingFromCode = false;

                    Toast.makeText(this, "Aturan dieksekusi: Lampu " + (changedIndex+1) + " " + (isNyala?"ON":"OFF"), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // LOGIKA TIMER (TETAP SAMA)
    // ==========================================
    private void showTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_timer, null);
        builder.setView(view);

        EditText etMenit = view.findViewById(R.id.etMenit);
        builder.setPositiveButton("Mulai", null);
        builder.setNegativeButton("Batal", (d, w) -> d.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String input = etMenit.getText().toString();
            if (input.isEmpty()) return;
            int menit = Integer.parseInt(input);
            if (menit < 1 || menit > 1440) {
                Toast.makeText(this, "Minimal 1 menit, Maksimal 1440 menit", Toast.LENGTH_LONG).show();
                return;
            }
            mulaiTimer(menit);
            dialog.dismiss();
        });
    }

    private void mulaiTimer(int menit) {
        if (countDownTimer != null) countDownTimer.cancel();
        long durasiMs = menit * 60L * 1000L;
        Toast.makeText(this, "Timer aktif: " + menit + " menit lagi lampu menyala", Toast.LENGTH_LONG).show();

        countDownTimer = new CountDownTimer(durasiMs, 1000) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override
            public void onFinish() {
                setSemuaLampu(true);
                Toast.makeText(MainActivity.this, "Waktu habis! Semua lampu dinyalakan.", Toast.LENGTH_LONG).show();
            }
        }.start();
    }
    }
