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

import java.util.Collection;
import java.util.HashSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.screenslicer.api.datatype.Result;
import com.screenslicer.api.request.Extract;
import com.screenslicer.api.request.Fetch;
import com.screenslicer.api.request.FormLoad;
import com.screenslicer.api.request.FormQuery;
import com.screenslicer.api.request.KeywordQuery;
import com.screenslicer.api.request.Request;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Crypto;
import com.screenslicer.common.Log;
import com.screenslicer.core.nlp.Person;
import com.screenslicer.core.scrape.Scrape;
import com.screenslicer.webapp.WebResource;

@Path("core-batch")
public class ScreenSlicerBatch implements WebResource {
  private static final Collection<String> cancelledJobs = new HashSet<String>();
  private static final Object cancelledLock = new Object();

  public static boolean isCancelled(String runGuid) {
    if (CommonUtil.isEmpty(runGuid)) {
      return false;
    }
    synchronized (cancelledLock) {
      return cancelledJobs.contains(runGuid);
    }
  }

  private static void cancel(String runGuid) {
    if (!CommonUtil.isEmpty(runGuid)) {
      synchronized (cancelledLock) {
        cancelledJobs.add(runGuid);
      }
    }
  }

  private static void done(String runGuid) {
    if (!CommonUtil.isEmpty(runGuid)) {
      synchronized (cancelledLock) {
        cancelledJobs.remove(runGuid);
      }
    }
  }

  @POST
  @Path("busy")
  @Produces("application/json")
  @Consumes("application/json")
  public static Response isBusy(String reqString) {
    String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
    if (reqDecoded != null) {
      try {
        if (Scrape.busy() || Result.hasHolds()) {
          return Response.status(423).build();
        }
        return Response.status(205).build();
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return Response.ok(null).build();
  }

  @POST
  @Path("cancel")
  @Produces("application/json")
  @Consumes("application/json")
  public static Response cancelRun(String reqString) {
    String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
    if (reqDecoded != null) {
      try {
        Request req = CommonUtil.gson.fromJson(reqDecoded, Request.class);
        cancel(req.runGuid);
        return Response.ok(Crypto.encode("", CommonUtil.myInstance())).build();
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return Response.ok(null).build();
  }

  @POST
  @Path("fetch")
  @Produces("application/json")
  @Consumes("application/json")
  public static Response fetch(String reqString) {
    String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
    if (reqDecoded != null) {
      try {
        Fetch fetch = CommonUtil.gson.fromJson(reqDecoded, Fetch.class);
        Request req = CommonUtil.gson.fromJson(reqDecoded, Request.class);
        Result resp = new Result();
        resp.pageHtml = Scrape.get(fetch, req);
        return Response.ok(Crypto.encode(CommonUtil.gson.toJson(resp), CommonUtil.myInstance())).build();
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return Response.ok(null).build();
  }

  @POST
  @Path("load-form")
  @Produces("application/json")
  @Consumes("application/json")
  public static Response loadForm(String reqString) {
    String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
    if (reqDecoded != null) {
      try {
        FormLoad load = CommonUtil.gson.fromJson(reqDecoded, FormLoad.class);
        Request req = CommonUtil.gson.fromJson(reqDecoded, Request.class);
        return Response.ok(Crypto.encode(CommonUtil.gson.toJson(Scrape.loadForm(load, req)), CommonUtil.myInstance())).build();
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return Response.ok(null).build();
  }

  @POST
  @Path("query-form")
  @Produces("application/json")
  @Consumes("application/json")
  public static Response queryForm(String reqString) {
    String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
    if (reqDecoded != null) {
      Request req = null;
      try {
        FormQuery query = CommonUtil.gson.fromJson(reqDecoded, FormQuery.class);
        req = CommonUtil.gson.fromJson(reqDecoded, Request.class);
        return Response.ok(Crypto.encode(CommonUtil.gson.toJson(Scrape.scrape(query, req)), CommonUtil.myInstance())).build();
      } catch (Throwable t) {
        Log.exception(t);
      } finally {
        if (req != null) {
          done(req.runGuid);
        }
      }
    }
    return Response.ok(null).build();
  }

  @POST
  @Path("query-keyword")
  @Produces("application/json")
  @Consumes("application/json")
  public static Response queryKeyword(String reqString) {
    String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
    if (reqDecoded != null) {
      Request req = null;
      try {
        KeywordQuery query = CommonUtil.gson.fromJson(reqDecoded, KeywordQuery.class);
        req = CommonUtil.gson.fromJson(reqDecoded, Request.class);
        return Response.ok(Crypto.encode(CommonUtil.gson.toJson(Scrape.scrape(query, req)), CommonUtil.myInstance())).build();
      } catch (Throwable t) {
        Log.exception(t);
      } finally {
        if (req != null) {
          done(req.runGuid);
        }
      }
    }
    return Response.ok(null).build();
  }

  @POST
  @Path("expand-search-result")
  @Produces("application/json")
  @Consumes("application/json")
  public static Response expandSearchResult(String reqString) {
    String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
    if (reqDecoded != null) {
      Request req = null;
      try {
        Result searchResult = CommonUtil.gson.fromJson(reqDecoded, Result.class);
        if (searchResult.open()) {
          Result.removeHold();
        }
        return Response.ok(Crypto.encode(CommonUtil.gson.toJson(searchResult), CommonUtil.myInstance())).build();
      } catch (Throwable t) {
        Log.exception(t);
      } finally {
        if (req != null) {
          done(req.runGuid);
        }
      }
    }
    return Response.ok(null).build();
  }

  @POST
  @Path("extract-person")
  @Produces("application/json")
  @Consumes("application/json")
  public static Response extractPerson(String reqString) {
    String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
    if (reqDecoded != null) {
      try {
        Extract extract = CommonUtil.gson.fromJson(reqDecoded, Extract.class);
        return Response.ok(Crypto.encode(CommonUtil.gson.toJson(Person.extractContact(extract.content)), CommonUtil.myInstance())).build();
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return Response.ok(null).build();
  }
}
