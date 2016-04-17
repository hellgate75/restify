package com.service.restfy.selenium.server.automated;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

import com.service.restfy.selenium.server.automated.WebDriveFactory.SELECTOR_TYPE;
import com.service.restfy.selenium.server.cases.TestEngine;
import com.service.restfy.selenium.server.exceptions.FrameworkException;
import com.service.restfy.selenium.server.exceptions.NotFoundException;

public class SeleniumAutomatedServer {
	static {
		if (System.getProperty("log4j.configurationFile")==null)
			System.setProperty("log4j.configurationFile", "log4j2.xml");
	}
	private static Logger logger = LoggerFactory.getLogger("com.service.restfy.selenium.server");
	private static final String logginPrefix = "Selenium Automated Server : ";
	
	private final TestEngine testEngine = new TestEngine();
	private Properties engineProperties = new Properties();
	private WebDriverSelector driverSelector = null;
	private boolean loggingActive = true;

	public SeleniumAutomatedServer() {
		super();
	}

	public void readConfig(String filePath) throws NotFoundException, FrameworkException{
		try {
			engineProperties.load(new FileInputStream(filePath));
		} catch (FileNotFoundException e) {
			throw new NotFoundException(logginPrefix+"Unable to locate config file " + filePath + " due to : ", e);
		} catch (IOException e) {
			throw new FrameworkException(logginPrefix+"Unable to read config file " + filePath + " due to : ", e);
		} catch (Throwable e) {
			throw new FrameworkException(logginPrefix+"Unable to read config file " + filePath + " due to : ", e);
		}
	}

	public void readConfigXml(String filePath) throws NotFoundException, FrameworkException{
		try {
			engineProperties.loadFromXML(new FileInputStream(filePath));
		} catch (FileNotFoundException e) {
			throw new NotFoundException(logginPrefix+"Unable to locate xml config file " + filePath + " due to : ", e);
		} catch (IOException e) {
			throw new FrameworkException(logginPrefix+"Unable to read xml config file " + filePath + " due to : ", e);
		} catch (Throwable e) {
			throw new FrameworkException(logginPrefix+"Unable to read xml config file " + filePath + " due to : ", e);
		}
	}
	
	public void startTests() throws FrameworkException {
		if (engineProperties.size()==0) {
			throw new FrameworkException(logginPrefix+"Configuration not loaded correctly ...");
		}
		try {
			engineProperties.store(System.out, "No comment");
		} catch (IOException e2) {
		}
		if (!engineProperties.containsKey(SeleniumServerConstants.driverSelector)) {
			throw new FrameworkException(logginPrefix+"Driver Selector not found ...");
		}
		List<Object> parameters = new ArrayList<Object>(0);
		SELECTOR_TYPE selector = null;
		try {
			selector = SELECTOR_TYPE.valueOf(engineProperties.getProperty(SeleniumServerConstants.driverSelector));
			if (engineProperties.containsKey(SeleniumServerConstants.driverSubSelector)) {
				parameters.add(SELECTOR_TYPE.valueOf(engineProperties.getProperty(SeleniumServerConstants.driverSubSelector)));
			}
			if (engineProperties.containsKey(SeleniumServerConstants.driverSubDriver)) {
				parameters.add(Class.forName(engineProperties.getProperty(SeleniumServerConstants.driverSubSelector)).newInstance());
			}
			if (engineProperties.containsKey(SeleniumServerConstants.driverService)) {
				parameters.add(Class.forName(engineProperties.getProperty(SeleniumServerConstants.driverService)).newInstance());
			}
			if (engineProperties.containsKey(SeleniumServerConstants.driverCommander)) {
				parameters.add(Class.forName(engineProperties.getProperty(SeleniumServerConstants.driverCommander)).newInstance());
			}
			if (engineProperties.containsKey(SeleniumServerConstants.driverCapabilities)) {
				String capabilitiesMethod = engineProperties.getProperty(SeleniumServerConstants.driverCapabilities);
				Capabilities capabilities = (Capabilities)(DesiredCapabilities.class.getDeclaredMethod(capabilitiesMethod, Void.class)).invoke(null);
				parameters.add(capabilities);
			}
		} catch (Throwable e) {
			throw new FrameworkException(logginPrefix+"Unable to run the server due to : ", e);
		}
		
		try {
			if (engineProperties.containsKey(SeleniumServerConstants.testCaseClasses)) {
				String[] classes = engineProperties.getProperty(SeleniumServerConstants.testCaseClasses).split(",");
				for (String className: classes) {
					this.testEngine.addCaseByClassName(className);
				}
			}
			if (engineProperties.containsKey(SeleniumServerConstants.testCasePackages)) {
				String[] packages = engineProperties.getProperty(SeleniumServerConstants.testCasePackages).split(",");
				for (String packageName: packages) {
					this.testEngine.addCaseByPackageName(packageName);;
				}
			}
		} catch (Throwable e1) {
			throw new FrameworkException(logginPrefix+"Unable to load the test cases due to : ", e1);
		}
		
		
		try {
			driverSelector = WebDriveFactory.getInstance().getDriverSelector(selector, parameters.toArray());
		} catch (Throwable e) {
			throw new FrameworkException(logginPrefix+"Unable to run the web driver due to : ", e);
		}
		
		try {
			testEngine.setWebDriver(driverSelector.getWebDriver());
			testEngine.run();
		} catch (Throwable e) {
			throw new FrameworkException(logginPrefix+"Unable to run the web driver due to : ", e);
		}
		finally {
			if (driverSelector!=null)
				driverSelector.stopWebDriver();
		}
		testEngine.report(System.out);
	}
	
}