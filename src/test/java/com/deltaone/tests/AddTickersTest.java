package com.deltaone.tests;

import com.deltaone.pages.PortfolioPage;
import com.deltaone.utils.ExcelUtil;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddTickersTest extends BaseTest {
    PortfolioPage portfolio;

    @BeforeClass
    public void setupPageObjects() {
        portfolio = new PortfolioPage(driver);
        System.out.println("Initializing Column Settings...");
        portfolio.ShowHideColumns_AllFields();
    }

    @Test(dataProvider = "UniversalProvider", dataProviderClass = ExcelUtil.class)
    public void addTickersFromExcel(String ticker) {
    	Log.step("STARTING PROCESS FOR: " + ticker);        
    	Map<String, Object> data = portfolio.parseEntry(ticker);
        portfolio.addTicker(ticker);
       System.out.println( portfolio.parseEntry(ticker));
        portfolio.handleInvalidPopup();
  
    }

    private int parseQuantity(String rawQty) {
        if (rawQty == null || rawQty.isEmpty()) return 0;
        String cleaned = rawQty.replaceAll("[,\\s]", "");
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            return -Integer.parseInt(cleaned.replaceAll("[()]", ""));
        }
        return Integer.parseInt(cleaned);
    }
}