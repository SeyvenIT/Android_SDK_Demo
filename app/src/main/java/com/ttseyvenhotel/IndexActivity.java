package com.ttseyvenhotel;

import androidx.databinding.DataBindingUtil;
import android.os.Bundle;

import com.ttseyvenhotel.databinding.ActivityIndexBinding;
import com.ttseyvenhotel.gateway.UserGatewayActivity;
import com.ttseyvenhotel.lock.UserLockActivity;

public class IndexActivity extends BaseActivity {

    private ActivityIndexBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_index);
        initListener();
    }

    private void initListener() {
        binding.btnLock.setOnClickListener(v -> startTargetActivity(UserLockActivity.class));
        binding.btnGateway.setOnClickListener(v -> startTargetActivity(UserGatewayActivity.class));
    }
}
