package com.rahul.ewaste_pickup;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.junit.jupiter.api.Test;

public class SmokeTest {
  @Test
  void chromeOpens() {
    WebDriverManager.chromedriver().setup();
    WebDriver driver = new ChromeDriver();
    driver.get("https://example.com");
    System.out.println(driver.getTitle());
    driver.quit();
  }
}