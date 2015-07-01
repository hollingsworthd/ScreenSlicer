/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
 */
package com.screenslicer.core.scrape.neural;

import java.util.ArrayList;
import java.util.List;

public class Neuron {
  private int[] inputs = new int[0];
  private int curInput = 0;
  private List<NeuronVector> connectionsList = new ArrayList<NeuronVector>();
  private NeuronVector[] connections = null;
  private int result = 0;

  public void reset() {
    result = 0;
    curInput = 0;
    inputs = new int[inputs.length];
  }

  public int[] weights() {
    int[] weightArray = new int[connectionsList.size()];
    int i = 0;
    for (NeuronVector vector : connectionsList) {
      weightArray[i++] = vector.weight();
    }
    return weightArray;
  }

  public NeuronVector[] connections() {
    return connectionsList.toArray(new NeuronVector[0]);
  }

  public void setWeights(int[] weights) {
    int x = 0;
    for (NeuronVector vector : connectionsList) {
      vector.setWeight(weights[x++]);
    }
    connections = null;
  }

  public void setNumInputs(int inputsSize) {
    inputs = new int[inputsSize];
  }

  public void connectOutput(NeuronVector vector) {
    connectionsList.add(vector);
    connections = null;
  }

  public void pushInput(int value) {
    inputs[curInput++] = value;
    if (curInput == inputs.length) {
      eval();
      curInput = 0;
    }
  }

  private void eval() {
    if (connections == null) {
      connections = connectionsList.toArray(new NeuronVector[0]);
    }
    result = 0;
    if (inputs.length == 1 && connections.length == 1) {
      connections[0].send(inputs[0]);
    } else {
      for (int i = 0; i < inputs.length; i++) {
        result += inputs[i];
      }
      result = result < 0 ? -1 : result > 0 ? 1 : 0;
      for (int i = 0; i < connections.length; i++) {
        connections[i].send(result);
      }
    }
  }

  public int get() {
    return result;
  }
}
