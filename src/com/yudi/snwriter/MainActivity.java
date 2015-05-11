package com.yudi.snwriter;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.Window;
import android.widget.EditText;
import java.util.StringTokenizer;

import com.motorolasolutions.adc.decoder.BarCodeReader;

public class MainActivity extends Activity implements
    BarCodeReader.DecodeCallback{
	public static final String TAG = "SNWriter";

	//TextView tv;
	//String str = "null";
	//private String string;

	//states
	static final int STATE_IDLE 		= 0;
	static final int STATE_DECODE 		= 1;
	private ToneGenerator tg 		= null;
	// BarCodeReader specifics
	private BarCodeReader bcr 		= null;
	private int state				= STATE_IDLE;
	private int decodes 			= 0;
	private String decodeDataString;
	private String decodeStatString;

	private boolean isLog =true;
	private static int decCount = 0;
	static
	{
		System.loadLibrary("IAL");
		System.loadLibrary("SDL");
		
		if(android.os.Build.VERSION.SDK_INT >= 19)
			System.loadLibrary("barcodereader44"); // Android 4.4
		else
			if(android.os.Build.VERSION.SDK_INT >= 18)
				System.loadLibrary("barcodereader43"); // Android 4.3
			else
				System.loadLibrary("barcodereader");   // Android 2.3 - Android 4.2
	}
	public void log(String str){
		if (isLog ) {
			Log.d("0127", str);
		}
	}
	private static final int MAC_BARCODE_DIGITS = 64;
	private static final int MAC_ADDRESS_DIGITS = 6;

	String sn;
			//="PE900S_201504290002";
	String wifiMacString; 
	//= "00:12:23:34:45:56";
	String btAddrString;
	//= "00:12:23:34:45:67";
	int number = 12; 
	WifiManager wifiManager;
	private EditText wifimac;
	private EditText btaddr;
	private EditText barcode;
	private EditText imei;
	private ProgressDialog progressDialog;

	private BluetoothAdapter adapter;

	private int trigMode = BarCodeReader.ParamVal.LEVEL;

	private int modechgEvents = 0;

	private int motionEvents = 0;
	
	int foc;
	private static Handler handler=new Handler();
	Animation shake;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		adapter = BluetoothAdapter.getDefaultAdapter();
		//tv = (TextView) this.findViewById(R.id.tv);
		//tv.setMovementMethod(new ScrollingMovementMethod());
	
		//tv.setText(str);
		wifimac = (EditText)this.findViewById(R.id.wifimac);
		btaddr = (EditText)this.findViewById(R.id.btaddr);
		barcode = (EditText)this.findViewById(R.id.barcode);	
		imei = (EditText)this.findViewById(R.id.imei);
		init(wifimac);
		init(btaddr);
		init(barcode);
		init(imei);
		//this.write(sn, wifiMacString, btAddrString);
		shake = AnimationUtils.loadAnimation(this,R.anim.shake);
		tg = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		System.out.println(event.getKeyCode());
	     if(((keyCode == 135)||(keyCode == 136)||(keyCode == 137)) && event.getAction() == KeyEvent.ACTION_DOWN){   
	    	 if(foc==1)
	 		    wifimac.requestFocus();
	 		if(foc==2)
	 			btaddr.requestFocus();
	 		if(foc==3)
	 			barcode.requestFocus();	 
	 		if(foc==4)
	 			imei.requestFocus();
	 		
	 		state = STATE_DECODE;
	 		decCount = 0;
	 		decodeDataString = new String("");
	 		decodeStatString = new String("");
	 		dspData("");
	 		dspStat(R.string.decoding);
	 		bcr.startDecode();
	         return true;   
	     }
	     return super.onKeyDown(keyCode, event);
	 }
	public void confirm(View view) {
		int isok = 1;
		wifiMacString = wifimac.getText().toString().trim();
		btAddrString = btaddr.getText().toString().trim();
		sn = barcode.getText().toString();
        if(wifimac.length()>0){
		    wifiMacString = formatString(wifiMacString).trim();
		    if(wifiMacString.length()==0){
				wifimac.setText("");
				wifimac.setHint("missmatch!");
				isok = 0;
				wifimac.setHintTextColor(Color.RED);
				wifimac.startAnimation(shake);
			}
        }
        else{
        	wifimac.setHint("please input!");
        	wifimac.setHintTextColor(Color.RED);
			wifimac.startAnimation(shake);
        }
		if(btaddr.length()>0){
		    btAddrString = formatString(btAddrString).trim();
		    if(btAddrString.length()==0){
				btaddr.setText("");
				btaddr.setHint("missmatch!");
				isok = 0;
				btaddr.setHintTextColor(Color.RED);
				btaddr.startAnimation(shake);
			}
		}
		else{
        	btaddr.setHint("please input!");
        	btaddr.setHintTextColor(Color.RED);
        	btaddr.startAnimation(shake);
        }
		
		if(sn.length()==0){
			barcode.setHint("please input!");
			barcode.setHintTextColor(Color.RED);
        	barcode.startAnimation(shake);
		}
		if (isok == 1)
			if (wifiMacString.length() == 17 || btAddrString.length() == 17
					|| sn.length() > 0) {
				write(sn, wifiMacString, btAddrString);

				progressDialog = ProgressDialog.show(MainActivity.this, "",
						"loading...", true);
				Thread t = new Thread(new Runnable() {
					public void run() {
						try {
							if (wifiManager.isWifiEnabled()) {
								wifiManager.setWifiEnabled(false);
							}
							if (adapter.isEnabled()) {
								adapter.disable();
							}
							Thread.sleep(1000);
							progressDialog.dismiss();

						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						handler.post(new Runnable() {
							@Override
							public void run() {
								dialog();
								if (!wifiManager.isWifiEnabled()) {
									wifiManager.setWifiEnabled(true);
								}
								if (!adapter.isEnabled()) {
									adapter.enable();
								}
							}
						});
					}
				});
				t.start();
			}

	}
	public Boolean ismatch(String s) {
		Boolean mat = false;
		String regex = "^[a-f0-9A-F]+$";
		String mut ="02468aceACE";
		if(s.matches(regex)){
			if(foc==1)
			if(mut.indexOf(s.substring(1, 2))!=-1){
				if(s.equals("000000000000")||s.equals("111111111111"))
					mat = false;
				else
					mat = true;
			}
			
				mat = true;
		}
		
		return mat;
	}
	public String formatString(String string) {
		String restr ="";
		if(ismatch(string))
		if(string.length()==12){
			String[]str = new String[6];
			str[0] = string.substring(0, 2);
			str[1] = string.substring(2, 4);
			str[2] = string.substring(4, 6);
			str[3] = string.substring(6, 8);
			str[4] = string.substring(8, 10);
			str[5] = string.substring(10, 12);
			
			for(int i=0;i<5;i++){
				restr += str[i]+":";
			}
			restr += str[5];		
		return restr;
		}
		return restr;
	}
	public void init(final EditText editText) {
		editText.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View arg0, boolean arg1) {
				// TODO Auto-generated method stub
				if(arg1){
					if(editText==wifimac){
						foc=1;
					}
					if(editText==btaddr){
						foc=2;
					}
					if(editText==barcode){
						foc=3;
					}
					if(editText==imei){
						foc=4;
					}
				}
				else
					foc=0;
			}
		});
		TextWatcher textWatcher = new TextWatcher() {
			CharSequence temp;
			int selectionStart;
			int selectionEnd;
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				temp = arg0;
			}
			
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				//int num = number-arg0.length();
				if(editText!=barcode){
					selectionStart = editText.getSelectionStart();
					selectionEnd = editText.getSelectionEnd();
					if(temp.length()>number){
						arg0.delete(selectionStart-1, selectionEnd);
						int tempselection = selectionStart;
						editText.setText(arg0);
						editText.setSelection(tempselection);
					}
				}
				
				if(temp.length()==0){
					editText.setHint("please input!");
					editText.setHintTextColor(Color.GRAY);
					if(editText==wifimac)
						foc=1;
					if(editText==btaddr)
						foc=2;
					if(editText==barcode)
						foc=3;
					if(editText==imei)
						foc=4;
				}
				
			}
		};		
		editText.addTextChangedListener(textWatcher);
	}
	protected void dialog() {
		  AlertDialog.Builder builder = new Builder(MainActivity.this);
		  builder.setMessage("Update successfully,go back to check£¿"); 
		  builder.setTitle("prompt message");  
		  builder.setPositiveButton("YES", new OnClickListener() {  
			  public void onClick(DialogInterface dialog, int which) {
		    dialog.dismiss(); 
		    
		    MainActivity.this.finish();
		   }
		  }); 
		  builder.setNegativeButton("NO", new OnClickListener() { 
			  public void onClick(DialogInterface dialog, int which) {
		    dialog.dismiss();
		   }
		  }); 
		  builder.create().show();
		}

	public void write(String barcode, String wifiMac, String btAddr) {
		//int flag = -1;
		if (wifiMac != null&&wifiMac.length()>0){
			updateWifiMacAddr(wifiMac);
		}

		if (btAddr != null&&btAddr.length()>0)
			updateBtAddr(btAddr);
		if(barcode !=null&&barcode.length()>0)
			updateBarcode(barcode);

		/*
		
		byte[] sn_b = barcode.getBytes();
		short[] wifiMac_b = macString2ByteArray(wifiMac);
		short[] btAddr_b = macString2ByteArray(btAddr);
		System.out.println(sn_b.length);
		try {
			NvRAMAgent agent = NvRAMAgentHelper.getAgent();

			byte[] buff = agent.readFile(NvRAMAgentHelper.PRODUCT_INFO_ID);

			dumpHex(buff);

			int i, j;
			i = 0;
			// sn
			if(sn_b !=null&&sn_b.length>0){
				for (j = 0; i < MAC_BARCODE_DIGITS; i++) {
					for (; j < (sn_b.length > MAC_BARCODE_DIGITS ? MAC_BARCODE_DIGITS: sn_b.length); j++, i++)
						buff[i] = sn_b[j];

					buff[i] = 0;
				}
				
			}
            i=64;
			// imei
			buff[i++] = (byte) 0x99;
			buff[i++] = (byte) 0x99;
			buff[i++] = (byte) 0x99;
			buff[i++] = (byte) 0x99;

			buff[i++] = (byte) 0x99;
			buff[i++] = (byte) 0x99;
			buff[i++] = (byte) 0x99;
			buff[i++] = (byte) 0xf4;
			for (j = 0; j < 32; j++) {
				buff[i++] = 0;
			}

			// bt
			if (btAddr_b != null) {
				for (j = 0; j < MAC_ADDRESS_DIGITS; j++)
					buff[i++] = (byte)btAddr_b[j];
			}

			// wifi
			if (wifiMac_b != null) {
				for (j = 0; j < MAC_ADDRESS_DIGITS; j++)
					buff[i++] = (byte)wifiMac_b[j];
			}

			dumpHex(buff);

			flag = agent.writeFile(NvRAMAgentHelper.PRODUCT_INFO_ID, buff);

			if (flag > 0) {
				Log.d(TAG, "Update successfully.\r\nPlease reboot this device");
			} else {
				Log.d(TAG, "Update failed");
			}

		} catch (Exception e) {
			Log.d(TAG, e.getMessage() + ":" + e.getCause());
			e.printStackTrace();
		}*/
		
	}

	private int updateBarcode(String sn) {
		int flag = -1;
		byte[] sn_b = sn.getBytes();

		try {
			NvRAMAgent agent = NvRAMAgentHelper.getAgent();

			byte[] buff = agent.readFile(NvRAMAgentHelper.PRODUCT_INFO_ID);

			dumpHex(buff);

			int i, j;
			// sn
			for (i = 0, j = 0; i < 116; i++) {
				for (; j < (sn_b.length > MAC_BARCODE_DIGITS ? MAC_BARCODE_DIGITS: sn_b.length); j++, i++)
					buff[i] = sn_b[j];

				buff[i] = 0;
			}

			dumpHex(buff);
			flag = agent.writeFile(NvRAMAgentHelper.PRODUCT_INFO_ID, buff);

			if (flag > 0) {
				Log.d(TAG, "Update sn successfully.\r\nPlease reboot this device");
			} else {
				Log.d(TAG, "Update failed");
			}

		} catch (Exception e) {
			Log.d(TAG, e.getMessage() + ":" + e.getCause());
			e.printStackTrace();
		}
		
		return flag;
	}

	private int updateWifiMacAddr(String wifiMac) {
		int flag = -1;
		short[] wifiMacAddr;

		wifiMacAddr = macString2ByteArray(wifiMac);
		if (wifiMacAddr == null){
			return -1;
		}

		try {
			NvRAMAgent agent = NvRAMAgentHelper.getAgent();
			byte[] buff = agent.readFile(NvRAMAgentHelper.WIFI_MAC_ADDR_ID);

			dumpHex(buff);

			for (int i = 0; i < MAC_ADDRESS_DIGITS; i++) {
				buff[i + 4] = (byte) wifiMacAddr[i];
			}
			dumpHex(buff);
			flag = agent.writeFile(NvRAMAgentHelper.WIFI_MAC_ADDR_ID, buff);
			if (flag > 0) {
				Log.d(TAG, "Update wifimac successfully.\r\nPlease reboot this device");
			} else {
				Log.d(TAG, "Update failed");
			}

		} catch (Exception e) {
			Log.d(TAG, e.getMessage() + ":" + e.getCause());
			e.printStackTrace();
		}

		return flag;
	}

	private int updateBtAddr(String btMac) {
		int flag = -1;
		short[] btAddr;

		btAddr = macString2ByteArray(btMac);
		if (btAddr == null)
			return -1;

		try {
			NvRAMAgent agent = NvRAMAgentHelper.getAgent();
			byte[] buff = agent.readFile(NvRAMAgentHelper.BT_ADDR_ID);

			dumpHex(buff);

			for (int i = 0; i < MAC_ADDRESS_DIGITS; i++) {
				buff[i] = (byte) btAddr[i];
			}
			dumpHex(buff);
			flag = agent.writeFile(NvRAMAgentHelper.BT_ADDR_ID, buff);
			if (flag > 0) {
				Log.d(TAG, "Update btaddr successfully.\r\nPlease reboot this device");
			} else {
				Log.d(TAG, "Update failed");
			}

		} catch (Exception e) {
			Log.d(TAG, e.getMessage() + ":" + e.getCause());
			e.printStackTrace();
		}

		return flag;
	}

	/*private int updateImei(String imei) {
		int flag = -1;

		String ImeiString = "AT+EGMR=1,7,\"" + imei + "\"";
		Phone phone = PhoneFactory.getDefaultPhone();
		phone.invokeOemRilRequestStrings(ImeiString, new WriteImeiHandler().obtainMessage(EVENT_WRITE_IMEI));

		return flag;
	}*/

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

	private short [] macString2ByteArray(String macString) {
		int i = 0;
		short[] macAddr = new short[6];

		if (macString == null)
			return null;

		// parse mac address firstly
		StringTokenizer txtBuffer = new StringTokenizer(macString, ":");
		while (txtBuffer.hasMoreTokens() && i < 6) {
			macAddr[i] = (short) Integer.parseInt(
					txtBuffer.nextToken(), 16);
			System.out.println(i + ":" + macAddr[i]);
			i++;
		}
		if (i != 6) {
			Log.d(TAG, "The format of mac address is not correct");
			
			return null;
		}

		return macAddr;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	
	public void doDecode(View v) {
		//doSetParam(905, 1);
		if(foc==1)
		    wifimac.requestFocus();
		if(foc==2)
			btaddr.requestFocus();
		if(foc==3)
			barcode.requestFocus();
		if(foc==4)
 			imei.requestFocus();
		if (setIdle() != STATE_IDLE)
			return;
		state = STATE_DECODE;
		decCount = 0;
		decodeDataString = new String("");
		decodeStatString = new String("");
		dspData("");
		dspStat(R.string.decoding);
		bcr.startDecode(); // start decode (callback gets results)
	}

	private void dspStat(int id) {
		// TODO Auto-generated method stub
		if(foc==1)
		    wifimac.setHint(id);
		if(foc==2)
			btaddr.setHint(id);
		if(foc==3)
			barcode.setHint(id);
		if(foc==4)
			imei.setHint(id);
	}

	private void dspData(String string) {
		// TODO Auto-generated method stub
		if(foc==1){
			wifimac.setText(string.trim());
		}		    
		if(foc==2)
			btaddr.setText(string.trim());
		if(foc==3)
			barcode.setText(string.trim());
		if(foc==4)
			imei.setText(string.trim());
	}

	private int setIdle() {
		// TODO Auto-generated method stub
		int prevState = state;
		int ret = prevState;		//for states taking time to chg/end
		
		state = STATE_IDLE;
		switch (prevState)
		{		
			//fall thru
		case STATE_DECODE:
			dspStat("decode stopped");
			bcr.stopDecode();
			break;			
		default:
			ret = STATE_IDLE;			
		}
		return ret;
	}

	private void dspStat(String string) {
		// TODO Auto-generated method stub
		if(foc==1)
		    wifimac.setHint(string);
		if(foc==2)
			btaddr.setHint(string);
		if(foc==3)
			barcode.setHint(string);
		if(foc==4)
			imei.setHint(string);
	}

	private void dspErr(String s) {
		// TODO Auto-generated method stub
		log("ERROR" + s);
	}

	@Override
	public void onDecodeComplete(int symbology, int length, byte[] data,
			BarCodeReader reader) {
		// TODO Auto-generated method stub
		log("into onDecodeComplete");
		if (state == STATE_DECODE)
			state = STATE_IDLE;
		
		// Get the decode count
		if(length == BarCodeReader.DECODE_STATUS_MULTI_DEC_COUNT)
			decCount = symbology;
		
		if (length > 0)
		{
			log("length > 0");
			if (isHandsFree()==false && isAutoAim()==false)
				bcr.stopDecode();

			++decodes;
			log("symbology:"+symbology);
			if (symbology == 0x69)	// signature capture
			{
				log("into symbology");
				
				decodeStatString += new String("[" + decodes + "] type: " + symbology + " len: " + length);
				decodeDataString += new String(data);  
				
			}
			else
			{ 	
				

			if (symbology == 0x99)	//type 99?
			{
				symbology = data[0];
				int n = data[1];
				int s = 2;
				int d = 0;
				int len = 0;
				byte d99[] = new byte[data.length];					
				for (int i=0; i<n; ++i)
				{
					s += 2;
					len = data[s++];
					System.arraycopy(data, s, d99, d, len);
					s += len;
					d += len;
				}
				d99[d] = 0;
				data = d99;
			}
			decodeStatString += new String("[" + decodes + "] type: " + symbology + " len: " + length);
			decodeDataString += new String(data);
			dspStat(decodeStatString);
			dspData(decodeDataString);
			log("=======");
			
			log("======= end");
			
			
			if(decCount > 1) // Add the next line only if multiple decode
			{
				decodeStatString += new String(" ; ");
				decodeDataString += new String(" ; ");
			}
			else
			{
				decodeDataString = new String("");
				decodeStatString = new String("");
			}
			}
			if(foc<5)
			    foc++;
			if(foc==5)
				foc=1;
			if (tg != null)
				tg.startTone(ToneGenerator.TONE_CDMA_NETWORK_CALLWAITING);
		}
      else	// no-decode
      {
      	dspData("");
      	switch (length)
      	{
      	case BarCodeReader.DECODE_STATUS_TIMEOUT:
      		dspStat("decode timed out");
      		break;
      		
      	case BarCodeReader.DECODE_STATUS_CANCELED:
      		dspStat("decode cancelled");
      		break;
      		
     		case BarCodeReader.DECODE_STATUS_ERROR:
      	default:
      		dspStat("decode failed");      		
      		break;
      	}
      }
	}

	private boolean isHandsFree() {
		// TODO Auto-generated method stub
		return (trigMode == BarCodeReader.ParamVal.HANDSFREE);
	}

	private boolean isAutoAim() {
		// TODO Auto-generated method stub
		return (trigMode == BarCodeReader.ParamVal.AUTO_AIM);
	}

	@Override
	public void onEvent(int event, int info, byte[] data, BarCodeReader reader) {
		// TODO Auto-generated method stub
		switch (event)
		{
		case BarCodeReader.BCRDR_EVENT_SCAN_MODE_CHANGED:
			++modechgEvents;
			dspStat("Scan Mode Changed Event (#" + modechgEvents + ")");
			break;

		case BarCodeReader.BCRDR_EVENT_MOTION_DETECTED:
			++motionEvents;
			dspStat("Motion Detect Event (#" + motionEvents + ")");
			break;

		default:
			// process any other events here
			break;
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (bcr != null)
		{
			setIdle();			
			bcr.release();
			bcr = null;
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		state = STATE_IDLE;

		try
		{
			dspStat(getResources().getString(R.string.app_name) + " v" + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
				Log.d("fff", "version:"+android.os.Build.VERSION.SDK_INT);
			if(android.os.Build.VERSION.SDK_INT >= 18)
				bcr = BarCodeReader.open(getApplicationContext()); // Android 4.3 and above
			else
				bcr = BarCodeReader.open(1); // Android 2.3
			
			if (bcr == null)
			{
				dspErr("open failed");
				return;
			}
			bcr.setDecodeCallback(this);
		}
		catch (Exception e)
		{
			dspErr("open excp:" + e);
			System.out.println("open excp:" + e);
		}
	}

}
