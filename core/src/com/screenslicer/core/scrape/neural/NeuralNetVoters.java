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

class NeuralNetVoters implements NeuralNet, NeuralNetProperties.Configurable {
  private final List<NeuralNetVote> nets = new ArrayList<NeuralNetVote>();
  private final List<NeuralNetProperties> propsList = new ArrayList<NeuralNetProperties>();
  private NeuralNetProperties props = null;
  private int voter = 0;
  private static final SecureRandom rand = new SecureRandom();

  void add(NeuralNetProperties props) {
    for (int i = 0; i < props.size(); i++) {
      NeuralNetVote vote = new NeuralNetVote(props.index(i));
      nets.add(vote);
      propsList.add(props.index(i));
    }
    this.props = new NeuralNetProperties(propsList);
  }

  @Override
  public void tweak(double percent) {
    if (nets.size() > 1 && rand.nextBoolean()) {
      nets.get(rand.nextInt(nets.size())).tweak(percent);
    } else {
      for (NeuralNetVote net : nets) {
        net.tweak(percent);
      }
    }
  }

  @Override
  public void set(int inputSlot, int input) {
    for (NeuralNetVote net : nets) {
      net.set(inputSlot, input);
    }
  }

  @Override
  public int pull() {
    if (voter >= nets.size()) {
      throw new IllegalStateException();
    }
    return nets.get(voter).pull();
  }

  @Override
  public void resetNext() {
    voter = 0;
  }

  @Override
  public boolean hasNext() {
    return voter < nets.size();
  }

  @Override
  public void next() {
    ++voter;
  }

  @Override
  public boolean isMulti() {
    return nets.size() > 1;
  }

  @Override
  public boolean isLast() {
    return voter + 1 == nets.size();
  }

  @Override
  public int multiSize() {
    return nets.size();
  }

  @Override
  public NeuralNetProperties properties() {
    return props;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (NeuralNetVote net : nets) {
      builder.append(net.toString());
      builder.append("\n");
    }
    return builder.toString();
  }
}
