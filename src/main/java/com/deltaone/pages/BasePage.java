package com.deltaone.pages;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.windows.WindowsDriver;

public class BasePage {
	protected WindowsDriver driver;
    protected WebDriverWait wait;
    private By portfoloDeleteWindow = By.className("Window");
    private By portfolio1Delete = By.xpath("//Window/Pane[1]/Pane[1]/Pane[1]/Pane[1]/Pane[1]/Custom[1]/Custom[1]/Tab[1]/TabItem[2]/Button[2]"); // Update as needed
    private By portfolio1DeleteYes = By.name("YES"); 
    private By defaultPortfolioGrid = By.name("AuttbPortfolio"); 

    public BasePage(WindowsDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // Common navigation actions
    public void clickAddTab() {
        wait.until(ExpectedConditions.elementToBeClickable(By.name("AutTbAddTab"))).click();
    }
    
    public void deletePortfolio() {
        wait.until(ExpectedConditions.elementToBeClickable(portfolio1Delete)).click();
        WebElement confirmWindow = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("Window")));
		WebElement deleteYesBtn = wait.until(ExpectedConditions.elementToBeClickable(portfolio1DeleteYes));
		deleteYesBtn.click();
		wait.until(ExpectedConditions.elementToBeClickable(defaultPortfolioGrid)).click();
    }
    
    public int getPortfolioItemCount() {
        WebElement dataGrid = driver.findElement(By.xpath("//Window/Pane[1]/Pane[1]/Pane[1]/Pane[1]/Pane[1]/Custom[1]/Custom[1]/Tab[1]/TabItem[2]/Custom[1]/DataGrid[1]"));
        dataGrid.click();
        String rowCountStr = dataGrid.getAttribute("Grid.RowCount");
        System.out.println("Extracted Portfolio Item Count: " + rowCountStr);
        if (rowCountStr != null && !rowCountStr.trim().isEmpty()) {
            return Integer.parseInt(rowCountStr.trim());
        } else {
            System.err.println("Could not find Grid.RowCount attribute, returning 0.");
            return 0;
        }
    }
}
