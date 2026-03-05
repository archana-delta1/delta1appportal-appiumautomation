package com.deltaone.pages;

import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortfolioPage {
	WindowsDriver driver;
    WebDriverWait wait;
    
    
    public PortfolioPage(WindowsDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }
    
    private By singleEntryField = By.name("AuttxtSingleEntry");
    private By portfolioTab = By.xpath("//TabItem[@Name='AuttbPortfolio']");
    private By okButtonPopup = By.xpath("//Window/Window[1]/Custom[1]/Button[2]");
    private By invalidPopup = By.xpath("//Window/Window[1]");
    
    public void addTicker(String ticker) {
    	WebElement element = wait.until(ExpectedConditions.elementToBeClickable(singleEntryField));
        element.click();
        element.clear();
        element.sendKeys(ticker);
        element.sendKeys(Keys.ENTER);
    }

    public void handleInvalidPopup() {
        try {
            if (driver.findElements(invalidPopup).size() > 0) {
                driver.findElement(okButtonPopup).click();
                System.out.println("Invalid ticker popup closed.");
            }
        } catch (Exception e) {
        }
    }
    
    public void ShowHideColumns_AllFields(){
    	WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    	driver.findElement(By.name("AutBtnViewHidePosition")).click();
    	WebElement selectAllBtn = wait.until(
    	        ExpectedConditions.elementToBeClickable(By.name("Select All"))
    	    );

    	    // 4. Check Toggle State
    	    if (selectAllBtn.getAttribute("Toggle.ToggleState").equals("0")) {
    	        selectAllBtn.click();
    	        System.out.println("Selected all columns");
    	    } else {
    	        System.out.println("All columns are already selected");
    	    }

    	    // 5. Click Close
    	    driver.findElement(By.name("CloseCommand")).click();
    }
    
    	public Map<String, Object> parseEntry(String singleEntry) {
            if (singleEntry == null || singleEntry.trim().isEmpty()) {
                return createEmptyEntryMap();
            }

            String input = singleEntry.replaceAll("[\\u00A0\\s]+", " ").trim();
            Map<String, Object> result = createEmptyEntryMap();
            String remaining = input.toUpperCase();

            Pattern tickerPattern = Pattern.compile("^([A-Z0-9/]+)");
            Matcher tickerMatcher = tickerPattern.matcher(remaining);
            if (tickerMatcher.find()) {
                String ticker = tickerMatcher.group(1);
                result.put("ticker", ticker);
                remaining = remaining.substring(ticker.length()).trim();
            }

            String lower = input.toLowerCase();
            result.put("euroflex", lower.matches(".*\\b(euroflex|euro|ef)\\b.*"));
            result.put("jellyroll", lower.matches(".*\\b(jellyroll|jelly|jroll|jr|roll)\\b.*"));

            Pattern limitPattern = Pattern.compile("(\\d+(?:\\.\\d+)?\\s*(?:B|C|BID|OFFER))");
            Matcher limitMatcher = limitPattern.matcher(remaining);
            if (limitMatcher.find()) {
                String limit = limitMatcher.group(1);
                result.put("limit", limit);
                remaining = remaining.replace(limit, "").trim();
            }

            Pattern dirPattern = Pattern.compile("\\b(CONVERT|REVERSE|CONV|REV|BUY|SELL)\\b");
            Matcher dirMatcher = dirPattern.matcher(remaining);
            if (dirMatcher.find()) {
                result.put("direction", dirMatcher.group(1));
                remaining = remaining.replace(dirMatcher.group(1), "").trim();
            }

            String contract = remaining.replaceAll("(?i)\\b(EF|JR|EUROFLEX|JELLYROLL)\\b", "").trim();
            result.put("contract", contract.isEmpty() ? null : contract);

            return result;
        }

        private Map<String, Object> createEmptyEntryMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("ticker", null);
            map.put("contract", null);
            map.put("limit", null);
            map.put("direction", null);
            map.put("quantity", null);
            map.put("euroflex", false);
            map.put("jellyroll", false);
            return map;
        }    	
    
    	
    	
    	public String getGridValue(int rowCount, int customIndex, String type) {
        String xpath = ".//DataGrid//DataItem[" + rowCount + "]//Custom[" + customIndex + "]//" + type + "[1]";
        WebElement cell = driver.findElement(portfolioTab).findElement(By.xpath(xpath));
        
        if (type.equals("Edit")) {
            return cell.getAttribute("Value.Value");
        }
        return cell.getAttribute("Name");
    }
}