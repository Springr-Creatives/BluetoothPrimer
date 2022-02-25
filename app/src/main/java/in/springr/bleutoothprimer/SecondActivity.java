package in.springr.bleutoothprimer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;

public class SecondActivity extends AppCompatActivity {
    TextView textStatus;
    Bluetooth bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        Button buttonConnect = findViewById(R.id.buttonConnect);
        Button buttonON = findViewById(R.id.buttonON);
        Button buttonOFF = findViewById(R.id.buttonOFF);
        Button buttonClose = findViewById(R.id.buttonClose);
        textStatus = findViewById(R.id.textStatus);

        bluetooth = new Bluetooth(this);
        bluetooth.setBluetoothCallback(bluetoothCallback);

        buttonON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.send("RELAY1 ON\r\n");
            }
        });

        buttonOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.send("RELAY1 OFF\r\n");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();
        bluetooth.connectToNameWithPortTrick("SPECTRA");
        if (bluetooth.isEnabled()) {
            // doStuffWhenBluetoothOn() ...
        } else {
            bluetooth.enable();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetooth.onStop();
    }

    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override
        public void onBluetoothTurningOn() {
        }

        @Override
        public void onBluetoothTurningOff() {
        }

        @Override
        public void onBluetoothOff() {
        }

        @Override
        public void onUserDeniedActivation() {
        }

        @Override
        public void onBluetoothOn() {
            // doStuffWhenBluetoothOn() ...
        }
    };

}