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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.tika.io.IOUtils;

public class Config {
  public static Config instance = new Config();

  private final Properties props = new Properties();
  private final String basicAuthUser;
  private final String basicAuthPass;
  private final String secretA;
  private final String secretB;
  private final String secretC;
  private final String secretD;
  private final String mandrillKey;
  private final String mandrillEmail;
  private static final long SLEEP = 1000;

  private Config() {
    File lock = new File("./screenslicer.config.lck");
    while (lock.exists()) {
      try {
        Thread.sleep(SLEEP);
      } catch (Throwable t) {}
    }
    try {
      FileUtils.touch(lock);
    } catch (Throwable t) {
      Log.exception(t);
    }
    File file = new File("./screenslicer.config");
    FileInputStream streamIn = null;
    try {
      FileUtils.touch(file);
      streamIn = new FileInputStream(file);
      props.load(streamIn);
    } catch (Throwable t) {
      Log.exception(t);
    }
    IOUtils.closeQuietly(streamIn);
    basicAuthUser = getAndSet("basic_auth_user", Random.next());
    basicAuthPass = getAndSet("basic_auth_pass", Random.next());
    secretA = getAndSet("secret_a", Random.next());
    secretB = getAndSet("secret_b", Random.next());
    secretC = getAndSet("secret_c", Random.next());
    secretD = getAndSet("secret_d", Random.next());
    mandrillKey = getAndSet("mandrill_key", "");
    mandrillEmail = getAndSet("mandrill_email", "");
    FileOutputStream streamOut = null;
    try {
      streamOut = new FileOutputStream(file);
      props.store(streamOut, null);
    } catch (Throwable t) {
      Log.exception(t);
    }
    IOUtils.closeQuietly(streamOut);
    FileUtils.deleteQuietly(lock);
  }

  private String getAndSet(String propertyName, String propertyDefaultValue) {
    String val = props.getProperty(propertyName, propertyDefaultValue);
    props.setProperty(propertyName, val);
    return val;
  }

  public void init() {
    Log.info("screenslicer.config: " + new File("screenslicer.config").getAbsolutePath());
  }

  public String secretA() {
    return secretA;
  }

  public String secretB() {
    return secretB;
  }

  public String secretC() {
    return secretC;
  }

  public String secretD() {
    return secretD;
  }

  public String basicAuthUser() {
    return basicAuthUser;
  }

  public String basicAuthPass() {
    return basicAuthPass;
  }

  public String mandrillKey() {
    return mandrillKey;
  }

  public String mandrillEmail() {
    return mandrillEmail;
  }

}
