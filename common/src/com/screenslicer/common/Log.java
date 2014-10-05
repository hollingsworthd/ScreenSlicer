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
package com.screenslicer.common;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.lang.exception.ExceptionUtils;

public class Log {

  private static Logger logger = null;
  private static String[] chattyClasses = new String[] {
      "java.text.",
      "java.lang.NumberFormatException",
      "org.openqa.selenium.",
      "java.net.ConnectException",
      "java.util.concurrent.ExecutionException",
  };
  static {
    System.setProperty("log4j.rootLogger", "ERROR, stdout");
    System.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
    System.setProperty("log4j.appender.stdout.Target", "System.out");
  }

  public static void init(String loggerName, boolean allowFileLogging) {
    logger = Logger.getLogger(loggerName);
    if (allowFileLogging) {
      FileHandler fh = null;
      try {
        fh = new FileHandler("../" + loggerName + ".log", 250000, 9, true);
        logger.addHandler(fh);
        String logLevel = System.getProperty("screenslicer-logging", "prod");
        if (logLevel.equals("prod")) {
          logger.setLevel(Level.INFO);
        } else if (logLevel.equals("debug")) {
          logger.setLevel(Level.ALL);
        }
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
      } catch (Throwable t) {
        t.printStackTrace();
        throw new RuntimeException(t);
      }
    }
  }

  public static void exception(Throwable t) {
    exception(t, "");
  }

  public static void exception(Throwable t, String supplementaryMessage) {
    if (logger == null) {
      init("screenslicer", true);
    }
    Level level = Level.SEVERE;
    String packageName = t.getClass().getName();
    for (int i = 0; i < chattyClasses.length; i++) {
      if (packageName.startsWith(chattyClasses[i])) {
        level = Level.FINE;
        break;
      }
    }
    String message = t.getMessage();
    message = CommonUtil.isEmpty(message) ? "" : message;
    logger.log(level, "Exception \"" + message + "\" ~ "
        + (CommonUtil.isEmpty(supplementaryMessage) ? "" : (supplementaryMessage + " ~ "))
        + "Stack trace: " + ExceptionUtils.getStackTrace(t));
  }

  public static void warn(String message) {
    if (logger == null) {
      init("screenslicer", true);
    }
    logger.log(Level.WARNING, "Message \"" + message + "\" ~ Current stack: " + ExceptionUtils.getStackTrace(new Throwable()));
  }

  public static void info(String message, boolean stackTrace) {
    if (stackTrace) {
      info(message);
      return;
    }
    if (logger == null) {
      init("screenslicer", true);
    }
    logger.log(Level.INFO, "Message \"" + message + "\".");
  }

  public static void info(String message) {
    if (logger == null) {
      init("screenslicer", true);
    }
    logger.log(Level.INFO, "Message \"" + message + "\" ~ Current stack: " + ExceptionUtils.getStackTrace(new Throwable()));
  }
}
