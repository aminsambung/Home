package com.example.kontrollampu;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.switchmaterial.SwitchMaterial;

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
    private boolean isUpdatingFromCode = false; // Mencegah looping saat Aturan berjalan

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
            
            // Baca status terakhir dari SharedPreferences (Fitur No 2)
            boolean isNyala = prefs.getBoolean("lampu_" + index, false);
            switches[index].setChecked(isNyala);
            updateTampilanLampu(index, isNyala, false); // Tanpa animasi saat start

            switches[i].setOnCheckedChangeListener((bv, isChecked) -> {
                updateTampilanLampu(index, isChecked, true);
                updateStatusInfo();
                simpanStatus(index, isChecked);
                
                // Jalankan aturan otomatis (Fitur No 5)
                if (!isUpdatingFromCode) {
                    cekAturanOtomatis(index, isChecked);
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

    // Fitur No 3: Animasi saat di-klik
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

    // Fitur No 2: Simpan Status
    private void simpanStatus(int index, boolean isNyala) {
        prefs.edit().putBoolean("lampu_" + index, isNyala).apply();
    }

    // Fitur No 4: Dialog Timer
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
            if (input.isEmpty()) {
                Toast.makeText(this, "Isi menit terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            int menit = Integer.parseInt(input);
            if (menit < 1 || menit > 1440) { // Maks 24 Jam (1440 menit)
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
        Toast.makeText(this, "Timer aktif: " + menit + " menit lagi lampu akan menyala", Toast.LENGTH_LONG).show();

        countDownTimer = new CountDownTimer(durasiMs, 1000) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override
            public void onFinish() {
                setSemuaLampu(true); // Aksi saat timer habis: nyalakan semua
                Toast.makeText(MainActivity.this, "Waktu habis! Semua lampu dinyalakan.", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    // Fitur No 5: Aturan Otomatis
    private void showAturanDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_aturan, null);
        builder.setView(view);

        Spinner spinnerPemicu = view.findViewById(R.id.spinnerPemicu);
        Spinner spinnerTarget = view.findViewById(R.id.spinnerTarget);
        Button btnSimpanAturan = view.findViewById(R.id.btnSimpanAturan);

        List<String> listLampu = new ArrayList<>();
        for (int i = 1; i <= JUMLAH_LAMPU; i++) {
            listLampu.add("Lampu " + i);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listLampu);
        spinnerPemicu.setAdapter(adapter);
        spinnerTarget.setAdapter(adapter);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSimpanAturan.setOnClickListener(v -> {
            int indexPemicu = spinnerPemicu.getSelectedItemPosition();
            int indexTarget = spinnerTarget.getSelectedItemPosition();

            // Simpan aturan di SharedPreferences
            // Format: aturan_4 = 6 (Jika lampu 5 hidup, matikan lampu 7. Index mulai dari 0)
            prefs.edit().putInt("aturan_" + indexPemicu, indexTarget).apply();
            
            Toast.makeText(this, "Aturan disimpan: Jika Lampu " + (indexPemicu+1) + " ON, Lampu " + (indexTarget+1) + " OFF", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
    }

    private void cekAturanOtomatis(int pemicuIndex, boolean isNyala) {
        if (!isNyala) return; // Aturan hanya jalan saat lampu dinyalakan

        int targetIndex = prefs.getInt("aturan_" + pemicuIndex, -1);
        
        if (targetIndex != -1 && switches[targetIndex].isChecked()) {
            isUpdatingFromCode = true; // Cegah looping
            switches[targetIndex].setChecked(false);
            isUpdatingFromCode = false;
            
            String pesan = "Aturan berjalan: Lampu " + (pemicuIndex+1) + " menyala, Lampu " + (targetIndex+1) + " dimatikan.";
            Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show();
        }
    }
}
