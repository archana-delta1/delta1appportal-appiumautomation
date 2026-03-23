package com.deltaone.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.appium.java_client.AppiumBy;
import java.time.Duration;

public class BlotterPage extends BasePage { 
	private WebDriver driver;
    private WebDriverWait wait;
    
    private By todayCheckbox = AppiumBy.accessibilityId("chkToday");
    private By customBtn = AppiumBy.accessibilityId("rdbtnCustom");    
    private By startDateParent = AppiumBy.accessibilityId("dtpkrDateRangeFrom");
    private By endDateParent = AppiumBy.accessibilityId("dtpkrDateRangeTo");
    private By internalTextBox = AppiumBy.accessibilityId("PART_TextBox");
    private By totalTradesValue = AppiumBy.accessibilityId("tbTotalTrades");
    private By tickersInput = By.name("AutBlotterTxtBoxTickers");
    private By expirationsInput = By.name("AutBlotterTxtBoxExpirations");
    
    public BlotterPage(WebDriver driver) {
        super(driver); 
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void selectFilter(String filterName) {
        String automationId = "rdbtn" + filterName;
        if (filterName.equals("All")) {
            automationId = "rdBtnAll"; 
        }
        By radioLocator = AppiumBy.accessibilityId(automationId);
        wait.until(ExpectedConditions.elementToBeClickable(radioLocator)).click();
        System.out.println("Selected "+filterName);
        waitForDataToLoad(); 
    }

    public void enterCustomDateRange(String uiStartDate, String uiEndDate, boolean useTodayCheckbox) {
        System.out.println("Applying Custom Date Range. Use Today Checkbox? " + useTodayCheckbox);
        System.out.println("Applying Custom Date Range: " + uiStartDate + " to " + uiEndDate);

        // 1. Click the "Custom" toggle button
        wait.until(ExpectedConditions.visibilityOfElementLocated(customBtn)).click();
        
        // 2. Locate the START Date Box
        WebElement startParentContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(startDateParent));
        WebElement startField = startParentContainer.findElement(internalTextBox);
        
        // Brute Force Clear: Click, go to the end, and backspace 10 times
        startField.click();
        startField.sendKeys(Keys.END);
        for (int i = 0; i < 10; i++) {
            startField.sendKeys(Keys.BACK_SPACE);
        }
        
        // Now that it's empty, type the new date
        startField.sendKeys(uiStartDate);
        startField.sendKeys(Keys.TAB);      
        
        WebElement chkToday = driver.findElement(todayCheckbox);
        boolean isChecked = chkToday.isSelected(); 
        if (chkToday.getAttribute("Toggle.ToggleState") != null) {
            isChecked = chkToday.getAttribute("Toggle.ToggleState").equals("1");
        }

        if (useTodayCheckbox) {
            if (!isChecked) {
                chkToday.click();
            }
            chkToday.sendKeys(Keys.ENTER); 
            
        } else {
            // We WANT a custom date. If the box is checked, UNCHECK it to unlock the field!
            if (isChecked) {
                chkToday.click();
            }
        
        // 3. Locate the END Date Box
        WebElement endParentContainer = driver.findElement(endDateParent);
        WebElement endField = endParentContainer.findElement(internalTextBox);
        
        // Brute Force Clear for the End Date
        endField.click();
        endField.sendKeys(Keys.END);
        for (int i = 0; i < 10; i++) {
            endField.sendKeys(Keys.BACK_SPACE);
        }
        
        endField.sendKeys(uiEndDate);
        endField.sendKeys(Keys.ENTER); 
        
        // 4. Wait for the grid to update
        waitForDataToLoad();
        }
    }
    
    

    public int getDisplayedTradeCount() {
        wait.until(ExpectedConditions.presenceOfElementLocated(totalTradesValue));
        String countText = driver.findElement(totalTradesValue).getAttribute("Name");
        
        String numericOnly = countText.replaceAll("[^0-9]", "");
        return Integer.parseInt(numericOnly.trim());
    }
    
    private void enterMultiLineText(WebElement field, String text) {
        if (text == null || text.isEmpty()) return;
        
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            field.sendKeys(lines[i]); 
            if (i < lines.length - 1) {
                field.sendKeys(Keys.chord(Keys.SHIFT, Keys.ENTER));
            }
        }
    }
    
    private void forceClear(WebElement field) {
        field.click();    
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"));      
        field.sendKeys(Keys.BACK_SPACE);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void applyTextFilters(String tickers, String expirations) {
        System.out.println("Applying Text Filters -> Tickers: [" + 
            (tickers != null ? tickers.replace("\n", " | ") : "EMPTY") + "], Expirations: [" + 
            (expirations != null ? expirations.replace("\n", " | ") : "EMPTY") + "]");

        WebElement tickerField = wait.until(ExpectedConditions.visibilityOfElementLocated(tickersInput));
        WebElement expField = wait.until(ExpectedConditions.visibilityOfElementLocated(expirationsInput));

        // 1. STRICT TICKER CHECK
        if (tickers != null && !tickers.trim().isEmpty()) {
            // Data exists: Clear any old junk, then type the new data
            forceClear(tickerField);
            enterMultiLineText(tickerField, tickers);
        } else {
            // No data in Excel: Enforce that the field is completely empty!
            System.out.println("   -> No Ticker data provided. Forcing Ticker field to clear.");
            forceClear(tickerField);
        }
        tickerField.sendKeys(Keys.TAB); 

        // 2. STRICT EXPIRATION CHECK
        if (expirations != null && !expirations.trim().isEmpty()) {
            // Data exists: Clear any old junk, then type the new data
            forceClear(expField);
            enterMultiLineText(expField, expirations);
        } else {
            // No data in Excel: Enforce that the field is completely empty!
            System.out.println("   -> No Expiration data provided. Forcing Expiration field to clear.");
            forceClear(expField);
        }
        expField.sendKeys(Keys.ENTER); 
        
        waitForDataToLoad();
    }
    
    private void waitForDataToLoad() {
    try {
            Thread.sleep(2000); // 2 second pause to let the grid refresh
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}