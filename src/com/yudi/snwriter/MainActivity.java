package com.yudi.snwriter;

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
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import java.util.StringTokenizer;

public class MainActivity extends Activity {
	public static final String TAG = "SNWriter";

	//TextView tv;
	//String str = "null";
	//private String string;

	private static final int MAC_BARCODE_DIGITS = 64;
	private static final int MAC_ADDRESS_DIGITS = 6;
	//private static final int MAX_ADDRESS_VALUE = 0xff;

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
	private ProgressDialog progressDialog;

	private BluetoothAdapter adapter;
	private static Handler handler=new Handler();
	//private RelativeLayout confirm;
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
		init(wifimac);
		init(btaddr);
		//this.write(sn, wifiMacString, btAddrString);
	}
	
	public void confirm(View view) {
		
		wifiMacString = wifimac.getText().toString();
		btAddrString = btaddr.getText().toString();
		sn = barcode.getText().toString();
		
		wifiMacString = formatString(wifiMacString);
		//int i =wifiMacString.length();
		//System.out.println(i);
		//Toast.makeText(this, i, Toast.LENGTH_SHORT).show();
		btAddrString = formatString(btAddrString);
		if(wifiMacString.length()==0){
			wifimac.setText("");
			wifimac.setHint("please input again!");
			wifimac.setHintTextColor(Color.RED);
		}
		if(btAddrString.length()==0){
			btaddr.setText("");
			btaddr.setHint("please input again!");
			btaddr.setHintTextColor(Color.RED);
		}
		if(sn.length()==0){
			barcode.setHint("please input");
		}
		if(wifiMacString.length()==17||btAddrString.length()==17||sn.length()>0){
			write(sn, wifiMacString, btAddrString);
			
				progressDialog = ProgressDialog.show(MainActivity.this, "", "loading...",true);
				Thread t = new Thread(new Runnable(){  
		            public void run(){              	
								try {
									if(wifiManager.isWifiEnabled()){
										wifiManager.setWifiEnabled(false);
									}
									if(adapter.isEnabled()){
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
		                            	if(!wifiManager.isWifiEnabled()){
		                    				wifiManager.setWifiEnabled(true);
		                    			}
		                            	if(!adapter.isEnabled()){
		                            		adapter.enable();
		                            	}
		                            }
		                        });    
		            }               
				});  
		        t.start();
		}
           
	}
	public String formatString(String string) {
		String restr ="";
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
		//TextWatcher textWatcher = null;
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
				selectionStart = editText.getSelectionStart();
				selectionEnd = editText.getSelectionEnd();
				if(temp.length()>number){
					arg0.delete(selectionStart-1, selectionEnd);
					int tempselection = selectionStart;
					editText.setText(arg0);
					editText.setSelection(tempselection);
				}
				if(temp.length()==0){
					editText.setHint("please input");
					editText.setHintTextColor(Color.GRAY);
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

	public int write(String barcode, String wifiMac, String btAddr) {
		int flag = -1;
		if (wifiMac != null){
			flag = updateWifiMacAddr(wifiMac);
		}

		if (btAddr != null)
			flag = updateBtAddr(btAddr);
		if(barcode !=null&&barcode.length()>0)
			flag = updateBarcode(barcode);

		return flag;
		/*int flag = -1;
		byte[] sn_b = barcode.getBytes();
		short[] wifiMac_b = macString2ByteArray(wifiMac);
		short[] btAddr_b = macString2ByteArray(btAddr);

		try {
			NvRAMAgent agent = NvRAMAgentHelper.getAgent();

			byte[] buff = agent.readFile(NvRAMAgentHelper.PRODUCT_INFO_ID);

			dumpHex(buff);

			int i, j;
			i = 0;
			// sn
			for (j = 0; i < MAC_BARCODE_DIGITS; i++) {
				for (; j < (sn_b.length > MAC_BARCODE_DIGITS ? MAC_BARCODE_DIGITS: sn_b.length); j++, i++)
					buff[i] = sn_b[j];

				buff[i] = 0;
			}

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
			for (i = 0, j = 0; i < MAC_BARCODE_DIGITS; i++) {
				for (; j < (sn_b.length > MAC_BARCODE_DIGITS ? MAC_BARCODE_DIGITS: sn_b.length); j++, i++)
					buff[i] = sn_b[j];

				buff[i] = 0;
			}

			flag = agent.writeFile(NvRAMAgentHelper.PRODUCT_INFO_ID, buff);

			if (flag > 0) {
				Log.d(TAG, "Update successfully.\r\nPlease reboot this device");
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

			flag = agent.writeFile(NvRAMAgentHelper.WIFI_MAC_ADDR_ID, buff);
			if (flag > 0) {
				Log.d(TAG, "Update successfully.\r\nPlease reboot this device");
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

			flag = agent.writeFile(NvRAMAgentHelper.BT_ADDR_ID, buff);
			if (flag > 0) {
				Log.d(TAG, "Update successfully.\r\nPlease reboot this device");
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

}
