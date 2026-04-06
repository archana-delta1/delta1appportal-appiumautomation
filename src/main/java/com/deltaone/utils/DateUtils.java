package com.deltaone.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {

    // Helper class to hold both dates
    public static class DateRange {
        public String startDate;
        public String endDate;

        public DateRange(String startDate, String endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
    
    public static class CustomDatePair {
        public String uiStartDate;
        public String uiEndDate;
        public String dbStartDate;
        public String dbEndDate;
    }
    
    public static CustomDatePair getRandomCustomDateRange() {
        int endDaysAgo = ThreadLocalRandom.current().nextInt(0, 31);
        LocalDate endDate = LocalDate.now().minusDays(endDaysAgo);

        int startDaysBeforeEnd = ThreadLocalRandom.current().nextInt(1, 91);
        LocalDate startDate = endDate.minusDays(startDaysBeforeEnd);

        DateTimeFormatter uiFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy"); 
        
        DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        CustomDatePair pair = new CustomDatePair();
        pair.uiStartDate = startDate.format(uiFormatter);
        pair.uiEndDate = endDate.format(uiFormatter);
        pair.dbStartDate = startDate.format(dbFormatter);
        pair.dbEndDate = endDate.format(dbFormatter);

        return pair;
    }


    public static DateRange getDatesForFilter(String filterType) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = null;
        LocalDate endDate = today; // Default end date is today for most filters

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        switch (filterType.toUpperCase()) {
            case "TODAY":
                startDate = today;
                System.out.println("Today:-"+startDate);
                break;
            case "YESTERDAY":
                // FIX: Ask the DB query class for the true business day instead of a literal minusDays(1)
                BlotterDbQueries dbQueries = new BlotterDbQueries();
                String trueYesterdayStr = dbQueries.getPreviousBusinessDay();
                
                // Parse it back to LocalDate so it plays nicely with the rest of this method
                startDate = LocalDate.parse(trueYesterdayStr, formatter);
                endDate = startDate; 
                System.out.println("Yesterday (Business Day):-"+startDate+" , "+endDate);
                break;
            case "1M":
                startDate = today.minusMonths(1);
                System.out.println("1M:-"+startDate);
                break;
            case "3M":
                startDate = today.minusDays(90); 
                System.out.println("3M:-"+startDate);
                break;
            case "6M":
                startDate = today.minusMonths(6);
                System.out.println("6M:-"+startDate);
                break;
            case "12M":
                startDate = today.minusMonths(12);
                System.out.println("12M:-"+startDate);
                break;
            case "MTD":
                startDate = today.withDayOfMonth(1); 
                System.out.println("MTD:-"+startDate);
                break;
            case "QTD":
                int startMonthOfQuarter = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                startDate = LocalDate.of(today.getYear(), startMonthOfQuarter, 1);
                System.out.println("QTD:-"+startDate);
                break;
            case "YTD":
                startDate = today.withDayOfYear(1); 
                System.out.println("YTD:-"+startDate);
                break;
            case "ALL":
                return new DateRange(null, null); 
            default:
                throw new IllegalArgumentException("Unknown filter type: " + filterType);
        }

        return new DateRange(startDate.format(formatter), endDate.format(formatter));   
    }
    
    
    public static String convertExpirationToDate(String exp) {
        exp = exp.trim();
        int currentYear = LocalDate.now().getYear(); 

        // 1. Exact Date (e.g., "mar6" or "mar 6")
        Matcher exactDateMatch = Pattern.compile("^([a-zA-Z]{3,})\\s*(\\d{1,2})$").matcher(exp);
        if (exactDateMatch.matches()) {
            int month = getMonthNumber(exactDateMatch.group(1));
            int day = Integer.parseInt(exactDateMatch.group(2));
            // Formats to yyyy-MM-dd (e.g., 2026-03-06)
            return LocalDate.of(currentYear, month, day).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        // 2. Specific Year (e.g., "mar'25")
        Matcher yearMatch = Pattern.compile("^([a-zA-Z]{3,})'(\\d{2})$").matcher(exp);
        if (yearMatch.matches()) {
            int month = getMonthNumber(yearMatch.group(1));
            int year = 2000 + Integer.parseInt(yearMatch.group(2)); 
            return getThirdFriday(year, month);
        }

        // 3. Just the Month (e.g., "mar") -> Assumes Third Friday of Current Year
        Matcher monthMatch = Pattern.compile("^([a-zA-Z]{3,})$").matcher(exp);
        if (monthMatch.matches()) {
            int month = getMonthNumber(monthMatch.group(1));
            return getThirdFriday(currentYear, month);
        }

        // Fallback if parsing fails
        return null; 
    }
    
    private static String getThirdFriday(int year, int month) {
        LocalDate thirdFriday = LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.FRIDAY));
        return thirdFriday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    
    private static int getMonthNumber(String monthStr) {
        String shortMonth = monthStr.substring(0, 3).toUpperCase();
        switch(shortMonth) {
            case "JAN": return 1; case "FEB": return 2; case "MAR": return 3;
            case "APR": return 4; case "MAY": return 5; case "JUN": return 6;
            case "JUL": return 7; case "AUG": return 8; case "SEP": return 9;
            case "OCT": return 10; case "NOV": return 11; case "DEC": return 12;
            default: return LocalDate.now().getMonthValue(); 
        }
    }
}