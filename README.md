# Selenium + RestAssured + JDBC Hybrid Test Framework

Production-grade Java test automation framework supporting **UI**, **API**, and **Database** 
validation — individually or combined in a single test. Multi-environment, multi-country, 
parallel-safe, with ExtentReports, retry logic, and HikariCP connection pooling.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                        TEST CLASS                             │
│  (UI / API / DB / Hybrid — all extend the same BaseTest)     │
└─────────┬──────────────────┬────────────────────────────────┘
          │                  │
  ┌───────▼───────┐  ┌───────▼─────────────────────────┐
  │   UI LAYER    │  │         API LAYER                │
  │  (Selenium)   │  │  (RestAssured + Jackson)         │
  │               │  │                                  │
  │  BasePage     │  │  RequestBuilder   → spec builder │
  │  LoginPage    │  │  ApiClient        → HTTP exec    │
  │  ...Page      │  │  ResponseValidator→ assertions   │
  │               │  │  AuthService / UserService /     │
  │  DriverManager│  │  OrderService     → domain svcs  │
  │  (ThreadLocal)│  │  ExtentApiFilter  → report logs  │
  └───────────────┘  └──────────────────────────────────┘
          │                  │
          └────────┬─────────┘
                   │
        ┌──────────▼─────────────────────────────────────┐
        │        TestContext  (ThreadLocal)               │
        │  API writes: authToken, userId, orderId        │
        │  UI reads them. DB validates them.             │
        └──────────────────────────────────────────────────┘
                   │
        ┌──────────▼─────────────────────────────────────┐
        │           DB LAYER  (JDBC + HikariCP)           │
        │                                                  │
        │  ConnectionManager → HikariCP pool per DbType  │
        │  DbClient          → query/execute/batch/proc  │
        │  DbValidator       → fluent assertion chain    │
        │  DbQueries         → central SQL registry      │
        │  RowMapper<T>      → ResultSet → typed POJO    │
        │  DbTestHelper      → setup/teardown helpers    │
        └──────────────────────────────────────────────────┘
```

### Thread Safety

| Component | Strategy | Purpose |
|---|---|---|
| `DriverManager` | `ThreadLocal<WebDriver>` | One browser per thread |
| `TestContext` | `ThreadLocal<TestContext>` | One data bag per test |
| `ExtentReportManager` | `ThreadLocal<ExtentTest>` | One report node per test |
| `ConnectionManager` | HikariCP pool (shared) | Pool shared; connections checked out per-thread |

---

## Project Structure (54 files)

```
src/main/java/com/framework/
├── api/
│   ├── builder/       ApiClient.java · RequestBuilder.java
│   ├── endpoints/     ApiEndpoints.java
│   ├── filters/       ExtentReportApiFilter.java
│   ├── services/      AuthService · UserService · OrderService
│   └── validators/    ResponseValidator.java
├── config/            ConfigManager.java
├── constants/         FrameworkConstants.java
├── context/           TestContext.java
├── db/
│   ├── connection/    ConnectionManager.java
│   ├── executor/      DbClient.java · DbTestHelper.java
│   ├── mapper/        RowMapper · GenericRowMapper · UserDbRecord · OrderDbRecord
│   ├── queries/       DbQueries.java
│   └── validator/     DbValidator.java
├── drivers/           DriverFactory.java · DriverManager.java
├── enums/             Browser · Country · DbType · Environment · ExecutionType
├── listeners/         ExtentReportManager · RetryAnalyzer · RetryTransformer
│                      SoftAssertListener · TestListener
├── models/            EnvConfig · TestData · request/ · response/
├── pages/             BasePage.java · LoginPage.java
└── utils/             AwaitilityUtil · DataProviderHelper · ExcelDataReader
                       JsonDataReader · ScreenshotUtil · WaitUtil

src/test/java/com/framework/
├── dataproviders/     LoginDataProvider.java
└── tests/
    ├── BaseTest.java          # all modes: driver + context + pgDb + orDb
    ├── BaseApiTest.java       # mode=API: no browser
    ├── BaseHybridTest.java    # mode=HYBRID: driver + context + DB
    ├── LoginTest.java
    ├── api/  UserApiTest.java
    ├── db/   UserDbTest.java
    └── hybrid/  CreateUserHybridTest.java · FullFlowTest.java
