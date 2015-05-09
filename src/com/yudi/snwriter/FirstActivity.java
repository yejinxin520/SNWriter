package com.yudi.snwriter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class FirstActivity extends Activity {

	private TextView wifimactv;
	private TextView btaddrtv;
	private TextView snctv;
	private WifiManager wifiManager;
	private static int IDENTIFY_LEN=17;
	private static BluetoothAdapter adapter;
	public static final String TAG = "SNWriter";
	private static final int MAC_BARCODE_DIGITS = 64;
	private long exitTime = 0;
 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_first);
		
		wifimactv = (TextView)findViewById(R.id.iniwifimac);
		btaddrtv = (TextView)findViewById(R.id.inibtaddr);
		snctv = (TextView)findViewById(R.id.inisn);
		String wifistr = 
				getMacFromDevice(getApplicationContext(), 100);//getMac();
		String btmacstr = 
				getBTAddr();
		 
		String serialName=getBarcode();
		btaddrtv.setText(btmacstr);
		wifimactv.setText(wifistr);
		snctv.setText(serialName);
	}
	public void change(View view) {
		Intent intent = new Intent(this,MainActivity.class);
		startActivity(intent);
	}
	private String getBarcode() {
		String barstr = null;
		byte[] sn_b = new byte[64] ;

		try {
			NvRAMAgent agent = NvRAMAgentHelper.getAgent();

			byte[] buff = agent.readFile(NvRAMAgentHelper.PRODUCT_INFO_ID);

			dumpHex(buff);

			int i, j;
			// sn
			for (i = 0, j = 0; i < MAC_BARCODE_DIGITS; i++) {
				for (; j < (buff.length > MAC_BARCODE_DIGITS ? MAC_BARCODE_DIGITS: buff.length); j++, i++)
					 sn_b[i] = buff[j] ;

				//buff[i] = 0;
			}
			barstr = new String(sn_b);

		} catch (Exception e) {
			Log.d(TAG, e.getMessage() + ":" + e.getCause());
			e.printStackTrace();
		}
		
		return barstr;
	}
	public String getMac() {
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
		if (!wifiManager.isWifiEnabled()) {
			tryOpenWifi(wifiManager);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
        String macSerial = null;
        String str = "";
        try {
                Process pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address");
                InputStreamReader ir = new InputStreamReader(pp.getInputStream());
                LineNumberReader input = new LineNumberReader(ir);

                for (; null != str;) {
                        str = input.readLine();
                        if (str != null) {
                                macSerial = str.trim();
                                break;
                        }
                }
        } catch (IOException e) {
                e.printStackTrace();
        }
        return macSerial;
}
	
	private void dumpHex(byte[] buff) {
		StringBuilder sb = new StringBuilder();
		sb.append("buff.length:" + buff.length + "\n");
		if (buff != null && buff.length > 0) {
			for (int i1 = 0; i1 < buff.length; i1++) {
				sb.append(Integer.toHexString(buff[i1]) + ',');
			}
		}
		Log.i(TAG, sb.toString());
	}
	public  String getBTAddr() {
		adapter = BluetoothAdapter.getDefaultAdapter();
		String btmac = "";
		if(adapter!=null){
			if(!adapter.isEnabled()){
				//Intent enBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);  
                //startActivityForResult(enBT, REQUEST_ENABLE_BT);
				adapter.enable();
				Thread t = new Thread(new Runnable(){  
		            public void run(){  
		            	for (int index = 0; index < 100; index++) {
							
							if (index != 0) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
		            }});  
		        t.start();				
				btmac = adapter.getAddress();				
			}else{
				btmac = adapter.getAddress();
				
			}	
			//adapter.disable();
		}else
			btmac = "No Bluetooth Device!";
			return btmac;
	}
	//尝试打开wifi
	public static Boolean tryOpenWifi(WifiManager manager) {
		Boolean softopenwifi = false;
		int state = manager.getWifiState();
		if(state!=WifiManager.WIFI_STATE_ENABLED&&state!=WifiManager.WIFI_STATE_ENABLING){
			manager.setWifiEnabled(true);
			softopenwifi = true;
		}
		return softopenwifi;
	}
	//尝试关闭wifi
	public static void tryCloseWifi(WifiManager manager) {
		manager.setWifiEnabled(false);
	}
	@SuppressLint("DefaultLocale")
	public static String tryGetWifiMac(WifiManager manager) {
		WifiInfo info = manager.getConnectionInfo();
		if(info==null||isNull(info.getMacAddress())){
			return null;
		}
		String mac = info.getMacAddress().replaceAll(":", "").trim().toUpperCase();
		mac = formatIdentify(mac);
		return mac;
	}
	
	//尝试读取MAC地址
	public static String getMacFromDevice(Context context,int internal)
	{
	String mac=null;	
	WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
	mac = tryGetWifiMac(wifiManager);
	if(!isNull(mac))
	{
	return mac;
	}

	//获取失败，尝试打开wifi获取
	boolean isOkWifi = tryOpenWifi(wifiManager);
	for(int index=0;index<internal;index++)
	{
	//如果第一次没有成功，第二次做100毫秒的延迟。
	if(index!=0)
	{
	try
	{
	Thread.sleep(100);
	}
	catch (InterruptedException e)
	{
	e.printStackTrace();
	}
	}
	mac = tryGetWifiMac(wifiManager);
	if(!isNull(mac))
	{
	break;
	}
	}

	//尝试关闭wifi
	if(isOkWifi)
	{
	//tryCloseWifi(wifiManager);
	}
	return mac;
	}
	 //格式化唯一标识
	 private static String formatIdentify(String identify)
	 {
	  //判空
	  if(isNull(identify))
	  {
	   return identify;
	  }
	  //去除空格
	  identify = identify.trim();
	  //求长度
	  int len = identify.length();
	  //正好
	  if(len== IDENTIFY_LEN)
	  {
	   return identify;
	  }
	  //过长，截取
	  if(len>IDENTIFY_LEN)
	  {
	   return identify.substring(0, IDENTIFY_LEN);
	  }
	  //过短，补0
	  for(;len<IDENTIFY_LEN;len++)
	  {
	   identify += "0";
	  }
	  //大于默认
	  return identify; 
	 }
	 public static boolean isNull(Object object){
	        if (null == object) {
	            return true;
	        }
	        if ((object instanceof String)){
	            return "".equals(((String)object).trim());
	        }
	        return false;
	    }
	 @Override
	 public boolean onKeyDown(int keyCode, KeyEvent event) {
	     if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){   
	         if((System.currentTimeMillis()-exitTime) > 2000){  
	             Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();                                
	             exitTime = System.currentTimeMillis();   
	         } else {
	             finish();
	             System.exit(0);
	         }
	         return true;   
	     }
	     return super.onKeyDown(keyCode, event);
	 }

@Override
protected void onResume(){
    super.onResume();
    String wifistr = getMac();
    		//getMacFromDevice(getApplicationContext(), 100);
	String btmacstr =  
			getBTAddr();
	 
	String serialName=getBarcode();
	btaddrtv.setText(btmacstr);
	wifimactv.setText(wifistr);
	snctv.setText(serialName);
}
}
