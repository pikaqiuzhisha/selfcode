/**
 * 
 */
package com.chargedot.charge.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author gmm
 *
 */
public final class GeneratorUtil {

	private final static SimpleDateFormat TS = new SimpleDateFormat("yyMMddHHmmsss");

	/**
	 * count
	 */
	private int cnt;
	/**
	 * minute
	 */
	private long minute;

	private static GeneratorUtil instance = new GeneratorUtil();
	
	/**
	 * 
	 */
	private GeneratorUtil() {
	}
	
	public static GeneratorUtil getInstance() {
		return instance;
	}

	/**
	 * generate an id
	 * @return
	 */
	public synchronized String generateId(String deviceId) {
		long now = System.currentTimeMillis();
		long m = (long) (now / 1000 / 60 * 60 * 1000);
		if (minute < m) {
			minute = m;
			cnt = 0;
		}
		cnt++;
		if(Integer.parseInt(deviceId) < 10){
			deviceId = deviceId + "00000";
		}else if(Integer.parseInt(deviceId) < 100){
			deviceId = deviceId + "0000";
		}else if(Integer.parseInt(deviceId) < 1000){
			deviceId = deviceId + "000";
		}else if(Integer.parseInt(deviceId) < 10000){
			deviceId = deviceId + "00";
		}else if(Integer.parseInt(deviceId) < 100000){
			deviceId = deviceId + "0";
		}else{
			deviceId = deviceId.substring(0, 6);
		}
		return TS.format(new Date(now)) + deviceId + ((int)(Math.random()*(999999-100000+1))+100000);
	}
}