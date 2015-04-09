/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
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
package com.screenslicer.api.datatype;

import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.common.CommonUtil;

/**
 * Internal use only.
 */
public final class JobSchedule {
  public static final JobSchedule instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<JobSchedule> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final JobSchedule instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(JobSchedule.class, args);
  }

  public static final List<JobSchedule> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(JobSchedule.class, args);
  }

  public static final List<JobSchedule> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(JobSchedule.class, args);
  }

  public static final String toJson(JobSchedule obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<JobSchedule>() {}.getType());
  }

  public static final String toJson(JobSchedule[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<JobSchedule[]>() {}.getType());
  }

  public static final String toJson(List<JobSchedule> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<JobSchedule>>() {}.getType());
  }

  /**
   * Internal use only.
   */
  public int intervalInHourTenths;
  /**
   * Internal use only.
   */
  public int intervalStartHour;
  /**
   * Internal use only.
   */
  public int intervalStartDay;
  /**
   * Internal use only.
   */
  public String accountId;
  /**
   * Internal use only.
   */
  public String jobGuid;
  /**
   * Internal use only.
   */
  public String appId;
  /**
   * Internal use only.
   */
  public long created;
  /**
   * Internal use only.
   */
  public long latestRun;
}
