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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tika.Tika;

import com.screenslicer.common.Log;

public class MimeTypeFinder {
  private static Tika tika = new Tika();

  public static String probeContentType(Path path) {
    String mimeType = null;
    try {
      mimeType = Files.probeContentType(path);
    } catch (IOException e) {
      // if any exception they try Apache Tika, ignore this exception
      // do nothing
    }
    if (null == mimeType || mimeType.trim().length() == 0) {
      try {
        mimeType = tika.detect(path.toFile());
      } catch (IOException ex) {
        Log.exception(ex);
        throw new RuntimeException(ex);
      }
    }
    return mimeType;

  }
}
