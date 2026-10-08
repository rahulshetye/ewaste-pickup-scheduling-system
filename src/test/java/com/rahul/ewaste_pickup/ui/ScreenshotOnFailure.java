package com.rahul.ewaste_pickup.ui;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Saves a screenshot when a test fails. This runs BEFORE @AfterEach,
 * so the browser is still open when the picture is taken.
 */
public class ScreenshotOnFailure implements TestExecutionExceptionHandler {

    private final Supplier<WebDriver> driverSupplier;

    public ScreenshotOnFailure(Supplier<WebDriver> driverSupplier) {
        this.driverSupplier = driverSupplier;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        WebDriver driver = driverSupplier.get();
        if (driver instanceof TakesScreenshot) {
            try {
                byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Path dir = Paths.get("target", "screenshots");
                Files.createDirectories(dir);
                String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                Path file = dir.resolve(context.getRequiredTestMethod().getName() + "_" + stamp + ".png");
                Files.write(file, png);
                System.out.println("SCREENSHOT SAVED: " + file.toAbsolutePath());
            } catch (Exception e) {
                System.out.println("Could not save screenshot: " + e.getMessage());
            }
        }
        throw throwable; // still mark the test as failed
    }
}
