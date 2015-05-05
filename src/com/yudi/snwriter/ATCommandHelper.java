package com.yudi.snwriter;

import java.lang.reflect.Method;

import android.os.IBinder;

public class ATCommandHelper {

	private int updateImei(String imei) {
		int flag = -1;

		String ImeiString = "AT+EGMR=1,7,\"" + imei + "\"";
		//Phone phone = PhoneFactory.getDefaultPhone();
		//phone.invokeOemRilRequestStrings(ImeiString, new WriteImeiHandler().obtainMessage(EVENT_WRITE_IMEI));

		return flag;
	}
}
