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
  static {
    Properties props = new Properties();
    String user = null;
    String pass = null;
    String secretA = null;
    String secretB = null;
    String secretC = null;
    String secretD = null;
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

  }
}
