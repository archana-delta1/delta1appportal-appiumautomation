package com.deltaone.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.DataProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

public class ExcelUtil {

    @DataProvider(name = "multiSheetProvider")
    public static Object[] getDataFromExcel(Method method) throws IOException {
        String filePath = System.getProperty("user.dir") + "/src/test/resources/TestData.xlsx";
        String sheetName = "";
        
        if (method.getName().equals("addTickersFromExcel")) {
            sheetName = "Add tickers to portfolio";
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