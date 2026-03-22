package com.framework.utils;

import com.framework.models.TestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility that converts a List<TestData> into TestNG Object[][] for @DataProvider.
 *
 * Also provides filtering helpers so individual DataProvider classes stay thin.
 *
 * Usage:
 *   @DataProvider(name = "loginData")
 *   public static Object[][] loginData() {
 *       List<TestData> all = ExcelDataReader.readSheet(EXCEL_FILE, "LoginData", null);
 *       return DataProviderHelper.toDataProvider(
 *                DataProviderHelper.filterByCountry(all, activeCountry()));
 *   }
 */
public class DataProviderHelper {

    private static final Logger log = LogManager.getLogger(DataProviderHelper.class);

    private DataProviderHelper() {}

    /**
     * Converts a list of TestData into the Object[][] TestNG expects from a @DataProvider.
     * Each row is wrapped in a single-element array: Object[]{ testData }.
     */
    public static Object[][] toDataProvider(List<TestData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.warn("DataProviderHelper.toDataProvider: empty data list — returning empty array");
            return new Object[0][0];
        }
        Object[][] result = new Object[dataList.size()][1];
        for (int i = 0; i < dataList.size(); i++) {
            result[i][0] = dataList.get(i);
        }
        return result;
    }

    /**
     * Filters test data rows for a specific country (case-insensitive).
     * Only rows with runFlag=true are included.
     */
    public static List<TestData> filterByCountry(List<TestData> all, String country) {
        return all.stream()
                .filter(TestData::isRunFlag)
                .filter(td -> country == null
                        || country.equalsIgnoreCase(td.getCountry()))
                .collect(Collectors.toList());
    }

    /**
     * Filters test data rows for a specific tag/group stored in description field.
     * e.g. filterByTag(all, "smoke") keeps rows whose description contains "smoke".
     */
    public static List<TestData> filterByTag(List<TestData> all, String tag) {
        return all.stream()
                .filter(TestData::isRunFlag)
                .filter(td -> td.getDescription() != null
                        && td.getDescription().toLowerCase().contains(tag.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Logs a summary of what was loaded — useful for debugging data-driven runs.
     */
    public static void logSummary(String source, List<TestData> data) {
        log.info("DataProvider → source='{}', totalRows={}, countries={}",
                source,
                data.size(),
                data.stream()
                    .map(TestData::getCountry)
                    .distinct()
                    .collect(Collectors.joining(", ")));
    }
}
