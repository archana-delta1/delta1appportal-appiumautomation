package com.deltaone.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

public class ScreenshotUtil {
    private static WebDriver driver;

    public static void setDriver(WebDriver d) {
        driver = d;
    }

    public static String takeScreenshot(String methodName) throws IOException {
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String path = System.getProperty("user.dir") + "/screenshots/" + methodName + ".png";
        FileUtils.copyFile(srcFile, new File(path));
        return path;
    }
}