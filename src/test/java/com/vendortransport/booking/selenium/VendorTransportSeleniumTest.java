
package com.vendortransport.booking.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
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

    private static final String CUSTOMER_URL =
            BASE_URL + "/customer";

    private static final String TEST_PASSWORD =
            "TestPassword123!";

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();

        boolean headless = Boolean.parseBoolean(
                System.getProperty("selenium.headless", "false")
        ) || "true".equalsIgnoreCase(System.getenv("CI"));

        if (headless) {
            options.addArguments("--headless=new", "--disable-gpu");
        }

        options.addArguments(
                "--window-size=1440,900",
                "--disable-notifications"
        );

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // The root URL is the public landing page.
        driver.get(BASE_URL);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Opens the customer booking and authentication page.
     */
    private void openCustomerPortal() {
        driver.get(CUSTOMER_URL);

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("register-form")
                )
        );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("login-form")
                )
        );
    }

    /**
     * Scrolls an element into view, waits for it to be clickable,
     * and clicks it using a normal Selenium mouse interaction.
     *
     * This does not bypass the UI with JavaScript click().
     */
    private void clickWhenReady(By locator) {
        WebElement element = wait.until(
                ExpectedConditions.presenceOfElementLocated(locator)
        );

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                element
        );

        wait.until(
                ExpectedConditions.elementToBeClickable(locator)
        );

        // Re-fetch the element after scrolling in case the page
        // updated or the DOM replaced the original element.
        element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(locator)
        );

        new Actions(driver)
                .moveToElement(element)
                .click()
                .perform();
    }

    /**
     * Waits for a registration request to finish successfully.
     * Uses the application's notification element without assuming
     * a particular success-message wording.
     */
