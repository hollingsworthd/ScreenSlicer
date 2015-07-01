/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * ScreenSlicer is made available under the terms of the GNU Affero General Public License version 3
 * with the following clarification and special exception:
 *
 *   Linking ScreenSlicer statically or dynamically with other modules is making a combined work
 *   based on ScreenSlicer. Thus, the terms and conditions of the GNU Affero General Public License
 *   version 3 cover the whole combination.
 *
 *   As a special exception, Machine Publishers, LLC gives you permission to link unmodified versions
 *   of ScreenSlicer with independent modules to produce an executable, regardless of the license
 *   terms of these independent modules, and to copy, distribute, and make available the resulting
 *   executable under terms of your choice, provided that you also meet, for each linked independent
 *   module, the terms and conditions of the license of that module. An independent module is a module
 *   which is not derived from or based on ScreenSlicer. If you modify ScreenSlicer, you may not
 *   extend this exception to your modified version of ScreenSlicer.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations, please see:
 * <https://www.gnu.org/licenses/gpl-violation.html> and email the author: ops@machinepublishers.com
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
