package com.deltaone.utils;

import org.testng.annotations.DataProvider;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelReader {
	
    @DataProvider(name = "TestData")
    
    public Object[][] getExcelData() throws IOException {
        String path = "src/test/resources/TestData.xlsx";
        FileInputStream fis = new FileInputStream(path);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        XSSFSheet sheet = workbook.getSheetAt(0);
        
        int rows = sheet.getPhysicalNumberOfRows();
        int cols = sheet.getRow(0).getLastCellNum();
        Object[] data = new Object[rows - 1];

        for (int i = 1; i < rows; i++) { 
                data[i-1] = sheet.getRow(i).getCell(i-1).toString();
        }
        workbook.close();
        return (Object[][]) data;
    }
}