package com.example.bluetoothbroadcast.utils;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;


import com.example.bluetoothbroadcast.receiver.BluetoothStateReceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * 蓝牙相关工具类
 */
public class BluetoothUtils {

	public static final String TAG = BluetoothUtils.class.getSimpleName();
	public static ParcelUuid BROADCAST_SERVICE_PARCEUUID = ParcelUuid.fromString("0000fff7-0000-1000-8000-000777f9b34fb");
	public static int MANUFACTURER_ID = 0x01;
	public static final String g5Hz = "5";
	public static final String g24Hz = "2";
	/**
	 * 不得超过180000毫秒。值为0将禁用时间限制
	 */
	public static final int BROADCAST_SETTING_TIME = 10 * 1000;

	private final BluetoothAdapter mBluetoothAdapter;
	private static BluetoothUtils mInstance;
	private BluetoothLeScanner mBluetoothLeScanner;
	private BluetoothStateReceiver mBluetoothStateReceiver;
	private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
	private boolean isSupport5GWifi;
	private boolean isRegister = false;

	public static synchronized BluetoothUtils getInstance() {
		if (mInstance == null) {
			mInstance = new BluetoothUtils();
		}
		return mInstance;
	}

	public void registerBluetoothStateReceiver(Activity activity, byte[] broadcastData) {
		mBluetoothStateReceiver = new BluetoothStateReceiver();
		if (!isRegister) {
			IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
			activity.registerReceiver(mBluetoothStateReceiver, intentFilter);
			isRegister = true;
		}

		mBluetoothStateReceiver.setMOnBluetoothStateListener(new BluetoothStateReceiver.OnBluetoothStateListener() {
			@SuppressLint("MissingPermission")
			@Override
			public void onStateOff() {
				unRegisterBluetoothStateReceiver(activity);
				Log.d(TAG, "close bluetooth");
			}

			@Override
			public void onStateOn() {
				Log.d(TAG, "successfully turn on bluetooth");
				sendBroadcast(broadcastData);
				receiveBroadcastData();
				unRegisterBluetoothStateReceiver(activity);
			}

			@Override
			public void onStateUnKnow() {
				unRegisterBluetoothStateReceiver(activity);
			}
		});
	}

	@SuppressLint("MissingPermission")
	public void unRegisterBluetoothStateReceiver(Activity activity) {
		Log.d(TAG, "cancel bluetoothBroadcast monitoring");
		if (isRegister) {
			mBluetoothStateReceiver.setMOnBluetoothStateListener(null);
			try {
				activity.unregisterReceiver(mBluetoothStateReceiver);
			} catch (Exception exception) {
				Log.d(TAG, "error when unregisterReceiver mBluetoothStateReceiver:" + exception);
			}

			isRegister = false;
		}
	}

	/**
	 * 发送广播
	 *
	 * @param broadcastData 广播数据
	 */
	@SuppressLint("MissingPermission")
	public void sendBroadcast(byte[] broadcastData) {
		boolean multipleAdvertisementSupported =
				mBluetoothAdapter.isMultipleAdvertisementSupported();
		if (multipleAdvertisementSupported) {
			//获取BLE广播的操作对象。
			if (mBluetoothLeAdvertiser == null) {
				mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
			}
//            mBluetoothLeAdvertiser.cleanup();
			//初始化广播数据
			AdvertiseData broadcastScanData = initBroadcastScanData(broadcastData);
			mBluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
			//开启广播
			mBluetoothLeAdvertiser.startAdvertising(initBroadcastSetting(),
					initBroadcastPacketData(), broadcastScanData, advertiseCallback);
			Log.d(TAG, "begin send ble broadcast.");
		} else {
			Log.d(TAG, "the phone chip does not support broadcasting");
		}
	}


	/**
	 * 通过bt发送apk
	 *
	 * @param context 上下文
	 */
	public static void sendApkByBt(Context context) {
		try {
			String APP_MIME_TYPE = "application/zip";
			String BT_PKG_NAME = "com.android.bluetooth";
			ApplicationInfo applicationInfo = null;
			String appDir = null;
			try {
				applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
				appDir = applicationInfo.sourceDir;
				Log.d(TAG, "sendApkByBt: getApkDir" + appDir);
			} catch (PackageManager.NameNotFoundException e) {
				Log.d(TAG, "sendApkByBt:e " + e);
				e.printStackTrace();
			}
			File fileAPK = new File(appDir);
			/*用于给apk改名 （指定一个具体路径，检查该路径，创建文件，拷贝文件）*/
//            String newAppDir = com.woos.databackuplibrary.utils.CommonUtils.getSavePath(context, Constant.TYPE_SEND, Constant.TYPE_APP) + "WikoSwitch.apk";
//            CommonUtils.checkFile(newAppDir);
//            File destAPK = new File(newAppDir);
//            copyFile(fileAPK, destAPK);

			Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", fileAPK);
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_STREAM, uri);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			intent.setType(APP_MIME_TYPE);
			intent.setPackage(BT_PKG_NAME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
			Log.d(TAG, "sendApkByBt: startActivity success  appDir" + fileAPK + "\n uri:" + uri);
		} catch (Exception e) {
			Log.d(TAG, "sendApkByBt: e" + e);
			e.printStackTrace();
		}
	}

