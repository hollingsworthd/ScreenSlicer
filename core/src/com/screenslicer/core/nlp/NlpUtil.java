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
package com.screenslicer.core.nlp;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.apache.commons.io.IOUtils;

import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.nlp.resource.NlpResource;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.morph.WordnetStemmer;

public class NlpUtil {
  private static final WordnetStemmer stemmer;
  private static final SentenceModel sentenceModel;
  private static final TokenizerModel tokenModel;
  private static final Collection<String> ignoredTerms = new HashSet<String>();
  private static final Collection<String> validTermsByCase = new HashSet<String>(Arrays.asList(new String[] { "US", "U.S." }));
  private static final int MAX_CACHE = 300;
  private static final Map<String, Boolean> hasStemCache = new HashMap<String, Boolean>();
  private static final Map<String, Collection<String>> stemsCache = new HashMap<String, Collection<String>>();
  static {
    IDictionary dict = null;
    WordnetStemmer stemmerTmp = null;
    try {
      dict = new Dictionary(new File("./resources/dict"));
      dict.open();
      stemmerTmp = new WordnetStemmer(dict);
    } catch (Throwable t) {
      Log.exception(t);
      stemmerTmp = null;
      dict = null;
    }
    stemmer = stemmerTmp;
    try {
      List<String> lines = IOUtils.readLines(NlpResource.class.getResourceAsStream("en-very-top-words-stems"));
      ignoredTerms.addAll(lines);
    } catch (Throwable t) {
      Log.exception(t);
    }
    SentenceModel sentenceModelTmp = null;
    TokenizerModel tokenModelTmp = null;
    InputStream modelIn = null;
    try {
      modelIn = NlpResource.class.getResourceAsStream("apache-open-nlp/en-sent.bin");
      sentenceModelTmp = new SentenceModel(modelIn);
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      IOUtils.closeQuietly(modelIn);
    }
    sentenceModel = sentenceModelTmp;
    try {
      modelIn = NlpResource.class.getResourceAsStream("apache-open-nlp/en-token.bin");
      tokenModelTmp = new TokenizerModel(modelIn);
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      IOUtils.closeQuietly(modelIn);
    }
    tokenModel = tokenModelTmp;
  }

  public static Collection<String> stems(String src, boolean ignoreCommonWords, boolean oneStemOnly) {
    if (stemsCache.size() > MAX_CACHE) {
      stemsCache.clear();
    }
    String cacheKey = src + "<<>>" + Boolean.toString(ignoreCommonWords) + "<<>>" + Boolean.toString(oneStemOnly);
    if (stemsCache.containsKey(cacheKey)) {
      return stemsCache.get(cacheKey);
    }
    ignoreCommonWords = false;
    Collection<String> tokens = tokens(src, true);
    Collection<String> stems = new LinkedHashSet<String>();
    for (String word : tokens) {
      List<String> curStems = null;
      try {
        curStems = stemmer.findStems(word, null);
      } catch (Throwable t) {}
      if (curStems != null) {
        if (curStems.isEmpty()) {
          String cleanWord = word.toLowerCase().trim();
          if (cleanWord.matches(".*?[^\\p{Punct}].*")
              && (!ignoreCommonWords || !ignoredTerms.contains(cleanWord) || validTermsByCase.contains(word.trim()))) {
            stems.add(cleanWord);
          }
        } else {
          if (!ignoreCommonWords) {
            if (oneStemOnly) {
              stems.add(curStems.get(0));
            } else {
              stems.addAll(curStems);
            }
          } else {
            for (String curStem : curStems) {
              if (!ignoredTerms.contains(curStem) || validTermsByCase.contains(word.trim())) {
                stems.add(curStem);
                if (oneStemOnly) {
                  break;
                }
              }
            }
          }
        }
      }
    }
    stemsCache.put(cacheKey, stems);
    return stems;
  }

  public static boolean hasStem(String query, String target) {
    if (hasStemCache.size() > MAX_CACHE) {
      hasStemCache.clear();
    }
    String cacheKey = query + "<<>>" + target;
    if (hasStemCache.containsKey(cacheKey)) {
      return hasStemCache.get(cacheKey);
    }
    Collection<String> queryStems = stems(query, false, false);
    Collection<String> targetStems = stems(target, !stems(query, true, false).isEmpty(), false);
    for (String cur : queryStems) {
      if (targetStems.contains(cur)) {
        hasStemCache.put(cacheKey, true);
        return true;
      }
    }
    hasStemCache.put(cacheKey, false);
    return false;
  }

  public static Collection<String> tokens(String src, boolean unique) {
    Collection<String> tokens = unique ? new LinkedHashSet<String>() : new ArrayList<String>();
    String[] sentences = sentences(src);
    for (int i = 0; i < sentences.length; i++) {
      String[] tokensFromSentence = tokensFromSentence(sentences[i]);
      for (int j = 0; j < tokensFromSentence.length; j++) {
        tokens.add(tokensFromSentence[j]);
      }
    }
    return tokens;
  }

  public static String[] tokensFromSentence(String sentence) {
    Tokenizer tokenizer = new TokenizerME(tokenModel);
    return tokenizer.tokenize(sentence);
  }

  public static String[] sentences(String src) {
    if (CommonUtil.isEmpty(src)) {
      return new String[0];
    }
    SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentenceModel);
    return sentenceDetector.sentDetect(src);
  }
}
