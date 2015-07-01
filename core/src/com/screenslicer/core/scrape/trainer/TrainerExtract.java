/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * ScreenSlicer is made available under the terms of the GNU Affero General Public License version 3
 * with the following clarification and special exception:
 *
 *   Linking ScreenSlicer statically or dynamically with other modules is making a combined work
 *   based on ScreenSlicer. Thus, the terms and conditions of the GNU Affero General Public License
 *   version 3 cover the whole combination.
 *
 *   As a special exception, Machine Publishers, LLC gives you permission to link unmodified versions
 *   of ScreenSlicer with independent modules to produce an executable, regardless of the license
 *   terms of these independent modules, and to copy, distribute, and make available the resulting
 *   executable under terms of your choice, provided that you also meet, for each linked independent
 *   module, the terms and conditions of the license of that module. An independent module is a module
 *   which is not derived from or based on ScreenSlicer. If you modify ScreenSlicer, you may not
 *   extend this exception to your modified version of ScreenSlicer.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations, please see:
 * <https://www.gnu.org/licenses/gpl-violation.html> and email the author: ops@machinepublishers.com
 */
package com.screenslicer.core.scrape.trainer;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.neural.NeuralNetInputBoundsException;
import com.screenslicer.core.scrape.neural.NeuralNetManager;

public class TrainerExtract {
  private enum Phase {
    SOLO_RANDOM(1), SOLO_VARY(2), GROUP_VARY(3), ALL_VARY(-1);
    private final int constant;
    private static int scalar;
    private static long iteration;

    Phase(int constant) {
      this.constant = constant;
    }

    static Phase current() {
      if (iteration < SOLO_RANDOM.constant * scalar) {
        return SOLO_RANDOM;
      }
      if (iteration < SOLO_VARY.constant * scalar || scalar <= 0) {
        return SOLO_VARY;
      }
      if (iteration < GROUP_VARY.constant * scalar) {
        return GROUP_VARY;
      }
      return ALL_VARY;
    }
  };

  private static final SecureRandom rand = new SecureRandom();
  private int numInputs = 1;
  private File defaultProps;
  private final Visitor visitor;
  private final double tweakPercent;
  private final String group;
  private final String member;
  private final boolean comboNets;
  private final int numNets;
  private final int numLayers;
  private final int numNodesPerLayer;
  private final boolean solo;

  public static interface Visitor {
    void init();

    int visitNext(int page);

    void reload(boolean sort);

    int trainingDataSize();
  }

  public TrainerExtract(double tweakPercent, Visitor visitor,
      int phaseIterations, boolean solo, String group, String member, int numNets, int numLayers, int numNodesPerLayer, boolean comboNets) {
    this(tweakPercent, visitor, phaseIterations, solo, group, member, numNets, numLayers, numNodesPerLayer, comboNets, null);
  }

  public TrainerExtract(double tweakPercent, Visitor visitor,
      int phaseIterations, boolean solo, String group, String member, int numNets, int numLayers, int numNodesPerLayer, boolean comboNets, File neuralNetConfig) {
    this.tweakPercent = tweakPercent;
    this.visitor = visitor;
    Phase.scalar = phaseIterations;
    this.solo = solo;
    this.group = group;
    this.member = member;
    this.numNets = numNets;
    this.numLayers = numLayers;
    this.numNodesPerLayer = numNodesPerLayer;
    this.comboNets = comboNets;
    this.defaultProps = neuralNetConfig;
    execute();
  }

  private void execute() {
    visitor.init();
    while (true) {
      try {
        perform();
        break;
      } catch (NeuralNetInputBoundsException e) {
        Log.exception(e);
        ++numInputs;
      }
    }
  }

