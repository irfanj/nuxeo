/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.webengine.test.web;

import java.io.File;
import java.util.ResourceBundle;

import org.openqa.selenium.Speed;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

public class StandardBrowserConfig implements BrowserConfig {

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (driver != null)
                    driver.quit();
            }
        });
    }

    protected final String host = System.getProperty("nuxeo.deploy.host",
            "localhost");

    protected final String port = System.getProperty("nuxeo.deploy.port",
            "8080");

    protected final BrowserFamily browser = Enum.valueOf(BrowserFamily.class,
            System.getProperty("webdriver.browser", "HU"));

    enum BrowserFamily {
        FF, IE, HU;

        public WebDriver getDriver() {
            switch (this) {
            case FF:
                return makeFirefoxDriver();
            case IE:
                return makeIEDriver();
            default:
                return makeHtmlunitDriver();
            }
        };

        private WebDriver makeFirefoxDriver() {
            String Xport = System.getProperty("nuxeo.xvfb.id", ":0");
            File firefoxPath = new File(System.getProperty("firefox.path",
                    "/usr/bin/firefox"));
            FirefoxBinary firefox = new FirefoxBinary(firefoxPath);
            firefox.setEnvironmentProperty("DISPLAY", Xport);
            WebDriver driver = new FirefoxDriver(firefox, null);
            driver.setVisible(false);
            driver.manage().setSpeed(Speed.FAST);
            return driver;
        }

        private InternetExplorerDriver makeIEDriver() {
            InternetExplorerDriver driver = new InternetExplorerDriver();
            driver.setVisible(true);
            driver.manage().setSpeed(Speed.FAST);
            return driver;
        }

        private HtmlUnitDriver makeHtmlunitDriver() {
            HtmlUnitDriver driver = new HtmlUnitDriver();
            driver.setJavascriptEnabled(true);
            return driver;
        }
    };

    // keep one driver for all tests - UGLY
    private static WebDriver driver;

    protected ResourceBundle messages;

    public WebDriver getTestDriver() {
        if (driver == null) {
            driver = browser.getDriver();
        }
        return driver;
    }

    public String getBrowser() {
        return browser.toString();
    }

    public void resetDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

}