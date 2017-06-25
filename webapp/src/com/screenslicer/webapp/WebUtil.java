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
package com.screenslicer.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.FileUtils;

import com.screenslicer.common.Log;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

public class WebUtil {
  private static Configuration templateConfig = new Configuration();
  public static Map<String, Template> templateCache = new HashMap<String, Template>();

  static {
    if (new File("./templates").exists()) {
      try {
        templateConfig.setDirectoryForTemplateLoading(new File("./templates"));
        templateConfig.setObjectWrapper(new DefaultObjectWrapper());
        templateConfig.setDefaultEncoding("UTF-8");
        templateConfig.setTemplateExceptionHandler(new TemplateExceptionHandler() {
          public void handleTemplateException(TemplateException te, Environment env, Writer out) {
            Log.exception(te);
          }
        });
        templateConfig.setIncompatibleImprovements(new Version(2, 3, 20));
        Collection<File> files = FileUtils.listFiles(new File("./templates"), new String[] { "ftl" }, false);
        for (File file : files) {
          templateCache.put(file.getName(), templateConfig.getTemplate(file.getName()));
        }
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
  }

  public static String get(MultivaluedMap<String, String> map, String key) {
    List<String> vals = map.get(key);
    if (vals != null) {
      return (String) map.get(key).get(0);
    }
    return null;
  }

  public static String applyTemplate(String template, Object dataModel) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Writer writer = new OutputStreamWriter(out);
      if (!WebApp.DEV) {
        templateCache.get(template + ".ftl").process(dataModel, writer);
        return out.toString();
      }
      templateConfig.getTemplate(template + ".ftl").process(dataModel, writer);
      return out.toString();
    } catch (Throwable t) {
      Log.exception(t);
      return "";
    }
  }
}
