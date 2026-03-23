package com.deltaone.pages;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.windows.WindowsDriver;

public class BasePage {
	protected WebDriver driver;
    protected WebDriverWait wait;
    private By portfoloDeleteWindow = By.className("Window");
    private By portfolioDeleteButton = AppiumBy.accessibilityId("btnDelete");
    private By portfolio1DeleteYes = By.name("YES"); 
    private By defaultPortfolioGrid = AppiumBy.accessibilityId("tbPortfolio"); 
    private By whiteBoardTab = AppiumBy.accessibilityId("tbWhiteBoard");

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }
    public void clickAddTab() {
        wait.until(ExpectedConditions.elementToBeClickable(By.name("AutTbAddTab"))).click();
    }
    
    public void clickWhiteBoard() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(whiteBoardTab)).click();
        System.out.println("Clicked Whteboard");
    }
    
   /* public void deletePortfolio() {
        wait.until(ExpectedConditions.elementToBeClickable(portfolio1Delete)).click();
        WebElement confirmWindow = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("Window")));
		WebElement deleteYesBtn = wait.until(ExpectedConditions.elementToBeClickable(portfolio1DeleteYes));
		deleteYesBtn.click();
		wait.until(ExpectedConditions.elementToBeClickable(defaultPortfolioGrid)).click();
    }*/
    
    public String currentlySelectedPortfolio() {
    	WebElement tabControl = driver.findElement(AppiumBy.accessibilityId("tbcCustomer"));
    	WebElement selectedTab = tabControl.findElement(By.xpath(".//*[@IsSelected='True']"));
    	String selectedTabName = selectedTab.getAttribute("Name");
    	System.out.println("Currently selected tab is: " + selectedTabName);
    	return selectedTabName;
    }
    
    public int getPortfolioItemCount() {
        String selectedTabName = currentlySelectedPortfolio(); // e.g., "Portfolio1"
        String gridAccessibilityId = "uc" + selectedTabName;   // e.g., "ucPortfolio1"
        
        // 1. Find the parent User Control container
        WebElement userControl = driver.findElement(AppiumBy.accessibilityId(gridAccessibilityId));
        userControl.click(); // Good to keep this to ensure the tab/grid has focus
        
        // 2. Find the actual data grid INSIDE the user control, then get the row count
        WebElement dataGrid = userControl.findElement(AppiumBy.accessibilityId("dgPortfolio"));
        String rowCountStr = dataGrid.getAttribute("Grid.RowCount");
        
        System.out.println("Extracted Portfolio Item Count: " + rowCountStr);
        
        if (rowCountStr != null && !rowCountStr.trim().isEmpty()) {
            return Integer.parseInt(rowCountStr.trim());
        } else {
            System.err.println("Could not find Grid.RowCount attribute, returning 0.");
            return 0;
        }
    }
    
    public void deleteCurrentPortfolio() {
    	String selectedTabName = currentlySelectedPortfolio();
    	driver.findElement(portfolioDeleteButton).click();
    	WebElement confirmWindow = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("Window")));
		WebElement deleteYesBtn = wait.until(ExpectedConditions.elementToBeClickable(portfolio1DeleteYes));
		deleteYesBtn.click();
		wait.until(ExpectedConditions.elementToBeClickable(defaultPortfolioGrid)).click();
    }
}
