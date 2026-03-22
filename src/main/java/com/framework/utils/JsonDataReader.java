package com.framework.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.models.TestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads test data from JSON files using Jackson.
 *
 * JSON file format (array of TestData objects):
 * [
 *   {
 *     "testCaseId": "TC001",
 *     "country": "IN",
 *     "runFlag": true,
 *     "username": "user@in.com",
 *     "password": "pass123",
 *     ...
 *   }
 * ]
 *
 * Usage:
 *   List<TestData> data = JsonDataReader.readTestData(LOGIN_JSON_PATH, "IN");
 */
public class JsonDataReader {

    private static final Logger log    = LogManager.getLogger(JsonDataReader.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonDataReader() {}

    /**
     * Reads all TestData entries from a JSON file, filtered by country.
     * Only runFlag=true entries are returned.
     *
     * @param filePath path to the JSON file
     * @param country  filter (null = return all)
     * @return filtered list of TestData
     */
    public static List<TestData> readTestData(String filePath, String country) {
        try {
            File jsonFile = new File(filePath);
            if (!jsonFile.exists()) {
                throw new RuntimeException("JSON data file not found: " + filePath);
            }

            List<TestData> allData = mapper.readValue(jsonFile, new TypeReference<>() {});

            List<TestData> filtered = allData.stream()
                    .filter(TestData::isRunFlag)
                    .filter(td -> country == null || country.equalsIgnoreCase(td.getCountry()))
                    .collect(Collectors.toList());

            log.info("JsonDataReader → file='{}', country='{}', rows loaded={}",
                    filePath, country, filtered.size());

            return filtered;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON: " + filePath, e);
        }
    }

    /**
     * Converts a List<TestData> into Object[][] for use with TestNG @DataProvider.
     */
    public static Object[][] toDataProviderArray(List<TestData> dataList) {
        Object[][] result = new Object[dataList.size()][1];
        for (int i = 0; i < dataList.size(); i++) {
            result[i][0] = dataList.get(i);
        }
        return result;
    }
}
