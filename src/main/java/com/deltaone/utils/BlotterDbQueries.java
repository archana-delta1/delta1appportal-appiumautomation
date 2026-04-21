package com.deltaone.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class BlotterDbQueries {
	private static final Logger logger = Logger.getLogger(BlotterDbQueries.class.getName());

	public int getUnifiedDatabaseCount(String startDate, String endDate, String tickers, String expirations) {
        int tradeHistoryCount = 0;
        int todaysMatchesCount = 0;
        
        String todayStr = LocalDate.now().toString();
        String yesterdayStr = getPreviousBusinessDay(); 

        // --- 1. ROUTING LOGIC ---
        boolean isTodayOnly = todayStr.equals(startDate) && todayStr.equals(endDate);
        boolean queryHistory = !isTodayOnly; 

        boolean queryTodaysMatches;
        if (startDate == null || endDate == null) {
            // "All" Filter: No date boundaries means we want absolutely everything, including today!
            queryTodaysMatches = true;
        } else {
            // Specific Dates: Only query today if 'today' falls inside the window.
            queryTodaysMatches = (todayStr.compareTo(startDate) >= 0) && (todayStr.compareTo(endDate) <= 0);
        }
        
        // --- 2. BUILD FILTERS ---
        String dateCondition = "";
        if (startDate != null && endDate != null) {
            dateCondition = " AND CAST(DateTime AS DATE) >= '" + startDate + "' AND CAST(DateTime AS DATE) <= '" + endDate + "'";
        }

        String baseTextConditions = ""; 
        String historyExpConditions = ""; 
        String todaysExpConditions = "";  

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
            baseTextConditions += " AND Ticker IN (" + finalTickers + ") ";
        }

        if (expirations != null && !expirations.trim().isEmpty()) {
            historyExpConditions += " AND (";
            todaysExpConditions += " AND (";
            
            String normalizedExpirations = expirations.replaceAll("[\\n\\r]+", ",");
            String[] expArray = normalizedExpirations.split(",");
            boolean addedCondition = false;
            
            for (int i = 0; i < expArray.length; i++) {
                String expItem = expArray[i].trim();
                if (expItem.isEmpty()) continue;
                if (addedCondition) {
                    historyExpConditions += " OR ";
                    todaysExpConditions += " OR ";
                }
                
                if (expItem.contains("/")) {
                    String[] legs = expItem.split("/");
                    
                    // Escape single quotes for SQL
                    String safeLeg0 = legs[0].trim().replace("'", "''");
                    String safeLeg1 = legs[1].trim().replace("'", "''");
                    
                    String innerCondition = buildDateCondition("[InnerExpiry]", legs[0]);
                    String outerCondition = buildDateCondition("[OuterExpiry]", legs[1]);
                    
                    if (innerCondition != null && outerCondition != null) {
                        historyExpConditions += "(isMultileg = 1 AND " + innerCondition + " AND " + outerCondition + ")";
                    } else {
                        historyExpConditions += "(isMultileg = 1 AND Contract LIKE '%" + safeLeg0 + "%" + safeLeg1 + "%')";
                    }
                    
                    todaysExpConditions += "(isMultileg = 1 AND ContractID LIKE '%" + safeLeg0 + "%" + safeLeg1 + "%')";
                    
                } else {
                    // Escape single quotes for SQL
                    String safeExpItem = expItem.replace("'", "''");
                    String singleCondition = buildDateCondition("Term", expItem);
                    
                    if (singleCondition != null) {
                        // FIX APPLIED HERE: Allow Box Contracts (Multileg where BOTH legs match the single term)
                        String innerMatch = buildDateCondition("[InnerExpiry]", expItem);
                        String outerMatch = buildDateCondition("[OuterExpiry]", expItem);
                        
                        historyExpConditions += "( (isMultileg = 0 AND " + singleCondition + ") OR (isMultileg = 1 AND " + innerMatch + " AND " + outerMatch + ") )";
                    } else {
                        historyExpConditions += "( (isMultileg = 0 AND Contract LIKE '%" + safeExpItem + "%') OR (isMultileg = 1 AND Contract LIKE '%" + safeExpItem + "%" + safeExpItem + "%') )";
                    }
                    
                    // FIX APPLIED HERE: Check for repeating term in ContractID string for Box Contracts
                    todaysExpConditions += "( (isMultileg = 0 AND ContractID LIKE '%" + safeExpItem + "%') OR (isMultileg = 1 AND ContractID LIKE '%" + safeExpItem + "%" + safeExpItem + "%') )";
                }
                addedCondition = true;
            }
            historyExpConditions += ") ";
            todaysExpConditions += ") ";
        }

        // --- 3. EXECUTE QUERIES ---
        try (Connection conn = DatabaseUtils.getConnection();
             Statement stmt = conn.createStatement()) {

            if (queryHistory) {
                String historyQuery;
                boolean hasTicker = (tickers != null && !tickers.trim().isEmpty());

                if (!hasTicker) {
                    // SCENARIO 1 (No Ticker): Grab top 10k FIRST, then apply date/exp filters to that chunk.
                    historyQuery = "SELECT COUNT(*) AS TradeCount FROM (" +
                            "SELECT TOP 10000 * FROM [DeltaOne].[dbo].[TradeHistory] ORDER BY DateTime DESC" +
                            ") AS TopTrades WHERE 1=1 " 
                            + dateCondition + historyExpConditions;
                } else {
                    // SCENARIO 2 (Ticker Provided): Search whole DB for the Ticker/Filters, but cap the FINAL result at 10k.
                    historyQuery = "SELECT COUNT(*) AS TradeCount FROM (" +
                            "SELECT TOP 10000 * FROM [DeltaOne].[dbo].[TradeHistory] WHERE 1=1 " 
                            + dateCondition + baseTextConditions + historyExpConditions +
                            " ORDER BY DateTime DESC) AS TopTrades";
                }
                
                System.out.println("Executing History Query: " + historyQuery);
                ResultSet rsHistory = stmt.executeQuery(historyQuery);
                if (rsHistory.next()) {
                    tradeHistoryCount = rsHistory.getInt("TradeCount");
                }
            }
            	
            if (queryTodaysMatches) {
                String combinedTodaysFilters = baseTextConditions + todaysExpConditions;
                
                String todaysTextFilters = combinedTodaysFilters
                    .replace("Ticker", "to_buy.Ticker")
                    .replace("isMultileg", "to_buy.isMultileg")
                    .replace("ContractID", "to_buy.ContractID");

                String todaysQuery = "SELECT COUNT(tm.Id) AS TodayTradesCount " +
                        "FROM [DeltaOne].[dbo].[TodaysMatches] AS tm " +
                        "LEFT JOIN [DeltaOne].[dbo].[TodaysOrder] AS to_buy ON tm.BuyOrderId = to_buy.OrderId " +
                        "LEFT JOIN [DeltaOne].[dbo].[TodaysOrder] AS to_sell ON tm.SellOrderId = to_sell.OrderId " +
                        "WHERE NOT (to_buy.Status = 'Cancelled' OR to_sell.Status = 'Cancelled')" + todaysTextFilters;
                        
                System.out.println("Executing TodaysMatches Query: " + todaysQuery);
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

		String todayStr = LocalDate.now().toString();
		String yesterdayStr = getPreviousBusinessDay();

		// --- 1. ROUTING LOGIC ---
		boolean isTodayOnly = todayStr.equals(startDate) && todayStr.equals(endDate);
		boolean queryHistory = !isTodayOnly;

		boolean queryTodaysMatches = false;

		if (startDate != null && endDate != null) {
			queryTodaysMatches = (todayStr.compareTo(startDate) >= 0) && (todayStr.compareTo(endDate) <= 0);
		}

		String dateCondition = "";
		if (startDate != null && endDate != null) {
			dateCondition = " AND CAST(DateTime AS DATE) >= '" + startDate + "' AND CAST(DateTime AS DATE) <= '"
					+ endDate + "'";
		}

		try (Connection conn = DatabaseUtils.getConnection(); Statement stmt = conn.createStatement()) {

			if (queryHistory) {
				String historyQuery = "SELECT COUNT(*) AS TradeCount FROM [DeltaOne].[dbo].[TradeHistory] WHERE 1=1 "
						+ dateCondition;

				ResultSet rsHistory = stmt.executeQuery(historyQuery);
				if (rsHistory.next()) {
					tradeHistoryCount = rsHistory.getInt("TradeCount");
				}
			}

			if (queryTodaysMatches) {
				String todaysQuery = "SELECT COUNT(tm.Id) AS TodayTradesCount "
						+ "FROM [DeltaOne].[dbo].[TodaysMatches] AS tm "
						+ "LEFT JOIN [DeltaOne].[dbo].[TodaysOrder] AS to_buy ON tm.BuyOrderId = to_buy.OrderId "
						+ "LEFT JOIN [DeltaOne].[dbo].[TodaysOrder] AS to_sell ON tm.SellOrderId = to_sell.OrderId "
						+ "WHERE NOT (to_buy.Status = 'Cancelled' OR to_sell.Status = 'Cancelled')";

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
	private String buildDateCondition(String columnName, String expItem) {
		expItem = expItem.trim();
		if (expItem.isEmpty())
			return null;

		LocalDate today = LocalDate.now();
		int currentMonth = today.getMonthValue();
		int currentYear = today.getYear();

		String monthStr = expItem.replaceAll("[^a-zA-Z]", "");
		if (monthStr.length() > 3)
			monthStr = monthStr.substring(0, 3);

		int month = getMonthNumber(monthStr);
		if (month == -1)
			return null;

		Integer day = null;
		Integer year = null;

		try {
			String modifiers = expItem.replaceAll("[a-zA-Z\\s]", "");
			if (modifiers.contains("'")) {
				String yearStr = modifiers.replace("'", "");
				if (yearStr.length() == 2) {
					year = 2000 + Integer.parseInt(yearStr);
				} else if (yearStr.length() == 4) {
					year = Integer.parseInt(yearStr);
				}
			} else if (!modifiers.isEmpty()) {
				day = Integer.parseInt(modifiers);
			}
		} catch (NumberFormatException e) {
			return null;
		}

		if (year == null) {
			if (month < currentMonth) {
				year = currentYear + 1;
			} else {
				year = currentYear;
			}
		}

		if (day != null) {
			String exactDate = String.format("%04d-%02d-%02d", year, month, day);
			return "TRY_CAST(" + columnName + " AS DATE) = '" + exactDate + "'";
		} else {
			return "(YEAR(TRY_CAST(" + columnName + " AS DATE)) = " + year + " AND MONTH(TRY_CAST(" + columnName
					+ " AS DATE)) = " + month + ")";
		}
	}

	public String getPreviousBusinessDay() {
		LocalDate candidateDate = LocalDate.now().minusDays(1);

		LocalDate fetchStartDate = LocalDate.now().minusDays(15);
		Set<LocalDate> recentHolidays = getRecentHolidays(fetchStartDate, LocalDate.now());

		while (true) {
			DayOfWeek day = candidateDate.getDayOfWeek();
			boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);

			boolean isHoliday = recentHolidays.contains(candidateDate);

			if (!isWeekend && !isHoliday) {
				break;
			}

			candidateDate = candidateDate.minusDays(1);
		}

		return candidateDate.toString();
	}

	private Set<LocalDate> getRecentHolidays(LocalDate startDate, LocalDate endDate) {
		Set<LocalDate> holidays = new HashSet<>();

		String query = "SELECT CAST([Date] AS DATE) as HDate " + "FROM [DeltaOne].[dbo].[Holidays] "
				+ "WHERE CAST([Date] AS DATE) >= '" + startDate.toString() + "' " + "AND CAST([Date] AS DATE) <= '"
				+ endDate.toString() + "'";

		try (Connection conn = DatabaseUtils.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(query)) {

			while (rs.next()) {
				java.sql.Date sqlDate = rs.getDate("HDate");
				if (sqlDate != null) {
					holidays.add(sqlDate.toLocalDate());
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to fetch holidays: " + e.getMessage());
		}
		return holidays;
	}

	private int getMonthNumber(String monthStr) {
		switch (monthStr.toLowerCase()) {
		case "jan":
			return 1;
		case "feb":
			return 2;
		case "mar":
			return 3;
		case "apr":
			return 4;
		case "may":
			return 5;
		case "jun":
			return 6;
		case "jul":
			return 7;
		case "aug":
			return 8;
		case "sep":
			return 9;
		case "oct":
			return 10;
		case "nov":
			return 11;
		case "dec":
			return 12;
		default:
			return -1;
		}
	}
}