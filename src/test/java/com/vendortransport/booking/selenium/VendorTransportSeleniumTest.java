
package com.vendortransport.booking.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(ScreenshotOnFailureExtension.class)

class VendorTransportSeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String BASE_URL =
            System.getProperty(
                    "selenium.baseUrl",
                    "http://localhost:8083"
            );

    private static final Path SCREENSHOT_DIRECTORY =
            Path.of("target", "selenium-screenshots");

    @BeforeEach
    void setUp() {

        ChromeOptions options = new ChromeOptions();

        if (Boolean.parseBoolean(
                System.getProperty("selenium.headless", "false")
        )) {
            options.addArguments("--headless=new");
        }

        options.addArguments(
                "--window-size=1440,900",
                "--disable-notifications"
        );

        driver = new ChromeDriver(options);

        wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );

        driver.get(BASE_URL);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {

        if (driver != null) {

            // Screenshots are also saved on failure through
            // the JUnit TestWatcher extension below.
            driver.quit();
        }
    }

    @Test
    void shouldOpenHomepage() {

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("register-form")
                )
        );

        assertTrue(
                driver.getPageSource()
                        .contains("Reliable Transport Booking")
        );

        assertTrue(
                driver.findElement(
                        By.id("login-form")
                ).isDisplayed()
        );
    }

    @Test
    void shouldRegisterCustomer() {

        String email = uniqueEmail();

        driver.findElement(By.id("register-name"))
                .sendKeys("Selenium Test User");

        driver.findElement(By.id("register-email"))
                .sendKeys(email);

        driver.findElement(By.id("register-password"))
                .sendKeys("TestPassword123!");

        driver.findElement(By.id("register-button"))
                .click();

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("notification"),
                        "Customer registered successfully"
                )
        );

        assertEquals(
                email,
                driver.findElement(
                        By.id("login-email")
                ).getAttribute("value")
        );
    }

    @Test
    void shouldLoginAndLogout() {

        String email = registerNewCustomer();

        login(email, "TestPassword123!");

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("login-status"),
                        "Signed in as"
                )
        );

        assertTrue(
                driver.findElement(
                        By.id("logout-button")
                ).isDisplayed()
        );

        driver.findElement(
                By.id("logout-button")
        ).click();

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("login-status"),
                        "Not signed in"
                )
        );

        assertFalse(
                driver.findElement(
                        By.id("logout-button")
                ).isDisplayed()
        );
    }

    @Test
    void shouldSearchAvailability() {

        String email = registerNewCustomer();
        login(email, "TestPassword123!");

        driver.findElement(
                By.id("availability-button")
        ).click();

        wait.until(driver -> {

            String results = driver.findElement(
                    By.id("availability-results")
            ).getText();

            return results.contains("Availability Slot ID")
                    || results.contains("No availability found");
        });

        String results = driver.findElement(
                By.id("availability-results")
        ).getText();

        assertFalse(results.isBlank());
    }

    @Test
    void shouldLoadBookingHistory() {

        String email = registerNewCustomer();
        login(email, "TestPassword123!");

        driver.findElement(
                By.id("history-button")
        ).click();

        wait.until(driver -> {

            String results = driver.findElement(
                    By.id("booking-history")
            ).getText();

            return results.contains("Booking ID")
                    || results.contains("No bookings found");
        });

        String results = driver.findElement(
                By.id("booking-history")
        ).getText();

        assertFalse(results.isBlank());
    }

    private String registerNewCustomer() {

        String email = uniqueEmail();

        driver.findElement(By.id("register-name"))
                .sendKeys("Selenium Customer");

        driver.findElement(By.id("register-email"))
                .sendKeys(email);

        driver.findElement(By.id("register-password"))
                .sendKeys("TestPassword123!");

        driver.findElement(By.id("register-button"))
                .click();

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("notification"),
                        "Customer registered successfully"
                )
        );

        return email;
    }

    private void login(String email, String password) {

        driver.findElement(By.id("login-email"))
                .clear();

        driver.findElement(By.id("login-email"))
                .sendKeys(email);

        driver.findElement(By.id("login-password"))
                .sendKeys(password);

        driver.findElement(By.id("login-button"))
                .click();

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("login-status"),
                        "Signed in as"
                )
        );
    }

    private String uniqueEmail() {

        return "selenium-" +
                UUID.randomUUID().toString().substring(0, 12) +
                "@example.com";
    }
    WebDriver getDriver() {
    return driver;
    }
}

