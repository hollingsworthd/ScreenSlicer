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
