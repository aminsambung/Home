package com.example.kontrollampu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private static final int JUMLAH_LAMPU = 8;

    private GridLayout gridLampu;
    private Button btnAllOn, btnAllOff;
    private TextView tvStatusInfo;

    private final SwitchMaterial[] switches = new SwitchMaterial[JUMLAH_LAMPU];
    private final TextView[] tvStatus = new TextView[JUMLAH_LAMPU];
    private final TextView[] tvIkon = new TextView[JUMLAH_LAMPU];
    private final CardView[] cards = new CardView[JUMLAH_LAMPU];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridLampu = findViewById(R.id.gridLampu);
        btnAllOn = findViewById(R.id.btnAllOn);
        btnAllOff = findViewById(R.id.btnAllOff);
        tvStatusInfo = findViewById(R.id.tvStatusInfo);

        buatItemLampu();

        btnAllOn.setOnClickListener(v -> setSemuaLampu(true));
        btnAllOff.setOnClickListener(v -> setSemuaLampu(false));

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
            switches[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateTampilanLampu(index, isChecked);
                updateStatusInfo();
            });

            // Klik kartu juga toggle switch
            itemView.setOnClickListener(v -> switches[index].toggle());

            gridLampu.addView(itemView);

            // GridLayout butuh layout params
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) itemView.getLayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(6, 6, 6, 6);
            itemView.setLayoutParams(params);
        }
    }

    private void updateTampilanLampu(int index, boolean nyala) {
        if (nyala) {
            tvStatus[index].setText("ON");
            tvStatus[index].setTextColor(0xFF4CAF50);
            tvIkon[index].setAlpha(1f);
            cards[index].setCardBackgroundColor(0xFF2A2A1A); // sedikit kuning
        } else {
            tvStatus[index].setText("OFF");
            tvStatus[index].setTextColor(0xFFF44336);
            tvIkon[index].setAlpha(0.3f);
            cards[index].setCardBackgroundColor(0xFF1E1E1E);
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
}
