package com.hankcs.hanlp.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeStampUtil {
	
	public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
	public static String YYYY_MM_DD = "yyyy-MM-dd";
	
	public static Long MAX_TIMESTAMP = 253402271999000L;
	
	public static Long ONE_DAT_TIMESTAMP = 86400000L;
	
	public static Date stringToDate(String str, String formatString) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat(formatString);
		Date date = null;
		date = format.parse(str);
		return date;
	}
	
	public static String dateToString(Date date, String formatString) {
		SimpleDateFormat format = new SimpleDateFormat(formatString);
		String str = format.format(date);
		return str;
	}
}
