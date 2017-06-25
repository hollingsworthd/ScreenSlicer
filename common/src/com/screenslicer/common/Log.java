/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 ScreenSlicer committers
 * https://github.com/MachinePublishers/ScreenSlicer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        String logLevel = System.getProperty("slicer.log", "prod");
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
    if (t != null) {
      String packageName = t.getClass().getName();
      for (int i = 0; i < chattyClasses.length; i++) {
        if (packageName.startsWith(chattyClasses[i])) {
          level = Level.FINE;
          break;
        }
      }
    }
    String message = t == null ? "n/a" : t.getMessage();
    message = CommonUtil.isEmpty(message) ? "" : message;
    logger.log(level, "Exception \"" + message + "\" ~ "
        + (CommonUtil.isEmpty(supplementaryMessage) ? "" : (supplementaryMessage + " ~ "))
        + "Stack trace: " + (t == null ? "n/a" : ExceptionUtils.getStackTrace(t)));
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

  public static void debug(Object message, boolean stdOutFallback) {
    if (logger == null) {
      init("screenslicer", true);
    }
    if (logger.isLoggable(Level.FINEST)) {
      logger.log(Level.FINEST, "Message \"" + message.toString() + "\".");
    } else if (stdOutFallback) {
      System.out.println("Debug -> " + message.toString());
    }
  }
}
