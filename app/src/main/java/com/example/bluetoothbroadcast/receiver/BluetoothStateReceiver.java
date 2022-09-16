package com.example.bluetoothbroadcast.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 监听蓝牙状态
 */
public class BluetoothStateReceiver extends BroadcastReceiver {

    public static final int DEFAULT_VALUE_BLUETOOTH = 1000;
    public static final String TAG="BluetoothStateReceiver";

    public OnBluetoothStateListener mOnBluetoothStateListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
            int state=intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,DEFAULT_VALUE_BLUETOOTH);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    Log.d(TAG, "bluetooth is on");
                    if (mOnBluetoothStateListener != null) {
                        mOnBluetoothStateListener.onStateOn();
                    }
                    break;
                case BluetoothAdapter.STATE_OFF:
                    if (mOnBluetoothStateListener != null) {
                        mOnBluetoothStateListener.onStateOff();
                    }
                    break;
                default:
                    if (mOnBluetoothStateListener != null) {
                        mOnBluetoothStateListener.onStateUnKnow();
                    }
                    Log.d(TAG, "bluetooth status unknown");
                    break;
            }
        }
    }

    public interface OnBluetoothStateListener {
        void onStateOff();

        void onStateOn();

        void onStateUnKnow();
    }

    public void setMOnBluetoothStateListener(OnBluetoothStateListener onBluetoothStateListener) {
        mOnBluetoothStateListener = onBluetoothStateListener;
    }

}
