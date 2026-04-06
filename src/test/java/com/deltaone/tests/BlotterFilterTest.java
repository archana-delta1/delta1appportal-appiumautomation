package com.deltaone.tests;

import com.deltaone.pages.BasePage;
import com.deltaone.pages.BlotterPage;
import com.deltaone.utils.BlotterDbQueries;
import com.deltaone.utils.DateUtils;
import com.deltaone.utils.ExcelUtil;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class BlotterFilterTest extends BaseTest { 
	
	private static List<String> filterDeck = new ArrayList<>();

    @DataProvider(name = "blotterFilters")
    public Object[][] getFilters() {
        return new Object[][] {
            {"Today"}, {"Yesterday"}, {"1M"}, {"3M"}, {"6M"}, 
            {"12M"}, {"MTD"}, {"QTD"}, {"YTD"}, {"All"}
        };
    }

    @Test(dataProvider = "UniversalProvider", dataProviderClass = ExcelUtil.class)
    public void BlotterValidations(String tickers, String expirations) {
    	System.out.println("\n=======================================================");
        System.out.println("STARTING MATRIX TEST -> Tickers: [" + tickers.replace("\n", " ") + "], Expirations: [" + expirations.replace("\n", " ") + "]");
        
        BlotterPage blotterPage = new BlotterPage(driver);
        BlotterDbQueries dbQueries = new BlotterDbQueries();

        blotterPage.clickWhiteBoard(); 
        blotterPage.applyTextFilters(tickers, expirations);

        
        int numFiltersToTest = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 3); 
        List<String> filtersToRun = new ArrayList<>();

        for (int i = 0; i < numFiltersToTest; i++) {
            if (filterDeck.isEmpty()) {
                Object[][] filterArray = getFilters(); // Calls your existing DataProvider
                for (Object[] row : filterArray) {
                    filterDeck.add((String) row[0]); // Extracts the string (e.g., "1M")
                }
                java.util.Collections.shuffle(filterDeck); // Shuffle the deck!
            }
            // Draw the top card
            filtersToRun.add(filterDeck.remove(0)); 
        }

        System.out.println("   -> Selected Filters for this row: " + filtersToRun);

        
        for (String filterName : filtersToRun) {
            System.out.println("   -> Applying Date Filter: " + filterName);
            
            blotterPage.selectFilter(filterName);
            DateUtils.DateRange dateRange = DateUtils.getDatesForFilter(filterName);

            int uiCount = blotterPage.getDisplayedTradeCount();
            int dbCount = dbQueries.getUnifiedDatabaseCount(dateRange.startDate, dateRange.endDate, tickers, expirations);

            if (dbCount > 10000) dbCount = 10000;

            System.out.println("      UI: " + uiCount + " | DB: " + dbCount);
            Assert.assertEquals(uiCount, dbCount, "Matrix Failure! [" + tickers + "] + [" + filterName + "]");
        }
    }
    
    @Test(dataProvider = "blotterFilters")
    public void verifyTradeHistoryCountsByStandardFilters(String filterName) {
        System.out.println("--- STARTING TEST FOR FILTER:"+filterName);

    	BlotterPage blotterPage = new BlotterPage(driver);
        BlotterDbQueries dbQueries = new BlotterDbQueries();

        DateUtils.DateRange dateRange = DateUtils.getDatesForFilter(filterName);

        blotterPage.clickWhiteBoard(); 
        blotterPage.applyTextFilters("", "");
        blotterPage.selectFilter(filterName);
        
        int uiCount = blotterPage.getDisplayedTradeCount();
        System.out.println("UI Count for " + filterName + ": " + uiCount);
        
        int dbCount = dbQueries.getDatabaseTradeCount(dateRange.startDate, dateRange.endDate);
        if(dbCount > 10000) dbCount=10000;
        System.out.println("DB Count for " + filterName + ": " + dbCount);

        Assert.assertEquals(uiCount, dbCount, 
            "Mismatch between UI and Database trade counts for filter: " + filterName);
    }
    
    @Test
    public void verifyTradeHistoryCountsByCustomDateRange() {
        System.out.println("--- STARTING TEST FOR FILTER: Custom (Random Dates) ---");
        
        BlotterPage blotterPage = new BlotterPage(driver);
        BlotterDbQueries dbQueries = new BlotterDbQueries();

        DateUtils.CustomDatePair randomDates = DateUtils.getRandomCustomDateRange();

        blotterPage.clickWhiteBoard(); 
        blotterPage.applyTextFilters("", "");
        blotterPage.enterCustomDateRange(randomDates.uiStartDate, randomDates.uiEndDate, false);
        
        int uiCount = blotterPage.getDisplayedTradeCount();
        int dbCount = dbQueries.getDatabaseTradeCount(randomDates.dbStartDate, randomDates.dbEndDate);

        Assert.assertEquals(uiCount, dbCount, "Mismatch for Custom Random Date Range!");
    }
    
    @Test
    public void verifyTradeHistoryCountsByCustomDateWithTodayCheckbox() {
        System.out.println("--- STARTING TEST FOR FILTER: Custom (With Today Checkbox) ---");
        
        BlotterPage blotterPage = new BlotterPage(driver);
        BlotterDbQueries dbQueries = new BlotterDbQueries();

        // 1. Get a random start date, but we know the end date will be Today!
        DateUtils.CustomDatePair randomDates = DateUtils.getRandomCustomDateRange();
        
        // Generate today's date exactly as the Database expects it (yyyy-MM-dd)
        String todayDbFormat = java.time.LocalDate.now().toString();

        blotterPage.clickWhiteBoard(); 
        blotterPage.applyTextFilters("", "");
        blotterPage.enterCustomDateRange(randomDates.uiStartDate, "IGNORED", true);
        
        int uiCount = blotterPage.getDisplayedTradeCount();
        
        int dbCount = dbQueries.getDatabaseTradeCount(randomDates.dbStartDate, todayDbFormat);

        Assert.assertEquals(uiCount, dbCount, "Mismatch for Custom Date using Today Checkbox!");
    }
}