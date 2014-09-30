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
package com.screenslicer.common;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class CustomClassLoader extends ClassLoader {
  private ChildClassLoader childClassLoader;

  public CustomClassLoader(List<URL> classpath) {
    super(Thread.currentThread().getContextClassLoader());
    URL[] urls = classpath.toArray(new URL[classpath.size()]);
    childClassLoader = new ChildClassLoader(urls, new DetectClass(this.getParent()));
  }

  public CustomClassLoader(URL... urls) {
    super(Thread.currentThread().getContextClassLoader());
    childClassLoader = new ChildClassLoader(urls, new DetectClass(this.getParent()));
  }

  @Override
  public synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    try {
      return childClassLoader.findClass(name);
    } catch (ClassNotFoundException e) {
      return super.loadClass(name, resolve);
    }
  }

  private static class ChildClassLoader extends URLClassLoader {
    private DetectClass realParent;

    public ChildClassLoader(URL[] urls, DetectClass realParent) {
      super(urls, null);
      this.realParent = realParent;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
      try {
        Class<?> loaded = super.findLoadedClass(name);
        if (loaded != null) {
          return loaded;
        }
        return super.findClass(name);
      } catch (ClassNotFoundException e) {
        return realParent.loadClass(name);
      }
    }
  }

  private static class DetectClass extends ClassLoader {
    public DetectClass(ClassLoader parent) {
      super(parent);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
      return super.findClass(name);
    }
  }
}