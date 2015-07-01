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
