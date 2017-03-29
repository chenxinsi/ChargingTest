package com.example.chargingtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Service;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.util.Log;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.SystemProperties;
import android.os.PowerManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class MainActivity extends Activity {
	
	private EditText minPercent ;
	private EditText maxPercent;
	private EditText chargingTimesCounts;
	private TextView charging_Status;
	private TextView nowChargingPercent;
	private TextView nowChargingTimes;
	private TextView testResult;
	private Button btnStart;
	private Button btnStop;
	private AudioManager mAudioManager;
	private MediaPlayer mMediaPlayer;


	private BroadcastReceiver mBroadcastReceiver;
	private static SharedPreferences mSharedPreferences = null;

	
	private int mCurBatteryLevel = 0 ;
	private int min = 0 ;
	private int max = 0 ;
	private int chaging_times_counts;
	private int charging_times = 0;
	private int betweenMinAndMax = 0;
	public static final byte[] CHARGE_ON = { '1' };
	public static final byte[] CHARGE_OFF = { '0' };
	
	private final String TAG = "chargingTest";
	private final String BATTERYTEMPPATH ="/sys/class/power_supply/bms/temp";
	//private final String CHARGEPATH = "/sys/class/power_supply/battery/charging_enabled";
	private final String CHARGEPATH = "/sys/devices/platform/mt-battery/kkx_accs/discharging_cmd";
	private static final String CMD_CHARGING = "echo %1$d > /sys/devices/platform/mt-battery/kkx_accs/discharging_cmd";
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;

	private boolean isFirst ;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(TAG, "start chargingTest");
		mSharedPreferences = this.getSharedPreferences("charging", Activity.MODE_PRIVATE);
		isFirst  = mSharedPreferences.getBoolean("first", true);
		if(mSharedPreferences.getBoolean("first", true)){
			SharedPreferences.Editor editor = mSharedPreferences.edit();
			editor.putBoolean("first", false);
			editor.commit();
		}

        powerManager = (PowerManager) this.getSystemService(Service.POWER_SERVICE);
        wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
		minPercent = (EditText) findViewById(R.id.the_min_percent);
		maxPercent = (EditText) findViewById(R.id.the_max_percent);
		chargingTimesCounts = (EditText) findViewById(R.id.charging_times_counts);
		charging_Status = (TextView) findViewById(R.id.charging_status);
		nowChargingPercent = (TextView) findViewById(R.id.now_charging_percent);
		nowChargingTimes = (TextView) findViewById(R.id.now_charging_times);
		testResult = (TextView) findViewById(R.id.test_result);
		btnStart = (Button) findViewById(R.id.start);
		btnStop = (Button) findViewById(R.id.stop);
		btnStop.setEnabled(false);
		//第一次 或者 不是第一次 判断执行程序 显示测试结果
		isPass(isFirst);
		nowChargingTimes.setText(""+mSharedPreferences.getInt("current_times", 0));
		mAudioManager = (AudioManager) getApplicationContext()
				.getSystemService("audio");
		btnStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(isLegal(minPercent,maxPercent,chargingTimesCounts)){
					min = Integer.parseInt(minPercent.getText().toString().trim());
					Log.d(TAG,"min" + min);
					max = Integer.parseInt(maxPercent.getText().toString().trim());
					Log.d(TAG,"max" + max);
					chaging_times_counts = Integer.parseInt(chargingTimesCounts.getText().toString().trim());
					//开始测试  注册广播
					mBroadcastReceiver = new BatteryBroadcastReciver();
					IntentFilter intentFilter = new IntentFilter(
							Intent.ACTION_BATTERY_CHANGED);
					registerReceiver(mBroadcastReceiver, intentFilter);
					btnStart.setEnabled(false);
					btnStop.setEnabled(true);
					minPercent.setEnabled(false);
					maxPercent.setEnabled(false);
					chargingTimesCounts.setEnabled(false);
				}
			}
		});
		
		
		btnStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				unregisterReceiver(mBroadcastReceiver);
				minPercent.setEnabled(true);
				maxPercent.setEnabled(true);
				chargingTimesCounts.setEnabled(true);
				btnStart.setEnabled(true);
				btnStop.setEnabled(false);
				stopSpeakerPlay();
				charging_Status.setTextColor(Color.BLACK);
				charging_Status.setText("状态：等待测试");
				//无论充电还是放电 都打开充电接口
				writeOneorZero(0);
			}
		});
	}


	private void isPass(boolean ispass){
		Log.d(TAG,"isPass:" + ispass);
		if(ispass){
			testResult.setText("无，未测试");
		}else{
			String test_result = mSharedPreferences.getBoolean("test_result", false)
					? "成功" : "失败" ;
			if(mSharedPreferences.getBoolean("test_result", false)){
				testResult.setTextColor(Color.GREEN);
			}else{
				testResult.setTextColor(Color.RED);
			}
			testResult.setTextSize(30);
			testResult.setText(test_result);
		}
	}

	//扬声器播放音乐
	private void speakerPlay(){
		Log.d(TAG,"speakerPlay");
		//设置扬声器播放
		mAudioManager.setMode(AudioManager.MODE_NORMAL);
		//设置声音大小
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
				mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
		if(mMediaPlayer == null) {
			mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.test1);
			mMediaPlayer.setVolume(1f, 1f);
			//对文件进行循环播放
			mMediaPlayer.setLooping(true);
			mMediaPlayer.start();
		}
	}

        //停止播放音乐
        private void stopSpeakerPlay(){
            Log.d(TAG, "stopSpeakerPlay");
			if(mMediaPlayer != null) {
				if (mMediaPlayer.isPlaying()) {
					mMediaPlayer.stop();
					mMediaPlayer.release();
					mMediaPlayer = null;
				}
			}

        }

	@Override
	protected void onResume() {
		super.onResume();
                wakeLock.acquire();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
                wakeLock.release();
                stopSpeakerPlay();
		//无论充电还是放电 都打开充电接口
		writeOneorZero(0);
	}
	
	class BatteryBroadcastReciver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
				int level = intent.getIntExtra("level", 0);
				int total = intent.getIntExtra("scale", 100);
				mCurBatteryLevel = (level * 100) / total;
				nowChargingPercent.setText(mCurBatteryLevel + "%");
				Log.d(TAG,"mCurBatteryLevel="+mCurBatteryLevel);
				//执行充电或者放电
				writeOneorZero(isCharging(mCurBatteryLevel,min,max));
				Log.d(TAG, "readFile(CHARGEPATH):" + readFile(CHARGEPATH));

				// => cmd_discharging = -1    => cmd_discharging = 1
				if(!readFile(CHARGEPATH).equals("=> cmd_discharging = 1")){
					charging_Status.setTextColor(Color.GREEN);
					charging_Status.setText("状态：正在充电");
				}else{
					charging_Status.setTextColor(Color.RED);
					charging_Status.setText("状态：正在放电");
				}
				//当正常循环测试到指定次数时 输出结果成功 
				if(charging_times >= chaging_times_counts){
					btnStart.setEnabled(true);
					btnStop.setEnabled(false);
					charging_Status.setText("状态：测试结束");
					unregisterReceiver(mBroadcastReceiver);
					stopSpeakerPlay();
					SharedPreferences.Editor editor1 = mSharedPreferences.edit();
					editor1.putBoolean("test_result", true);
					editor1.putInt("current_times",chaging_times_counts);
					editor1.commit();
					minPercent.setEnabled(true);
					maxPercent.setEnabled(true);
					chargingTimesCounts.setEnabled(true);
					//输出测试结果
					testResult.setTextSize(30);
					testResult.setText("成功");
					testResult.setTextColor(Color.GREEN);
					//无论充电还是放电 都打开充电接口
					writeOneorZero(0);
				}
			}
		}
	}
		
   //判断充电/放电过程
   private int isCharging(int mCurBatteryLevel, int min ,int max){
	   //小于 设置最小值 充电
	   if(mCurBatteryLevel <= min ){
		   return 0;
		   //大于 设置最大值 放电
	   }else if(mCurBatteryLevel >= max){
		   return 1;
	   }else if(min < mCurBatteryLevel && mCurBatteryLevel < max){
		   //如果 范围在min ，max之间第一次开始测试 充电  betweenMinAndMax 0
		   Log.d(TAG,"betweenMinAndMax:" +betweenMinAndMax);
		   if(betweenMinAndMax == 0){
			   betweenMinAndMax = 1;
			   Log.d(TAG,"first betweenMinAndMax:" + betweenMinAndMax);
			   return 0;
		   }
		   return 2;
	   }
	   return 2;
   }
   
    //读取节点 判断充电还是放电 0充电 1放电
    private String readFile(String filePath){
    	String res = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))));
			String str = null;
			while ((str = br.readLine()) != null) {
				res += str;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return res;
    }
   
   //给冲放电节点 写值  0 充电 1 放电
    private void writeOneorZero(int isCharging){
    	if(isCharging == 0){
    		//写 0 充电
    		if(!(readFile(CHARGEPATH).equals("=> cmd_discharging = 0") || readFile(CHARGEPATH).equals("=> cmd_discharging = -1"))){
        		writeFile(0);
                Log.d(TAG, "writeFile(0)");
				stopSpeakerPlay();
				Log.d(TAG, "stopSpeakerPlay()");
    		}
    	}else if(isCharging == 1){
    		//写 1 放电  每次放电时 算一次循环
			Log.d(TAG,"speakerPlay()");
    		if(!readFile(CHARGEPATH).equals("=> cmd_discharging = 1")){
				writeFile(1);
				Log.d(TAG, "writeFile(1)");
				speakerPlay();
	    		charging_times ++ ;
	    		nowChargingTimes.setText(""+charging_times);
	    	}
    	}else if(isCharging == 2){
    		//不管
    	}
    }

    //写值 0 充电  1 放电
    private void writeFile (int i) {
               if(i==1){
                 //SystemProperties.set("ctl.start", "charging_enable");
				   batteryDischarging(true);
				   Log.d(TAG, "write charging_disable");
               }else if(i == 0){
                 //SystemProperties.set("ctl.start", "charging_disable");
				   Log.d(TAG, "write charging_enable");
				   batteryDischarging(false);
               }
	}

    //0 充电 1 放电
	public  void batteryDischarging(boolean discharging) {
		int val = 0;
		if (discharging) {
			val = 1;
		}
		String cmd = String.format(CMD_CHARGING, val);
		Log.d(TAG, "cmd: " + cmd);
		executeCmd(cmd);
	}

	private  void executeCmd(String cmd) {
		boolean result = true;
		try {
			int ret = ShellExe.execCommand(cmd);
			Log.d(TAG, "ret: " + ret);
		} catch (IOException e) {
			Log.d(TAG, "IOException: " + e.getMessage());
			result = false;
		}
	}
    
	//获取当前的电池的温度
	private String getNowBatteryTemp(){
		try{
			FileInputStream inStream = new FileInputStream(BATTERYTEMPPATH);
			Log.d(TAG,"inStream :" + inStream);
			if(inStream != null){
				byte[] buffer = new byte[8];
				int nu = inStream.read(buffer);
				inStream.close();
				Log.e(TAG, "nu=" + nu);
				Log.e(TAG, "buffer.toString()=" + new String(buffer));
				return new String(buffer);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			// default test mode without usb_mode file.
			return "-1000";
		} catch (IOException e) {
			e.printStackTrace();
			return "-1000";
		}
		return "-1000";
	}
	
	
	//判断输入是否合法
	private boolean isLegal(EditText minPercent , EditText maxPercent , EditText chargingTimesCounts){
		int theMinPercent = Integer.parseInt(minPercent.getText().toString().trim());
		int theMaxPercent = Integer.parseInt(maxPercent.getText().toString().trim());
		int theChargingTimesCounts = Integer.parseInt(chargingTimesCounts.getText().toString().trim());
		
		if(theMinPercent < 5  || theChargingTimesCounts <1 || theMinPercent >= theMaxPercent ){
			Toast.makeText(getApplicationContext(), "请输入合法的参数", 0);
			return false;
		}
		Log.d(TAG,"isLegal:" + "true");
		return true;
	}

}
