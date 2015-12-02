package slave;

import io.appium.java_client.ios.IOSDriver;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SingleTest {

	private static final String IMG_LOC = "imgs/";
	private static final String CONFIG_LOC = "json/";

	private static WebDriver driver;

	private static int imgCount;

	/**
	 * Instantiates the {@link #driver} instance by using DesiredCapabilities
	 * which specify the 'iPhone Simulator' device and 'safari' app.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.out.println("Setting up image directory");
		imgCount = Integer.parseInt(System.getProperty("image_count"));
		setupDir(IMG_LOC);
		setupDir(CONFIG_LOC);
		System.out.println("Configuring Tests");
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability("deviceName", "iPhone 5s");
		capabilities.setCapability("platformName", "iOS");
		capabilities.setCapability("platformVersion", "9.1");
		capabilities.setCapability("browserName", "safari");
		System.out.println("Requesting Device");
		driver = new IOSDriver<WebElement>(
				new URL("http://0.0.0.0:4723/wd/hub"), capabilities);
		System.out.println("Completed Configurations");
	}

	/**
	 * Navigates to http://saucelabs.com/test/guinea-pig and interacts with the
	 * browser.
	 * 
	 * @throws Exception
	 */
	@Test
	public void runTest() throws Exception {
		System.out.println("Running Tests");
		System.out.println(System.getProperty("url"));
		driver.get(System.getProperty("url"));
		Thread.sleep(2000);
		getScreenshot(driver);
		System.out.println("Completed Tests");
	}

	/**
	 * Closes the {@link #driver} instance.
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		driver.quit();
	}

	public static void getScreenshot(WebDriver driver) throws IOException {
		System.out.println("Capturing the snapshot of the page ");
		File srcFiler = ((TakesScreenshot) driver)
				.getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(srcFiler,
				new File(IMG_LOC + Integer.toString(imgCount) + ".jpg"));
		imgCount++;
	}

	private static void setupDir(String loc) throws IOException {
		File file = new File(loc);
		if (!file.exists()) {
			file.mkdir();
		} else {
			FileUtils.cleanDirectory(file);
		}
	}
}