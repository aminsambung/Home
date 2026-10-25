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
    private com.google.android.material.button.MaterialButton btnTema;
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
        // Cek Tema sebelum layout dimuat
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

        // Setup Tombol Tema
        updateTombolTemaUI(modeTema);
        btnTema.setOnClickListener(v -> {
            int currentMode = AppCompatDelegate.getDefaultNightMode();
            int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES) 
                          ? AppCompatDelegate.MODE_NIGHT_NO 
                          : AppCompatDelegate.MODE_NIGHT_YES;
            
            prefs.edit().putInt("mode_tema", newMode).apply();
            AppCompatDelegate.setDefaultNightMode(newMode);
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
            btnTema.setText("☀️"); // Tampilkan matahari jika sedang dark mode
        } else {
            btnTema.setText("🌙"); // Tampilkan bulan jika sedang light mode
        }
    }

    // ... (FUNGSI buatItemLampu, updateTampilanLampu, dll TETAP SAMA SEPERTI SEBELUMNYA) ...
    
    // Hanya fungsi showAturanDialog yang perlu diubah sedikit untuk CheckBox
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
            
            // Mengatur warna teks checkbox
            if (isDarkMode()) checkBoxes[i].setTextColor(Color.WHITE);
            else checkBoxes[i].setTextColor(Color.BLACK);
            
            // Mengatur warna kotak centang agar terlihat jelas (Hijau jika dicentang)
            ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                    getColor(R.color.hijau_primary), // Warna saat dicentang
                    getColor(R.color.text_gray_light) // Warna saat kosong
                }
            );
            checkBoxes[i].setButtonTintList(colorStateList);

            layoutCheckboxTarget.addView(checkBoxes[i]);
        }
        
        // ... (SISA KODE EDIT & SIMPAN SAMA SEPERTI SEBELUMNYA) ...
    }
    
    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    
    // ... (copy sisa fungsi lainnya dari kode sebelumnya) ...
}
