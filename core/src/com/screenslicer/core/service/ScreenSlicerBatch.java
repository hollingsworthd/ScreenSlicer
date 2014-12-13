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

import java.util.Collection;
import java.util.HashSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.screenslicer.api.datatype.SearchResult;
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
        if (Scrape.busy()) {
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
        SearchResult resp = new SearchResult();
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
        SearchResult searchResult = CommonUtil.gson.fromJson(reqDecoded, SearchResult.class);
        searchResult.open();
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
