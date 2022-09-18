package com.example.bluetoothbroadcast;

import static com.example.bluetoothbroadcast.MainActivity.TAG;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.bluetoothbroadcast.utils.BluetoothUtils;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
	@Test
	public void useAppContext() {
		// Context of the app under test.
		Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		assertEquals("com.example.bluetoothbroadcast", appContext.getPackageName());
		System.out.println("doulongfei"+appContext.getPackageName());
		Log.d(TAG, "useAppContext: "+"doulongfei"+appContext.getPackageName());
		BluetoothUtils instance = BluetoothUtils.getInstance();
		instance.openBluetooth();
	}
}