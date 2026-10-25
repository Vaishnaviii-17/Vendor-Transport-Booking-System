
package com.vendortransport.booking.selenium;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ScreenshotOnFailureExtension
        implements AfterTestExecutionCallback {

    @Override
    public void afterTestExecution(
            ExtensionContext context
    ) throws Exception {

        if (context.getExecutionException().isEmpty()) {
            return;
        }

        Object testInstance =
                context.getRequiredTestInstance();

        if (!(testInstance instanceof VendorTransportSeleniumTest test)) {
            return;
        }

        WebDriver driver = test.getDriver();

        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            return;
        }

        Path directory = Path.of(
                "target",
                "selenium-screenshots"
        );

        Files.createDirectories(directory);

        Path source = screenshotDriver
                .getScreenshotAs(OutputType.FILE)
                .toPath();

        Path destination = directory.resolve(
                context.getRequiredTestMethod().getName()
                        + "-failure.png"
        );

        Files.copy(
                source,
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );
    }
}
