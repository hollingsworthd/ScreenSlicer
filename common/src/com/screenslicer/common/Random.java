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
