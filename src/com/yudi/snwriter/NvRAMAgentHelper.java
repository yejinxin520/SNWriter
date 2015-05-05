package com.yudi.snwriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.os.IBinder;

public class NvRAMAgentHelper {
	public static final int PRODUCT_INFO_ID = 35;
	public static final int BT_ADDR_ID = 1;
	public static final int WIFI_MAC_ADDR_ID = 29;

	public static NvRAMAgent getAgent() throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		NvRAMAgent agent = null;
		Object object = new Object();

		Method getService = Class.forName("android.os.ServiceManager")
				.getMethod("getService", String.class);
		Object obj = getService.invoke(object, new Object[] { new String(
				"NvRAMAgent") });
		System.out.println(obj.toString());
		agent = NvRAMAgent.Stub.asInterface((IBinder) obj);

		return agent;
	}
}
