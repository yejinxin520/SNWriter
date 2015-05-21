package com.yudi.snwriter;

import java.util.StringTokenizer;

import android.util.Log;

public class SNWriteMethod {

	public static final String TAG = "SNWriter";
	private static final int MAC_BARCODE_DIGITS = 64;
	private static final int MAC_ADDRESS_DIGITS = 6;
	public static void write(String barcode, String wifiMac, String btAddr) {
		int flag = -1;
		if (wifiMac != null && wifiMac.length() > 0) {
			updateWifiMacAddr(wifiMac);
		}

		if (btAddr != null && btAddr.length() > 0)
			updateBtAddr(btAddr);
		//if (barcode != null && barcode.length() > 0)
		//	updateBarcode(barcode);

		 
		  byte[] sn_b = barcode.getBytes(); short[] wifiMac_b =
		  macString2ByteArray(wifiMac); short[] btAddr_b =
		  macString2ByteArray(btAddr); System.out.println(sn_b.length); try {
		  NvRAMAgent agent = NvRAMAgentHelper.getAgent();
		  
		  byte[] buff = agent.readFile(NvRAMAgentHelper.PRODUCT_INFO_ID);
		  
		  dumpHex(buff);
		  
		  int i, j; i = 0; 
		  // sn 
		  if(sn_b !=null&&sn_b.length>0){ for (j = 0; i
		  < MAC_BARCODE_DIGITS; i++) { for (; j < (sn_b.length >
		  MAC_BARCODE_DIGITS ? MAC_BARCODE_DIGITS: sn_b.length); j++, i++)
		  buff[i] = sn_b[j];
		  
		  buff[i] = 0; }
		  
		  } i=64; 
		  // imei 
		  buff[i++] = (byte) 0x99; buff[i++] = (byte) 0x99;
		  buff[i++] = (byte) 0x99; buff[i++] = (byte) 0x99;
		  
		  buff[i++] = (byte) 0x99; buff[i++] = (byte) 0x99; buff[i++] = (byte)
		  0x99; buff[i++] = (byte) 0xf4; for (j = 0; j < 32; j++) { buff[i++] =
		  0; }
		  
		  // bt 
		  if (btAddr_b != null) { for (j = 0; j < MAC_ADDRESS_DIGITS;
		  j++) buff[i++] = (byte)btAddr_b[j]; }
		  
		  // wifi 
		  if (wifiMac_b != null) { for (j = 0; j < MAC_ADDRESS_DIGITS;
		  j++) buff[i++] = (byte)wifiMac_b[j]; }
		  
		  dumpHex(buff);
		  
		  flag = agent.writeFile(NvRAMAgentHelper.PRODUCT_INFO_ID, buff);
		  
		  if (flag > 0) { Log.d(TAG,
		  "Update successfully.\r\nPlease reboot this device"); } else {
		  Log.d(TAG, "Update failed"); }
		  
		  } catch (Exception e) { Log.d(TAG, e.getMessage() + ":" +
		  e.getCause()); e.printStackTrace(); }
		 

	}

	private static int updateBarcode(String sn) {
		int flag = -1;
		byte[] sn_b = sn.getBytes();

		try {
			NvRAMAgent agent = NvRAMAgentHelper.getAgent();

			byte[] buff = agent.readFile(NvRAMAgentHelper.PRODUCT_INFO_ID);

			dumpHex(buff);

			int i, j;
			// sn
			for (i = 0, j = 0; i < 116; i++) {
				for (; j < (sn_b.length > MAC_BARCODE_DIGITS ? MAC_BARCODE_DIGITS
						: sn_b.length); j++, i++)
					buff[i] = sn_b[j];

				buff[i] = 0;
			}

			dumpHex(buff);
			flag = agent.writeFile(NvRAMAgentHelper.PRODUCT_INFO_ID, buff);

			if (flag > 0) {
				Log.d(TAG,
						"Update sn successfully.\r\nPlease reboot this device");
			} else {
				Log.d(TAG, "Update failed");
			}

		} catch (Exception e) {
			Log.d(TAG, e.getMessage() + ":" + e.getCause());
			e.printStackTrace();
		}

		return flag;
	}

	private static int updateWifiMacAddr(String wifiMac) {
		int flag = -1;
		short[] wifiMacAddr;

		wifiMacAddr = macString2ByteArray(wifiMac);
		if (wifiMacAddr == null) {
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
				Log.d(TAG,
						"Update wifimac successfully.\r\nPlease reboot this device");
			} else {
				Log.d(TAG, "Update failed");
			}

		} catch (Exception e) {
			Log.d(TAG, e.getMessage() + ":" + e.getCause());
			e.printStackTrace();
		}

		return flag;
	}

	private static int updateBtAddr(String btMac) {
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
				Log.d(TAG,
						"Update btaddr successfully.\r\nPlease reboot this device");
			} else {
				Log.d(TAG, "Update failed");
			}

		} catch (Exception e) {
			Log.d(TAG, e.getMessage() + ":" + e.getCause());
			e.printStackTrace();
		}

		return flag;
	}

	/*
	 * private int updateImei(String imei) { int flag = -1;
	 * 
	 * String ImeiString = "AT+EGMR=1,7,\"" + imei + "\""; Phone phone =
	 * PhoneFactory.getDefaultPhone();
	 * phone.invokeOemRilRequestStrings(ImeiString, new
	 * WriteImeiHandler().obtainMessage(EVENT_WRITE_IMEI));
	 * 
	 * return flag; }
	 */

	private static void dumpHex(byte[] buff) {
		StringBuilder sb = new StringBuilder();
		sb.append("buff.length:" + buff.length + "\n");
		if (buff != null && buff.length > 0) {
			for (int i1 = 0; i1 < buff.length; i1++) {
				sb.append(Integer.toHexString(buff[i1]) + ',');
			}
		}
		Log.i(TAG, sb.toString());
	}

	private static short[] macString2ByteArray(String macString) {
		int i = 0;
		short[] macAddr = new short[6];

		if (macString == null)
			return null;

		// parse mac address firstly
		StringTokenizer txtBuffer = new StringTokenizer(macString, ":");
		while (txtBuffer.hasMoreTokens() && i < 6) {
			macAddr[i] = (short) Integer.parseInt(txtBuffer.nextToken(), 16);
			System.out.println(i + ":" + macAddr[i]);
			i++;
		}
		if (i != 6) {
			Log.d(TAG, "The format of mac address is not correct");

			return null;
		}

		return macAddr;
	}
}