private void waitForRegistrationResult() {
    try {
        wait.until(currentDriver -> {
            WebElement notification =
                    currentDriver.findElement(By.id("notification"));

            String message = notification.getText().trim();
            String notificationClass = notification.getAttribute("class");

            // Stop waiting when the application reports either outcome.
            return !message.isEmpty()
                    && (notificationClass.contains("error")
                        || notificationClass.contains("success"));
        });

        String message = driver.findElement(By.id("notification"))
                .getText().trim();

        String notificationClass = driver.findElement(By.id("notification"))
                .getAttribute("class");

        System.out.println("REGISTRATION MESSAGE: " + message);
        System.out.println("NOTIFICATION CLASS: " + notificationClass);

        assertFalse(
                notificationClass.contains("error"),
                "Registration failed: " + message
        );

        assertTrue(
                driver.findElement(By.id("register-name"))
                        .getAttribute("value").isEmpty(),
                "Registration notification appeared, but the form was not reset."
        );

    } catch (org.openqa.selenium.TimeoutException exception) {
        System.out.println("CURRENT URL: " + driver.getCurrentUrl());
        System.out.println("REGISTER NAME: "
                + driver.findElement(By.id("register-name")).getAttribute("value"));
        System.out.println("REGISTER EMAIL: "
                + driver.findElement(By.id("register-email")).getAttribute("value"));
        System.out.println("LOGIN EMAIL: "
                + driver.findElement(By.id("login-email")).getAttribute("value"));
        System.out.println("NOTIFICATION: "
                + driver.findElement(By.id("notification")).getText());
        System.out.println("REGISTER BUTTON ENABLED: "
                + driver.findElement(By.id("register-button")).isEnabled());

        throw exception;
    }
}
    @Test
    void shouldOpenHomepage() {
        wait.until(
                ExpectedConditions.urlToBe(BASE_URL + "/")
        );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.tagName("body")
                )
        );

        String pageSource = driver.getPageSource();

        assertTrue(
                pageSource.contains("Better journeys.")
                        && pageSource.contains("Smarter"),
                "Landing page should contain its current main heading"
        );

        // The landing page should link to the customer portal.
        assertTrue(
                driver.findElements(
                        By.cssSelector("a[href='/customer#login-section']")
                ).size() > 0
                        || driver.findElements(
                        By.cssSelector("a[href='/customer']")
                ).size() > 0,
                "Landing page should provide a link to the customer portal"
        );
    }

    @Test
    void shouldRegisterCustomer() {
        openCustomerPortal();

        String email = uniqueEmail();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("register-name")
                )
        ).sendKeys("Selenium Test User");

        driver.findElement(By.id("register-email"))
                .sendKeys(email);

        driver.findElement(By.id("register-password"))
                .sendKeys(TEST_PASSWORD);

        clickWhenReady(By.id("register-button"));

        waitForRegistrationResult();

        assertEquals(
                email,
                driver.findElement(By.id("login-email"))
                        .getAttribute("value"),
                "Successful registration should populate the login email"
        );
    }

    @Test
    void shouldLoginAndLogout() {
        String email = registerNewCustomer();

        login(email, TEST_PASSWORD);

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("login-status"),
                        "Signed in as"
                )
        );

        assertTrue(
                driver.findElement(By.id("logout-button")).isDisplayed(),
                "Logout button should be visible after login"
        );

        clickWhenReady(By.id("logout-button"));

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("login-status"),
                        "Not signed in"
                )
        );

        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        By.id("logout-button")
                )
        );
    }

    @Test
    void shouldSearchAvailability() {
        String email = registerNewCustomer();

        login(email, TEST_PASSWORD);

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("login-status"),
                        "Signed in as"
                )
        );

        assertTrue(
                driver.findElement(By.id("login-status"))
                        .getText()
                        .contains("Signed in as"),
                "The authenticated user should have the CUSTOMER role"
        );

        By dateLocator = By.id("search-date");
        By searchButtonLocator = By.id("search-button");
        By resultsLocator = By.id("transport-results");

        WebElement dateInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(dateLocator)
        );

        // Use tomorrow's date so the test stays valid as the calendar advances.
        String searchDate = LocalDate.now()
                .plusDays(1)
                .toString();

        // Set the date and dispatch events so the browser and frontend
        // receive the updated value.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                        "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                dateInput,
                searchDate
        );

        clickWhenReady(searchButtonLocator);

        try {
            wait.until(currentDriver -> {
                String results = currentDriver.findElement(resultsLocator)
                        .getText();

                String notification = currentDriver
                        .findElement(By.id("notification"))
                        .getText();

                return !results.isBlank()
                        || notification.toLowerCase().contains("matching journey")
                        || notification.toLowerCase().contains("error")
                        || notification.toLowerCase().contains("failed");
            });
        } catch (org.openqa.selenium.TimeoutException exception) {
            System.out.println("CURRENT URL: " + driver.getCurrentUrl());

            System.out.println(
                    "LOGIN STATUS: "
                            + driver.findElement(By.id("login-status"))
                            .getText()
            );

            System.out.println(
                    "NOTIFICATION: "
                            + driver.findElement(By.id("notification"))
                            .getText()
            );

            System.out.println(
                    "TRANSPORT RESULTS: "
                            + driver.findElement(resultsLocator).getText()
            );

            throw exception;
        }

        String results = driver.findElement(resultsLocator).getText();

        assertFalse(
                results.isBlank(),
                "Availability search should display results or an empty-state message"
        );

        assertFalse(
                results.toLowerCase().contains("search could not be completed"),
                "Availability search should not end in an application error"
        );
    }

    @Test
    void shouldLoadBookingHistory() {
        String email = registerNewCustomer();

        login(email, TEST_PASSWORD);

        clickWhenReady(By.id("history-button"));

        wait.until(currentDriver -> {
            String results = currentDriver.findElement(
                    By.id("booking-history")
            ).getText();

            return results.contains("Booking ID")
                    || results.contains("No bookings found");
        });

        String results = driver.findElement(
                By.id("booking-history")
        ).getText();

        assertFalse(
                results.isBlank(),
                "Booking history should display bookings or an empty-state message"
        );
    }

    /**
     * Registers a fresh customer for tests that need authentication.
     */
    private String registerNewCustomer() {
        openCustomerPortal();

        String email = uniqueEmail();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("register-name")
                )
        ).sendKeys("Selenium Customer");

        driver.findElement(By.id("register-email"))
                .sendKeys(email);

        driver.findElement(By.id("register-password"))
                .sendKeys(TEST_PASSWORD);

        clickWhenReady(By.id("register-button"));

        waitForRegistrationResult();

        assertEquals(
                email,
                driver.findElement(By.id("login-email"))
                        .getAttribute("value"),
                "Successful registration should populate the login email"
        );

        return email;
    }

    /**
     * Logs in with the supplied customer credentials.
     */
    private void login(String email, String password) {
        // Registration and login are both on the customer portal.
        if (!driver.getCurrentUrl().startsWith(CUSTOMER_URL)) {
            openCustomerPortal();
        }

        WebElement emailInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("login-email")
                )
        );

        emailInput.clear();
        emailInput.sendKeys(email);

        WebElement passwordInput = driver.findElement(
                By.id("login-password")
        );

        passwordInput.clear();
        passwordInput.sendKeys(password);

        clickWhenReady(By.id("login-button"));

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("login-status"),
                        "Signed in as"
                )
        );
    }

    private String uniqueEmail() {
        return "selenium-"
                + java.util.UUID.randomUUID()
                + "@example.com";
    }

    WebDriver getDriver() {
        return driver;
    }
}
