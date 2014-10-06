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

  private Config() {
    File file = new File("./screenslicer.config");
    String basicAuthUserTmp = null;
    String basicAuthPassTmp = null;
    String secretATmp = null;
    String secretBTmp = null;
    String secretCTmp = null;
    String secretDTmp = null;
    String mandrillKeyTmp = null;
    String mandrillEmailTmp = null;
    try {
      FileUtils.touch(file);
      props.load(new FileInputStream(file));
      basicAuthUserTmp = props.getProperty("basic_auth_user", Random.next());
      basicAuthPassTmp = props.getProperty("basic_auth_pass", Random.next());
      secretATmp = props.getProperty("secret_a", Random.next());
      secretBTmp = props.getProperty("secret_b", Random.next());
      secretCTmp = props.getProperty("secret_c", Random.next());
      secretDTmp = props.getProperty("secret_d", Random.next());
      mandrillKeyTmp = props.getProperty("mandrill_key");
      mandrillEmailTmp = props.getProperty("mandrill_email");
      props.store(new FileOutputStream(new File("./screenslicer.config")), null);
    } catch (Throwable t) {
      Log.exception(t);
    }
    basicAuthUser = basicAuthUserTmp;
    basicAuthPass = basicAuthPassTmp;
    secretA = secretATmp;
    secretB = secretBTmp;
    secretC = secretCTmp;
    secretD = secretDTmp;
    mandrillKey = mandrillKeyTmp;
    mandrillEmail = mandrillEmailTmp;
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
