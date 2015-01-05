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
