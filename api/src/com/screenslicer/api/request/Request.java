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
package com.screenslicer.api.request;

import java.util.List;
import java.util.Map;

import com.screenslicer.common.CommonUtil;

public final class Request {
  public static final Request instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Request.class, args);
  }

  public static final List<Request> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Request.class, args);
  }

  public String[] instances;
  public int timeout = 25;
  public Proxy proxy = new Proxy();
  public String appId;
  public String jobId;
  public String jobGuid;
  public String runGuid;
  public String[] outputNames;
  public boolean emailResults;
  public EmailExport emailExport;
  public boolean continueSession = false;
}
