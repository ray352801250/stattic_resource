package com.dodoca.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

/**
 * @Author: TianGuangHui
 * @Date: 2019/3/12 17:20
 * @Description:
 */
public class DateUtils_java8 {

    private static final String DEAFAULT_DATA_PATTERN ="yyyy-MM-dd";
    private static final String DEAFAULT_TIME_PATTERN = "HH:mm:ss";
    private static final String DEAFAULT_DATATIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** String ---> LocalDate */
    public static LocalDate parseToLocalDate(String dateStr){
        return parseToLocalDate(dateStr, DEAFAULT_DATA_PATTERN);
    }

    public static LocalDate parseToLocalDate(String dateStr,String pattern){
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    /** String ---> LocalDateTime */
    public static LocalDateTime parseToLocalDateTime(String dateTimeStr){
        return parseToLocalDateTime(dateTimeStr,DEAFAULT_DATATIME_PATTERN);
    }

    public static LocalDateTime parseToLocalDateTime(String dateTimeStr,String pattern){
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /** String ---> LocalTime */
    public static LocalTime parseToLocalTime(String timeStr){
        return parseToLocalTime(timeStr, DEAFAULT_TIME_PATTERN);
    }

    public static LocalTime parseToLocalTime(String timeStr,String pattern){
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /** LocalTime / LocalDate / LocalDateTime ---> String */
    public static String formatToString(Temporal temporal, String pattern){
        DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern);
        return df.format(temporal);
    }

    /**  LocalDate / LocalDateTime ---> String("yyyy-MM-dd") */
    public static String formatLoalDate(Temporal temporal){
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DEAFAULT_DATA_PATTERN);
        return df.format(temporal);
    }

    /** LocalTime / LocalDateTime ---> String ("HH:mm:ss") */
    public static String formatLoalTime(Temporal temporal){
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DEAFAULT_TIME_PATTERN);
        return df.format(temporal);
    }

    /** LocalDate / LocalDateTime ---> String ("yyyy-MM-dd HH:mm:ss")*/
    public static String formatLoalDateTime(Temporal temporal){
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DEAFAULT_DATATIME_PATTERN);
        return df.format(temporal);
    }
}
