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
