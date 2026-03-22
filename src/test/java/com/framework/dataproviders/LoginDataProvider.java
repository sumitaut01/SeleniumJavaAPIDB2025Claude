package com.framework.dataproviders;

import com.framework.config.ConfigManager;
import com.framework.constants.FrameworkConstants;
import com.framework.models.TestData;
import com.framework.utils.ExcelDataReader;
import com.framework.utils.JsonDataReader;
import org.testng.annotations.DataProvider;

import java.util.List;

/**
 * TestNG DataProviders for Login tests.
 *
 * Two providers:
 *  1. loginDataFromExcel – reads TestData.xlsx → LoginData sheet
 *  2. loginDataFromJson  – reads testdata/json/login.json
 *
 * Both filter by the active country (from ConfigManager).
 */
public class LoginDataProvider {

    private static String activeCountry() {
        return ConfigManager.getInstance().getCountry().name();
    }

    /**
     * Supplies login data from Excel.
     * parallel=true: each data row runs in its own thread.
     */
    @DataProvider(name = "loginDataExcel", parallel = true)
    public static Object[][] loginDataFromExcel() {
        List<TestData> data = ExcelDataReader.readSheet(
                FrameworkConstants.EXCEL_DATA_FILE,
                FrameworkConstants.LOGIN_SHEET,
                activeCountry()
        );
        return toArray(data);
    }

    /**
     * Supplies login data from JSON.
     */
    @DataProvider(name = "loginDataJson", parallel = true)
    public static Object[][] loginDataFromJson() {
        List<TestData> data = JsonDataReader.readTestData(
                FrameworkConstants.JSON_DATA_DIR + FrameworkConstants.LOGIN_JSON,
                activeCountry()
        );
        return toArray(data);
    }

    private static Object[][] toArray(List<TestData> list) {
        Object[][] result = new Object[list.size()][1];
        for (int i = 0; i < list.size(); i++) {
            result[i][0] = list.get(i);
        }
        return result;
    }
}
