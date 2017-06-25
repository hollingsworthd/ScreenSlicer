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
package com.screenslicer.core.util;

import java.io.File;

import com.screenslicer.core.scrape.trainer.TrainerExtract;
import com.screenslicer.core.scrape.trainer.TrainerSimple;
import com.screenslicer.core.scrape.trainer.TrainerVisitorAll;
import com.screenslicer.core.scrape.trainer.TrainerVisitorExtract;

public class Test {
  public static void main(String[] args) throws Exception {
    new TrainerSimple(new TrainerVisitorAll(), new File("./resources/neural/config"));
  }

  private static void extract(String[] args) {
    boolean comboNets = false;
    if (args.length > 2 && "combo".equals(args[2])) {
      comboNets = true;
    }
    int phaseIterations = 200;
    if (args.length > 3 && args[3] != null) {
      phaseIterations = Integer.parseInt(args[3]);
    }
    int numNets = 1;
    if (args.length > 4 && args[4] != null) {
      try {
        numNets = Integer.parseInt(args[4]);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    int numLayers = 2;
    if (args.length > 5 && args[5] != null) {
      try {
        numLayers = Integer.parseInt(args[5]);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    int numNodesPerLayer = 2;
    if (args.length > 6 && args[6] != null) {
      try {
        numNodesPerLayer = Integer.parseInt(args[6]);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    boolean solo = false;
    if (args.length > 7 && "solo".equals(args[7])) {
      solo = true;
    }
    new TrainerExtract(.95, new TrainerVisitorExtract(), phaseIterations, solo, args[0],
        args[1], numNets, numLayers, numNodesPerLayer, comboNets);
  }

}
