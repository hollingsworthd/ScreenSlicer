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
package com.screenslicer.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.tika.io.IOUtils;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class Spreadsheet {
  public static String csv(List<List<String>> rows) {
    StringWriter sw = new StringWriter();
    CSVWriter writer = new CSVWriter(sw);
    try {
      List<String[]> elements = new ArrayList<String[]>();
      for (List<String> row : rows) {
        elements.add(row.toArray(new String[0]));
      }
      writer.writeAll(elements);
      return sw.toString();
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        Log.exception(e);
      }
    }
  }

  public static List<String[]> fromCsv(String rows) {
    Reader stringReader = new StringReader(rows);
    try {
      return new CSVReader(stringReader).readAll();
    } catch (Throwable t) {
      Log.exception(t);
      return null;
    } finally {
      IOUtils.closeQuietly(stringReader);
    }
  }

  public static List<Map<String, String>> json(List<List<String>> rows) {
    List<Map<String, String>> json = new ArrayList<Map<String, String>>();
    if (rows.size() < 2) {
      return null;
    }
    List<String> header = rows.get(0);
    int len = header.size();
    for (int i = 1; i < rows.size(); i++) {
      List<String> row = rows.get(i);
      if (row.size() != len) {
        throw new IllegalArgumentException("All rows must be same length");
      }
      Map<String, String> jsonRow = new LinkedHashMap<String, String>();
      for (int j = 0; j < len; j++) {
        jsonRow.put(header.get(j), row.get(j));
      }
      json.add(jsonRow);
    }
    return json;
  }

  public static byte[] xls(List<List<String>> rows) {
    HSSFWorkbook workbook = new HSSFWorkbook();
    HSSFSheet sheet = workbook.createSheet("Sheet 1");
    int curRow = 0;
    for (List<String> row : rows) {
      Row xlsRow = sheet.createRow(curRow);
      int curCell = 0;
      for (String cell : row) {
        Cell xlsCell = xlsRow.createCell(curCell);
        if (CommonUtil.isEmpty(cell)) {
          xlsCell.setCellValue("");
        } else if (CommonUtil.isUrl(cell)) {
          xlsCell.setCellFormula("HYPERLINK(\"" + cell + "\")");
        } else {
          xlsCell.setCellValue(cell);
        }
        ++curCell;
      }
      ++curRow;
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      try {
        workbook.write(out);
      } catch (IOException e) {
        Log.exception(e);
      }
      return out.toByteArray();
    } finally {
      try {
        out.close();
      } catch (IOException e) {
        Log.exception(e);
      }
    }
  }
}
