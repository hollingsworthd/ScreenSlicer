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
package com.screenslicer.core.service;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.screenslicer.api.datatype.Result;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Crypto;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.Scrape;
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
        r.url = result.url;
        r.title = result.title;
        r.date = result.date;
        r.summary = result.summary;
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
