package com.example.bluetoothbroadcast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.bluetoothbroadcast.utils.BluetoothUtils;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
	public static final String TAG = MainActivity.class.getSimpleName();

	private BluetoothUtils mBluetoothUtils;
	private Button permission;
	private Button sendOrReceive;
	private Button sendApk;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mBluetoothUtils = BluetoothUtils.getInstance();


		permission = findViewById(R.id.permission);
		permission.setOnClickListener(this);

		sendOrReceive = findViewById(R.id.send_or_receive);
		sendOrReceive.setOnClickListener(this);

		sendApk = findViewById(R.id.send_apk);
		sendApk.setOnClickListener(this);

	}

	//	Android 12 蓝牙广播相关权限
	public static final String[] bluetoothPermissionsS = {
			Manifest.permission.BLUETOOTH_ADVERTISE,
			Manifest.permission.BLUETOOTH_SCAN,
			Manifest.permission.BLUETOOTH_CONNECT
	};

	//	android 11和10 需要动态申请位置权限
	public static final String[] bluetoothLocationPermissionsRQ = {
			Manifest.permission.ACCESS_FINE_LOCATION
	};

	//	android 9及一下 需要动态申请位置权限
	public static final String[] bluetoothLocationPermissionsP = {
			Manifest.permission.ACCESS_FINE_LOCATION
	};

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
			case R.id.permission:
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					PermissionX.init(this)
							.permissions(bluetoothPermissionsS)
							.request(new RequestCallback() {
								@Override
								public void onResult(boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
									Log.d(TAG, "onResult: " + allGranted + "授予列表" + grantedList + "拒绝列表" + deniedList);
								}
							});
				} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					PermissionX.init(this)
							.permissions(bluetoothLocationPermissionsRQ)
							.request(new RequestCallback() {
								@Override
								public void onResult(boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
									Log.d(TAG, "onResult: " + allGranted + "授予列表" + grantedList + "拒绝列表" + deniedList);
								}
							});
				} else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
					PermissionX.init(this)
							.permissions(bluetoothLocationPermissionsP)
							.request(new RequestCallback() {
								@Override
								public void onResult(boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
									Log.d(TAG, "onResult: " + allGranted + "授予列表" + grantedList + "拒绝列表" + deniedList);
								}
							});
				}
				/*registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
					@Override
					public void onActivityResult(Map<String, Boolean> result) {
						Log.d(TAG, "onActivityResult: " + result);
					}
				}).launch(new String[]{Manifest.permission_group.LOCATION});*/
				break;
			case R.id.send_or_receive:

				sendOrReceiveBroadcast();
				break;
			case R.id.send_apk:
				mBluetoothUtils.getBonded();
//				mBluetoothUtils.sendApkByBt(this);
			default:
				break;
		}

	}

	/**
	 * 发送或接收广播
	 */
	public void sendOrReceiveBroadcast() {

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "该手机不支持蓝牙广播", Toast.LENGTH_SHORT).show();
			return;
		}
		SendBroadcast();

		mBluetoothUtils.receiveBroadcastData();
	}

	/**
	 * 发送蓝牙广播
	 */
	@SuppressLint("MissingPermission")
	private void SendBroadcast() {
		char wifiTypeChar = mBluetoothUtils.getWifiType(this);
		String wifiTypeStr = String.valueOf(wifiTypeChar);
		byte[] wifiTypeByteArr = mBluetoothUtils.stringToByteArr(wifiTypeStr);
		Log.d(TAG, "wifi type:" + wifiTypeStr);
		if (!mBluetoothUtils.isSupportBluetooth()) {
			Log.d(TAG, "this device does not support bluetooth");
			return;
		}
		if (!mBluetoothUtils.isEnabled()) {
			mBluetoothUtils.registerBluetoothStateReceiver(this, wifiTypeByteArr);
			mBluetoothUtils.openBluetooth();
		} else {
			mBluetoothUtils.sendBroadcast(wifiTypeByteArr);
		}
	}


}