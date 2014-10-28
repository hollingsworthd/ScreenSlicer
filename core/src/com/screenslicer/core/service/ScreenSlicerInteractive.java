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
package com.screenslicer.core.service;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Crypto;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.Scrape;
import com.screenslicer.core.scrape.type.Result;
import com.screenslicer.webapp.WebResource;

@Path("core")
public class ScreenSlicerInteractive implements WebResource {
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  public static Response execute(String reqString) {
    String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
    if (reqDecoded != null) {
      try {
        Request req = CommonUtil.gson.fromJson(reqDecoded, Request.class);
        if (req.status) {
          if (Scrape.busy()) {
            return Response.status(423).build();
          }
          return Response.status(205).build();
        } else if (req.progress) {
          ResponseProgress resp = new ResponseProgress();
          resp.progress = Scrape.progress(req.go);
          return Response.ok(CommonUtil.gson.toJson(resp)).build();
        } else if (req.go == null) {
          return buildResp(Scrape.scrape(req.site, req.query, Integer.parseInt(req.pages), req.cur, req.next));
        } else {
          return buildResp(Scrape.cached(req.go));
        }
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return Response.ok("").build();
  }

  private static Response buildResp(List<Result> results) {
    if (results != null && results != Scrape.WAITING) {
      List<ResponseResult> resp = new ArrayList<ResponseResult>();
      for (Result result : results) {
        ResponseResult r = new ResponseResult();
        r.url = result.url();
        r.title = result.title();
        r.date = result.date();
        r.summary = result.summary();
        resp.add(r);
      }
      return Response.ok(CommonUtil.gson.toJson(resp)).build();
    }
    if (results == Scrape.WAITING) {
      return Response.status(420).build();
    }
    return Response.status(410).build();
  }

  public static class Request {
    public String site;
    public String query;
    public String pages;
    public String next;
    public String cur;
    public String go;
    public boolean status;
    public boolean progress;
  }

  public static class ResponseResult {
    public String url;
    public String title;
    public String summary;
    public String date;
  }

  public static class ResponseProgress {
    public String progress;
  }
}
