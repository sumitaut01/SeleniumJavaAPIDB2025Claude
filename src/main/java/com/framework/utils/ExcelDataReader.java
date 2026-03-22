package com.framework.utils;

import com.framework.models.TestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads test data from .xlsx files using Apache POI.
 *
 * Sheet structure (row 0 = headers, row 1+ = data):
 * | testCaseId | country | description | runFlag | username | password | ... |
 *
 * Usage:
 *   List<TestData> rows = ExcelDataReader.readSheet(EXCEL_DATA_FILE, "LoginData", "IN");
 */
public class ExcelDataReader {

    private static final Logger log = LogManager.getLogger(ExcelDataReader.class);

    private ExcelDataReader() {}

    /**
     * Reads all rows from a sheet that match the given country.
     * Only rows with runFlag=true are returned.
     *
     * @param filePath   path to the .xlsx file
     * @param sheetName  sheet name
     * @param country    filter by this country code (null = no filter)
     * @return list of TestData objects
     */
    public static List<TestData> readSheet(String filePath, String sheetName, String country) {
        List<TestData> dataList = new ArrayList<>();

        try (FileInputStream fis     = new FileInputStream(filePath);
             Workbook         workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("Sheet '" + sheetName + "' not found in " + filePath);
            }

            // Build header index map from row 0
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headers = buildHeaderMap(headerRow);

            // Iterate data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String rowCountry = getCellValue(row, headers, "country");
                boolean runFlag   = Boolean.parseBoolean(getCellValue(row, headers, "runFlag"));

                // Skip if run flag is false
                if (!runFlag) continue;

                // Skip if country filter doesn't match
                if (country != null && !country.equalsIgnoreCase(rowCountry)) continue;

                TestData td = TestData.builder()
                        .testCaseId(getCellValue(row, headers, "testCaseId"))
                        .country(rowCountry)
                        .description(getCellValue(row, headers, "description"))
                        .runFlag(runFlag)
                        .username(getCellValue(row, headers, "username"))
                        .password(getCellValue(row, headers, "password"))
                        .expectedTitle(getCellValue(row, headers, "expectedTitle"))
                        .firstName(getCellValue(row, headers, "firstName"))
                        .lastName(getCellValue(row, headers, "lastName"))
                        .email(getCellValue(row, headers, "email"))
                        .phone(getCellValue(row, headers, "phone"))
                        .expectedMessage(getCellValue(row, headers, "expectedMessage"))
                        .expectedUrl(getCellValue(row, headers, "expectedUrl"))
                        .build();

                dataList.add(td);
            }

            log.info("ExcelDataReader → sheet='{}', country='{}', rows loaded={}",
                    sheetName, country, dataList.size());

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel: " + filePath, e);
        }

        return dataList;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) return map;
        for (Cell cell : headerRow) {
            map.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
        }
        return map;
    }

    private static String getCellValue(Row row, Map<String, Integer> headers, String key) {
        if (!headers.containsKey(key)) return "";
        Cell cell = row.getCell(headers.get(key));
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                // Avoid trailing .0 for whole numbers
                double val = cell.getNumericCellValue();
                yield (val == Math.floor(val)) ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> "";
        };
    }
}
