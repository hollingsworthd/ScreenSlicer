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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import com.google.gson.reflect.TypeToken;

public class Crypto {
  private static final Type stringType = new TypeToken<Map<String, String>>() {}.getType();
  private static final String transitSecret = Config.instance.transitSecret();
  private static final int storageSecretVersion = Config.instance.storageSecretVersion();
  private static final Map<String, Long> usedTokens = new HashMap<String, Long>();
  private static final Object lock = new Object();
  private static final long EXPIRE = 10 * 60 * 1000;

  private static Map<String, String> asMap(String... keysAndVals) {
    HashMap<String, String> map = new HashMap<String, String>();
    int mid = keysAndVals.length / 2;
    for (int i = 0; i < mid; i++) {
      map.put(keysAndVals[i], keysAndVals[i + mid]);
    }
    return map;
  }

  public static String fastHash(String str) {
    try {
      return new String(DigestUtils.sha256Hex(str));
    } catch (Exception e) {
      Log.exception(e);
      throw new RuntimeException(e);
    }
  }

  public static int fastHashInt(String str) {
    try {
      return str.hashCode();
    } catch (Exception e) {
      Log.exception(e);
      throw new RuntimeException(e);
    }
  }

  private static void push(String token, long time) {
    synchronized (lock) {
      long curTime = System.currentTimeMillis();
      List<String> toRemove = new ArrayList<String>();
      for (Map.Entry<String, Long> usedToken : usedTokens.entrySet()) {
        if (curTime - usedToken.getValue() > EXPIRE) {
          toRemove.add(usedToken.getKey());
        }
      }
      for (String key : toRemove) {
        usedTokens.remove(key);
      }
      usedTokens.put(token, time);
    }
  }

  private static long validateTime(String token, String time) {
    if (token == null) {
      return 0;
    }
    try {
      if (time == null || time.indexOf(transitSecret) != 0) {
        return 0;
      }
      long authTime = Long.parseLong(time.substring(transitSecret.length()));
      if (System.currentTimeMillis() - authTime > EXPIRE) {
        return 0;
      }
      synchronized (lock) {
        return !usedTokens.containsKey(token) ? authTime : 0;
      }
    } catch (Exception e) {
      Log.exception(e);
      return 0;
    }
  }

  private static String getTransitKey(String token, String auth, String recipient) {
    if (token == null || auth == null || recipient == null) {
      return null;
    }
    try {
      String newKey = createTransitKey(token, recipient);
      String str = decodeHelper(auth, newKey);
      long time = validateTime(token, str);
      if (time != 0) {
        push(token, time);
        return newKey;
      }
      return null;
    } catch (Exception e) {
      Log.exception(e);
    }
    return null;
  }

  private static String createAuth(String secretKey) {
    return encodeHelper(transitSecret + System.currentTimeMillis(), secretKey);
  }

  private static String createTransitKey(String token, String recipient) {
    return new String(recipient + transitSecret + token);
  }

  public static String encode(String string, String recipient) {
    if (string == null || recipient == null) {
      return null;
    }
    String token = Random.next();
    String secretKey = createTransitKey(token, recipient);
    String auth = createAuth(secretKey);
    String message = encodeHelper(string, secretKey);
    return CommonUtil.gson.toJson(asMap("auth", "token", "message", auth, token, message), stringType);
  }

  public static String encode(String string) {
    if (string == null) {
      return null;
    }
    String token = Random.next();
    return "encodedv" + storageSecretVersion + ":token~" + token + "~"
        + encodeHelper(string, Config.instance.storageSecret() + token);
  }

  public static String decode(String string) {
    if (CommonUtil.isEmpty(string)) {
      return null;
    }
    for (int i = 1; i <= storageSecretVersion; i++) {
      if (string.startsWith("encodedv" + i + ":")) {
        String token = "";
        String message;
        if (string.startsWith("encodedv" + i + ":token~")) {
          String[] parts = string.split("~", 3);
          token = parts[1];
          message = parts[2];
        } else {
          message = string.split(":", 2)[1];
        }
        return decodeHelper(message, Config.instance.storageSecret(i) + token);
      }
    }
    return string;
  }

  public static String decode(String string, String recipient) {
    if (CommonUtil.isEmpty(string)) {
      return null;
    }
    Map<String, String> map = CommonUtil.gson.fromJson(string, stringType);
    String secretKey = getTransitKey(map.get("token"), map.get("auth"), recipient);
    if (secretKey == null) {
      return null;
    }
    return decodeHelper(map.get("message"), secretKey);
  }

  private static String encodeHelper(String plainText, String encryptionKey) {
    if (plainText == null || encryptionKey == null) {
      return null;
    }
    try {
      Cipher aesCipher = Cipher.getInstance("AES");
      aesCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(
          DigestUtils.sha256(encryptionKey), "AES"));
      return Base64.encodeBase64String(aesCipher.doFinal(plainText.getBytes("utf-8")));
    } catch (Exception e) {
      return null;
    }
  }

  private static String decodeHelper(String cipherText, String encryptionKey) {
    if (cipherText == null || encryptionKey == null) {
      return null;
    }
    try {
      Cipher aesCipher = Cipher.getInstance("AES");
      aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(
          DigestUtils.sha256(encryptionKey), "AES"), aesCipher.getParameters());
      return new String(aesCipher.doFinal(
          Base64.decodeBase64(cipherText)), "utf-8");
    } catch (Exception e) {
      return null;
    }
  }
}
