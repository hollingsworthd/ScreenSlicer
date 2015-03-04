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
package com.screenslicer.core.scrape.neural;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.screenslicer.common.Log;
import com.screenslicer.webapp.WebApp;

public class NeuralNetManager {
  private static NeuralNetVoters[] net = new NeuralNetVoters[WebApp.THREADS];
  static {
    for (int i = 0; i < WebApp.THREADS; i++) {
      net[i] = new NeuralNetVoters();
    }
  }

  private NeuralNetManager() {

  }

  //TODO make thread safe
  public static NeuralNet instance(int thread) {
    return net[thread];
  }

  public static String asString() {
    return net.toString();
  }

  public static NeuralNet randomInstance(int numInputs,
      int numNets, int numLayers, int numNodesPerLayer) {
    for (int i = 0; i < numNets; i++) {
      if (i == 0) {
        reset(new NeuralNetVote(NeuralNetProperties.randomInstance(
            numInputs, numLayers, numNodesPerLayer)), 0);
      } else {
        add(new NeuralNetVote(NeuralNetProperties.randomInstance(
            numInputs, numLayers, numNodesPerLayer)), 0);
      }
    }
    return net[0];
  }

  public static void add(String config, int thread) {
    if (config != null) {
      net[thread].add(NeuralNetProperties.load(config));
    }
  }

  public static void add(File config, int thread) {
    if (config != null) {
      try {
        add(FileUtils.readFileToString(config, "utf-8"), thread);
      } catch (Throwable t) {
        Log.exception(t);
        throw new RuntimeException(t);
      }
    }
  }

  public static void add(NeuralNet nn, int thread) {
    if (nn != null) {
      net[thread].add(((NeuralNetProperties.Configurable) nn).properties());
    }
  }

  public static void reset(String config, int thread) {
    net[thread] = new NeuralNetVoters();
    if (config != null) {
      net[thread].add(NeuralNetProperties.load(config));
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static void reset(File config, int thread) {
    try {
      reset(FileUtils.readFileToString(config, "utf-8"), thread);
    } catch (Throwable t) {
      Log.exception(t);
      throw new RuntimeException(t);
    }
  }

  public static void reset(NeuralNet nn, int thread) {
    net[thread] = new NeuralNetVoters();
    if (nn != null) {
      net[thread].add(((NeuralNetProperties.Configurable) nn).properties());
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static NeuralNet copy() {
    NeuralNetVoters copy = new NeuralNetVoters();
    copy.add(NeuralNetProperties.load(net.toString()));
    return copy;
  }
}
