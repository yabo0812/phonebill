package com.phonebill.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 날짜/시간 관련 유틸리티
 * 날짜 포맷팅, 파싱 등의 공통 기능을 제공
 */
public class DateTimeUtils {
    
    /**
     * 표준 날짜/시간 포맷터
     */
    public static final DateTimeFormatter STANDARD_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 날짜 포맷터
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 시간 포맷터
     */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * ISO 8601 포맷터
     */
    public static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    /**
     * LocalDateTime을 문자열로 변환
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(STANDARD_DATETIME_FORMATTER);
    }
    
    /**
     * LocalDateTime을 지정된 포맷으로 변환
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(formatter);
    }
    
    /**
     * 문자열을 LocalDateTime으로 파싱
     */
    public static LocalDateTime parse(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeString, STANDARD_DATETIME_FORMATTER);
    }
    
    /**
     * 문자열을 지정된 포맷으로 LocalDateTime으로 파싱
     */
    public static LocalDateTime parse(String dateTimeString, DateTimeFormatter formatter) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeString, formatter);
    }
    
    /**
     * 현재 날짜/시간을 표준 포맷으로 반환
     */
    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(STANDARD_DATETIME_FORMATTER);
    }
    
    /**
     * 현재 날짜를 반환
     */
    public static String getCurrentDate() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }
    
    /**
     * 현재 시간을 반환
     */
    public static String getCurrentTime() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}