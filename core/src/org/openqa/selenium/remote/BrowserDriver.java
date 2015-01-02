/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.HasInputDevices;
import org.openqa.selenium.interactions.Keyboard;
import org.openqa.selenium.interactions.Mouse;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.internal.FindsByClassName;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByLinkText;
import org.openqa.selenium.internal.FindsByName;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.internal.FindsByXPath;
import org.openqa.selenium.internal.Killable;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.internal.WrapsDriver;

import com.google.common.io.Resources;
import com.screenslicer.common.Log;
import com.screenslicer.core.util.BrowserUtil;

public class BrowserDriver implements WebDriver, JavascriptExecutor, FindsById,
    FindsByClassName, FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName,
    FindsByXPath, HasInputDevices, HasCapabilities, TakesScreenshot, Killable {
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

  public static class Fatal extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Fatal() {
      super();
    }

    public Fatal(Throwable nested) {
      super(nested);
    }

    public Fatal(String message) {
      super(message);
    }
  }

  private static final int RETRY_WAIT = 30000;
  private static final int RETRIES = 12;

  private static interface Executor {
    Object perform();
  }

  private static Object exec(BrowserDriver driver, Executor action) {
    Throwable throwable = null;
    for (int i = 0; i < RETRIES; i++) {
      try {
        Object ret = action.perform();
        try {
          synchronized (driver.lock) {
            Set<String> handles = driver.firefoxDriver.getWindowHandles();
            int num = -1;
            for (String cur : handles) {
              ++num;
              driver.windowTranslator.put(cur, num);
            }
          }
        } catch (Throwable t) {
          Log.exception(t);
        }
        return ret;
      } catch (UnreachableBrowserException e) {
        Log.warn("Browser was unreachable... retrying...");
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
    Log.warn("Browser was unreachable... unwinding stack...");
    Log.exception(throwable);
    throw new Retry(throwable);
  }

  private static List<WebElement> convert(BrowserDriver driver, List<WebElement> elements) {
    List<WebElement> myElements = new ArrayList<WebElement>();
    for (WebElement element : elements) {
      myElements.add(new MyElement(driver, (RemoteWebElement) element));
    }
    return myElements;
  }

  private FirefoxDriver firefoxDriver;
  private final Profile profile;
  private final Map<String, Integer> windowTranslator = new HashMap<String, Integer>();
  private final Object lock = new Object();

  public BrowserDriver(Profile profile) {
    firefoxDriver = new FirefoxDriver(profile);
    this.profile = profile;
  }

  private String translate(String windowHandle) {
    synchronized (lock) {
      Integer num = windowTranslator.get(windowHandle);
      String[] windows = firefoxDriver.getWindowHandles().toArray(new String[0]);
      if (num == null || num.intValue() >= windows.length) {
        throw new Fatal();
      }
      return windows[num.intValue()];
    }
  }

  @Override
  public void kill() {
    firefoxDriver.kill();
  }

  public void reset() {
    firefoxDriver.kill();
    firefoxDriver = new FirefoxDriver(profile);
    BrowserUtil.driverSleepReset();
    synchronized (lock) {
      int numWindows = windowTranslator.size();
      String[] curWindows = firefoxDriver.getWindowHandles().toArray(new String[0]);
      for (int i = numWindows; i < curWindows.length; i++) {
        firefoxDriver.switchTo().window(curWindows[i]).close();
      }
    }
  }

  public Actions actions() {
    return new MyActions(this);
  }

  @Override
  public void close() {
    exec(this, new Executor() {
      @Override
      public Object perform() {
        firefoxDriver.close();
        return null;
      }
    });
  }

  @Override
  public Object executeAsyncScript(final String script, final Object... args) {
    return exec(this, new Executor() {
      @Override
      public Object perform() {
        Object[] myArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
          if (args[i] instanceof MyElement) {
            myArgs[i] = ((MyElement) args[i]).element;
          } else {
            myArgs[i] = args[i];
          }
        }
        return firefoxDriver.executeAsyncScript(script, args);
      }
    });
  }

  @Override
  public Object executeScript(final String script, final Object... args) {
    return exec(this, new Executor() {
      @Override
      public Object perform() {
        Object[] myArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
          if (args[i] instanceof MyElement) {
            myArgs[i] = ((MyElement) args[i]).element;
          } else {
            myArgs[i] = args[i];
          }
        }
        return firefoxDriver.executeScript(script, myArgs);
      }
    });
  }

  @Override
  public WebElement findElement(final By by) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElement(by);
      }
    }));
  }

  protected WebElement findElement(final String by, final String using) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElement(by, using);
      }
    }));
  }

  @Override
  public WebElement findElementByClassName(final String using) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementByClassName(using);
      }
    }));
  }

  @Override
  public WebElement findElementByCssSelector(final String using) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementByCssSelector(using);
      }
    }));
  }

  @Override
  public WebElement findElementById(final String using) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementById(using);
      }
    }));
  }

  @Override
  public WebElement findElementByLinkText(final String using) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementByLinkText(using);
      }
    }));
  }

  @Override
  public WebElement findElementByName(final String using) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementByName(using);
      }
    }));
  }

  @Override
  public WebElement findElementByPartialLinkText(final String using) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementByPartialLinkText(using);
      }
    }));
  }

  @Override
  public WebElement findElementByTagName(final String using) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementByTagName(using);
      }
    }));
  }

  @Override
  public WebElement findElementByXPath(final String using) {
    return new MyElement(this, (RemoteWebElement) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementByXPath(using);
      }
    }));
  }

  @Override
  public List<WebElement> findElements(final By by) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElements(by);
      }
    }));
  }

  protected List<WebElement> findElements(final String by, final String using) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElements(by, using);
      }
    }));
  }

  public List<WebElement> findElementsByClassName(final String using) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementsByClassName(using);
      }
    }));
  }

  @Override
  public List<WebElement> findElementsByCssSelector(final String using) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementsByCssSelector(using);
      }
    }));
  }

  @Override
  public List<WebElement> findElementsById(final String using) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementsById(using);
      }
    }));
  }

  @Override
  public List<WebElement> findElementsByLinkText(final String using) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementsByLinkText(using);
      }
    }));
  }

  @Override
  public List<WebElement> findElementsByName(final String using) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementsByName(using);
      }
    }));
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(final String using) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementsByPartialLinkText(using);
      }
    }));
  }

  @Override
  public List<WebElement> findElementsByTagName(final String using) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementsByTagName(using);
      }
    }));
  }

  @Override
  public List<WebElement> findElementsByXPath(final String using) {
    return convert(this, (List<WebElement>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.findElementsByXPath(using);
      }
    }));
  }

  @Override
  public void get(final String url) {
    exec(this, new Executor() {
      @Override
      public Object perform() {
        firefoxDriver.get(url);
        return null;
      }
    });
  }

  @Override
  public String getCurrentUrl() {
    return (String) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.getCurrentUrl();
      }
    });
  }

  @Override
  public Keyboard getKeyboard() {
    return new MyKeyboard(this, (RemoteKeyboard) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.getKeyboard();
      }
    }));
  }

  @Override
  public Mouse getMouse() {
    return new MyMouse(this, (RemoteMouse) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.getMouse();
      }
    }));
  }

  @Override
  public String getPageSource() {
    return (String) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.getPageSource();
      }
    });
  }

  @Override
  public String getTitle() {
    return (String) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.getTitle();
      }
    });
  }

  @Override
  public String getWindowHandle() {
    return (String) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.getWindowHandle();
      }
    });
  }

  @Override
  public Set<String> getWindowHandles() {
    return (Set<String>) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.getWindowHandles();
      }
    });
  }

  @Override
  public Navigation navigate() {
    return new MyNav(this, (Navigation) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.navigate();
      }
    }));
  }

  @Override
  public void quit() {
    exec(this, new Executor() {
      @Override
      public Object perform() {
        firefoxDriver.quit();
        return null;
      }
    });
  }

  @Override
  public TargetLocator switchTo() {
    return new MyLoc((TargetLocator) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.switchTo();
      }
    }), this);
  }

  @Override
  public Options manage() {
    return firefoxDriver.manage();
  }

  @Override
  public <X> X getScreenshotAs(final OutputType<X> arg0) throws WebDriverException {
    return (X) exec(this, new Executor() {
      @Override
      public Object perform() {
        return firefoxDriver.getScreenshotAs(arg0);
      }
    });
  }

  @Override
  public Capabilities getCapabilities() {
    return firefoxDriver.getCapabilities();
  }

  private static class MyElement implements WebElement, FindsByLinkText, FindsById, FindsByName,
      FindsByTagName, FindsByClassName, FindsByCssSelector, FindsByXPath, WrapsDriver, Locatable {
    private final RemoteWebElement element;
    private final BrowserDriver driver;

    public MyElement(BrowserDriver driver, RemoteWebElement element) {
      this.driver = driver;
      this.element = element;
    }

    @Override
    public void clear() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          element.clear();
          return null;
        }
      });
    }

    @Override
    public void click() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          element.click();
          return null;
        }
      });
    }

    @Override
    public WebElement findElement(final By arg0) {
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElement(arg0);
        }
      }));
    }

    @Override
    public List<WebElement> findElements(final By arg0) {
      return convert(driver, (List<WebElement>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElements(arg0);
        }
      }));
    }

    @Override
    public String getAttribute(final String arg0) {
      return (String) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.getAttribute(arg0);
        }
      });
    }

    @Override
    public String getCssValue(final String arg0) {
      return (String) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.getCssValue(arg0);
        }
      });
    }

    @Override
    public Point getLocation() {
      return (Point) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.getLocation();
        }
      });
    }

    @Override
    public Dimension getSize() {
      return (Dimension) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.getSize();
        }
      });
    }

    @Override
    public String getTagName() {
      return (String) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.getTagName();
        }
      });
    }

    @Override
    public String getText() {
      return (String) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.getText();
        }
      });
    }

    @Override
    public boolean isDisplayed() {
      return (Boolean) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.isDisplayed();
        }
      });
    }

    @Override
    public boolean isEnabled() {
      return (Boolean) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.isEnabled();
        }
      });
    }

    @Override
    public boolean isSelected() {
      return (Boolean) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.isSelected();
        }
      });
    }

    @Override
    public void sendKeys(final CharSequence... arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          element.sendKeys(arg0);
          return null;
        }
      });
    }

    @Override
    public void submit() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          element.submit();
          return null;
        }
      });
    }

    @Override
    public WebElement findElementByClassName(final String using) {
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementByClassName(using);
        }
      }));
    }

    @Override
    public WebElement findElementByCssSelector(final String using) {
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementByCssSelector(using);
        }
      }));
    }

    @Override
    public WebElement findElementById(final String using) {
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementById(using);
        }
      }));
    }

    @Override
    public WebElement findElementByLinkText(final String using) {
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementByLinkText(using);
        }
      }));
    }

    @Override
    public WebElement findElementByName(final String using) {
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementByName(using);
        }
      }));
    }

    @Override
    public WebElement findElementByPartialLinkText(final String using) {
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementByPartialLinkText(using);
        }
      }));
    }

    @Override
    public WebElement findElementByTagName(final String using) {
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementByTagName(using);
        }
      }));
    }

    @Override
    public WebElement findElementByXPath(final String using) {
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementByXPath(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByClassName(final String using) {
      return convert(driver, (List<WebElement>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByClassName(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByCssSelector(final String using) {
      return convert(driver, (List<WebElement>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByCssSelector(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsById(final String using) {
      return convert(driver, (List<WebElement>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementsById(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByLinkText(final String using) {
      return convert(driver, (List<WebElement>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByLinkText(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByName(final String using) {
      return convert(driver, (List<WebElement>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByName(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByPartialLinkText(final String using) {
      return convert(driver, (List<WebElement>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByPartialLinkText(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByTagName(final String using) {
      return convert(driver, (List<WebElement>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByTagName(using);
        }
      }));
    }

    @Override
    public List<WebElement> findElementsByXPath(final String using) {
      return convert(driver, (List<WebElement>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.findElementsByXPath(using);
        }
      }));
    }

    @Override
    public Coordinates getCoordinates() {
      return (Coordinates) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return element.getCoordinates();
        }
      });
    }

    @Override
    public WebDriver getWrappedDriver() {
      return driver;
    }
  }

  private static class MyKeyboard extends RemoteKeyboard {
    private final BrowserDriver driver;

    public MyKeyboard(BrowserDriver driver, RemoteKeyboard keyboard) {
      super(keyboard.executor);
      this.driver = driver;
    }

    @Override
    public void pressKey(final CharSequence arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyKeyboard.super.pressKey(arg0);
          return null;
        }
      });
    }

    @Override
    public void releaseKey(final CharSequence arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyKeyboard.super.releaseKey(arg0);
          return null;
        }
      });
    }

    @Override
    public void sendKeys(final CharSequence... arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyKeyboard.super.sendKeys(arg0);
          return null;
        }
      });
    }
  }

  private static class MyMouse extends RemoteMouse {
    private final BrowserDriver driver;

    public MyMouse(BrowserDriver driver, RemoteMouse mouse) {
      super(mouse.executor);
      this.driver = driver;
    }

    @Override
    public void click(final Coordinates arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.click(arg0);
          return null;
        }
      });
    }

    @Override
    public void contextClick(final Coordinates arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.contextClick(arg0);
          return null;
        }
      });
    }

    @Override
    public void doubleClick(final Coordinates arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.doubleClick(arg0);
          return null;
        }
      });
    }

    @Override
    public void mouseDown(final Coordinates arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.mouseDown(arg0);
          return null;
        }
      });
    }

    @Override
    public void mouseMove(final Coordinates arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.mouseMove(arg0);
          return null;
        }
      });
    }

    @Override
    public void mouseMove(final Coordinates arg0, final long arg1, final long arg2) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.mouseMove(arg0, arg1, arg2);
          return null;
        }
      });
    }

    @Override
    public void mouseUp(final Coordinates arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.mouseUp(arg0);
          return null;
        }
      });
    }

    @Override
    protected void moveIfNeeded(final Coordinates where) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyMouse.super.moveIfNeeded(where);
          return null;
        }
      });

    }

    @Override
    protected Map<String, Object> paramsFromCoordinates(final Coordinates where) {
      return (Map<String, Object>) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyMouse.super.paramsFromCoordinates(where);
        }
      });
    }
  }

  public static class Profile extends FirefoxProfile {
    private File profileDir = null;

    public Profile(File profileDir) {
      super(profileDir);
    }

    @Override
    protected Reader onlyOverrideThisIfYouKnowWhatYouAreDoing() {
      URL resource = Resources.getResource(BrowserDriver.class, "/org/openqa/selenium/remote/browserdriver_prefs.json");
      try {
        return new InputStreamReader(resource.openStream());
      } catch (IOException e) {
        throw new WebDriverException(e);
      }
    }

    @Override
    public File layoutOnDisk() {
      if (profileDir == null) {
        profileDir = super.layoutOnDisk();
      } else {
        File userPrefs = new File(profileDir, "user.js");
        try {
          installExtensions(profileDir);
        } catch (IOException e) {
          Log.exception(e);
          return super.layoutOnDisk();
        }
        deleteLockFiles(profileDir);
        deleteExtensionsCacheIfItExists(profileDir);
        updateUserPrefs(userPrefs);
      }
      return profileDir;
    }
  }

  private static class MyNav implements Navigation {
    private final Navigation nav;
    private final BrowserDriver driver;

    public MyNav(BrowserDriver driver, Navigation nav) {
      this.driver = driver;
      this.nav = nav;
    }

    @Override
    public void back() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          nav.back();
          return null;
        }
      });
    }

    @Override
    public void forward() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          nav.forward();
          return null;
        }
      });
    }

    @Override
    public void refresh() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          nav.refresh();
          return null;
        }
      });
    }

    @Override
    public void to(final String arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          nav.to(arg0);
          return null;
        }
      });
    }

    @Override
    public void to(final URL arg0) {
      exec(driver, new Executor() {
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
      return new MyElement(driver, (RemoteWebElement) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return loc.activeElement();
        }
      }));
    }

    @Override
    public Alert alert() {
      return (Alert) exec(driver, new Executor() {
        @Override
        public Object perform() {
          return loc.alert();
        }
      });
    }

    @Override
    public WebDriver defaultContent() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return loc.defaultContent();
        }
      });
      return driver;
    }

    @Override
    public WebDriver frame(final int arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return loc.frame(arg0);
        }
      });
      return driver;
    }

    @Override
    public WebDriver frame(final String arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return loc.frame(arg0);
        }
      });
      return driver;
    }

    @Override
    public WebDriver frame(final WebElement arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return loc.frame(arg0);
        }
      });
      return driver;
    }

    @Override
    public WebDriver parentFrame() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return loc.parentFrame();
        }
      });
      return driver;
    }

    @Override
    public WebDriver window(final String arg0) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          try {
            return loc.window(arg0);
          } catch (Throwable t) {
            return loc.window(driver.translate(arg0));
          }
        }
      });
      return driver;
    }
  }

  private static class MyAction implements Action {
    private Action action;
    private final BrowserDriver driver;

    public MyAction(BrowserDriver driver, Action action) {
      this.driver = driver;
      this.action = action;
    }

    @Override
    public void perform() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          action.perform();
          return null;
        }
      });
    }
  }

  private static class MyActions extends Actions {
    private final BrowserDriver driver;

    public MyActions(BrowserDriver driver) {
      super(driver);
      this.driver = driver;
    }

    public MyActions(BrowserDriver driver, Keyboard keyboard) {
      super(keyboard);
      this.driver = driver;
    }

    public MyActions(BrowserDriver driver, Keyboard keyboard, Mouse mouse) {
      super(keyboard, mouse);
      this.driver = driver;
    }

    @Override
    public Action build() {
      return new MyAction(driver, super.build());
    }

    @Override
    public Actions click() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.click();
        }
      });
      return this;
    }

    @Override
    public Actions click(final WebElement onElement) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.click(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions clickAndHold() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.clickAndHold();
        }
      });
      return this;
    }

    @Override
    public Actions clickAndHold(final WebElement onElement) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.clickAndHold(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions contextClick() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.contextClick();
        }
      });
      return this;
    }

    @Override
    public Actions contextClick(final WebElement onElement) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.contextClick(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions doubleClick() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.doubleClick();
        }
      });
      return this;
    }

    @Override
    public Actions doubleClick(final WebElement onElement) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.doubleClick(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions dragAndDrop(final WebElement source, final WebElement target) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.dragAndDrop(source, target);
        }
      });
      return this;
    }

    @Override
    public Actions dragAndDropBy(final WebElement source, final int xOffset, final int yOffset) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.dragAndDropBy(source, xOffset, yOffset);
        }
      });
      return this;
    }

    @Override
    public Actions keyDown(final Keys theKey) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.keyDown(theKey);
        }
      });
      return this;
    }

    @Override
    public Actions keyDown(final WebElement element, final Keys theKey) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.keyDown(element, theKey);
        }
      });
      return this;
    }

    @Override
    public Actions keyUp(final Keys theKey) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.keyUp(theKey);
        }
      });
      return this;
    }

    @Override
    public Actions keyUp(final WebElement element, final Keys theKey) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.keyUp(element, theKey);
        }
      });
      return this;
    }

    @Override
    public Actions moveByOffset(final int xOffset, final int yOffset) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.moveByOffset(xOffset, yOffset);
        }
      });
      return this;
    }

    @Override
    public Actions moveToElement(final WebElement toElement, final int xOffset, final int yOffset) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.moveToElement(toElement, xOffset, yOffset);
        }
      });
      return this;
    }

    @Override
    public Actions moveToElement(final WebElement toElement) {
      exec(driver, new Executor() {
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
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.pause(pause);
        }
      });
      return this;
    }

    @Override
    public void perform() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          MyActions.super.perform();
          return null;
        }
      });
    }

    @Override
    public Actions release() {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.release();
        }
      });
      return this;
    }

    @Override
    public Actions release(final WebElement onElement) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.release(onElement);
        }
      });
      return this;
    }

    @Override
    public Actions sendKeys(final CharSequence... keysToSend) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.sendKeys(keysToSend);
        }
      });
      return this;
    }

    @Override
    public Actions sendKeys(final WebElement element, final CharSequence... keysToSend) {
      exec(driver, new Executor() {
        @Override
        public Object perform() {
          return MyActions.super.sendKeys(element, keysToSend);
        }
      });
      return this;
    }
  }
}
