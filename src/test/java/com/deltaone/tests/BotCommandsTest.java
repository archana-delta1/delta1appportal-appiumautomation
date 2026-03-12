package com.deltaone.tests;

import java.awt.AWTException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.deltaone.pages.BotPage;
import com.deltaone.utils.ExcelUtil;
import java.time.Duration;

public class BotCommandsTest extends BaseTest {
    private BotPage botPage;

    @BeforeClass
    public void setupBotWindow() throws AWTException {
        botPage = new BotPage(driver);
        botPage.clickAddTab();
        botPage.doubleClickOpenBot();
        botPage.clickBotTextArea();
        botPage.sendChordToBot(Keys.F2);
        botPage.sendChordToBot(Keys.F2);
    }

    @Test(dataProvider = "UniversalProvider", dataProviderClass = ExcelUtil.class)
    public void BotCommands(String originalCmd, String modifiedCmd, String expValid, String expInvalid) throws Exception {
        int expectedValid = Integer.parseInt(expValid.replace(".0", ""));
        try {                       
            int countBefore = botPage.getPortfolioItemCount();
            if (modifiedCmd.contains("|")) {
                String[] lines = modifiedCmd.split("\\|");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        botPage.sendKeysToBot(line.trim());
                        botPage.sendChordToBot(Keys.SHIFT, Keys.ENTER);
                    }
                }
                botPage.sendChordToBot(Keys.ENTER);
            } else {
                botPage.sendKeysToBot(modifiedCmd);
                botPage.sendChordToBot(Keys.ENTER);
            }
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(d -> !botPage.getBotText().isEmpty());

            String botText = botPage.getBotText();
            int actualAdded = parseBotCount(botText, "Added");
            int countAfter = botPage.getPortfolioItemCount();
            int expectedFinalCount = countBefore + actualAdded;

            Assert.assertEquals(actualAdded, expectedValid, "Bot 'Added' count mismatch!");
            Assert.assertEquals(countAfter, expectedFinalCount, "Portfolio UI count mismatch!");

        } catch (Exception e) {
            System.out.println("Test failed. Cleaning up: " + e.getMessage());
            throw e; 
        }
    }

    @AfterClass
    public void tearDown() {
        try {
            botPage.sendChordToBot(Keys.F2); 
            botPage.closeBotWindow();
            botPage.deletePortfolio();
        } catch (Exception e) {
            System.out.println("Cleanup error: " + e.getMessage());
        }
    }

    private int parseBotCount(String text, String keyword) {
        try {
            int index = text.lastIndexOf(keyword);
            if (index == -1) return 0;
            int offset = keyword.equals("Added") ? 6 : 8; 
            String countStr = text.substring(index + offset, index + offset + 3).trim();
            return Integer.parseInt(countStr.replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}