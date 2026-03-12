package com.deltaone.pages;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.windows.WindowsDriver;

public class BotPage extends BasePage{
	private WindowsDriver driver;
    private Actions actions;
    private WebDriverWait wait;

    private By addTab = By.name("AutTbAddTab"); 
    //private By openBot = By.name("AutBtnOpenPortfolio");
    private By openBot = AppiumBy.accessibilityId("btnBot");
    private By botTextArea = AppiumBy.accessibilityId("StatusCount");
    private By botWindow = By.name("BotWindow");
    private By botClose = By.xpath("//Window/Window[1]/Button[1]");
    
    public BotPage(WindowsDriver driver) {
        super(driver); 
    }
    
    public void clickAddTab() { 
    	//driver.findElement(addTab).click(); 
    	wait.until(ExpectedConditions.elementToBeClickable(addTab)).click();	
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
    	//driver.findElement(botTextArea).click(); 
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
       // driver.findElement(botWindow).click();
      //  driver.findElement(botClose).click();
    	wait.until(ExpectedConditions.elementToBeClickable(botWindow)).click();
		wait.until(ExpectedConditions.elementToBeClickable(botClose)).click();
    }

    

   
}
