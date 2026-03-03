package com.deltaone.pages;

import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.By;

public class InvalidTickerPopup {
    private WindowsDriver driver;

    public InvalidTickerPopup(WindowsDriver driver) {
        this.driver = driver;
    }

    public boolean isDisplayed() {
        try {
            return driver.findElement(By.xpath("//Window/Window[1]")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void closePopup() {
        driver.findElement(By.xpath("//Window/Window[1]/Custom[1]/Button[2]")).click();
    }
}
