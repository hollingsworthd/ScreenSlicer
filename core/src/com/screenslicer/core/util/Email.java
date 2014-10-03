/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * 717 Martin Luther King Dr W Ste I, Cincinnati, Ohio 45220
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--contact Machine Publishers, LLC for details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
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
package com.screenslicer.core.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;

import com.screenslicer.api.request.EmailExport;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.webapp.WebApp;

public class Email {
  public static void sendResults(EmailExport export) {
    if (WebApp.DEV) {
      return;
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("key", "XKzt8lIeBD4Ik6Vt2CihUw");
    List<Map<String, String>> to = new ArrayList<Map<String, String>>();
    for (int i = 0; i < export.recipients.length; i++) {
      to.add(CommonUtil.asMap("email", "name", "type",
          export.recipients[i], export.recipients[i].split("@")[0], "to"));
    }
    List<Map<String, String>> attachments = new ArrayList<Map<String, String>>();
    for (Map.Entry<String, byte[]> entry : export.attachments.entrySet()) {
      attachments.add(CommonUtil.asMap("type", "name", "content",
          new Tika().detect(entry.getValue()), entry.getKey(),
          Base64.encodeBase64String(entry.getValue())));
    }
    params.put("message", CommonUtil.asObjMap(
        "track_clicks", "track_opens", "html",
        "text", "headers", "subject",
        "from_email", "from_name", "to", "attachments",
        false, false, "Results attached.",
        "Results attached.", CommonUtil.asMap("Reply-To", "ops@machinepublishers.com"), export.title,
        "ops@machinepublishers.com", "Machine Publishers", to, attachments));
    params.put("async", true);
    HttpURLConnection conn = null;
    String resp = null;
    Log.info("Sending email: " + export.title, false);
    try {
      conn = (HttpURLConnection) new URL(
          "https://mandrillapp.com/api/1.0/messages/send.json").openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("User-Agent", "Mandrill-Curl/1.0");
      String data = CommonUtil.gson.toJson(params, CommonUtil.objectType);
      byte[] bytes = data.getBytes("utf-8");
      conn.setRequestProperty("Content-Length", "" + bytes.length);
      OutputStream os = conn.getOutputStream();
      os.write(bytes);
      conn.connect();
      resp = IOUtils.toString(conn.getInputStream(), "utf-8");
      if (resp.contains("\"rejected\"") || resp.contains("\"invalid\"")) {
        Log.warn("Invalid/rejected email addreses");
      }
    } catch (Exception e) {
      Log.exception(e);
    }
  }
}
