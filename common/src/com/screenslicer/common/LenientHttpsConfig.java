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

import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpProviderConfig;
import com.ning.http.client.ProxyServerSelector;
import com.ning.http.client.Realm;
import com.ning.http.client.date.TimeConverter;
import com.ning.http.client.filter.IOExceptionFilter;
import com.ning.http.client.filter.RequestFilter;
import com.ning.http.client.filter.ResponseFilter;

public class LenientHttpsConfig extends AsyncHttpClientConfig {

  private static final HostnameVerifier hostnameVerifier = new HostnameVerifier() {
    @Override
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  };
  private final AsyncHttpClientConfig config;
  private final SSLContext sslContext;
  private static final AsyncHttpClientConfig instance = new LenientHttpsConfig();

  public static AsyncHttpClientConfig instance() {
    return instance;
  }

  private LenientHttpsConfig() {
    AsyncHttpClientConfig configTmp = null;
    SSLContext sslContextTmp = null;
    try {
      AsyncHttpClient client = new AsyncHttpClient();
      configTmp = client.getConfig();
      IOUtils.closeQuietly(client);
      client = null;

      X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
          .generateCertificate(CommonUtil.class.getResourceAsStream("screenslicer.internal.cert"));
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null);
      keyStore.setCertificateEntry(cert.getSubjectX500Principal().getName(), cert);
      KeyManagerFactory keyManager = KeyManagerFactory.getInstance("SunX509");
      keyManager.init(keyStore, null);
      TrustManagerFactory trustManager = TrustManagerFactory.getInstance("X509");
      trustManager.init(keyStore);
      sslContextTmp = SSLContext.getInstance("TLS");
      sslContextTmp.init(keyManager.getKeyManagers(), trustManager.getTrustManagers(), null);
    } catch (Throwable t) {
      Log.exception(t);
    }
    config = configTmp;
    sslContext = sslContextTmp;
  }

  @Override
  public SSLContext getSSLContext() {
    return sslContext;
  }

  @Override
  public HostnameVerifier getHostnameVerifier() {
    return hostnameVerifier;
  }

  @Override
  public ExecutorService executorService() {
    return config.executorService();
  }

  @Override
  public AsyncHttpProviderConfig<?, ?> getAsyncHttpProviderConfig() {
    return config.getAsyncHttpProviderConfig();
  }

  @Override
  public int getConnectionTTL() {
    return config.getConnectionTTL();
  }

  @Override
  public int getConnectionTimeout() {
    return config.getConnectionTimeout();
  }

  @Override
  public List<IOExceptionFilter> getIOExceptionFilters() {
    return config.getIOExceptionFilters();
  }

  @Override
  public int getIoThreadMultiplier() {
    return config.getIoThreadMultiplier();
  }

  @Override
  public int getMaxConnections() {
    return config.getMaxConnections();
  }

  @Override
  public int getMaxConnectionsPerHost() {
    return config.getMaxConnectionsPerHost();
  }

  @Override
  public int getMaxRedirects() {
    return config.getMaxRedirects();
  }

  @Override
  public int getMaxRequestRetry() {
    return config.getMaxRequestRetry();
  }

  @Override
  public int getPooledConnectionIdleTimeout() {
    return config.getPooledConnectionIdleTimeout();
  }

  @Override
  public ProxyServerSelector getProxyServerSelector() {
    return config.getProxyServerSelector();
  }

  @Override
  public int getReadTimeout() {
    return config.getReadTimeout();
  }

  @Override
  public Realm getRealm() {
    return config.getRealm();
  }

  @Override
  public List<RequestFilter> getRequestFilters() {
    return config.getRequestFilters();
  }

  @Override
  public int getRequestTimeout() {
    return config.getRequestTimeout();
  }

  @Override
  public List<ResponseFilter> getResponseFilters() {
    return config.getResponseFilters();
  }

  @Override
  public TimeConverter getTimeConverter() {
    return config.getTimeConverter();
  }

  @Override
  public String getUserAgent() {
    return config.getUserAgent();
  }

  @Override
  public int getWebSocketTimeout() {
    return config.getWebSocketTimeout();
  }

  @Override
  public boolean isAcceptAnyCertificate() {
    return config.isAcceptAnyCertificate();
  }

  @Override
  public boolean isAllowPoolingConnections() {
    return config.isAllowPoolingConnections();
  }

  @Override
  public boolean isAllowPoolingSslConnections() {
    return config.isAllowPoolingSslConnections();
  }

  @Override
  public boolean isCompressionEnforced() {
    return config.isCompressionEnforced();
  }

  @Override
  public boolean isDisableUrlEncodingForBoundedRequests() {
    return config.isDisableUrlEncodingForBoundedRequests();
  }

  @Override
  public boolean isFollowRedirect() {
    return config.isFollowRedirect();
  }

  @Override
  public boolean isRemoveQueryParamOnRedirect() {
    return config.isRemoveQueryParamOnRedirect();
  }

  @Override
  public boolean isStrict302Handling() {
    return config.isStrict302Handling();
  }

  @Override
  public boolean isUseRelativeURIsWithConnectProxies() {
    return config.isUseRelativeURIsWithConnectProxies();
  }

  @Override
  public boolean isValid() {
    return config.isValid();
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  @Override
  public boolean equals(Object obj) {
    return config.equals(obj);
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
  }

  @Override
  public int hashCode() {
    return config.hashCode();
  }

  @Override
  public String toString() {
    return config.toString();
  }

}
