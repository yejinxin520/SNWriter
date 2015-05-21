package com.yudi.snwriter;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.motorolasolutions.adc.decoder.BarCodeReader;
import com.motorolasolutions.adc.decoder.DecodeUtil;

public class MainActivity extends Activity  {	

	DecodeUtil decodeMethod = new DecodeUtil();

	// BarCodeReader specifics
	private BarCodeReader bcr = null;
	private boolean isLog = true;
	public void log(String str) {
		if (isLog) {
			Log.d("0127", str);
		}
	}

	String sn;
	// ="PE900S_201504290002";
	String wifiMacString;
	// = "00:12:23:34:45:56";
	String btAddrString;
	// = "00:12:23:34:45:67";
	int number = 12;
	WifiManager wifiManager;
	private static EditText wifimac;
	private EditText btaddr;
	private EditText barcode;
	private EditText imei;
	ImageView ib;
	ImageView ib1;
	ImageView ib2;
	ImageView ib3;
	private ProgressDialog progressDialog;

	private BluetoothAdapter adapter;

	Boolean issan = false;
	int foc,state=0;
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler(){
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle bundle = msg.getData();			
			String s = bundle.getString("barc");
			dspData(s);issan = !issan;
			if(foc<3&&s.length()==12||foc<5&&foc>=3)
				foc++;
			if(foc==5)
				foc=1;
			if (foc == 1)
				wifimac.requestFocus();
			if (foc == 2)
				btaddr.requestFocus();
			if (foc == 3)
				barcode.requestFocus();
			if (foc == 4)
				imei.requestFocus();
		};
	};
	Animation shake;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		adapter = BluetoothAdapter.getDefaultAdapter();
		// tv = (TextView) this.findViewById(R.id.tv);
		// tv.setMovementMethod(new ScrollingMovementMethod());

		// tv.setText(str);
		wifimac = (EditText) this.findViewById(R.id.wifimac);
		btaddr = (EditText) this.findViewById(R.id.btaddr);
		barcode = (EditText) this.findViewById(R.id.barcode);
		imei = (EditText) this.findViewById(R.id.imei);
		ib = (ImageView) findViewById(R.id.imgcancle);
		ib1 = (ImageView) findViewById(R.id.imgcancle1);
		ib2 = (ImageView) findViewById(R.id.imgcancle2);
		ib3 = (ImageView) findViewById(R.id.imgcancle3);
		editTextInit(wifimac);
		editTextInit(btaddr);
		editTextInit(barcode);
		editTextInit(imei);
		// this.write(sn, wifiMacString, btAddrString);
		shake = AnimationUtils.loadAnimation(this, R.anim.shake);
		new ToneGenerator(AudioManager.STREAM_MUSIC,
				ToneGenerator.MAX_VOLUME);		
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		System.out.println(event.getKeyCode());
		if (((keyCode == 135) || (keyCode == 136) )
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			doDecode((RelativeLayout)findViewById(R.id.confirm));				
			return true;
		}
		if(keyCode == 137&& event.getAction() == KeyEvent.ACTION_DOWN){
			RelativeLayout changerl = (RelativeLayout)findViewById(R.id.change);
			confirm(changerl);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void confirm(View view) {
		int isok = 1;
		wifiMacString = wifimac.getText().toString().trim();
		btAddrString = btaddr.getText().toString().trim();
		sn = barcode.getText().toString();
		if (sn.length() == 0) {
			barcode.setHint(R.string.input);
			barcode.setHintTextColor(Color.RED);
			barcode.startAnimation(shake);
			barcode.requestFocus();
		}
		if (btaddr.length() > 0) {
			if(isBTLawful(btAddrString))
			    btAddrString = formatString(btAddrString).trim();
			else
				btAddrString ="";
			if (btAddrString.length() == 0) {
				btaddr.setText("");
				btaddr.setHint(R.string.missmatch);
				isok = 0;
				btaddr.setHintTextColor(Color.RED);
				btaddr.startAnimation(shake);
			}
		} else {
			btaddr.setHint(R.string.input);
			btaddr.setHintTextColor(Color.RED);
			btaddr.startAnimation(shake);
			btaddr.requestFocus();
		}
		if (wifimac.length() > 0) {
			if(isWifiLawful(wifiMacString))
			    wifiMacString = formatString(wifiMacString).trim();
			else
				wifiMacString = "";
			if (wifiMacString.length() == 0) {
				wifimac.setText("");
				wifimac.setHint(R.string.missmatch);
				isok = 0;
				wifimac.setHintTextColor(Color.RED);
				wifimac.startAnimation(shake);
			}
		} else {
			wifimac.setHint(R.string.input);
			wifimac.setHintTextColor(Color.RED);
			wifimac.startAnimation(shake);
			wifimac.requestFocus();
		}

		if (isok == 1)
			if (wifiMacString.length() == 17 || btAddrString.length() == 17
					|| sn.length() > 0) {				
				SNWriteMethod.write(sn, wifiMacString, btAddrString);

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
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
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

	private Boolean isWifiLawful(String s) {
		Boolean mat = false;
		String regex = "^[a-f0-9A-F]+$";
		String mut = "02468aceACE";
		if (s.equals("000000000000") || s.equals("111111111111"))
			return false;
		if (s.matches(regex)) {			
			if (mut.indexOf(s.substring(1, 2)) != -1) {
				mat = true;
			} else {
				mat = false;
			}
		} else {
			mat = false;
		}
		return mat;
	}
	private Boolean isBTLawful(String s) {
		Boolean mat = false;
		String regex = "^[a-f0-9A-F]+$";
		if (s.matches(regex)) {			
			mat = true;
		} else {
			mat = false;
		}
		return mat;
	}

	private String formatString(String string) {
		String restr = "";		
		if (string.length() == 12) {
			String[] str = new String[6];
			str[0] = string.substring(0, 2);
			str[1] = string.substring(2, 4);
			str[2] = string.substring(4, 6);
			str[3] = string.substring(6, 8);
			str[4] = string.substring(8, 10);
			str[5] = string.substring(10, 12);

			for (int i = 0; i < 5; i++) {
				restr += str[i] + ":";
			}
			restr += str[5];
			return restr;
		}
		return restr;
	}

	private void editTextInit(final EditText editText) {

		editText.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View arg0, boolean arg1) {
				// TODO Auto-generated method stub
				if (arg1) {
					if (editText == wifimac) {
						foc = 1;
					}
					if (editText == btaddr) {
						foc = 2;
					}
					if (editText == barcode) {
						foc = 3;
					}
					if (editText == imei) {
						foc = 4;
					}
				} else
					foc = 0;
			}
		});
		TextWatcher textWatcher = new TextWatcher() {
			CharSequence temp;
			int selectionStart;
			int selectionEnd;

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				// TODO Auto-generated method stub
				temp = arg0;
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				// int num = number-arg0.length();
				if (editText == wifimac||editText == btaddr) {
					selectionStart = editText.getSelectionStart();
					selectionEnd = editText.getSelectionEnd();
					if (temp.length() > number) {
						arg0.delete(selectionStart - 1, selectionEnd);
						int tempselection = selectionStart;
						editText.setText(arg0);
						editText.setSelection(tempselection);
					}
				}

				if (temp.length() == 0) {
					editText.requestFocus();
					editText.setHint(R.string.input);
					editText.setHintTextColor(Color.GRAY);
					if (editText == imei) {
						hideBtn(ib3);
						foc = 4;
					}
					if (editText == barcode) {
						hideBtn(ib2);
						foc = 3;
					}
					if (editText == btaddr) {
						hideBtn(ib1);
						foc = 2;
					}
					if (editText == wifimac) {
						hideBtn(ib);
						foc = 1;
					}
				} else {
					if (editText == wifimac) {
						showBtn(ib);
					}
					if (editText == btaddr) {
						showBtn(ib1);
					}
					if (editText == barcode) {
						showBtn(ib2);
					}
					if (editText == imei) {
						showBtn(ib3);
					}
				}

			}
		};
		editText.addTextChangedListener(textWatcher);
	}

	public void doClear(View v) {
		hideBtn(v);
		if (v.equals(ib))
			wifimac.setText("");
		if (v.equals(ib1))
			btaddr.setText("");
		if (v.equals(ib2))
			barcode.setText("");
		if (v.equals(ib3))
			imei.setText("");
	}

	public void hideBtn(View v) {
		if (v.isShown())
			v.setVisibility(View.GONE);
	}

	public void showBtn(View v) {
		if (!v.isShown())
			v.setVisibility(View.VISIBLE);
	}

	public void doBack(View v) {
		MainActivity.this.finish();
	}

	protected void dialog() {
		AlertDialog.Builder builder = new Builder(MainActivity.this);
		builder.setMessage(R.string.message);
		builder.setTitle(R.string.message_title);
		builder.setPositiveButton(R.string.yes, new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();

				MainActivity.this.finish();
			}
		});
		builder.setNegativeButton(R.string.no, new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void doDecode(View v) {
		if (foc == 1)
			wifimac.requestFocus();
		if (foc == 2)
			btaddr.requestFocus();
		if (foc == 3)
			barcode.requestFocus();
		if (foc == 4)
			imei.requestFocus();
		dspData("");
		if(state==0){
			dspStat(R.string.decoding);
			state = 1;
		}
		else if(state==1){
			dspStat(R.string.cancel);
			state = 0;
		}
		decodeMethod.doDecode();
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					while(decodeMethod.getData().length()==0){
						Thread.sleep(500);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String s = decodeMethod.getData().trim();
				Message msg = new Message();  
	            Bundle bundle = new Bundle();  
	            bundle.putString("barc", s);  
	            msg.setData(bundle);  
				MainActivity.this.handler.sendMessage(msg);

			}
		});
		if(!issan){
			t.start();
			issan = !issan;
		}			
		else{
			handler.removeCallbacks(t);
			
		}
	}

	private void dspStat(int id) {
		// TODO Auto-generated method stub
		if (foc == 1)
			wifimac.setHint(id);
		if (foc == 2)
			btaddr.setHint(id);
		if (foc == 3)
			barcode.setHint(id);
		if (foc == 4)
			imei.setHint(id);
	}

	private void dspData(String string) {
		// TODO Auto-generated method stub
		if (foc == 1 || foc == 2)
			if (string.trim().length() > 0 && string.trim().length() != number) {
				string = "";
				AlertDialog.Builder builder = new Builder(MainActivity.this);
				builder.setMessage(R.string.message1);
				builder.setTitle(R.string.message_title1);
				builder.setPositiveButton(R.string.yes, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.create().show();
			}
		
			if (foc == 1) {
				wifimac.setText(string.trim());
			}
			if (foc == 2)
				btaddr.setText(string.trim());
			if (foc == 3)
				barcode.setText(string.trim());
			if (foc == 4)
				imei.setText(string.trim());
				
	}

	

	private void dspErr(String s) {
		// TODO Auto-generated method stub
		log("ERROR" + s);
	}

	

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (bcr != null) {			
			bcr.release();
			bcr = null;
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		

		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					
					if (android.os.Build.VERSION.SDK_INT >= 18)
						bcr = BarCodeReader.open(getApplicationContext()); // Android
																			// 4.3 and
																			// above
					else
						bcr = BarCodeReader.open(1); // Android 2.3

					decodeMethod.decodeinit(bcr);
					if (bcr == null) {
						dspErr("open failed");
						return;
					}
					
				} catch (Exception e) {
					dspErr("open excp:" + e);
					System.out.println("open excp:" + e);
				}
			}
		});
		t.start();
		
	}

}
