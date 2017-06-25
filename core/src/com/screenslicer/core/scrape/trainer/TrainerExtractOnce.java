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
    NeuralNetManager.reset(props[0], 0);
    for (int i = 1; i < props.length; i++) {
      NeuralNetManager.add(props[i], 0);
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
