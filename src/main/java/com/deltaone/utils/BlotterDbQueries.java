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

        // --- 1. TRADE HISTORY QUERY ---
        String historyQuery = "SELECT COUNT(*) AS TradeCount FROM [DeltaOne].[dbo].[TradeHistory] WHERE 1=1 ";

        // Append Date Logic
        if (startDate != null && endDate != null) {
            historyQuery += " AND CAST(DateTime AS DATE) >= '" + startDate + "' AND CAST(DateTime AS DATE) <= '" + endDate + "'";
        }

        // Append Ticker Logic
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

        // Append Expiration Logic
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
                    // MULTI-LEG LOGIC (e.g., "Mar/May")
                    String[] legs = expItem.split("/");
                    String innerDate = DateUtils.convertExpirationToDate(legs[0]);
                    String outerDate = DateUtils.convertExpirationToDate(legs[1]);
                    
                    if (innerDate != null && outerDate != null) {
                        // ADDED: isMultileg = 1 condition
                        historyQuery += "(isMultileg = 1 AND [InnerExpiry] = '" + innerDate + "' AND [OuterExpiry] = '" + outerDate + "')";
                    } else {
                        // Fallback just in case parsing failed
                        historyQuery += "(isMultileg = 1 AND Contract LIKE '%" + legs[0].trim() + "%' AND Contract LIKE '%" + legs[1].trim() + "%')";
                    }
                    
                } else {
                    // SINGLE EXPRIATION LOGIC (e.g., "mar6", "mar'25")
                    String exactDate = DateUtils.convertExpirationToDate(expItem);
                    
                    if (exactDate != null) {
                        // ADDED: isMultileg = 0 condition
                        historyQuery += "(isMultileg = 0 AND Term = '" + exactDate + "')";
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
                
                // NOTE: If TodaysMatches also needs to be filtered by Ticker/Expiration, 
                // you must append the exact same StringBuilder logic to this query below!
                // Assuming standard volume for now.
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
}