	/**
	 * 复制文件到另一个文件中
	 *
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		FileInputStream infile = new FileInputStream(sourceFile);
		FileOutputStream outfile = new FileOutputStream(destFile);
		byte[] bytesCar = new byte[1024 * 3];
		int length;
		while (-1 != (length = infile.read(bytesCar))) {
			outfile.write(bytesCar, 0, length);
		}
		outfile.flush();
		outfile.close();
		infile.close();
	}

	private BluetoothUtils() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	@SuppressLint("MissingPermission")
	public void getBonded(){
		Log.d(TAG, "getBonded: ");
		Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
//		bondedDevices.iterator().
		for (BluetoothDevice device : bondedDevices) {
			Log.d("Jason", "Name:" + device.getName() + "   Mac:" + device.getAddress());

			try {
				//使用反射调用获取设备连接状态方法
				Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
				isConnectedMethod.setAccessible(true);
				boolean isConnected = (boolean) isConnectedMethod.invoke(device, (Object[]) null);
				Log.d("dou", "isConnected：" + isConnected);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * 字节数组--->16进制字符串
	 *
	 * @param bytes  字节数组
	 * @param length 字节数组长度
	 * @return 16进制字符串 有空格
	 */
	public static String bytes2HexString(byte[] bytes, int length) {
		StringBuffer result = new StringBuffer();
		String hex;
		for (int i = 0; i < length; i++) {
			hex = Integer.toHexString(bytes[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			result.append(hex.toUpperCase()).append(" ");
		}
		return result.toString();
	}

	/**
	 * 16进制字符串--->字节数组
	 *
	 * @param src 16进制字符串
	 * @return byte[]
	 */
	public static byte[] hexString2Bytes(String src) {
		int strLength = src.length() / 2;
		byte[] ret = new byte[strLength];
		for (int i = 0; i < strLength; i++) {
			ret[i] = (byte) Integer
					.valueOf(src.substring(i * 2, i * 2 + 2), 16).byteValue();
		}
		return ret;
	}

	/**
	 * 是否支持蓝牙
	 *
	 * @return
	 */
	public Boolean isSupportBluetooth() {
		return mBluetoothAdapter != null;
	}

	/**
	 * 判断设备是否打开了蓝牙。
	 *
	 * @return
	 */
	public boolean isEnabled() {
		return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
	}

	/**
	 * 用户无感知打开蓝牙
	 */
	@SuppressLint("MissingPermission")
	public boolean openBluetooth() {
		return mBluetoothAdapter != null && mBluetoothAdapter.enable();
	}

	@SuppressLint("MissingPermission")
	public Boolean closeBluetooth() {
		return mBluetoothAdapter != null && mBluetoothAdapter.disable();
	}

	public byte[] charToByte(char c) {
		byte[] ret = new byte[2];
		ret[0] = (byte) ((c & 0xFF00) >> 8);
		ret[1] = (byte) (c & 0xFF);
		return ret;
	}

	/**
	 * 初始化广播设置
	 *
	 * @return AdvertiseSettings
	 */
	public AdvertiseSettings initBroadcastSetting() {
		return new AdvertiseSettings.Builder()
				//设置广播模式，以控制广播的功率和延迟。
				.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
				//发射功率级别
				.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
				//不得超过180000毫秒。值为0将禁用时间限制。
				.setTimeout(BROADCAST_SETTING_TIME)
				//设置是否可以连接
				.setConnectable(false)
				.build();
	}

	/**
	 * 初始化广播包
	 *
	 * @return AdvertiseData
	 */
	public AdvertiseData initBroadcastPacketData() {
		return new AdvertiseData.Builder()
				//设置广播设备名称
				.setIncludeDeviceName(true)
				//设置发射功率级别
				.setIncludeDeviceName(true)
				.build();
	}

	/**
	 * 初始化扫描响应包
	 *
	 * @param manufacturerData 厂商数据 byte[]
	 * @return AdvertiseData
	 */
	public AdvertiseData initBroadcastScanData(byte[] manufacturerData) {
		return new AdvertiseData.Builder()
				//隐藏广播设备名称
				.setIncludeDeviceName(false)
				//隐藏发射功率级别
				.setIncludeDeviceName(false)
				//设置广播的服务UUID
				.addServiceUuid(BROADCAST_SERVICE_PARCEUUID)
				// .addServiceData()   也可用于添加数据
				//设置厂商数据
				.addManufacturerData(MANUFACTURER_ID, manufacturerData)
				.build();
	}

	/**
	 * 广播发送后的回调
	 */
	public AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			Log.d(TAG, "broadcast send successfully" + settingsInEffect);
			super.onStartSuccess(settingsInEffect);
		}

		@Override
		public void onStartFailure(int errorCode) {
			super.onStartFailure(errorCode);
			Log.d(TAG, "broadcast sending failed：may be sending" + errorCode);
		}
	};

