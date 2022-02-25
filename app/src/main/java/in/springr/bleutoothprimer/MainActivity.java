package in.springr.bleutoothprimer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;
    OutputStream outputStream;
    InputStream inputStream;
    Thread bluetoothThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    TextView textStatus;
    private static final int BT_PERMISSION_REQUEST_CODE = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonConnect = findViewById(R.id.buttonConnect);
        Button buttonON = findViewById(R.id.buttonON);
        Button buttonOFF = findViewById(R.id.buttonOFF);
        Button buttonClose = findViewById(R.id.buttonClose);
        textStatus = findViewById(R.id.textStatus);

        buttonConnect.setOnClickListener(v -> {
            findBluetoothDevice();
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                try {
                    openBluetoothConnection();
                } catch (IOException e) {
                    textStatus.setText("MESSAGE>" + e.getMessage());
                    Log.e("OPEN_EROR", "HERE");
                }
            }, 1000);

        });

        buttonON.setOnClickListener(v -> {
            try {
                sendData("RELAY1 ON");
            } catch (IOException e) {
                Log.e("SEND_EROR1", "HERE");
            }
        });

        buttonOFF.setOnClickListener(v -> {
            try {
                sendData("RELAY1 OFF");
            } catch (IOException e) {
                Log.e("SEND_EROR2", "HERE");
            }
        });

        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    closeBluetoothConnection();
                } catch (IOException e) {
                    Log.e("CLOSE_EROR", "HERE");
                }
            }
        });
        if (!checkPermissionForBluetooth()) {
            requestPermissionForBluetooth();
        }
    }

    @SuppressLint("MissingPermission")
    void findBluetoothDevice() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            textStatus.setText("No bluetooth adapter available");
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-05") || device.getName().equals("SPECTRA")) {
                    bluetoothDevice = device;
                    textStatus.setText("Bluetooth Device Found");
                    break;
                }
            }
        }

    }

    @SuppressLint("MissingPermission")
    void openBluetoothConnection() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //serial ports uuid
        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        try {
            bluetoothSocket.connect(); //fail here
        } catch (Exception e) {
            try {
                Class<?> clazz = bluetoothSocket.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{Integer.valueOf(1)};
                bluetoothSocket = (BluetoothSocket) m.invoke(bluetoothSocket.getRemoteDevice(), params);
                if (bluetoothSocket != null) {
                    Thread.sleep(1000);
                    bluetoothSocket.connect();
                } else {
                    Log.d("ERR", "fallback_socket received null....: ");
                }
            } catch (Exception ex) {
                Log.e("ERR", "exception_in_code....: " + e);
                e.printStackTrace();
            }
        }
        outputStream = bluetoothSocket.getOutputStream();
        inputStream = bluetoothSocket.getInputStream();
        beginListenForData();
        textStatus.setText("Bluetooth Opened");
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10;

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        bluetoothThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                try {
                    int bytesAvailable = inputStream.available();
                    if (bytesAvailable > 0) {
                        Log.e("DATA_INCOMING", "HERE");
                        byte[] packetBytes = new byte[bytesAvailable];
                        inputStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            if (b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;
                                Log.e("DATA_RECEIVED", data);
                                handler.post(() -> textStatus.setText("RECEIVED FROM DEVICE: " + data));
                            } else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException ex) {
                    stopWorker = true;
                }
            }
        });

        bluetoothThread.start();
    }

    void sendData(String data) throws IOException {
        data += "\r\n";
        outputStream.write(data.getBytes());
        textStatus.setText("Data Sent");
    }

    void closeBluetoothConnection() throws IOException {
        stopWorker = true;
        outputStream.close();
        inputStream.close();
        bluetoothSocket.close();
        textStatus.setText("Bluetooth Closed");
    }

    private boolean checkPermissionForBluetooth() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int resultBluetooth = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT);
            return resultBluetooth == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermissionForBluetooth() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH_CONNECT)) {
            Toast.makeText(this, "Bluetooth permission needed.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BT_PERMISSION_REQUEST_CODE);
        }
    }

}