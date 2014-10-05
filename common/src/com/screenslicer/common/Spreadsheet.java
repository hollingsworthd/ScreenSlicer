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
package com.screenslicer.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

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