```

---

## Tech Stack

| Dependency | Version | Purpose |
|---|---|---|
| Selenium | 4.18.1 | Browser automation |
| TestNG | 7.9.0 | Test runner + parallel execution |
| RestAssured | 5.4.0 | API testing |
| Jackson | 2.17.0 | JSON serialization |
| HikariCP | 5.1.0 | JDBC connection pooling |
| PostgreSQL JDBC | 42.7.3 | Postgres driver |
| Oracle ojdbc11 | 23.4.0.24.05 | Oracle driver |
| ExtentReports | 5.1.1 | HTML test reports |
| Apache POI | 5.2.5 | Excel data reading |
| AssertJ | 3.25.3 | Fluent assertions + SoftAssertions |
| Awaitility | 4.2.1 | Async/eventual-consistency polling |
| WebDriverManager | 5.7.0 | Auto browser driver downloads |
| Log4j2 | 2.23.1 | Logging |
| Lombok | 1.18.32 | Boilerplate reduction |

---

## Configuration (env-config.json)

```json
{
  "environments": {
    "qa": {
      "IN": {
        "baseUrl": "https://qa.myapp.in",
        "apiUrl":  "https://qa-api.myapp.in",
        "postgres": {
          "url":      "jdbc:postgresql://qa-pg.myapp.in:5432/appdb_in",
          "username": "qa_pg_user",
          "password": "qa_pg_pass",
          "schema":   "public",
          "poolSize": 5
        },
        "oracle": {
          "url":      "jdbc:oracle:thin:@qa-ora.myapp.in:1521:ORCL",
          "username": "qa_ora_user",
          "password": "qa_ora_pass",
          "schema":   "QA_SCHEMA",
          "poolSize": 3
        }
      }
    }
  }
}
```

Set `"oracle": null` for countries where Oracle is not deployed — handled gracefully.

### Maven system properties

| Property | Default | Options |
|---|---|---|
| `-Denv` | `qa` | `dev` · `qa` · `uat` |
| `-Dcountry` | `IN` | `IN` · `US` · `UK` · `AU` |
| `-Dexecution` | `local` | `local` · `remote` |
| `-Dbrowser` | `chrome` | `chrome` · `firefox` · `edge` |
| `-Dretry.count` | `1` | `0`=off · `2`=two retries |
| `-Dsuite` | `testng.xml` | any file in `config/` |

---

## Running Tests

```bash
# All tests — defaults: qa / IN / local / chrome
mvn test

# Different env + country
mvn test -Denv=uat -Dcountry=US -Dbrowser=firefox

# Remote Grid
export SELENIUM_GRID_URL=http://grid:4444/wd/hub
mvn test -Dexecution=remote -Denv=qa -Dcountry=UK

# API tests only (no browser)
mvn test -Dgroups=api

# DB tests only (no browser, no API)
mvn test -Dgroups=db

# Hybrid full-stack tests
mvn test -Dgroups=fullflow

# Smoke across all layers
mvn test -Dgroups=smoke

# Disable retries
mvn test -Dretry.count=0
```

---

## Test Patterns

### Pattern 1 — Pure UI

```java
public class ProfileTest extends BaseTest {
    @Test(groups = "regression")
    public void testUpdateProfile() {
        new LoginPage().openLoginPage().login("user@qa.in", "Test@1234");
        // assert UI state...
    }
}
```

### Pattern 2 — Pure API

```java
public class UserApiTest extends BaseApiTest {
    @Test(groups = "api")
    public void testCreateUser() {
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");
        UserResponse user = userService.createUserExpectSuccess(
            userService.buildUniqueUserRequest());
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
    }
}
```

### Pattern 3 — Pure DB

```java
public class UserDbTest extends BaseApiTest {   // no browser needed
    @Test(groups = "db")
    public void testUserRecord() {
        dbAssert(pgDb)
            .forQuery(DbQueries.User.FIND_BY_EMAIL, "qa@myapp.in")
            .rowExists()
            .columnEquals("status", "ACTIVE")
            .columnIsNull("deleted_at")
            .validate();
    }
}
```

### Pattern 4 — API → DB → UI (Full Stack)

```java
public class FullFlowTest extends BaseHybridTest {
    @Test(groups = "fullflow")
    public void testCreateViaApi_VerifyDb_LoginUI() {
        // Phase 1: API
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");
        UserResponse user = userService.createUserExpectSuccess(
            userService.buildUniqueUserRequest());

        // Phase 2: DB — Postgres
        dbAssert(pgDb)
            .forQuery(DbQueries.User.FIND_BY_EMAIL, user.getEmail())
            .rowExists()
            .columnEquals("status", "ACTIVE")
            .validate();

        // Phase 2B: DB — Oracle audit
        if (orDb != null) {
            dbAssert(orDb)
                .forQuery(DbQueries.Audit.COUNT_EVENTS_BY_ENTITY,
                          user.getUserId(), "USER_CREATED")
                .scalarEquals(1L)
                .validate();
        }

        // Phase 3: UI
        LoginPage login = loginViaUI(ctx().getCreatedEmail(), "Test@1234");
        assertThat(login.isLogoutLinkVisible()).isTrue();
    }
}
```

### Pattern 5 — Async / Eventual Consistency

```java
// Kafka consumer writes to DB asynchronously after API returns 201
orderService.createOrder(payload);

