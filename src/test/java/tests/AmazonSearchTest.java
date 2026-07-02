package tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

public class AmazonSearchTest {

    WebDriver driver;

    @BeforeMethod
    public void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @Test
    public void searchPhoneAndSelectFirstProduct() {
        driver.get("https://www.demoblaze.com");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Click on "Phones" category
        WebElement phonesCategory = wait.until(
                ExpectedConditions.elementToBeClickable(By.linkText("Phones")));
        phonesCategory.click();

        // Wait for product list to be present AND stable
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".card-title a")));

        // Small buffer to let any lazy re-render settle
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String firstProductName = null;
        int attempts = 0;
        int maxAttempts = 3;

        while (attempts < maxAttempts) {
            try {
                List<WebElement> products = driver.findElements(By.cssSelector(".card-title a"));
                Assert.assertTrue(products.size() > 0, "No phone products found");

                firstProductName = products.get(0).getText();
                System.out.println("Selecting first product: " + firstProductName);

                // Re-locate right before click to avoid staleness
                WebElement firstProductFresh = driver.findElements(By.cssSelector(".card-title a")).get(0);
                wait.until(ExpectedConditions.elementToBeClickable(firstProductFresh));
                firstProductFresh.click();
                break; // success, exit retry loop
            } catch (StaleElementReferenceException e) {
                attempts++;
                System.out.println("Stale element, retrying... attempt " + attempts);
                if (attempts == maxAttempts) {
                    throw e;
                }
            }
        }

        // Verify product page loaded
        WebElement productNameHeader = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".name")));

        String openedProductName = productNameHeader.getText();
        System.out.println("Opened product page: " + openedProductName);

        Assert.assertEquals(openedProductName, firstProductName,
                "Opened product does not match selected product");
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        if (ITestResult.FAILURE == result.getStatus()) {
            try {
                File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                File dest = new File("test-output/screenshots/" + result.getName() + ".png");
                dest.getParentFile().mkdirs();
                Files.copy(src.toPath(), dest.toPath());
            } catch (Exception e) {
                System.out.println("Screenshot capture failed: " + e.getMessage());
            }
        }
        if (driver != null) {
            driver.quit();
        }
    }
}
