/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * 717 Martin Luther King Dr W Ste I, Cincinnati, Ohio 45220
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License version 3
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * version 3 along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 */
package org.openqa.selenium.remote;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Keyboard;
import org.openqa.selenium.interactions.Mouse;
import org.openqa.selenium.interactions.internal.Coordinates;

import com.screenslicer.common.Log;

public class BrowserDriver implements WebDriver {
  public static class Retry extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Retry() {
      super();
    }

    public Retry(Throwable nested) {
      super(nested);
    }

    public Retry(String message) {
      super(message);
    }
  }

  private static final int RETRY_WAIT = 30000;
  private static final int RETRIES = 6;

  private static interface Executor {
    Object perform();
  }

  private static Object exec(Executor action) {
    Throwable throwable = null;
    for (int i = 0; i < RETRIES; i++) {
      try {
        return action.perform();
      } catch (UnreachableBrowserException e) {
        throwable = e;
      } catch (TimeoutException e) {
        Log.exception(e);
        throw e;
      } catch (Throwable t) {
        Log.exception(t);
        throw t;
      }
      try {
        Thread.sleep(RETRY_WAIT);
      } catch (InterruptedException e) {}
    }
    Log.exception(throwable);
    throw new Retry(throwable);
  }

  private static List<WebElement> convert(List<WebElement> elements) {
    List<WebElement> myElements = new ArrayList<WebElement>();
    for (WebElement element : elements) {
      myElements.add(new MyElement((RemoteWebElement) element));
    }
    return myElements;
  }

  private final FirefoxDriver firefoxDriver;
  private final RemoteWebDriver remoteWebDriver;

  public BrowserDriver(FirefoxProfile profile) {
    firefoxDriver = new FirefoxDriver(profile);
    remoteWebDriver = firefoxDriver;
  }

  public BrowserDriver(BrowserDriver driver) {
    remoteWebDriver = new RemoteWebDriver(driver.firefoxDriver.getCommandExecutor(),
        driver.firefoxDriver.getCapabilities(), driver.firefoxDriver.getCapabilities());
    firefoxDriver = driver.firefoxDriver;
  }

  public void kill() {
    firefoxDriver.kill();
  }

  public Actions actions() {
    return new MyActions(this);
  }

  public void close() {
    exec(new Executor() {
      @Override
      public Object perform() {
        remoteWebDriver.close();
        return null;
      }
    });
  }

  protected Response execute(final String driverCommand, final Map<String, ?> parameters) {
    return (Response) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.execute(driverCommand, parameters);
      }
    });
  }

  protected Response execute(final String command) {
    return (Response) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.execute(command);
      }
    });
  }

  public Object executeAsyncScript(final String script, final Object... args) {
    return exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.executeAsyncScript(script, args);
      }
    });
  }

  public Object executeScript(final String script, final Object... args) {
    return exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.executeScript(script, args);
      }
    });
  }

  public WebElement findElement(final By by) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElement(by);
      }
    }));
  }

  protected WebElement findElement(final String by, final String using) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElement(by, using);
      }
    }));
  }

  public WebElement findElementByClassName(final String using) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementByClassName(using);
      }
    }));
  }

  public WebElement findElementByCssSelector(final String using) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementByCssSelector(using);
      }
    }));
  }

  public WebElement findElementById(final String using) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementById(using);
      }
    }));
  }

  public WebElement findElementByLinkText(final String using) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementByLinkText(using);
      }
    }));
  }

  public WebElement findElementByName(final String using) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementByName(using);
      }
    }));
  }

  public WebElement findElementByPartialLinkText(final String using) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementByPartialLinkText(using);
      }
    }));
  }

  public WebElement findElementByTagName(final String using) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementByTagName(using);
      }
    }));
  }

  public WebElement findElementByXPath(final String using) {
    return new MyElement((RemoteWebElement) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementByXPath(using);
      }
    }));
  }

  public List<WebElement> findElements(final By by) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElements(by);
      }
    }));
  }

  protected List<WebElement> findElements(final String by, final String using) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElements(by, using);
      }
    }));
  }

  public List<WebElement> findElementsByClassName(final String using) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementsByClassName(using);
      }
    }));
  }

  public List<WebElement> findElementsByCssSelector(final String using) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementsByCssSelector(using);
      }
    }));
  }

  public List<WebElement> findElementsById(final String using) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementsById(using);
      }
    }));
  }

  public List<WebElement> findElementsByLinkText(final String using) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementsByLinkText(using);
      }
    }));
  }

  public List<WebElement> findElementsByName(final String using) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementsByName(using);
      }
    }));
  }

  public List<WebElement> findElementsByPartialLinkText(final String using) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementsByPartialLinkText(using);
      }
    }));
  }

  public List<WebElement> findElementsByTagName(final String using) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementsByTagName(using);
      }
    }));
  }

  public List<WebElement> findElementsByXPath(final String using) {
    return convert((List<WebElement>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.findElementsByXPath(using);
      }
    }));
  }

  public void get(final String url) {
    exec(new Executor() {
      @Override
      public Object perform() {
        remoteWebDriver.get(url);
        return null;
      }
    });
  }

  public String getCurrentUrl() {
    return (String) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.getCurrentUrl();
      }
    });
  }

  public Keyboard getKeyboard() {
    return new MyKeyboard((RemoteKeyboard) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.getKeyboard();
      }
    }));
  }

  public Mouse getMouse() {
    return new MyMouse((RemoteMouse) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.getMouse();
      }
    }));
  }

  public String getPageSource() {
    return (String) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.getPageSource();
      }
    });
  }

  public String getTitle() {
    return (String) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.getTitle();
      }
    });
  }

  public String getWindowHandle() {
    return (String) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.getWindowHandle();
      }
    });
  }

  public Set<String> getWindowHandles() {
    return (Set<String>) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.getWindowHandles();
      }
    });
  }

  public Navigation navigate() {
    return new MyNav((Navigation) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.navigate();
      }
    }));
  }

  public void quit() {
    exec(new Executor() {
      @Override
      public Object perform() {
        remoteWebDriver.quit();
        return null;
      }
    });
  }

  public TargetLocator switchTo() {
    return new MyLoc((TargetLocator) exec(new Executor() {
      @Override
      public Object perform() {
        return remoteWebDriver.switchTo();
      }
    }), this);
  }

  public Options manage() {
    return remoteWebDriver.manage();
  }

  private static class MyElement extends RemoteWebElement {
    private final RemoteWebElement element;

    public MyElement(RemoteWebElement element) {
      super.fileDetector = element.fileDetector;
      super.id = element.id;
      super.mouse = element.mouse;
      super.parent = element.parent;
      this.element = element;
    }

    @Override
    public void clear() {
      exec(new Executor() {
        @Override
        public Object perform() {
          element.clear();
          return null;
        }
      });
    }

    @Override
    public void click() {
      exec(new Executor() {
        @Override
        public Object perform() {
          element.click();
          return null;
        }
      });
    }

    @Override
    public WebElement findElement(final By arg0) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElement(arg0);
        }
      }));
    }

    @Override
    public List<WebElement> findElements(final By arg0) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElements(arg0);
        }
      }));
    }

    @Override
    public String getAttribute(final String arg0) {
      return (String) exec(new Executor() {
        @Override
        public Object perform() {
          return element.getAttribute(arg0);
        }
      });
    }

    @Override
    public String getCssValue(final String arg0) {
      return (String) exec(new Executor() {
        @Override
        public Object perform() {
          return element.getCssValue(arg0);
        }
      });
    }

    @Override
    public Point getLocation() {
      return (Point) exec(new Executor() {
        @Override
        public Object perform() {
          return element.getLocation();
        }
      });
    }

    @Override
    public Dimension getSize() {
      return (Dimension) exec(new Executor() {
        @Override
        public Object perform() {
          return element.getSize();
        }
      });
    }

    @Override
    public String getTagName() {
      return (String) exec(new Executor() {
        @Override
        public Object perform() {
          return element.getTagName();
        }
      });
    }

    @Override
    public String getText() {
      return (String) exec(new Executor() {
        @Override
        public Object perform() {
          return element.getText();
        }
      });
    }

    @Override
    public boolean isDisplayed() {
      return (Boolean) exec(new Executor() {
        @Override
        public Object perform() {
          return element.isDisplayed();
        }
      });
    }

    @Override
    public boolean isEnabled() {
      return (Boolean) exec(new Executor() {
        @Override
        public Object perform() {
          return element.isEnabled();
        }
      });
    }

    @Override
    public boolean isSelected() {
      return (Boolean) exec(new Executor() {
        @Override
        public Object perform() {
          return element.isSelected();
        }
      });
    }

    @Override
    public void sendKeys(final CharSequence... arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          element.sendKeys(arg0);
          return null;
        }
      });
    }

    @Override
    public void submit() {
      exec(new Executor() {
        @Override
        public Object perform() {
          element.submit();
          return null;
        }
      });
    }

    @Override
    protected Response execute(final String command, final Map<String, ?> parameters) {
      return (Response) exec(new Executor() {
        @Override
        public Object perform() {
          return element.execute(command, parameters);
        }
      });
    }

    @Override
    protected WebElement findElement(final String using, final String value) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElement(using, value);
        }
      }));
    }

    @Override
    public WebElement findElementByClassName(final String using) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementByClassName(using);
        }
      }));
    }

    @Override
    public WebElement findElementByCssSelector(final String using) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementByCssSelector(using);
        }
      }));
    }

    @Override
    public WebElement findElementById(final String using) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementById(using);
        }
      }));
    }

    @Override
    public WebElement findElementByLinkText(final String using) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementByLinkText(using);
        }
      }));
    }

    @Override
    public WebElement findElementByName(final String using) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementByName(using);
        }
      }));
    }

    @Override
    public WebElement findElementByPartialLinkText(final String using) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementByPartialLinkText(using);
        }
      }));
    }

    @Override
    public WebElement findElementByTagName(final String using) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementByTagName(using);
        }
      }));
    }

    @Override
    public WebElement findElementByXPath(final String using) {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementByXPath(using);
        }
      }));
    }

    @Override
    protected List<WebElement> findElements(final String using, final String value) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElements(using, value);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByClassName(final String using) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByClassName(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByCssSelector(final String using) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByCssSelector(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsById(final String using) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementsById(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByLinkText(final String using) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByLinkText(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByName(final String using) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByName(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByPartialLinkText(final String using) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByPartialLinkText(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByTagName(final String using) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByTagName(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByXPath(final String using) {
      return convert((List<WebElement>) exec(new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByXPath(using);
        }
      }));
    }

    @Override
    public Coordinates getCoordinates() {
      return (Coordinates) exec(new Executor() {
        @Override
        public Object perform() {
          return element.getCoordinates();
        }
      });
    }

    @Override
    public String getId() {
      return (String) exec(new Executor() {
        @Override
        public Object perform() {
          return element.getId();
        }
      });
    }

    @Override
    public WebDriver getWrappedDriver() {
      return (WebDriver) exec(new Executor() {
        @Override
        public Object perform() {
          return element.getWrappedDriver();
        }
      });
    }

    @Override
    public void setFileDetector(FileDetector detector) {
      element.setFileDetector(detector);
    }

    @Override
    protected void setFoundBy(SearchContext foundFrom, String locator, String term) {
      element.setFoundBy(foundFrom, locator, term);
    }

    @Override
    public void setId(String id) {
      element.setId(id);
    }

    @Override
    public void setParent(RemoteWebDriver parent) {
      element.setParent(parent);
    }
  }

  private static class MyKeyboard extends RemoteKeyboard {
    public MyKeyboard(RemoteKeyboard keyboard) {
      super(keyboard.executor);
    }

    @Override
    public void pressKey(final CharSequence arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyKeyboard.super.pressKey(arg0);
          return null;
        }
      });
    }

    @Override
    public void releaseKey(final CharSequence arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyKeyboard.super.releaseKey(arg0);
          return null;
        }
      });
    }

    @Override
    public void sendKeys(final CharSequence... arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyKeyboard.super.sendKeys(arg0);
          return null;
        }
      });
    }
  }

  private static class MyMouse extends RemoteMouse {
    public MyMouse(RemoteMouse mouse) {
      super(mouse.executor);
    }

    @Override
    public void click(final Coordinates arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.click(arg0);
          return null;
        }
      });
    }

    @Override
    public void contextClick(final Coordinates arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.contextClick(arg0);
          return null;
        }
      });
    }

    @Override
    public void doubleClick(final Coordinates arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.doubleClick(arg0);
          return null;
        }
      });
    }

    @Override
    public void mouseDown(final Coordinates arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.mouseDown(arg0);
          return null;
        }
      });
    }

    @Override
    public void mouseMove(final Coordinates arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.mouseMove(arg0);
          return null;
        }
      });
    }

    @Override
    public void mouseMove(final Coordinates arg0, final long arg1, final long arg2) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.mouseMove(arg0, arg1, arg2);
          return null;
        }
      });
    }

    @Override
    public void mouseUp(final Coordinates arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.mouseUp(arg0);
          return null;
        }
      });
    }

    @Override
    protected void moveIfNeeded(final Coordinates where) {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.moveIfNeeded(where);
          return null;
        }
      });

    }

    @Override
    protected Map<String, Object> paramsFromCoordinates(final Coordinates where) {
      return (Map<String, Object>) exec(new Executor() {
        @Override
        public Object perform() {
          return MyMouse.super.paramsFromCoordinates(where);
        }
      });
    }
  }

  private static class MyNav implements Navigation {
    private final Navigation nav;

    public MyNav(Navigation nav) {
      this.nav = nav;
    }

    @Override
    public void back() {
      exec(new Executor() {
        @Override
        public Object perform() {
          nav.back();
          return null;
        }
      });
    }

    @Override
    public void forward() {
      exec(new Executor() {
        @Override
        public Object perform() {
          nav.forward();
          return null;
        }
      });
    }

    @Override
    public void refresh() {
      exec(new Executor() {
        @Override
        public Object perform() {
          nav.refresh();
          return null;
        }
      });
    }

    @Override
    public void to(final String arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          nav.to(arg0);
          return null;
        }
      });
    }

    @Override
    public void to(final URL arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          nav.to(arg0);
          return null;
        }
      });
    }
  }

  private static class MyLoc implements TargetLocator {
    private final TargetLocator loc;
    private final BrowserDriver driver;

    public MyLoc(TargetLocator loc, BrowserDriver driver) {
      this.loc = loc;
      this.driver = driver;
    }

    @Override
    public WebElement activeElement() {
      return new MyElement((RemoteWebElement) exec(new Executor() {
        @Override
        public Object perform() {
          return loc.activeElement();
        }
      }));
    }

    @Override
    public Alert alert() {
      return (Alert) exec(new Executor() {
        @Override
        public Object perform() {
          return loc.alert();
        }
      });
    }

    @Override
    public WebDriver defaultContent() {
      exec(new Executor() {
        @Override
        public Object perform() {
          return loc.defaultContent();
        }
      });
      return driver;
    }

    @Override
    public WebDriver frame(final int arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return loc.frame(arg0);
        }
      });
      return driver;
    }

    @Override
    public WebDriver frame(final String arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return loc.frame(arg0);
        }
      });
      return driver;
    }

    @Override
    public WebDriver frame(final WebElement arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return loc.frame(arg0);
        }
      });
      return driver;
    }

    @Override
    public WebDriver parentFrame() {
      exec(new Executor() {
        @Override
        public Object perform() {
          return loc.parentFrame();
        }
      });
      return driver;
    }

    @Override
    public WebDriver window(final String arg0) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return loc.window(arg0);
        }
      });
      return driver;
    }
  }

  private static class MyActions extends Actions {

    public MyActions(BrowserDriver driver) {
      super(driver);
    }

    @Override
    @Deprecated
    public Action build() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Actions click() {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.click();
        }
      });
      return this;
    }

    @Override
    public Actions click(final WebElement onElement) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.click(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions clickAndHold() {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.clickAndHold();
        }
      });
      return this;
    }

    @Override
    public Actions clickAndHold(final WebElement onElement) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.clickAndHold(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions contextClick() {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.contextClick();
        }
      });
      return this;
    }

    @Override
    public Actions contextClick(final WebElement onElement) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.contextClick(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions doubleClick() {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.doubleClick();
        }
      });
      return this;
    }

    @Override
    public Actions doubleClick(final WebElement onElement) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.doubleClick(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions dragAndDrop(final WebElement source, final WebElement target) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.dragAndDrop(source, target);
        }
      });
      return this;
    }

    @Override
    public Actions dragAndDropBy(final WebElement source, final int xOffset, final int yOffset) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.dragAndDropBy(source, xOffset, yOffset);
        }
      });
      return this;
    }

    @Override
    public Actions keyDown(final Keys theKey) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.keyDown(theKey);
        }
      });
      return this;
    }

    @Override
    public Actions keyDown(final WebElement element, final Keys theKey) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.keyDown(element, theKey);
        }
      });
      return this;
    }

    @Override
    public Actions keyUp(final Keys theKey) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.keyUp(theKey);
        }
      });
      return this;
    }

    @Override
    public Actions keyUp(final WebElement element, final Keys theKey) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.keyUp(element, theKey);
        }
      });
      return this;
    }

    @Override
    public Actions moveByOffset(final int xOffset, final int yOffset) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.moveByOffset(xOffset, yOffset);
        }
      });
      return this;
    }

    @Override
    public Actions moveToElement(final WebElement toElement, final int xOffset, final int yOffset) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.moveToElement(toElement, xOffset, yOffset);
        }
      });
      return this;
    }

    @Override
    public Actions moveToElement(final WebElement toElement) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.moveToElement(toElement);
        }
      });
      return this;
    }

    @Override
    @Deprecated
    public Actions pause(final long pause) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.pause(pause);
        }
      });
      return this;
    }

    @Override
    public void perform() {
      exec(new Executor() {
        @Override
        public Object perform() {
          MyActions.super.perform();
          return null;
        }
      });
    }

    @Override
    public Actions release() {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.release();
        }
      });
      return this;
    }

    @Override
    public Actions release(final WebElement onElement) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.release(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions sendKeys(final CharSequence... keysToSend) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.sendKeys(keysToSend);
        }
      });
      return this;
    }

    @Override
    public Actions sendKeys(final WebElement element, final CharSequence... keysToSend) {
      exec(new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.sendKeys(element, keysToSend);
        }
      });
      return this;
    }
  }
}
