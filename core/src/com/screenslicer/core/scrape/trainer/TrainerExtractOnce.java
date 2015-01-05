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
package com.screenslicer.core.scrape.trainer;

import java.io.File;

import com.screenslicer.core.scrape.neural.NeuralNetManager;

public class TrainerExtractOnce {
  private final Visitor visitor = new TrainerVisitorExtractOnce();

  public static interface Visitor {
    void init();

    int visit(int curTrainingData, int page);

    int trainingDataSize();
  }

  public TrainerExtractOnce(File[] props) {
    NeuralNetManager.reset(props[0]);
    for (int i = 1; i < props.length; i++) {
      NeuralNetManager.add(props[i]);
    }
    visitor.init();
    perform();
  }

  private void perform() {
    for (int i = 0; i < visitor.trainingDataSize(); i++) {
      visitor.visit(i, 1);
    }
  }
}