  private void perform() {
    long tries = 0;
    int myBest = Integer.MAX_VALUE;
    int groupBest = Integer.MAX_VALUE;
    Map<String, File> groupWinnerFiles = new HashMap<String, File>();
    Map<String, Integer> groupWinnerScores = new HashMap<String, Integer>();
    int allBest = Integer.MAX_VALUE;
    int comboBest = Integer.MAX_VALUE;
    File myBestFile = null;
    File groupBestFile = null;
    File allBestFile = null;
    File comboBestFile = null;
    boolean forceGroup = false;
    if (new File("./skip-rand").exists()) {
      tries = (Phase.SOLO_RANDOM.constant * Phase.scalar) - 1;
    }
    while (true) {
      String outId = "_" + group + "_" + member;
      ++tries;
      Phase.iteration = tries;
      File[] files = new File("./").listFiles();
      for (int i = 0; i < files.length; i++) {
        File tmpBestFile = files[i];
        if (tmpBestFile.getName().equals("quit")) {
          return;
        }
        String[] parts = tmpBestFile.getName().split("_");
        try {
          final int tmpBest = Integer.parseInt(parts[0]);
          final String tmpGroup = parts[1];
          final String tmpMember = parts[2];
          final boolean isCombo = parts.length > 3 && parts[3].equals("combo");
          final boolean isMine = tmpGroup.equals(group) && tmpMember.equals(member);
          final boolean isGroup = tmpGroup.equals(group);
          if (!isCombo) {
            if (!groupWinnerScores.containsKey(tmpGroup)) {
              groupWinnerScores.put(tmpGroup, tmpBest);
              groupWinnerFiles.put(tmpGroup, tmpBestFile);
            } else if (tmpBest < groupWinnerScores.get(tmpGroup).intValue()) {
              groupWinnerScores.put(tmpGroup, tmpBest);
              groupWinnerFiles.put(tmpGroup, tmpBestFile);
            }
            if (isMine && tmpBest < myBest) {
              myBest = tmpBest;
              myBestFile = tmpBestFile;
            }
            if (isGroup && tmpBest < groupBest) {
              groupBest = tmpBest;
              groupBestFile = tmpBestFile;
            }
            if (tmpBest < allBest) {
              allBest = tmpBest;
              allBestFile = tmpBestFile;
            }
          } else {
            if (tmpBest < comboBest) {
              comboBest = tmpBest;
              comboBestFile = tmpBestFile;
            }
          }
        } catch (Exception e) {}
      }
      if (myBest == 0) {
        return;
      }
      boolean tweak = true;
      int effectiveBest = myBest;
      if (Phase.current() == Phase.SOLO_RANDOM || myBestFile == null) {
        tweak = false;
        if (defaultProps != null) {
          NeuralNetManager.reset(defaultProps, 0);
          defaultProps = null;
        } else {
          NeuralNetManager.randomInstance(numInputs, numNets, numLayers, numNodesPerLayer);
        }
      } else if (Phase.current() == Phase.SOLO_VARY || solo) {
        NeuralNetManager.reset(myBestFile, 0);
      } else if (Phase.current() == Phase.GROUP_VARY || forceGroup) {
        NeuralNetManager.reset(groupBestFile, 0);
        effectiveBest = groupBest;
        forceGroup = false;
      } else if (Phase.current() == Phase.ALL_VARY) {
        if (comboNets) {
          outId = outId + "_combo";
          if (rand.nextBoolean() && comboBestFile != null) {
            NeuralNetManager.reset(comboBestFile, 0);
          } else {
            int upperBound = Math.min(groupWinnerScores.size(), 11);
            int range = upperBound - 2;
            range = Math.max(1, range);
            int comboSize = rand.nextInt(range) + 3;
            if (comboSize % 2 == 0) {
              if (rand.nextBoolean()) {
                --comboSize;
              } else {
                ++comboSize;
              }
            }
            comboSize = Math.min(groupWinnerScores.size(), comboSize);
            if (comboSize % 2 == 0) {
              --comboSize;
            }
            List<File> winnerFiles = new ArrayList<File>(groupWinnerFiles.values());
            for (int i = 0; i < comboSize; i++) {
              File toAdd = winnerFiles.remove(rand.nextInt(winnerFiles.size()));
              if (i == 0) {
                NeuralNetManager.reset(toAdd, 0);
              } else {
                NeuralNetManager.add(toAdd, 0);
              }
            }
          }
          forceGroup = true;
          effectiveBest = comboBest;
        } else {
          NeuralNetManager.reset(allBestFile, 0);
          effectiveBest = allBest;
        }
      } else {
        throw new IllegalStateException();
      }
      if (tweak) {
        NeuralNetManager.instance(0).tweak(tweakPercent);
      }
      int curBest = 0;
      for (int i = 0; i < visitor.trainingDataSize(); i++) {
        if (curBest >= effectiveBest) {
          break;
        }
        curBest += visitor.visitNext(1);
      }
      boolean sort = false;
      if (curBest < effectiveBest) {
        sort = true;
        forceGroup = false;
        try {
          FileUtils.writeStringToFile(new File("./" + curBest + outId),
              NeuralNetManager.asString(), "utf-8");
        } catch (IOException e) {
          System.out.println(NeuralNetManager.asString());
        }
      } else if (effectiveBest < myBest) {
        sort = true;
      }
      visitor.reload(sort);
    }
  }
}
