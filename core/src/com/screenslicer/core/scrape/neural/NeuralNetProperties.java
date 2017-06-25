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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

class NeuralNetProperties {
  private static final int MAX_WEIGHT = 63;
  private final List<Integer> numInputsList;
  private final List<Neuron[][]> neuronsList;
  private static SecureRandom rand = new SecureRandom();

  static interface Configurable {
    NeuralNetProperties properties();
  }

  static NeuralNetProperties load(String str) {
    String[] lines = str.split("\n");
    List<Integer> numInputsList = new ArrayList<Integer>();
    List<Neuron[][]> neuronsList = new ArrayList<Neuron[][]>();
    for (int line = 0; line < lines.length; line++) {
      if (!lines[line].trim().isEmpty()) {
        String[] parts = lines[line].split(";");
        int numInputs = Integer.parseInt(parts[0]);
        int numRows = Integer.parseInt(parts[1]);
        int numCols = Integer.parseInt(parts[2]);
        String[] connectionParts = parts[3].split("\\+");
        Neuron[][] neurons = new Neuron[numRows][numCols];
        for (int i = 0; i < numRows; i++) {
          for (int j = 0; j < numCols; j++) {
            neurons[i][j] = new Neuron();
          }
        }
        for (int i = 0; i < connectionParts.length; i++) {
          String[] connection = connectionParts[i].split(":");
          String[] fromNeuron = connection[0].split(",");
          String[] toNeuron = connection[1].split(",");
          int row = Integer.parseInt(fromNeuron[0]);
          int col = Integer.parseInt(fromNeuron[1]);
          int toRow = Integer.parseInt(toNeuron[0]);
          int toCol = Integer.parseInt(toNeuron[1]);
          int weight = Integer.parseInt(toNeuron[2]);
          neurons[row][col].connectOutput(
              new NeuronVector(neurons[toRow][toCol], toRow, toCol, weight));
        }
        numInputsList.add(numInputs);
        neuronsList.add(neurons);
      }
    }
    return new NeuralNetProperties(numInputsList, neuronsList);
  }

  static NeuralNetProperties randomInstance(int numInputs, int numLayers, int numNodesPerLayer) {
    ++numLayers;
    final int weightMin = 0;
    final int weightMax = MAX_WEIGHT - weightMin + 1;

    int layerSize = Math.min(numNodesPerLayer, numInputs);

    int inputOffMax = Math.max((numInputs * 4) / 5, 1);
    int inputOff = rand.nextInt(inputOffMax);
    Neuron[][] connections = new Neuron[numLayers][numInputs];
    for (int i = 0; i < numLayers; i++) {
      for (int j = 0; j < numInputs; j++) {
        connections[i][j] = new Neuron();
      }
    }
    for (int i = 0; i < numLayers; i++) {
      int nodeMax = i == 0 ? numInputs : layerSize;
      for (int j = 0; j < nodeMax; j++) {
        int numConnections = (i == numLayers - 1) ? 0 : (i == numLayers - 2) ? 1 : layerSize;
        boolean noWeight = (i == 0 && rand.nextInt(numInputs) < inputOff) ? true : false;
        for (int k = 0; k < numConnections; k++) {
          int layer = i + 1;
          int pos = k;
          int weight = rand.nextInt(weightMax) + weightMin;
          weight *= (i == 0 && rand.nextBoolean()) ? -1 : 1;
          weight = noWeight ? 0 : weight;
          connections[i][j].connectOutput(new NeuronVector(connections[layer][pos], layer, pos, weight));
        }
      }
    }
    return new NeuralNetProperties(numInputs, connections);
  }

  NeuralNetProperties(List<Integer> numInputsList, List<Neuron[][]> neuronsList) {
    this.numInputsList = numInputsList;
    this.neuronsList = neuronsList;
  }

  NeuralNetProperties(List<NeuralNetProperties> propsList) {
    this.numInputsList = new ArrayList<Integer>();
    this.neuronsList = new ArrayList<Neuron[][]>();
    for (NeuralNetProperties props : propsList) {
      this.numInputsList.addAll(props.numInputs());
      this.neuronsList.addAll(props.neurons());
    }
  }

  NeuralNetProperties(int numInputs, Neuron[][] neurons) {
    this.numInputsList = new ArrayList<Integer>();
    this.neuronsList = new ArrayList<Neuron[][]>();
    this.numInputsList.add(numInputs);
    this.neuronsList.add(neurons);
  }

  NeuralNetProperties index(int i) {
    return new NeuralNetProperties(this.numInputsList.get(i), this.neuronsList.get(i));
  }

  int size() {
    return this.numInputsList.size();
  }

  List<Integer> numInputs() {
    return numInputsList;
  }

  List<Neuron[][]> neurons() {
    return neuronsList;
  }
}
