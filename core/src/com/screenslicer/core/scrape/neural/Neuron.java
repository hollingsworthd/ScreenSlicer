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
