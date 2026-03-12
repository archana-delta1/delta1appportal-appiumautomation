package com.deltaone.tests;

import io.appium.java_client.windows.WindowsDriver;
import io.appium.java_client.windows.options.WindowsOptions;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.deltaone.utils.*;

@Listeners(com.deltaone.utils.TestListener.class)
public class BaseTest {
    protected WindowsDriver driver;
    protected XSSFWorkbook workbook;
    private static AppiumDriverLocalService service;

    
    static {
        // Silences the HttpClient logs you see in the console
        Logger.getLogger("org.asynchttpclient").setLevel(Level.OFF);
        Logger.getLogger("org.openqa.selenium.remote").setLevel(Level.OFF);
    }
    
    static {
        // This turns off the console flooding from the HTTP client
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
        System.setProperty("io.netty.noUnsafe", "true");
        
        // Silence specific chatty libraries
        java.util.logging.Logger.getLogger("org.asynchttpclient").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("io.netty").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("io.netty.util").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.OFF);
    }
    
    /* private void killProcessByPort(int port) {
        try {
            Process process = Runtime.getRuntime().exec("cmd /c netstat -ano | findstr :" + port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("LISTENING")) {
                    String[] parts = line.split("\\s+");
                    String pid = parts[parts.length - 1];
                    Log.info("Killing existing process on port " + port + " (PID: " + pid + ")");
                    Runtime.getRuntime().exec("taskkill /F /PID " + pid);
                    Thread.sleep(1000); // Give OS time to release the port
                }
            }
        } catch (Exception e) {
            Log.info("No existing process found on port " + port);
        }
    }
    */
    @BeforeSuite (alwaysRun = true) 

    public void startAppiumServer() {
    	if (service != null && service.isRunning()) {
            return; 
        }
        int port = 4725;
        int maxRetries = 3;
        boolean started = false;

        while (maxRetries > 0 && !started) {
            try {
                // 1. Clean up the specific port we want to use
              //  killProcessByPort(port);
                
                AppiumServiceBuilder builder = new AppiumServiceBuilder()
                        .usingDriverExecutable(new File("C:\\Program Files\\nodejs\\node.exe"))
                        .withAppiumJS(new File("C:\\Users\\Archana\\AppData\\Roaming\\npm\\node_modules\\appium\\build\\lib\\main.js"))
                        .withIPAddress("127.0.0.1")
                        .usingPort(port)
                        .withArgument(GeneralServerFlag.SESSION_OVERRIDE)
                        .withArgument(GeneralServerFlag.LOG_LEVEL, "error");

                service = AppiumDriverLocalService.buildService(builder);
                service.start();
                
                if (service.isRunning()) {
                    Log.step("Appium Server started on port: " + port);
                    started = true;
                }
            } catch (Exception e) {
                Log.info("Port " + port + " is busy/locked. Retrying with next port...");
                port++;
                maxRetries--;
            }
        }

        if (!started) {
            throw new RuntimeException("Could not start Appium server after multiple attempts.");
        }
    }

    @BeforeClass
    public void setUp() throws Exception {
        // 1. Start server (this uses the dynamic port logic we built)
        startAppiumServer(); 
        
        // 2. Use the dynamic URL from the service instead of hardcoded 4725
        URL dynamicUrl = service.getUrl(); 

        WindowsOptions rootOptions = new WindowsOptions();
        rootOptions.setCapability("app", "Root");
        rootOptions.setCapability("platformName", "Windows");
        rootOptions.setCapability("automationName", "Windows");

        // Initialize Root Session with dynamic URL
        WindowsDriver desktopSession = new WindowsDriver(dynamicUrl, rootOptions);
        WebElement appWindow = null;
        
        Log.step("Searching for Bloomberg Delta One window...");
        
        for(int i=0; i<5; i++) {
            try {
                appWindow = desktopSession.findElement(By.name("Delta One"));
                break;
            } catch (Exception e) {
                try {
                    appWindow = desktopSession.findElement(By.xpath("//Window[contains(@Name, 'Delta One')]"));
                    break;
                } catch (Exception e2) {
                    Log.info("Window not found, retrying... (" + (i + 1) + ")");
                    Thread.sleep(2000);
                }
            }
        }


        String handle = appWindow.getAttribute("NativeWindowHandle");
        String hexHandle = Integer.toHexString(Integer.parseInt(handle));
        Log.step("Found Window Handle: " + hexHandle);
        
        WindowsOptions appOptions = new WindowsOptions();
        appOptions.setCapability("appTopLevelWindow", hexHandle);
        appOptions.setCapability("ms:waitForAppLaunch", "5");

        // Use dynamicUrl again for the main driver
        driver = new WindowsDriver(dynamicUrl, appOptions);
        Log.step("Main Driver Session established successfully.");
        ScreenshotUtil.setDriver(driver);
    }
    
    public void captureScreen(String fileName) {
        // 1. Take the screenshot from the Appium/WinAppDriver session
        File srcFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        
        // 2. Save it to the 'target/screenshots' folder
        String path = System.getProperty("user.dir") + "/target/screenshots/" + fileName + ".png";
        try {
            FileUtils.copyFile(srcFile, new File(path));
            System.out.println("Screenshot saved to: " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterSuite(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
        if (service != null) service.stop();
    }
    
    public class Log {
        // ANSI Color Codes
        public static final String RESET = "\u001B[0m";
        public static final String GREEN = "\u001B[32m";
        public static final String CYAN  = "\u001B[36m";
        public static final String YELLOW = "\u001B[33m";
        public static final String BOLD  = "\u001B[1m";

        public static void step(String message) {
            System.out.println(BOLD + GREEN + ">>> [STEP]: " + RESET + CYAN + message + RESET);
        }

        public static void info(String message) {
            System.out.println(YELLOW + "[INFO]: " + RESET + message);
        }
    }
    
}