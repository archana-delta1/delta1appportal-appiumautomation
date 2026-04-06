package com.deltaone.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.logging.Logger;

public class BlotterDbQueries {
    private static final Logger logger = Logger.getLogger(BlotterDbQueries.class.getName());

    public int getUnifiedDatabaseCount(String startDate, String endDate, String tickers, String expirations) {
        int tradeHistoryCount = 0;
        int todaysMatchesCount = 0;
        String yesterdayStr = LocalDate.now().minusDays(1).toString();

        String historyQuery = "SELECT COUNT(*) AS TradeCount FROM [DeltaOne].[dbo].[TradeHistory] WHERE 1=1 ";

        if (startDate != null && endDate != null) {
            historyQuery += " AND CAST(DateTime AS DATE) >= '" + startDate + "' AND CAST(DateTime AS DATE) <= '" + endDate + "'";
        }

        if (tickers != null && !tickers.trim().isEmpty()) {
            String normalizedTickers = tickers.replaceAll("[\\n\\r\\s]+", ",");	
            String[] tickerArray = normalizedTickers.split(",");
            StringBuilder formattedTickers = new StringBuilder();
            for (int i = 0; i < tickerArray.length; i++) {
                if (!tickerArray[i].trim().isEmpty()) {
                    formattedTickers.append("'").append(tickerArray[i].trim()).append("'");
                    if (i < tickerArray.length - 1) formattedTickers.append(",");
                }
            }
            String finalTickers = formattedTickers.toString().replaceAll(",$", "");
            historyQuery += " AND Ticker IN (" + finalTickers + ") ";
        }

        if (expirations != null && !expirations.trim().isEmpty()) {
            historyQuery += " AND (";
            String normalizedExpirations = expirations.replaceAll("[\\n\\r]+", ",");
            String[] expArray = normalizedExpirations.split(",");
            boolean addedCondition = false;
            
            for (int i = 0; i < expArray.length; i++) {
                String expItem = expArray[i].trim();
                if (expItem.isEmpty()) continue;
                if (addedCondition) historyQuery += " OR ";
                
                if (expItem.contains("/")) {
                    String[] legs = expItem.split("/");
                    String innerCondition = buildDateCondition("[InnerExpiry]", legs[0]);
                    String outerCondition = buildDateCondition("[OuterExpiry]", legs[1]);
                    
                    if (innerCondition != null && outerCondition != null) {
                        historyQuery += "(isMultileg = 1 AND " + innerCondition + " AND " + outerCondition + ")";
                    } else {
                        historyQuery += "(isMultileg = 1 AND Contract LIKE '%" + legs[0].trim() + "%' AND Contract LIKE '%" + legs[1].trim() + "%')";
                    }
                    
                } else {
                    String singleCondition = buildDateCondition("Term", expItem);
                    
                    if (singleCondition != null) {
                        historyQuery += "(isMultileg = 0 AND " + singleCondition + ")";
                    } else {
                        // Fallback just in case parsing failed
                        historyQuery += "(isMultileg = 0 AND Contract LIKE '%" + expItem + "%')";
                    }
                }
                addedCondition = true;
            }
            historyQuery += ") ";
        }

        System.out.println("Executing Unified DB Query: " + historyQuery);

        // --- 2. EXECUTE QUERIES ---
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement()) {

            // Execute History Query
            ResultSet rsHistory = stmt.executeQuery(historyQuery);
            if (rsHistory.next()) {
                tradeHistoryCount = rsHistory.getInt("TradeCount");
            }

            if (startDate != null && endDate != null && !(yesterdayStr.equals(startDate) && yesterdayStr.equals(endDate))) {
                String todaysQuery = "SELECT COUNT(tm.Id) AS TodayTradesCount " +
                    "FROM [DeltaOne].[dbo].[TodaysMatches] AS tm " +
                    "LEFT JOIN [DeltaOne].[dbo].[TodaysOrder] AS to_buy ON tm.BuyOrderId = to_buy.OrderId " +
                    "LEFT JOIN [DeltaOne].[dbo].[TodaysOrder] AS to_sell ON tm.SellOrderId = to_sell.OrderId " +
                    "WHERE NOT (to_buy.Status = 'Cancelled' OR to_sell.Status = 'Cancelled')";

                ResultSet rsTodays = stmt.executeQuery(todaysQuery);
                if (rsTodays.next()) {
                    todaysMatchesCount = rsTodays.getInt("TodayTradesCount");
                }
            } 

        } catch (Exception e) {
            System.err.println("Failed to fetch DB counts: " + e.getMessage());
        }
        
