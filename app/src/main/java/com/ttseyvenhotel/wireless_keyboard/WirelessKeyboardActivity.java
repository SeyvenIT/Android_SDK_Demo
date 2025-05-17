package com.ttseyvenhotel.wireless_keyboard;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import androidx.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.reflect.TypeToken;
import com.ttlock.bl.sdk.api.TTLockClient;
import com.ttlock.bl.sdk.constant.FeatureValue;
import com.ttlock.bl.sdk.device.WirelessKeypad;
import com.ttlock.bl.sdk.keypad.InitKeypadCallback;
import com.ttlock.bl.sdk.keypad.ScanKeypadCallback;
import com.ttlock.bl.sdk.keypad.WirelessKeypadClient;
import com.ttlock.bl.sdk.keypad.model.InitKeypadResult;
import com.ttlock.bl.sdk.keypad.model.KeypadError;
import com.ttlock.bl.sdk.util.FeatureValueUtil;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import com.ttseyvenhotel.BaseActivity;
import com.ttseyvenhotel.DateUtils;
import com.ttseyvenhotel.MyApplication;
import com.ttseyvenhotel.R;
import com.ttseyvenhotel.databinding.ActivityWirelessKeyboardBinding;
import com.ttseyvenhotel.retrofit.ApiService;
import com.ttseyvenhotel.retrofit.RetrofitAPIManager;
import com.ttseyvenhotel.wireless_keyboard.adapter.KeyboardListAdapter;
import com.ttseyvenhotel.wireless_keyboard.adapter.KeyboardListAdapter.onLockItemClick;

public class WirelessKeyboardActivity extends BaseActivity implements onLockItemClick {
    ActivityWirelessKeyboardBinding binding;
    protected static final int REQUEST_PERMISSION_REQ_CODE = 11;
    private KeyboardListAdapter mListApapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_wireless_keyboard);
        initList();
        initListener();
        if(WirelessKeypadClient.getDefault().isBLEEnabled(this)){
            WirelessKeypadClient.getDefault().prepareBTService(MyApplication.getmInstance().getApplicationContext());
        }

    }

    private void initList(){
        mListApapter = new KeyboardListAdapter(this);
        binding.rvLockList.setAdapter(mListApapter);
        binding.rvLockList.setLayoutManager(new LinearLayoutManager(this));
        mListApapter.setOnLockItemClick(this);
    }


    private void initListener(){
        binding.btnEnableBle.setOnClickListener(v -> {
            boolean isBtEnable =  TTLockClient.getDefault().isBLEEnabled(WirelessKeyboardActivity.this);
            if(!isBtEnable){
                TTLockClient.getDefault().requestBleEnable(WirelessKeyboardActivity.this);
            }
        });
        binding.btnScanDevice.setOnClickListener(v -> startScan());
        binding.btnStopScan.setOnClickListener(v -> WirelessKeypadClient.getDefault().stopScanKeyboard());
    }


    /**
     * before call startScanLock,the location permission should be granted.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void startScan(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_REQ_CODE);
            return;
        }

        getScanWirelessKeyboardCallback();
    }



    @Override
    public void onClick(WirelessKeypad device) {
        if(!FeatureValueUtil.isSupportFeature(mCurrentLock.getFeatureValue(), FeatureValue.WIRELESS_KEYBOARD)){
            makeToast("--lock does not support add wireless keyboard--");
            return;
        }
        /**
         * WirelessKeypad device,
         * String lockmac: the lock which need add wireless key pad Mac address
         * InitKeypadCallback callback :
         */
        WirelessKeypadClient.getDefault().initializeKeypad(device, mCurrentLock.getLockMac(),  new InitKeypadCallback() {
            @Override
            public void onInitKeypadSuccess(InitKeypadResult initKeypadResult) {
                makeToast("=---add success-- upload to server to finish-");
                uploadToServer(device, initKeypadResult);
            }

            @Override
            public void onFail(KeypadError error) {
                makeErrorToast(error);
            }
        });
    }

    /**
     * start scan Wireless Keyboard device
     *
     */
    private void getScanWirelessKeyboardCallback(){
        WirelessKeypadClient.getDefault().startScanKeyboard(new ScanKeypadCallback() {
            @Override
            public void onScanKeyboardSuccess(WirelessKeypad device) {
                if(mListApapter != null){
                    mListApapter.updateData(device);
                }
            }

            @Override
            public void onScanFailed(int error) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length == 0 ){

            return;
        }

        switch (requestCode) {
            case REQUEST_PERMISSION_REQ_CODE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getScanWirelessKeyboardCallback();
                } else {
                    if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)){

                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private void uploadToServer(WirelessKeypad device, InitKeypadResult initKeypadResult){
        ApiService apiService = RetrofitAPIManager.provideClientApi();
        HashMap<String,String> params = new HashMap<>(8);
        String wirelessKeypadAlias = "MyTestPad-" + DateUtils.getMillsTimeFormat(System.currentTimeMillis());
        params.put("clientId",ApiService.CLIENT_ID);
        params.put("accessToken",MyApplication.getmInstance().getAccountInfo().getAccess_token());
        params.put("lockId",String.valueOf(mCurrentLock.getLockId()));
        params.put("wirelessKeyboardNumber",device.getName());
        params.put("wirelessKeyboardName",wirelessKeypadAlias);
        params.put("wirelessKeyboardMac",String.valueOf(device.getAddress()));
        params.put("wirelessKeypadFeatureValue", initKeypadResult.getFeatureValue());
        params.put("date",String.valueOf(System.currentTimeMillis()));


        Call<ResponseBody> call = apiService.addWirelessKeypad(params);
        RetrofitAPIManager.enqueue(call, new TypeToken<Object>() {
        }, result -> {
            if (!result.success) {
                makeToast("-modify add wireless  fail -" + result.getMsg());
                //if upload fail you should cache lockData and upload again until success,or you should reset lock and do init again.
                return;
            }
            makeToast("--upload success--");


        }, requestError -> {
            makeToast(requestError.getMessage());
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        /**
         * BT service should be released before Activity finished.
         */
        WirelessKeypadClient.getDefault().stopBTService();
    }
}
