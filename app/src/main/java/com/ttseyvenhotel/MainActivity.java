package com.ttseyvenhotel;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.ttlock.bl.sdk.api.ExtendedBluetoothDevice;
import com.ttlock.bl.sdk.api.TTLockClient;
import com.ttlock.bl.sdk.callback.ConnectLockCallback;
import com.ttlock.bl.sdk.callback.ScanLockCallback;
import com.ttlock.bl.sdk.callback.SetHotelDataCallback;
import com.ttlock.bl.sdk.entity.HotelData;
import com.ttlock.bl.sdk.entity.LockError;
import com.ttseyvenhotel.databinding.ActivityMainBinding;
import com.ttseyvenhotel.fingerprint.FingerprintActivity;
import com.ttseyvenhotel.firmwareupdate.FirmwareUpdateActivity;
import com.ttseyvenhotel.iccard.ICCardActivity;
import com.ttseyvenhotel.lock.LockApiActivity;
import com.ttseyvenhotel.passcode.PasscodeActivity;
import com.ttseyvenhotel.utils.AppUtil;
import com.ttseyvenhotel.wireless_keyboard.WirelessKeyboardActivity;


public class MainActivity extends BaseActivity {
    private static final int REQUEST_BT_CONNECT = 1001;
    private static final int REQUEST_ENABLE_BT = 1002;
    private ActivityResultLauncher<String> btConnectPermissionLauncher;

    // 2) Launcher untuk ACTION_REQUEST_ENABLE
    private ActivityResultLauncher<Intent> enableBtLauncher;

    // Simpan device yang mau dikonfigurasi
    private ExtendedBluetoothDevice pendingDevice;
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTLockClient.getDefault().prepareBTService(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initListener();
        btConnectPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Kalau diijinkan, lanjut ke flow Bluetooth aktif
                        checkAndEnableBluetooth();
                    } else {
                        Toast.makeText(this,
                                "Bluetooth permission denied", Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        // --- Inisialisasi launcher untuk enabling Bluetooth ---
        enableBtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // User sudah mengaktifkan Bluetooth
                        connectToLock();
                    } else {
                        Toast.makeText(this,
                                "Bluetooth not enabled", Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
        if (AppUtil.isAndroid12OrOver()) {
            AppUtil.checkPermission(this, Manifest.permission.BLUETOOTH_CONNECT);
        }
    }

    private void initListener(){
        binding.btnLock.setOnClickListener(v -> startTargetActivity(LockApiActivity.class));
        binding.btnPasscode.setOnClickListener(v -> startTargetActivity(PasscodeActivity.class));
        binding.btnFirmware.setOnClickListener(v ->  startTargetActivity(FirmwareUpdateActivity.class));
        binding.btnFingerprint.setOnClickListener(v -> startTargetActivity(FingerprintActivity.class));
        binding.btnIc.setOnClickListener(v -> startTargetActivity(ICCardActivity.class));
        binding.btnWirelessKeyboard.setOnClickListener(v -> startTargetActivity(WirelessKeyboardActivity.class));
    }
    public void setHotelData(HotelData hotelData, String lockData, SetHotelDataCallback callback) {
        TTLockClient.getDefault().setHotelData(hotelData, lockData, callback);
    }

    /**
     * Configures the hotel data on a TTlock device after successful initialization.
     *
     * This method performs the following steps:
     * 1. Checks if Bluetooth is enabled. If not, it returns.
     * 2. Connects to the specified {@link ExtendedBluetoothDevice}.
     * 3. Upon successful connection:
     *     a. Creates a new {@link HotelData} object.
     *     b. Retrieves hotel information from a cloud API (implementation needed in {@link #getHotelInfoFromCloud()}).
     *     c. Sets the retrieved hotel information, building number, and floor number in the {@link HotelData} object.
     *     d. Writes the configured {@link HotelData} to the lock using {@link #setHotelData(HotelData, String, SetHotelDataCallback)}.
     *     e. Displays a success or failure Toast message based on the outcome of writing the hotel data.
     * 4. If the connection to the lock fails, it displays a connection failure Toast message.
     *
     * @param device The {@link ExtendedBluetoothDevice} to configure.
     */ // Metode untuk mengatur hotel data setelah inisialisasi kunci
    private void configureHotelData(ExtendedBluetoothDevice device) {
        // Pastikan Bluetooth sudah aktif
        BluetoothManager btManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        BluetoothAdapter adapter = btManager != null
                ? btManager.getAdapter()
                : null;

        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Runtime-permission cek (Android 12+)
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            // Minta permission ke user atau handle ditolak
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BT_CONNECT);
            return;
        }

            // 3. Cek apakah Bluetooth aktif
        if (!adapter.isEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBt);
            return;
        }

        // Connect ke kunci
        TTLockClient.getDefault().connectLock(String.valueOf(device), new ConnectLockCallback() {
            public void onConnectLockSuccess(String lockData, int specialValue, int electricQuantity) {
                // Buat objek HotelData
                HotelData hotelData = new HotelData();

                // Dapatkan hotelInfo dari cloud API
                String hotelInfo = getHotelInfoFromCloud(); // Implementasikan metode ini

                // Set data hotel
                hotelData.setHotelInfo(hotelInfo);
                hotelData.setBuildingNumber(0);
                hotelData.setFloorNumber(0);

                // Tulis data hotel ke kunci
                setHotelData(hotelData, lockData, new SetHotelDataCallback() {
                    @Override
                    public void onSetHotelDataSuccess() {
                        // Hotel data berhasil dikonfigurasi
                        Toast.makeText(MainActivity.this, "Hotel data configured successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFail(LockError error) {
                        // Gagal mengkonfigurasi hotel data
                        Toast.makeText(MainActivity.this, "Failed to configure hotel data: " + error.getErrorMsg(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onConnectSuccess() {

            }

            @Override
            public void onFail(LockError error) {
                // Gagal terhubung ke kunci
                Toast.makeText(MainActivity.this, "Connection failed: " + error.getErrorMsg(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Implementasi untuk mendapatkan hotelInfo dari cloud API
    private String getHotelInfoFromCloud() {
        // Implementasikan logika untuk mendapatkan hotelInfo dari cloud API
        // Contoh implementasi sederhana:
        // Gunakan Retrofit atau library HTTP lainnya untuk memanggil API

        // Untuk sementara, kita return dummy data
        return "hotelInfoFromCloud";
    }

    // Tambahkan di MainActivity metode untuk scan dan konfigurasi kunci
    public void scanAndConfigureLock() {
        TTLockClient.getDefault().startScanLock(new ScanLockCallback() {
            @Override
            public void onScanLockSuccess(ExtendedBluetoothDevice device) {
                // Hentikan pemindaian setelah menemukan kunci
                TTLockClient.getDefault().stopScanLock();

                // Konfigurasi data hotel pada kunci
                configureHotelData(device);
            }

            @Override
            public void onFail(LockError error) {
                Toast.makeText(MainActivity.this, "Scan failed: " + error.getErrorMsg(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Jangan lupa untuk menambahkan di onDestroy()
    @Override
    protected void onDestroy() {
        super.onDestroy();
        TTLockClient.getDefault().stopBTService();
    }
}

