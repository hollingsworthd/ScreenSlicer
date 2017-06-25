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
