package com.ttseyvenhotel;

import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.os.Bundle;

import com.ttlock.bl.sdk.api.TTLockClient;
import com.ttseyvenhotel.databinding.ActivityMainBinding;
import com.ttseyvenhotel.fingerprint.FingerprintActivity;
import com.ttseyvenhotel.firmwareupdate.FirmwareUpdateActivity;
import com.ttseyvenhotel.iccard.ICCardActivity;
import com.ttseyvenhotel.lock.LockApiActivity;
import com.ttseyvenhotel.passcode.PasscodeActivity;
import com.ttseyvenhotel.utils.AppUtil;
import com.ttseyvenhotel.wireless_keyboard.WirelessKeyboardActivity;


public class MainActivity extends BaseActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTLockClient.getDefault().prepareBTService(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initListener();
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
}
