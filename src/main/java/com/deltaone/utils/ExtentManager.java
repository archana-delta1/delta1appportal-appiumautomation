package com.deltaone.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

public class ExtentManager {
    private static ExtentReports extent;

    public static ExtentReports getInstance() {
        if (extent == null) {
            String path = System.getProperty("user.dir") + "/test-output/ExtentReport.html";
            ExtentSparkReporter htmlReporter = new ExtentSparkReporter(path);
            
            htmlReporter.config().setTheme(Theme.STANDARD);
            htmlReporter.config().setDocumentTitle("Desktop Automation Report");
            htmlReporter.config().setReportName("WinAppDriver Execution Report");

            extent = new ExtentReports();
            extent.attachReporter(htmlReporter);
            extent.setSystemInfo("Platform", "Windows");
            extent.setSystemInfo("Environment", "QA");
        }
        return extent;
    }
}