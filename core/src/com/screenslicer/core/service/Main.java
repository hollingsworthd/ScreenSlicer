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

import com.screenslicer.core.scrape.Scrape;
import com.screenslicer.webapp.ExceptionListener;
import com.screenslicer.webapp.WebApp;
import com.screenslicer.webapp.WebApp.Callback;

public class Main {
  public static void main(String[] args) throws Exception {
    WebApp.start("core", 8888, false, new ExceptionListener() {
      @Override
      public void exception() {
        for (int i = 0; i < WebApp.THREADS; i++) {
          Scrape.forceQuit(i);
        }
      }
    }, new Callback() {
      @Override
      public void call() {
        Scrape.init();
      }
    });
  }
}
