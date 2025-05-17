package com.ttseyvenhotel.wireless_keyboard.adapter;

import android.content.Context;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import com.ttseyvenhotel.BaseActivity;
import com.ttseyvenhotel.MainActivity;
import com.ttseyvenhotel.MyApplication;
import com.ttseyvenhotel.R;
import com.ttseyvenhotel.databinding.UserLockListItemBinding;
import com.ttseyvenhotel.model.LockObj;

;

/**
 * Created on  2019/4/12 0012 14:19
 *
 * @author theodre
 */
public class UserLockListAdapter extends  RecyclerView.Adapter<UserLockListAdapter.DeviceViewHolder>{

    public ArrayList<LockObj> mDataList = new ArrayList<>();

    private Context mContext;
    public UserLockListAdapter(Context context){
        mContext = context;
    }

    public void updateData(ArrayList<LockObj> lockList) {
        if (lockList != null) {
            mDataList.clear();
            mDataList.addAll(lockList);
            notifyDataSetChanged();
        }
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View mView = LayoutInflater.from(mContext).inflate(R.layout.user_lock_list_item, parent, false);
        return new DeviceViewHolder(mView);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder _holder, int position) {
        final LockObj item = mDataList.get(position);
        _holder.Bind(item);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {

        UserLockListItemBinding itemBinding;

        public DeviceViewHolder(View itemView){
            super(itemView);
            itemBinding = DataBindingUtil.bind(itemView);
        }

        public void Bind(LockObj item){
            itemBinding.tvLockName.setText(item.getLockAlias());
                itemBinding.getRoot().setOnClickListener(view -> {
                    MyApplication.getmInstance().saveChoosedLock(item);
                    ((BaseActivity) mContext).startTargetActivity(MainActivity.class);
                });
            }
        }
}
