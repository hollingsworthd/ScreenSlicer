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

public class Config {
  public static final String BASIC_AUTH_USER;
  public static final String BASIC_AUTH_PASS;
  public static final String SECRET_A;
  public static final String SECRET_B;
  public static final String SECRET_C;
  public static final String SECRET_D;
  public static final String MANDRILL_KEY;
  public static final String MANDRILL_EMAIL;
  static {
    Properties props = new Properties();
    String user = null;
    String pass = null;
    String secretA = null;
    String secretB = null;
    String secretC = null;
    String secretD = null;
    String mandrillKey = null;
    String mandrillEmail = null;
    File file = new File("./screenslicer.config");
    try {
      FileUtils.touch(file);
      props.load(new FileInputStream(file));
      user = props.getProperty("basic_auth_user", Crypto.random());
      props.setProperty("basic_auth_user", user);
      pass = props.getProperty("basic_auth_pass", Crypto.random());
      props.setProperty("basic_auth_pass", pass);
      secretA = props.getProperty("secret_a", Crypto.random());
      props.setProperty("secret_a", secretA);
      secretB = props.getProperty("secret_b", Crypto.random());
      props.setProperty("secret_b", secretB);
      secretC = props.getProperty("secret_c", Crypto.random());
      props.setProperty("secret_c", secretC);
      secretD = props.getProperty("secret_d", Crypto.random());
      props.setProperty("secret_d", secretD);
      mandrillKey = props.getProperty("mandrill_key");
      mandrillEmail = props.getProperty("mandrill_email");
      props.store(new FileOutputStream(new File("./screenslicer.config")), null);

    } catch (Throwable t) {
      Log.exception(t);
    }
    BASIC_AUTH_USER = user;
    BASIC_AUTH_PASS = pass;
    SECRET_A = secretA;
    SECRET_B = secretB;
    SECRET_C = secretC;
    SECRET_D = secretD;
    MANDRILL_KEY = mandrillKey;
    MANDRILL_EMAIL = mandrillEmail;
  }
}