        return tradeHistoryCount + todaysMatchesCount;
    }
    
    public int getDatabaseTradeCount(String startDate, String endDate) {
        int tradeHistoryCount = 0;
        int todaysMatchesCount = 0;

        String yesterdayStr = LocalDate.now().minusDays(1).toString();

        String historyQuery = "SELECT COUNT(*) AS TradeCount FROM [DeltaOne].[dbo].[TradeHistory] ";
        if (startDate != null && endDate != null) {
            historyQuery += "WHERE CAST(DateTime AS DATE) >= '" + startDate + "' AND CAST(DateTime AS DATE) <= '" + endDate + "'";
        }
        
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rsHistory = stmt.executeQuery(historyQuery);
            if (rsHistory.next()) {
                tradeHistoryCount = rsHistory.getInt("TradeCount");
            }

            if (!(yesterdayStr.equals(startDate) && yesterdayStr.equals(endDate))) {
                    String todaysQuery = "SELECT COUNT(tm.Id) AS TodayTradesCount " +
                        "FROM [DeltaOne].[dbo].[TodaysMatches] AS tm " +
                        "LEFT JOIN [DeltaOne].[dbo].[TodaysOrder] AS to_buy ON tm.BuyOrderId = to_buy.OrderId " +
                        "LEFT JOIN [DeltaOne].[dbo].[TodaysOrder] AS to_sell ON tm.SellOrderId = to_sell.OrderId " +
                        "WHERE NOT (to_buy.Status = 'Cancelled' OR to_sell.Status = 'Cancelled')";

                ResultSet rsTodays = stmt.executeQuery(todaysQuery);
                if (rsTodays.next()) {
                    todaysMatchesCount = rsTodays.getInt("TodayTradesCount");
                }
            } 

        } catch (Exception e) {
            System.err.println("Failed to fetch DB counts: " + e.getMessage());
        }
        return tradeHistoryCount + todaysMatchesCount;
    }

    // --- NEW DATE PARSING LOGIC ---
    /**
     * Parses financial expiry strings (e.g., "Mar", "Apr24", "Mar'26") and returns a SQL condition.
     */
    private String buildDateCondition(String columnName, String expItem) {
        expItem = expItem.trim();
        if (expItem.isEmpty()) return null;

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        // Extract month string (e.g., "Apr24" -> "Apr")
        String monthStr = expItem.replaceAll("[^a-zA-Z]", "");
        if (monthStr.length() > 3) monthStr = monthStr.substring(0, 3); // Normalize to 3 chars
        
        int month = getMonthNumber(monthStr);
        if (month == -1) return null; // Invalid month, trigger fallback

        Integer day = null;
        Integer year = null;

        try {
            // Extract numerical/symbol modifiers (e.g., "Apr24" -> "24", "Mar'26" -> "'26")
            String modifiers = expItem.replaceAll("[a-zA-Z\\s]", "");
            
            if (modifiers.contains("'")) {
                // Rule: Has an apostrophe -> It is an explicit year (e.g., '26)
                String yearStr = modifiers.replace("'", "");
                if (yearStr.length() == 2) {
                    year = 2000 + Integer.parseInt(yearStr);
                } else if (yearStr.length() == 4) {
                    year = Integer.parseInt(yearStr);
                }
            } else if (!modifiers.isEmpty()) {
                // Rule: Has digits but no apostrophe -> It is an explicit day (e.g., 24)
                day = Integer.parseInt(modifiers);
            }
        } catch (NumberFormatException e) {
            return null; // Safe fallback if parsing fails
        }

        // Rule: Calculate rolling year if not explicitly provided via '
        if (year == null) {
            if (month < currentMonth) {
                year = currentYear + 1; // Month already passed this year, roll to next year
            } else {
                year = currentYear;     // Month is current or future, use current year
            }
        }

        // Build the final SQL string
        if (day != null) {
            // Rule: Specific Day requested -> Match exact date (YYYY-MM-DD)
            String exactDate = String.format("%04d-%02d-%02d", year, month, day);
            return columnName + " = '" + exactDate + "'";
        } else {
            // Rule: No specific day requested -> Match ANY date in that month & year
        	return "(YEAR(TRY_CAST(" + columnName + " AS DATE)) = " + year + " AND MONTH(TRY_CAST(" + columnName + " AS DATE)) = " + month + ")";
        	}
    }

    private int getMonthNumber(String monthStr) {
        switch (monthStr.toLowerCase()) {
            case "jan": return 1;
            case "feb": return 2;
            case "mar": return 3;
            case "apr": return 4;
            case "may": return 5;
            case "jun": return 6;
            case "jul": return 7;
            case "aug": return 8;
            case "sep": return 9;
            case "oct": return 10;
            case "nov": return 11;
            case "dec": return 12;
            default: return -1;
        }
    }
}