	@SuppressLint("MissingPermission")
	public void receiveBroadcastData() {
		Log.d(TAG, "begin receive broadcasts");
		if (isEnabled()) {
			mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
			mBluetoothLeScanner.startScan(createScanFilter(), new ScanSettings.Builder().build(), mScanCallBack);
		} else {
			openBluetooth();
		}
	}

	@SuppressLint("MissingPermission")
	public void unReceiveBroadcastData() {
		Log.d(TAG, "stop receiving broadcasts");
		try {
			if (mBluetoothLeScanner != null && isEnabled() && mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
				mBluetoothLeScanner.stopScan(mScanCallBack);
			}
		} catch (Exception e) {
			Log.d(TAG, "unReceiveBroadcastData: Exception " + e);
			e.printStackTrace();
		}
	}

	private List<ScanFilter> createScanFilter() {
		List<ScanFilter> scanFilters = new ArrayList<>();
		scanFilters.add(new ScanFilter.Builder().setServiceUuid(BROADCAST_SERVICE_PARCEUUID).build());
		return scanFilters;
	}

	private ScanCallback mScanCallBack = new ScanCallback() {
		@SuppressLint("MissingPermission")
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			super.onScanResult(callbackType, result);
			String paresResult = onReceiveData(result);
			is5GSupported(paresResult);
			Log.d(TAG, "scan was successful" + callbackType);
		}

		@Override
		public void onScanFailed(int errorCode) {
			super.onScanFailed(errorCode);
			Log.d(TAG, "scan filed" + errorCode);
		}
	};

	/**
	 * ScanResult扫描的所有结果处理
	 *
	 * @param result
	 */
	@SuppressLint("MissingPermission")
	public String onReceiveData(ScanResult result) {
		if (result == null) {
			Log.d(TAG, "result is null");
			return null;
		}
		ScanRecord record = result.getScanRecord();
		if (null == record) {
			Log.d(TAG, "result.getScanRecord is null");
			return null;
		}
		String parseResults = null;
		byte[] manufacturerSpecificData = record.getManufacturerSpecificData(BluetoothUtils.MANUFACTURER_ID);
		parseResults = byteArr2Str(manufacturerSpecificData);
		Log.d(TAG, "parseResults: " + parseResults);
		return parseResults;
	}


	/**
	 * is5 gsupported
	 *
	 * @param parseResults 解析结果
	 */
	private void is5GSupported(String parseResults) {
		if (g5Hz.equals(parseResults) || g24Hz.equals(parseResults)) {
			if (g5Hz.equals(parseResults)) {
				isSupport5GWifi = true;
			} else {
				isSupport5GWifi = false;
			}
			unReceiveBroadcastData();
		}
	}

	public static byte[] stringToByteArr(String str) {
		return str.getBytes();
	}

	/**
	 * byte[] 解析为String
	 *
	 * @param byteArr
	 * @return
	 */
	public static String byteArr2Str(byte[] byteArr) {
		if (null == byteArr || byteArr.length < 1) return "";
		StringBuilder sb = new StringBuilder();
		for (byte t : byteArr) {
			if ((t & 0xF0) == 0) sb.append("0");
			sb.append(Integer.toHexString(t & 0xFF));
		}
		String hexResult = sb.toString();

		if (hexResult == null || hexResult.equals("")) {
			return null;
		}
		hexResult = hexResult.replace(" ", "");
		byte[] baKeyword = new byte[hexResult.length() / 2];
		for (int i = 0; i < baKeyword.length; i++) {
			try {
				baKeyword[i] = (byte) (0xff & Integer.parseInt(hexResult.substring(i * 2, i * 2 + 2), 16));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			hexResult = new String(baKeyword, "UTF-8");
		} catch (Exception e1) {
			Log.d(TAG, "hexResult error: " + e1);
			e1.printStackTrace();
		}
		return hexResult;
	}

	/***********************wifi***************************/

	/**
	 * 获取WiFi类型
	 *
	 * @return 2:2.4G     5:5G
	 */
	public char getWifiType(Context context) {
		char gHz = '2';
		boolean g5 = false;
		try {
			WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			g5 = wifiManager.is5GHzBandSupported();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (g5) {
			gHz = '5';
			isSupport5GWifi = true;
		} else {
			gHz = '2';
			isSupport5GWifi = false;
		}
		return gHz;
	}

	/**
	 * is24Supported在 Android 12s中支持
	 * 判断是否支持 2.4G网络
	 */

	@RequiresApi(api = Build.VERSION_CODES.S)
	private boolean is24Supported(WifiManager wifiManager) {
		return wifiManager.is24GHzBandSupported();
	}

	/**
	 * getWifiTypeOnConnected
	 * 判断WiFi类型，2.4G\5G    需要先打开WIFI
	 *
	 * @param context
	 * @return 2:2.4G  5:5G
	 */
	public char getWifiTypeOnConnected(Context context) {
		WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo connectionInfo = wifiManager.getConnectionInfo();
		int frequency = connectionInfo.getFrequency();
		return String.valueOf(frequency).charAt(0);
	}

	public boolean isSupport5G() {
		return isSupport5GWifi;
	}

}