AwaitilityUtil.waitUntilDbRowExists(pgDb,
    "SELECT COUNT(*) FROM orders WHERE user_id = ?", userId);

dbAssert(pgDb)
    .forQuery(DbQueries.Order.FIND_BY_ID, ctx().getOrderId())
    .rowExists()
    .columnEquals("status", "PENDING")
    .validate();
```

---

## DB Layer Quick Reference

### DbClient methods

| Method | Returns | Use for |
|---|---|---|
| `query(sql, mapper, params)` | `List<T>` | SELECT → multiple rows |
| `query(sql, params)` | `List<Map>` | SELECT → generic rows |
| `queryOne(sql, mapper, params)` | `Optional<T>` | SELECT → 0 or 1 row |
| `queryScalar(sql, params)` | `T` | COUNT / MAX / single value |
| `execute(sql, params)` | `int` | INSERT / UPDATE / DELETE |
| `executeAndReturnKey(sql, params)` | `Object` | INSERT + return generated PK |
| `executeBatch(sql, paramSets)` | `int[]` | Batch insert / update |
| `callProcedure(callSql, params)` | `Object` | Stored procedure / function |

### DbValidator assertions

| Method | What it checks |
|---|---|
| `rowExists()` | ≥ 1 row returned |
| `rowNotExists()` | 0 rows returned |
| `rowCountEquals(n)` | exactly n rows |
| `rowCountAtLeast(n)` | ≥ n rows |
| `columnEquals(col, val)` | first row, column = value |
| `columnNotBlank(col)` | first row, column not null/empty |
| `columnContains(col, sub)` | first row, column contains substring |
| `columnIsNull(col)` | first row, column IS NULL |
| `columnIsNotNull(col)` | first row, column IS NOT NULL |
| `scalarEquals(val)` | single-value query = val |
| `scalarGreaterThan(n)` | single-value query > n |

---

## API Layer Quick Reference

### RequestBuilder chain

```java
RequestSpecification spec = RequestBuilder.create()
    .withAuth()            // Bearer token from TestContext
    .withCountryHeader()   // X-Country-Code header
    .withQueryParam("page", 1)
    .relaxedHttps()        // ignores self-signed cert
    .build();
```

### ResponseValidator chain

```java
ResponseValidator.of(response)
    .statusCode(201)
    .hasField("data.userId")
    .fieldEquals("data.status", "ACTIVE")
    .fieldContains("data.email", "@myapp")
    .listNotEmpty("data")
    .responseTimeBelow(3000)
    .matchesSchema("schemas/user-response.json")
    .validate();
```

---

## Adding New Components

### New Page Object
```java
public class DashboardPage extends BasePage {
    @FindBy(css = ".welcome-banner")
    private WebElement welcomeBanner;

    public String getWelcomeMessage() {
        return getText(By.cssSelector(".welcome-banner"));
    }
}
```

### New API Service
```java
public class ProductService {
    public Response getProduct(String productId) {
        var spec = RequestBuilder.create().withAuth().build();
        return ApiClient.get(spec, ApiEndpoints.Product.GET_BY_ID,
                Map.of("productId", productId));
    }
}
```

### New DB Query
```java
// DbQueries.java
public static final class Product {
    public static final String FIND_BY_ID =
        "SELECT product_id, name, price FROM products WHERE product_id = ?";
}

// In test
dbAssert(pgDb)
    .forQuery(DbQueries.Product.FIND_BY_ID, productId)
    .rowExists()
    .columnEquals("price", "999.99")
    .validate();
```

### New Country
1. `Country.java` → add `SG("Singapore", "en-SG", "+65")`
2. `env-config.json` → add `"SG": { ... }` block for each env
3. Test data → add rows with `"country": "SG"`
4. Run: `mvn test -Dcountry=SG`

---

## Excel Data Format

Sheet: **LoginData**

| testCaseId | country | description | runFlag | username | password | expectedTitle | expectedUrl |
|---|---|---|---|---|---|---|---|
| TC_LOGIN_001 | IN | Valid login India | TRUE | qa@myapp.in | Test@1234 | Dashboard | /dashboard |
| TC_LOGIN_002 | US | Valid login US | TRUE | qa@myapp.us | Test@1234 | Dashboard | /dashboard |
| TC_LOGIN_003 | UK | Disabled | FALSE | skip@myapp.co.uk | Test@1234 | | |

- Row 1 = column headers (exact match with `ExcelDataReader` key names)
- `runFlag = FALSE` → automatically skipped by DataProvider
- Country filtering applied automatically based on `-Dcountry` parameter
"# SeleniumJavaAPIDB2025Claude" 
