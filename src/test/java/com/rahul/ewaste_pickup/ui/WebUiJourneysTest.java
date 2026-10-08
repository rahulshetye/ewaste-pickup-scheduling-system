package com.rahul.ewaste_pickup.ui;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI journeys for the E-Waste Pickup Scheduler.
 * Needs the app running. If it is not reachable, the tests are skipped
 * (so a plain "mvn test" in the Jenkins build stage does not break).
 */
class WebUiJourneysTest {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    private WebDriver driver;
    private WebDriverWait wait;

    @RegisterExtension
    ScreenshotOnFailure screenshotOnFailure = new ScreenshotOnFailure(() -> driver);

    @BeforeAll
    static void appMustBeRunning() {
        boolean up;
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(BASE_URL).openConnection();
            c.setConnectTimeout(2000);
            c.setReadTimeout(2000);
            up = c.getResponseCode() < 500;
        } catch (Exception e) {
            up = false;
        }
        if (Boolean.getBoolean("requireApp")) {
            assertTrue(up, "App not reachable at " + BASE_URL);
        } else {
            Assumptions.assumeTrue(up, "App not reachable at " + BASE_URL + " - skipping UI tests");
        }
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        String chromeBinary = System.getProperty("chromeBinary");
        if (chromeBinary != null && !chromeBinary.isBlank()) {
            options.setBinary(chromeBinary);
        }
        if (Boolean.getBoolean("headless")) {
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        }
        options.addArguments("--window-size=1280,1000");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(BASE_URL);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ---------- helpers ----------

    private String uniqueEmail() {
        return "user" + System.nanoTime() + "@example.com";
    }

    private String uniqueArea() {
        return "Area" + System.nanoTime();
    }

    private void type(String id, String text) {
        WebElement field = driver.findElement(By.id(id));
        field.clear();
        field.sendKeys(text);
    }

    private void clickRegister(String name, String email) {
        type("reg-name", name);
        type("reg-email", email);
        type("reg-password", "Passw0rd!");
        driver.findElement(By.id("reg-submit")).click();
    }

    /** Waits until the message box has any text, then returns it. */
    private String waitForMessage(String id) {
        wait.until(d -> !d.findElement(By.id(id)).getText().isBlank());
        return driver.findElement(By.id(id)).getText();
    }

    private void registerAndWait(String name, String email) {
        clickRegister(name, email);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("reg-message"), "Registered successfully"));
    }

    private void createSlot(String area) {
        type("slot-date", "2026-12-01");
        type("slot-start", "09:00");
        type("slot-end", "11:00");
        type("slot-area", area);
        driver.findElement(By.id("slot-submit")).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("slot-message"), "Slot created in " + area));
    }

    /** Refreshes the slots table and returns the numeric slot id for the area. */
    private String findSlotId(String area) {
        driver.findElement(By.id("refresh-slots")).click();
        By row = By.xpath("//tbody[@id='slots-body']//td[@class='slot-area' and text()='" + area + "']/..");
        WebElement tr = wait.until(ExpectedConditions.presenceOfElementLocated(row));
        return tr.getAttribute("id").replace("slot-", "");
    }

    private String bookSlot(String slotId) {
        driver.findElement(By.id("book-" + slotId)).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("slots-message"), "CONFIRMED"));
        Matcher m = Pattern.compile("Booking (\\d+) CONFIRMED")
                .matcher(driver.findElement(By.id("slots-message")).getText());
        assertTrue(m.find(), "Could not read booking id from slots message");
        return m.group(1);
    }

    // ---------- journeys ----------

    @Test
    @DisplayName("UI-01 Registration succeeds")
    void ui01_registration() {
        clickRegister("Test User", uniqueEmail());
        String msg = waitForMessage("reg-message");

        assertTrue(msg.contains("Registered successfully"), "Unexpected message: " + msg);
        assertTrue(driver.findElement(By.id("reg-message")).getAttribute("class").contains("ok"));
        assertTrue(driver.findElement(By.id("current-user")).getText().contains("Signed in as Test User"));
    }

    @Test
    @DisplayName("UI-02 Duplicate registration is rejected")
    void ui02_duplicateRegistration() {
        String email = uniqueEmail();
        registerAndWait("Dup User", email);

        // second attempt with the same email
        driver.findElement(By.id("reg-message")).getText(); // current success text
        clickRegister("Dup User", email);
        wait.until(d -> d.findElement(By.id("reg-message")).getAttribute("class").contains("err"));

        String msg = driver.findElement(By.id("reg-message")).getText();
        assertFalse(msg.contains("Registered successfully"), "Duplicate was accepted: " + msg);
    }

    @Test
    @DisplayName("UI-03 Admin creates a pickup slot")
    void ui03_slotCreation() {
        String area = uniqueArea();
        createSlot(area);

        assertTrue(driver.findElement(By.id("slot-message")).getAttribute("class").contains("ok"));
        String slotId = findSlotId(area);
        assertFalse(slotId.isBlank());
        assertTrue(driver.findElement(By.id("slot-" + slotId)).isDisplayed());
    }

    @Test
    @DisplayName("UI-04 User books a slot")
    void ui04_booking() {
        String area = uniqueArea();
        registerAndWait("Booker", uniqueEmail());
        createSlot(area);
        String slotId = findSlotId(area);

        String bookingId = bookSlot(slotId);

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("slot-" + slotId)));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("#booking-" + bookingId + " .status"), "CONFIRMED"));
    }

    @Test
    @DisplayName("UI-05 User cancels a booking and the slot returns")
    void ui05_cancellation() {
        String area = uniqueArea();
        registerAndWait("Canceller", uniqueEmail());
        createSlot(area);
        String slotId = findSlotId(area);
        String bookingId = bookSlot(slotId);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("slot-" + slotId)));

        driver.findElement(By.id("cancel-" + bookingId)).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("#booking-" + bookingId + " .status"), "CANCELLED"));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("cancel-" + bookingId)));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("slot-" + slotId)));
    }
}
