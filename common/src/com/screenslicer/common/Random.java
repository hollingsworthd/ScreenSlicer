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
package com.screenslicer.common;

import java.security.SecureRandom;

import org.apache.commons.codec.digest.DigestUtils;

public class Random {
  private static final SecureRandom rand = new SecureRandom();
  private static final long[] seeds = new long[] { rand.nextLong(), rand.nextLong(), rand.nextLong() };

  public static String next() {
    StackTraceElement[] elements = new Throwable().getStackTrace();
    StringBuilder builder = new StringBuilder();
    builder.append(rand.nextLong());
    builder.append((seeds[rand.nextInt(seeds.length)] - rand.nextLong()));
    builder.append(Runtime.getRuntime().freeMemory());
    builder.append(elements[rand.nextInt(elements.length)].getClassName());
    builder.append(elements[rand.nextInt(elements.length)].getFileName());
    builder.append(elements[rand.nextInt(elements.length)].getLineNumber());
    builder.append(elements[rand.nextInt(elements.length)].getMethodName());
    builder.append(Long.toString(rand.nextLong(),
        rand.nextInt(Character.MAX_RADIX - Character.MIN_RADIX + 1) + Character.MIN_RADIX));
    return DigestUtils.sha256Hex(builder.toString());
  }
}
