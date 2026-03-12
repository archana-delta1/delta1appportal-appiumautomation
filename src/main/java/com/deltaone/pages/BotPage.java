package com.deltaone.pages;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.windows.WindowsDriver;

public class BotPage extends BasePage {
    // Removed the shadowed driver and wait variables!
    private Actions actions;

    // clickAddTab() was removed because it's already inherited from BasePage

    private By openBot = AppiumBy.accessibilityId("btnBot");
    private By botTextArea = AppiumBy.accessibilityId("StatusCount");
    private By botWindow = By.name("BotWindow");
    private By botClose = By.xpath("//Window/Window[1]/Button[1]");
    
    public BotPage(WindowsDriver driver) {
        super(driver); // Initializes protected driver and wait from BasePage
        this.actions = new Actions(driver);
    }
    
    public void doubleClickOpenBot() throws AWTException {
        WebElement element = driver.findElement(openBot);
        driver.findElement(AppiumBy.accessibilityId("tbcCustomer")).click();
        Robot robot = new Robot();
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(KeyEvent.VK_BACK_QUOTE);
        robot.keyRelease(KeyEvent.VK_BACK_QUOTE);
        robot.keyRelease(KeyEvent.VK_SHIFT);        
    }
   
    public void clickBotTextArea() { 
        wait.until(ExpectedConditions.elementToBeClickable(botTextArea)).click();
    }
    
    public void sendKeysToBot(String text) {
        driver.findElement(botTextArea).sendKeys(text);
    }
    
    public void sendChordToBot(CharSequence... keys) {
        driver.findElement(botTextArea).sendKeys(Keys.chord(keys));
    }
    
    public String getBotText() {
        return driver.findElement(botTextArea).getText();
    }

    public void closeBotWindow() {
        wait.until(ExpectedConditions.elementToBeClickable(botWindow)).click();
        wait.until(ExpectedConditions.elementToBeClickable(botClose)).click();
    }
}