package com.framework.api.filters;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.framework.listeners.ExtentReportManager;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * RestAssured Filter that logs every API request + response
 * into the active ExtentReport test node.
 *
 * Register in RequestBuilder or per-spec:
 *   specBuilder.addFilter(new ExtentReportApiFilter());
 *
 * This means every API call made in a test is visible
 * in the HTML report with full request/response detail.
 */
public class ExtentReportApiFilter implements Filter {

    private static final Logger log = LogManager.getLogger(ExtentReportApiFilter.class);

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        Response response = ctx.next(requestSpec, responseSpec);

        try {
            ExtentTest test = ExtentReportManager.getTest();
            if (test != null) {
                // ── Request ──────────────────────────────────────────────────
                String requestInfo = String.format(
                        "Method : %s\nURI    : %s\nHeaders: %s\nBody   : %s",
                        requestSpec.getMethod(),
                        requestSpec.getURI(),
                        requestSpec.getHeaders(),
                        requestSpec.getBody() != null ? requestSpec.getBody().toString() : "(empty)"
                );
                test.info(MarkupHelper.createCodeBlock("API REQUEST\n" + requestInfo, CodeLanguage.JSON));

                // ── Response ─────────────────────────────────────────────────
                String responseInfo = String.format(
                        "Status : %d %s\nTime   : %d ms\nBody   : %s",
                        response.statusCode(),
                        response.statusLine(),
                        response.time(),
                        response.body().asPrettyString()
                );
                test.info(MarkupHelper.createCodeBlock("API RESPONSE\n" + responseInfo, CodeLanguage.JSON));
            }
        } catch (Exception e) {
            log.warn("ExtentReportApiFilter: failed to log to report → {}", e.getMessage());
        }

        return response;
    }
}
