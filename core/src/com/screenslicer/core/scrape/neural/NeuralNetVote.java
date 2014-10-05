/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * 717 Martin Luther King Dr W Ste I, Cincinnati, Ohio 45220
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

import java.security.SecureRandom;

class NeuralNetVote implements NeuralNet, NeuralNetProperties.Configurable {
  private static final double FLOAT_SIGNIFICANT_DIGITS = 1000d;
  private static final int NODES_TO_CHANGE_MAX = 100;
  private static final int NODES_TO_CHANGE_MIN = 25;
  private static final int DRAW_ITERATIONS_MIN = 2;
  private static final int DRAW_ITERATIONS_MAX = 4;
  private static final int ZEROING_RATIO_DENOM = 20;
  private static final int ZEROING_MAX = 100;
  private static final int TWEAK_REPEAT_MIN = 1;
  private static final int TWEAK_REPEAT_MAX = 2;
  private final NeuralNetProperties props;
  private final Neuron[][] neurons;
  private final int numInputs;
  private int[][][] prevWeights = null;
  private static final SecureRandom rand = new SecureRandom();
  private boolean hasNext = true;
  private boolean refine = true;

  NeuralNetVote(NeuralNetProperties props) {
    if (props.numInputs().size() != 1 || props.neurons().size() != 1) {
      throw new IllegalStateException("Invalid size");
    }
    this.numInputs = props.numInputs().get(0);
    this.neurons = props.neurons().get(0);
    this.props = props;

    int[][] inputsTally = new int[neurons.length][neurons[0].length];
    for (int i = 0; i < inputsTally.length; i++) {
      for (int j = 0; j < inputsTally[0].length; j++) {
        inputsTally[i][j] = 0;
      }
    }
    for (int i = 0; i < neurons.length; i++) {
      for (int j = 0; j < neurons[0].length; j++) {
        if (neurons[i][j] != null) {
          NeuronVector[] outputs = neurons[i][j].connections();
          for (int k = 0; k < outputs.length; k++) {
            ++inputsTally[outputs[k].x()][outputs[k].y()];
          }
        }
      }
    }
    for (int i = 0; i < inputsTally.length; i++) {
      for (int j = 0; j < inputsTally[0].length; j++) {
        if (i == 0) {
          if (j < this.numInputs) {
            neurons[i][j].setNumInputs(1);
          } else {
            neurons[i][j].setNumInputs(0);
          }
        } else {
          neurons[i][j].setNumInputs(inputsTally[i][j]);
        }
      }
    }
  }

  private static int draw(int max, int iterations) {
    int result = max;
    for (int i = 0; i < iterations; i++) {
      result = rand.nextInt(result) + 1;
    }
    return result - 1;
  }

  @Override
  public void tweak(double percent) {
    refine = rand.nextBoolean();
    int repeat = rand.nextInt(refine ? TWEAK_REPEAT_MIN : TWEAK_REPEAT_MAX) + 1;
    for (int i = 0; i < repeat; i++) {
      tweakHelper(percent);
    }
  }

  private void tweakHelper(double percent) {
    int iterations = rand.nextInt(refine ? DRAW_ITERATIONS_MAX : DRAW_ITERATIONS_MIN) + 1;
    prevWeights = new int[neurons.length][neurons[0].length][];
    int nodesToChange = rand.nextInt(refine ? NODES_TO_CHANGE_MIN : NODES_TO_CHANGE_MAX);
    int zeroMax = ZEROING_MAX;
    int zero = rand.nextInt(zeroMax / ZEROING_RATIO_DENOM);
    int unzeroMax = ZEROING_MAX;
    int unzero = rand.nextInt(unzeroMax / ZEROING_RATIO_DENOM);
    boolean allowZero = rand.nextBoolean();
    boolean allowUnzero = rand.nextBoolean();
    for (int i = 0; i < neurons.length; i++) {
      for (int j = 0; j < neurons[0].length; j++) {
        int[] weights = neurons[i][j].weights();
        int[] newWeights = new int[weights.length];
        boolean doUnzero = !allowUnzero ? false : ((i == 0 && rand.nextInt(unzeroMax) < unzero) ? true : false);
        boolean doZero = !allowZero ? false : ((i == 0 && rand.nextInt(zeroMax) < zero) ? true : false);
        for (int k = 0; k < weights.length; k++) {
          double weight = (double) weights[k];
          if (rand.nextInt(NODES_TO_CHANGE_MAX) <= nodesToChange) {
            if (weights[k] == 0 && doUnzero) {
              weight = rand.nextInt(draw(200, iterations) + 1);
              weight *= ((i == 0 && rand.nextBoolean()) ? -1 : 1);
            } else if (doZero) {
              weight = 0;
            } else {
              int deltaInt = draw((int) (percent * FLOAT_SIGNIFICANT_DIGITS), iterations);
              double delta = ((double) deltaInt) / FLOAT_SIGNIFICANT_DIGITS;
              weight = rand.nextBoolean() ?
                  weight - (weight * delta) : weight * (delta + 1d);
            }
          }
          newWeights[k] = (int) weight;
        }
        prevWeights[i][j] = weights;
        neurons[i][j].setWeights(newWeights);
      }
    }
  }

  @Override
  public void set(int inputSlot, int input) {
    if (inputSlot >= neurons[0].length) {
      throw new NeuralNetInputBoundsException();
    }
    neurons[0][inputSlot].pushInput(input);
  }

  @Override
  public int pull() {
    if (!hasNext) {
      throw new IllegalStateException();
    }
    int result = 0;
    for (int i = 0; i < neurons[neurons.length - 1].length; i++) {
      result += neurons[neurons.length - 1][i].get();
    }
    for (int i = 0; i < neurons.length; i++) {
      for (int j = 0; j < neurons[0].length; j++) {
        neurons[i][j].reset();
      }
    }
    return result;
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public void next() {
    hasNext = false;
  }

  @Override
  public void resetNext() {
    hasNext = true;
  }

  @Override
  public boolean isMulti() {
    return false;
  }

  @Override
  public boolean isLast() {
    return true;
  }

  @Override
  public int multiSize() {
    return 1;
  }

  @Override
  public NeuralNetProperties properties() {
    return props;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(numInputs + ";" + neurons.length + ";" + neurons[0].length + ";");
    for (int i = 0; i < neurons.length; i++) {
      for (int j = 0; j < neurons[0].length; j++) {
        NeuronVector[] connections = neurons[i][j].connections();
        for (int k = 0; k < connections.length; k++) {
          builder.append(i + "," + j + ":" + connections[k].x() + "," + connections[k].y() + "," + connections[k].weight() + "+");
        }
      }
    }

    return builder.toString();
  }
}
