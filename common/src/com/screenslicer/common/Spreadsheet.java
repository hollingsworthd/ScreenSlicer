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
