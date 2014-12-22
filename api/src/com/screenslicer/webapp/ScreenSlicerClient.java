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
package com.screenslicer.webapp;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import com.screenslicer.api.request.Cancel;
import com.screenslicer.api.request.Request;
import com.screenslicer.common.CommonFile;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Crypto;
import com.screenslicer.common.Log;
import com.screenslicer.common.Spreadsheet;

@Path("/custom-app")
public final class ScreenSlicerClient implements ClientWebResource {
  private static final Collection<String> cancelledJobs = new HashSet<String>();
  private static final Object cancelledLock = new Object();
  private static final long WAIT = 2000;
  private static final Object doneMapLock = new Object();
  private static Map<String, AtomicBoolean> doneMap = new HashMap<String, AtomicBoolean>();
  private static AtomicLong latestThread = new AtomicLong();
  private static AtomicLong curThread = new AtomicLong();
  protected static final int PORT = 9000;
  private static ScreenSlicer.CustomApp customApp;

  public static final void init(ScreenSlicer.CustomApp customApp) {
    ScreenSlicerClient.customApp = customApp;
  }

  @Path("result")
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  public static final Response result(String reqString) {
    try {
      if (reqString != null) {
        final String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
        if (reqDecoded != null) {
          Request request = CommonUtil.gson.fromJson(reqDecoded, Request.class);
          List<String> resultNames = CommonFile.readLines(new File("./data/" + request.runGuid + "-result-names"));
          for (int i = 0; i < resultNames.size(); i++) {
            if (request.outputNames[0].equals(resultNames.get(i))) {
              return Response.ok(Crypto.encode(Base64.encodeBase64String(
                  CommonFile.readFileToByteArray(new File("./data/" + request.runGuid + "-result" + i))),
                  CommonUtil.myInstance())).build();
            }
          }
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  @Path("cancel")
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  public static final Response cancel(String reqString) {
    try {
      if (reqString != null) {
        final String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
        if (reqDecoded != null) {
          Request request = CommonUtil.gson.fromJson(reqDecoded, Request.class);
          synchronized (cancelledLock) {
            cancelledJobs.add(request.runGuid);
          }
          Cancel cancel = new Cancel();
          cancel.instances = request.instances;
          cancel.runGuid = request.runGuid;
          ScreenSlicer.cancel(cancel);
          return Response.ok(Crypto.encode("", CommonUtil.myInstance())).build();
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  @Path("context")
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  public static final Response context(String reqString) {
    try {
      if (reqString != null) {
        final String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
        if (reqDecoded != null) {
          Request request = CommonUtil.gson.fromJson(reqDecoded, Request.class);
          File file = new File("./data/" + request.runGuid + "-context");
          if (reqDecoded != null && file.exists()) {
            return Response.ok(Crypto.encode(Crypto.decode(CommonFile.readFileToString(
                file), CommonUtil.myInstance()))).build();
          }
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  @Path("started")
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  public static final Response started(String reqString) {
    try {
      if (reqString != null) {
        final String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
        if (reqDecoded != null) {
          Request request = CommonUtil.gson.fromJson(reqDecoded, Request.class);
          Collection<File> files = FileUtils.listFiles(new File("./data"), null, false);
          List<Map<String, Object>> started = new ArrayList<Map<String, Object>>();
          for (File file : files) {
            if (file.getName().endsWith("-meta" + request.appId)) {
              List<String> lines = CommonFile.readLines(file);
              if (lines != null && lines.size() == 3) {
                started.add(CommonUtil.asObjMap("runGuid", "jobId", "jobGuid", "Started", "Started-UTC",
                    file.getName().split("-meta" + request.appId)[0], lines.get(0), lines.get(1), CommonUtil.asUtc(lines.get(2)), lines.get(2)));
              }
            }
          }
          Collections.sort(started, new CommonUtil.MapDateComparator("Started-UTC"));
          Map<String, Map<String, Object>> resp = new LinkedHashMap<String, Map<String, Object>>();
          for (Map<String, Object> cur : started) {
            resp.put((String) cur.get("runGuid"), cur);
          }
          return Response.ok(Crypto.encode(CommonUtil.gson.toJson(resp, CommonUtil.objectType),
              CommonUtil.myInstance())).build();
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  @Path("finished")
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  public static final Response finished(String reqString) {
    try {
      if (reqString != null) {
        final String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
        if (reqDecoded != null) {
          Request request = CommonUtil.gson.fromJson(reqDecoded, Request.class);
          Collection<File> files = FileUtils.listFiles(new File("./data"), null, false);
          List<Map<String, Object>> finished = new ArrayList<Map<String, Object>>();
          for (File file : files) {
            if (file.getName().endsWith("-meta" + request.appId)) {
              List<String> lines = CommonFile.readLines(file);
              if (lines != null && lines.size() == 4) {
                String runGuid = file.getName().split("-meta" + request.appId)[0];
                List<String> resultNames = CommonFile.readLines(new File("./data/" + runGuid + "-result-names"));
                finished.add(CommonUtil.asObjMap(
                    "runGuid", "jobId", "jobGuid", "resultNames",
                    "Started", "Finished", "Started-UTC", "Finished-UTC",
                    runGuid, lines.get(0), lines.get(1), resultNames,
                    CommonUtil.asUtc(lines.get(2)), CommonUtil.asUtc(lines.get(3)), lines.get(2), lines.get(3)));
              }
            }
          }
          Collections.sort(finished, new CommonUtil.MapDateComparator("Finished-UTC"));
          Map<String, Map<String, Object>> resp = new LinkedHashMap<String, Map<String, Object>>();
          for (Map<String, Object> cur : finished) {
            resp.put((String) cur.get("runGuid"), cur);
          }
          return Response.ok(Crypto.encode(CommonUtil.gson.toJson(resp, CommonUtil.objectType),
              CommonUtil.myInstance())).build();
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  @Path("configure")
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  public static final Response configure(String reqString) {
    try {
      if (reqString != null) {
        final String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
        if (reqDecoded != null) {
          final Map<String, Object> args = CommonUtil.gson.fromJson(reqDecoded, CommonUtil.objectType);
          final Request request = CommonUtil.gson.fromJson(reqDecoded, Request.class);
          Field[] fields = request.getClass().getFields();
          for (Field field : fields) {
            args.remove(field.getName());
          }
          Map<String, Object> conf = customApp.configure(request, args);
          if (conf != null) {
            return Response.ok(Crypto.encode(
                CommonUtil.gson.toJson(conf, CommonUtil.objectType), CommonUtil.myInstance())).build();
          }
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  public static final boolean isCancelled(String runGuid) {
    if (!CommonUtil.isEmpty(runGuid)) {
      synchronized (cancelledLock) {
        return cancelledJobs.contains(runGuid);
      }
    }
    return false;
  }

  @Path("create")
  @POST
  @Consumes("application/json")
  @Produces("application/json")
  public static final Response create(String reqString) {
    if (reqString != null) {
      final String reqDecoded = Crypto.decode(reqString, CommonUtil.myInstance());
      if (reqDecoded != null) {
        final Map<String, Object> args = CommonUtil.gson.fromJson(reqDecoded, CommonUtil.objectType);
        final Request request = CommonUtil.gson.fromJson(reqDecoded, Request.class);
        Field[] fields = request.getClass().getFields();
        for (Field field : fields) {
          args.remove(field.getName());
        }
        new Thread(new Runnable() {
          @Override
          public void run() {
            String myInstance = null;
            AtomicBoolean myDone = null;
            try {
              CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-meta" + request.appId),
                  request.jobId + "\n" + request.jobGuid + "\n" + Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis(), false);
              CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-context"), Crypto.encode(reqDecoded), false);
              Map<String, AtomicBoolean> myDoneMap = new HashMap<String, AtomicBoolean>();
              synchronized (doneMapLock) {
                for (int i = 0; i < request.instances.length; i++) {
                  if (!doneMap.containsKey(request.instances[i])) {
                    doneMap.put(request.instances[i], new AtomicBoolean(true));
                  }
                }
                myDoneMap.putAll(doneMap);
              }
              long myThread = latestThread.incrementAndGet();
              while (true) {
                if (isCancelled(request.runGuid)) {
                  curThread.incrementAndGet();
                  throw new CancellationException();
                }
                if (myThread == curThread.get() + 1) {
                  for (Map.Entry<String, AtomicBoolean> done : myDoneMap.entrySet()) {
                    if (done.getValue().compareAndSet(true, false)) {
                      if (ScreenSlicer.isBusy(done.getKey())) {
                        done.getValue().set(true);
                      } else {
                        myInstance = done.getKey();
                        myDone = done.getValue();
                        break;
                      }
                    }
                  }
                  if (myInstance != null) {
                    break;
                  }
                }
                try {
                  Thread.sleep(WAIT);
                } catch (Throwable t) {
                  Log.exception(t);
                }
              }
              curThread.incrementAndGet();
              int outputNumber = 0;
              request.instances = new String[] { myInstance };
              Map<String, List<List<String>>> tables = customApp.tableData(request, args);
              Map<String, Map<String, Object>> jsons = customApp.jsonData(request, args);
              Map<String, byte[]> binaries = customApp.binaryData(request, args);
              request.emailExport.attachments = new LinkedHashMap<String, byte[]>();
              if (tables != null) {
                for (Map.Entry<String, List<List<String>>> table : tables.entrySet()) {
                  boolean xlsFail = false;
                  if (table.getKey().toLowerCase().endsWith(".xls")) {
                    try {
                      byte[] result = Spreadsheet.xls(table.getValue());
                      CommonFile.writeByteArrayToFile(new File("./data/" + request.runGuid + "-result" + outputNumber), result, false);
                      CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-result-names"),
                          escapeName(table.getKey()) + "\n", true);
                      ++outputNumber;
                      if (request.emailResults) {
                        request.emailExport.attachments.put(table.getKey(), result);
                      }
                    } catch (Throwable t) {
                      Log.exception(t);
                      xlsFail = true;
                    }
                  }
                  if (xlsFail || table.getKey().toLowerCase().endsWith(".csv")) {
                    String outputName = table.getKey();
                    outputName = xlsFail ? outputName.substring(0, outputName.lastIndexOf(".")) + ".csv" : outputName;
                    String result = Spreadsheet.csv(table.getValue());
                    CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-result" + outputNumber), result, false);
                    CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-result-names"),
                        escapeName(outputName) + "\n", true);
                    ++outputNumber;
                    if (request.emailResults) {
                      request.emailExport.attachments.put(table.getKey(), result.getBytes("utf-8"));
                    }
                  } else if (table.getKey().toLowerCase().endsWith(".xcsv")) {
                    String result = Spreadsheet.csv(table.getValue());
                    CommonUtil.internalHttpCall(CommonUtil.myInstance(),
                        "https://" + CommonUtil.myInstance() + ":8000/_/"
                            + Crypto.fastHash(table.getKey() + ":" + request.runGuid),
                        "PUT", CommonUtil.asMap("Content-Type", "text/csv; charset=utf-8"),
                        result.getBytes("utf-8"), null);
                    CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-result-names"),
                        escapeName(table.getKey()) + "\n", true);
                    ++outputNumber;
                    if (request.emailResults) {
                      request.emailExport.attachments.put(table.getKey(), result.getBytes("utf-8"));
                    }
                  } else {
                    String result = CommonUtil.gson.toJson(table.getValue(), table.getValue().getClass());
                    CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-result" + outputNumber), result, false);
                    CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-result-names"),
                        escapeName(table.getKey()) + "\n", true);
                    ++outputNumber;
                    if (request.emailResults) {
                      request.emailExport.attachments.put(table.getKey(), result.getBytes("utf-8"));
                    }
                  }
                }
              }
              if (jsons != null) {
                for (Map.Entry<String, Map<String, Object>> json : jsons.entrySet()) {
                  String result = CommonUtil.gson.toJson(json.getValue(), CommonUtil.objectType);
                  CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-result" + outputNumber), result, false);
                  CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-result-names"),
                      escapeName(json.getKey()) + "\n", true);
                  ++outputNumber;
                  if (request.emailResults) {
                    request.emailExport.attachments.put(json.getKey(), result.getBytes("utf-8"));
                  }
                }
              }
              if (binaries != null) {
                for (Map.Entry<String, byte[]> binary : binaries.entrySet()) {
                  CommonFile.writeByteArrayToFile(new File("./data/" + request.runGuid + "-result" + outputNumber), binary.getValue(), false);
                  CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-result-names"),
                      escapeName(binary.getKey()) + "\n", true);
                  ++outputNumber;
                  if (request.emailResults) {
                    request.emailExport.attachments.put(binary.getKey(), binary.getValue());
                  }
                }
              }
              if (request.emailResults) {
                ScreenSlicer.export(request, request.emailExport);
              }
            } catch (Throwable t) {
              Log.exception(t);
            } finally {
              try {
                CommonFile.writeStringToFile(new File("./data/" + request.runGuid + "-meta" + request.appId), "\n" + Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis(), true);
              } catch (Throwable t) {
                Log.exception(t);
              }
              myDone.set(true);
              synchronized (cancelledLock) {
                cancelledJobs.remove(request.runGuid);
              }
            }
          }
        }).start();
        return Response.ok(Crypto.encode(request.runGuid, CommonUtil.myInstance())).build();
      }
    }
    return null;
  }

  private static final String escapeName(String str) {
    str = CommonUtil.stripNewlines(str);
    try {
      return new URI(str).toASCIIString().replace("%20", " ");
    } catch (Throwable t1) {
      try {
        return URLEncoder.encode(str, "utf-8").replace("+", " ");
      } catch (Throwable t2) {
        return str.replaceAll("[^\\w.]", "");
      }
    }
  }
}
