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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class CommonFile {
  private static final Map<String, Object> fileLocks = new HashMap<String, Object>();
  private static final Map<String, Integer> fileLocksCount = new HashMap<String, Integer>();
  private static final Object fileLocksMutex = new Object();

  private static Object lock(String filepath) {
    synchronized (fileLocksMutex) {
      Object fileLock;
      if (fileLocks.containsKey(filepath)) {
        fileLock = fileLocks.get(filepath);
        fileLocksCount.put(filepath, fileLocksCount.get(filepath).intValue() + 1);
      } else {
        final Object newFileLock = new Object();
        fileLocks.put(filepath, newFileLock);
        fileLocksCount.put(filepath, 1);
        fileLock = newFileLock;
      }
      return fileLock;
    }
  }

  private static void unlock(String filepath) {
    synchronized (fileLocksMutex) {
      fileLocksCount.put(filepath, fileLocksCount.get(filepath).intValue() - 1);
      if (fileLocksCount.get(filepath).intValue() == 0) {
        fileLocks.remove(filepath);
        fileLocksCount.remove(filepath);
      }
    }
  }

  public static void writeStringToFile(File file, String data, boolean append) {
    synchronized (lock(file.getAbsolutePath())) {
      try {
        FileUtils.writeStringToFile(file, data, "utf-8", append);
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    unlock(file.getAbsolutePath());
  }

  public static void writeByteArrayToFile(File file, byte[] data, boolean append) {
    synchronized (lock(file.getAbsolutePath())) {
      try {
        FileUtils.writeByteArrayToFile(file, data, append);
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    unlock(file.getAbsolutePath());
  }

  public static byte[] readFileToByteArray(File file) {
    byte[] content = null;
    synchronized (lock(file.getAbsolutePath())) {
      try {
        content = FileUtils.readFileToByteArray(file);
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    unlock(file.getAbsolutePath());
    return content;
  }

  public static String readFileToString(File file) {
    String content = null;
    synchronized (lock(file.getAbsolutePath())) {
      try {
        content = FileUtils.readFileToString(file, "utf-8");
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    unlock(file.getAbsolutePath());
    return content;
  }

  public static List<String> readLines(File file) {
    List<String> content = null;
    synchronized (lock(file.getAbsolutePath())) {
      try {
        content = FileUtils.readLines(file, "utf-8");
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    unlock(file.getAbsolutePath());
    return content;
  }

  public static void writeLines(File file, Collection<String> lines, boolean append) {
    synchronized (lock(file.getAbsolutePath())) {
      try {
        FileUtils.writeLines(file, "utf-8", lines, append);
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    unlock(file.getAbsolutePath());
  }
}
