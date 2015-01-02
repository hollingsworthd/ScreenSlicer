/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
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
package com.screenslicer.core.nlp;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

import org.apache.commons.io.IOUtils;

import com.screenslicer.api.datatype.Contact;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.nlp.resource.NlpResource;

public class Person {
  public static final Pattern email = Pattern.compile("(?<=\r\n|[\r\n]|^).*?([^&\"':><\\s@]+@[^&\"':><\\s@.]+\\.[^&\"':><\\s@]*[^&\"':><\\s@.]+).*(?=\r\n|[\r\n]|$)", Pattern.UNICODE_CHARACTER_CLASS);
  public static final Pattern phone = Pattern.compile("(?<=\r\n|[\r\n]|^).*?\\s*((?=.?(?:([0-9].*?){7,15}))[-+().0-9\\s]{8,20}[0-9]).*(?=\r\n|[\r\n]|$)", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern invalidNameChars = Pattern.compile("!|@|#|\\$|%|\\^|&|\\*|\\(|\\)|_|\\+|=|\\{|\\}|\\[|\\]|\\||:|;|\"|'|<|>|\\?|/|\\\\|~|`", Pattern.UNICODE_CHARACTER_CLASS);
  private static Collection<String> firstNames = null;
  private static Collection<String> lastNames = null;
  private static Collection<String> firstNamesPopular = null;
  private static Collection<String> englishWords = null;
  private static TokenNameFinderModel nameModel = null;

  static {
    InputStream modelIn = null;
    try {
      modelIn = NlpResource.class.getResourceAsStream("apache-open-nlp/en-ner-person.bin");
      nameModel = new TokenNameFinderModel(modelIn);
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      IOUtils.closeQuietly(modelIn);
    }

    try {
      firstNames = new HashSet<String>(IOUtils.readLines(NlpResource.class.getResourceAsStream("us-firstnames")));
    } catch (Throwable t) {
      Log.exception(t);
    }

    try {
      firstNamesPopular = new HashSet<String>(IOUtils.readLines(NlpResource.class.getResourceAsStream("us-firstnames-popular")));
    } catch (Throwable t) {
      Log.exception(t);
    }

    try {
      lastNames = new HashSet<String>(IOUtils.readLines(NlpResource.class.getResourceAsStream("us-surnames")));
    } catch (Throwable t) {
      Log.exception(t);
    }

    try {
      englishWords = new HashSet<String>(IOUtils.readLines(NlpResource.class.getResourceAsStream("en-words")));
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  public static Contact extractContact(String src) {
    if (CommonUtil.isEmpty(src)) {
      return new Contact();
    }
    List<String> lines = new ArrayList<String>();
    String emailMatch = null;
    Matcher matcher = email.matcher(src);
    if (matcher.find()) {
      emailMatch = matcher.group(1);
      lines.add(matcher.group(0));
    }
    String phoneMatch = null;
    matcher = phone.matcher(src);
    if (matcher.find()) {
      phoneMatch = matcher.group(1);
      lines.add(matcher.group(0));
    }
    Collection<String> names = new HashSet<String>();
    for (String line : lines) {
      String name = extractName(line, false, false);
      if (!CommonUtil.isEmpty(name)) {
        names.add(name);
      }
    }
    String name = null;
    if (names.size() == 1) {
      name = names.iterator().next();
    }
    if (name == null) {
      name = extractName(src, true, false);
    }
    Contact person = new Contact();
    person.name = name;
    person.email = emailMatch;
    person.phone = phoneMatch;
    return person;
  }

  public static String extractName(String src, boolean strict, boolean dictionaryOnly) {
    NameFinderME nameFinder = new NameFinderME(nameModel);
    String[] sentences = NlpUtil.sentences(src);
    Collection<String> nlpNames = new HashSet<String>();
    Collection<String> nlpFallbacks = new HashSet<String>();
    Collection<String> dictionaryNames = new HashSet<String>();
    Collection<String> dictionaryFallbacks = new HashSet<String>();
    for (int i = 0; i < sentences.length; i++) {
      String[] tokens = NlpUtil.tokensFromSentence(sentences[i]);
      for (int j = 0; j < tokens.length; j++) {
        String first = tokens[j];
        String last = null;
        if (j + 1 < tokens.length) {
          last = tokens[j + 1];
        }
        if (isFirstName(first, strict)
            && isLastName(last)
            && isFullName(first + " " + last, strict)) {
          dictionaryNames.add(first + " " + last);
        } else if (!strict && isFirstName(first, strict)) {
          dictionaryFallbacks.add(first);
        }
      }
      Span[] spans = nameFinder.find(tokens);
      for (int j = 0; !dictionaryOnly && j < spans.length; j++) {
        List<String> curNames = Arrays.asList(Span.spansToStrings(spans, tokens));
        for (String curName : curNames) {
          if (curName.contains(" ")
              && isFullName(curName, strict)) {
            nlpNames.add(curName);
          } else if (isFirstName(curName, strict)) {
            nlpFallbacks.add(curName);
          }
        }
      }
    }
    if (nlpNames.isEmpty()) {
      nlpNames = nlpFallbacks;
    }
    if (dictionaryNames.isEmpty()) {
      dictionaryNames = dictionaryFallbacks;
    }

    if ((dictionaryOnly || nlpNames.size() != 1) && dictionaryNames.size() != 1) {
      nlpNames.clear();
      nlpFallbacks.clear();
      dictionaryNames.clear();
      dictionaryFallbacks.clear();
      nameFinder.clearAdaptiveData();
      for (int s = 0; s < sentences.length; s++) {
        String[] tokens = sentences[s].split("[\\W\\s]|$|^");
        for (int i = 0; i < tokens.length; i++) {
          String first = tokens[i];
          String last = null;
          if (i + 1 < tokens.length) {
            last = tokens[i + 1];
          }
          if (isFirstName(first, strict)
              && isLastName(last)
              && isFullName(first + " " + last, strict)) {
            dictionaryNames.add(first + " " + last);
          } else if (!strict && isFirstName(first, strict)) {
            dictionaryFallbacks.add(first);
          }
        }
        Span[] spans = nameFinder.find(tokens);
        for (int j = 0; !dictionaryOnly && j < spans.length; j++) {
          List<String> curNames = Arrays.asList(Span.spansToStrings(spans, tokens));
          for (String curName : curNames) {
            if (curName.contains(" ")
                && isFullName(curName, strict)) {
              nlpNames.add(curName);
            } else if (isFirstName(curName, strict)) {
              nlpFallbacks.add(curName);
            }
          }
        }
      }
    }
    if (nlpNames.isEmpty()) {
      nlpNames = nlpFallbacks;
    }
    if (dictionaryNames.isEmpty()) {
      dictionaryNames = dictionaryFallbacks;
    }
    if (nlpNames.size() == 1) {
      return nlpNames.iterator().next();
    }
    if (nlpFallbacks.size() == 1) {
      return nlpFallbacks.iterator().next();
    }
    if (dictionaryNames.size() == 1) {
      return dictionaryNames.iterator().next();
    }
    if (dictionaryFallbacks.size() == 1) {
      return dictionaryFallbacks.iterator().next();
    }
    return null;
  }

  private static boolean isValidNameChars(String str) {
    return !CommonUtil.isEmpty(str) && !invalidNameChars.matcher(str).find();
  }

  private static boolean isFullName(String str, boolean strict) {
    if (!isValidNameChars(str)) {
      return false;
    }
    if (str.contains(" ")) {
      String[] parts = str.split(" ");
      int upper = 0;
      int nonDictionary = 0;
      if (!firstNames.contains(parts[0])) {
        return false;
      }
      for (int i = 0; i < parts.length; i++) {
        if (Character.isUpperCase(parts[i].charAt(0))) {
          ++upper;
        }
        if (!englishWords.contains(parts[i].toLowerCase())
            || (i == 0 && ((strict && firstNamesPopular.contains(parts[i]))
            || (!strict && firstNames.contains(parts[i]))))
            || (i > 0 && isLastName(parts[i]))) {
          ++nonDictionary;
        }
      }
      return upper > 1 && nonDictionary > 0;
    }
    return false;
  }

  private static boolean isFirstName(String str, boolean strict) {
    if (!isValidNameChars(str)) {
      return false;
    }
    if (Character.isLowerCase(str.charAt(0))) {
      return false;
    }
    return (strict && firstNamesPopular.contains(str))
        || (!strict && firstNames.contains(str));
  }

  private static boolean isLastName(String str) {
    if (!isValidNameChars(str)) {
      return false;
    }
    if (Character.isLowerCase(str.charAt(0))) {
      return false;
    }
    return lastNames.contains(str.toUpperCase());
  }
}
