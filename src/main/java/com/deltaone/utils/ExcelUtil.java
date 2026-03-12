package com.deltaone.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.DataProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {

	@DataProvider(name = "UniversalProvider")
    public static Object[][] getData(Method method) {
        // Logic: Sheet Name in Excel must match the @Test Method Name
        String sheetName = method.getName(); 
        String filePath = System.getProperty("user.dir") + "/src/test/resources/TestData.xlsx";        
        List<Object[]> dataList = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(new 	File(filePath));
            Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                System.err.println("Error: Sheet [" + sheetName + "] not found in Excel!");
                return new Object[0][0];
            }

            int rowCount = sheet.getLastRowNum();
            int colCount = sheet.getRow(0).getLastCellNum();
            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String runFlag = formatter.formatCellValue(row.getCell(0));

                if ("1".equals(runFlag)) {
                    Object[] dataRow = new Object[colCount - 1];
                    FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    
                    // This loop fills the dataRow array
                    for (int j = 1; j < colCount; j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null && cell.getCellType() == CellType.FORMULA) {
                            dataRow[j - 1] = formatter.formatCellValue(cell, evaluator);
                        } else {
                            dataRow[j - 1] = formatter.formatCellValue(row.getCell(j));
                        }
                    }
                    dataList.add(dataRow); 
                }
            }
            }catch (Exception e) {
            e.printStackTrace();
        }
        return dataList.toArray(new Object[0][0]);
    }
}

/*
    @DataProvider(name = "multiSheetProvider")
    public static Object[] getDataFromExcel(Method method) throws IOException {
        String filePath = System.getProperty("user.dir") + "/src/test/resources/TestData.xlsx";
        String sheetName = "";
        
        if (method.getName().equals("addTickersFromExcel")) {
            sheetName = "Add tickers to portfolio";
        }
        else if(method.getName().equals("BotCommandsTest")) {
            sheetName = "BotCommands";
        }

        try (FileInputStream fis = new FileInputStream(new File(filePath));
                Workbook workbook = new XSSFWorkbook(fis)) {
               
               Sheet sheet = workbook.getSheet(sheetName);
               // Use PhysicalNumberOfRows to ignore empty rows at the end
               int rowCount = sheet.getPhysicalNumberOfRows() - 1; 
               Object[] data = new Object[rowCount];
               DataFormatter formatter = new DataFormatter();

               for (int i = 0; i < rowCount; i++) {
                   Row row = sheet.getRow(i + 1); // Skip header
                   if (row != null) {
                       Cell cell = row.getCell(0);
                       String formattedValue = formatter.formatCellValue(cell);
                       
                       // Force print to console using err if out is being swallowed
                       System.err.println("Reading Row " + (i+1) + ": " + formattedValue);
                       
                       data[i] = formattedValue;
                   }
               }
               return data;
           } catch (Exception e) {
               e.printStackTrace();
               return new Object[0];
           }
       }
   }
*/