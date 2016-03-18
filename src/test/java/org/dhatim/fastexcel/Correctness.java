/*
 * Copyright 2016 Dhatim.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dhatim.fastexcel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class Correctness {

    private byte[] writeWorkbook(Consumer<Workbook> consumer) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Workbook wb = new Workbook(os, "Test", "1.0");
        consumer.accept(wb);
        wb.finish();
        return os.toByteArray();
    }

    @Test
    public void colToName() throws Exception {
        assertThat(Range.colToString(26)).isEqualTo("AA");
        assertThat(Range.colToString(702)).isEqualTo("AAA");
        assertThat(Range.colToString(Worksheet.MAX_COLS - 1)).isEqualTo("XFD");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noWorksheet() throws Exception {
        writeWorkbook(wb -> {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void badVersion() throws Exception {
        Workbook dummy = new Workbook(new NullOutputStream(), "Test", "1.0.1");
    }

    @Test
    public void singleEmptyWorksheet() throws Exception {
        byte[] data = writeWorkbook(wb -> wb.newWorksheet("Worksheet 1"));
    }

    @Test
    public void checkMaxRows() throws Exception {
        byte[] data = writeWorkbook(wb -> wb.newWorksheet("Worksheet 1").value(Worksheet.MAX_ROWS - 1, 0, "test"));
    }

    @Test
    public void checkMaxCols() throws Exception {
        byte[] data = writeWorkbook(wb -> wb.newWorksheet("Worksheet 1").value(0, Worksheet.MAX_COLS - 1, "test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceedMaxRows() throws Exception {
        byte[] data = writeWorkbook(wb -> wb.newWorksheet("Worksheet 1").value(Worksheet.MAX_ROWS, 0, "test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeRow() throws Exception {
        byte[] data = writeWorkbook(wb -> wb.newWorksheet("Worksheet 1").value(-1, 0, "test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceedMaxCols() throws Exception {
        byte[] data = writeWorkbook(wb -> wb.newWorksheet("Worksheet 1").value(0, Worksheet.MAX_COLS, "test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCol() throws Exception {
        byte[] data = writeWorkbook(wb -> wb.newWorksheet("Worksheet 1").value(0, -1, "test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void notSupportedTypeCell() throws Exception {
        byte[] data = writeWorkbook(wb -> wb.newWorksheet("Worksheet 1").value(0, 0, new Object()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRange() throws Exception {
        byte[] data = writeWorkbook(wb -> {
            Worksheet ws = wb.newWorksheet("Worksheet 1");
            ws.range(-1, -1, Worksheet.MAX_COLS, Worksheet.MAX_ROWS);
        });
    }

    @Test
    public void reorderedRange() throws Exception {
        byte[] data = writeWorkbook(wb -> {
            Worksheet ws = wb.newWorksheet("Worksheet 1");
            assertThat(ws.range(0, 1, 10, 11)).isEqualTo(ws.range(10, 11, 0, 1));
        });
    }

    @Test
    public void mergedRanges() throws Exception {
        byte[] data = writeWorkbook(wb -> {
            Worksheet ws = wb.newWorksheet("Worksheet 1");
            ws.value(0, 0, "One");
            ws.value(0, 1, "Two");
            ws.value(0, 2, "Three");
            ws.value(1, 0, "Merged");
            ws.range(1, 0, 1, 2).merge();
            ws.style(1, 0).horizontalAlignment("center").set();
        });
    }

    @Test
    public void singleWorksheet() throws Exception {
        String sheetName = "Worksheet 1";
        String stringValue = "Sample text with chars to escape : < > & \\ \" ~ é è à ç ù µ £ €";
        Date dateValue = new Date();
        LocalDateTime localDateTimeValue = LocalDateTime.now();
        ZoneId timezone = ZoneId.of("Australia/Sydney");
        ZonedDateTime zonedDateValue = ZonedDateTime.ofInstant(dateValue.toInstant(), timezone);
        double doubleValue = 1.234;
        int intValue = 2_016;
        long longValue = 2_016_000_000_000L;
        BigDecimal bigDecimalValue = BigDecimal.TEN;
        byte[] data = writeWorkbook(wb -> {
            Worksheet ws = wb.newWorksheet(sheetName);
            int i = 1;
            ws.value(i, i++, stringValue);
            ws.value(i, i++, dateValue);
            ws.value(i, i++, localDateTimeValue);
            ws.value(i, i++, zonedDateValue);
            ws.value(i, i++, doubleValue);
            ws.value(i, i++, intValue);
            ws.value(i, i++, longValue);
            ws.value(i, i++, bigDecimalValue);
        });

        // Check generated workbook with Apache POI
        XSSFWorkbook xwb = new XSSFWorkbook(new ByteArrayInputStream(data));
        assertThat(xwb.getActiveSheetIndex()).isEqualTo(0);
        assertThat(xwb.getNumberOfSheets()).isEqualTo(1);
        XSSFSheet xws = xwb.getSheet(sheetName);
        @SuppressWarnings("unchecked")
        Comparable<XSSFRow> row = (Comparable) xws.getRow(0);
        assertThat(row).isNull();
        int i = 1;
        assertThat(xws.getRow(i).getCell(i++).getStringCellValue()).isEqualTo(stringValue);
        assertThat(xws.getRow(i).getCell(i++).getDateCellValue()).isEqualTo(dateValue);
        // Check zoned timestamps have the same textual representation as the Dates extracted from the workbook
        // (Excel date serial numbers do not carry timezone information)
        assertThat(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.ofInstant(xws.getRow(i).getCell(i++).getDateCellValue().toInstant(), ZoneId.systemDefault()))).isEqualTo(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTimeValue));
        assertThat(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.ofInstant(xws.getRow(i).getCell(i++).getDateCellValue().toInstant(), ZoneId.systemDefault()))).isEqualTo(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(zonedDateValue));
        assertThat(xws.getRow(i).getCell(i++).getNumericCellValue()).isEqualTo(doubleValue);
        assertThat(xws.getRow(i).getCell(i++).getNumericCellValue()).isEqualTo(intValue);
        assertThat(xws.getRow(i).getCell(i++).getNumericCellValue()).isEqualTo(longValue);
        assertThat(new BigDecimal(xws.getRow(i).getCell(i++).getRawValue())).isEqualTo(bigDecimalValue);
    }

    @Test
    public void multipleWorksheets() throws Exception {
        int numWs = 10;
        int numRows = 5000;
        int numCols = 6;
        byte[] data = writeWorkbook(wb -> {
            @SuppressWarnings("unchecked")
            CompletableFuture<Void>[] cfs = new CompletableFuture[numWs];
            for (int i = 0; i < cfs.length; ++i) {
                Worksheet ws = wb.newWorksheet("Sheet " + i);
                CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < numCols; ++j) {
                        ws.value(0, j, "Column " + j);
                        ws.style(0, j).bold().fillColor(Color.GRAY2).set();
                        for (int k = 1; k <= numRows; ++k) {
                            switch (j) {
                                case 0:
                                    ws.value(k, j, "String value " + k);
                                    break;
                                case 1:
                                    ws.value(k, j, 2);
                                    break;
                                case 2:
                                    ws.value(k, j, 3L);
                                    break;
                                case 3:
                                    ws.value(k, j, 0.123);
                                    break;
                                case 4:
                                    ws.value(k, j, new Date());
                                    ws.style(k, j).format("yyyy-MM-dd HH:mm:ss").set();
                                    break;
                                case 5:
                                    ws.value(k, j, LocalDate.now());
                                    ws.style(k, j).format("yyyy-MM-dd").set();
                                    break;
                                default:
                                    throw new IllegalArgumentException();
                            }
                        }
                    }
                    ws.formula(numRows + 1, 1, "=SUM(" + ws.range(1, 1, numRows, 1).toString() + ")");
                    ws.formula(numRows + 1, 2, "=SUM(" + ws.range(1, 2, numRows, 2).toString() + ")");
                    ws.formula(numRows + 1, 3, "=SUM(" + ws.range(1, 3, numRows, 3).toString() + ")");
                    ws.formula(numRows + 1, 4, "=AVERAGE(" + ws.range(1, 4, numRows, 4).toString() + ")");
                    ws.style(numRows + 1, 4).format("yyyy-MM-dd HH:mm:ss").set();
                    ws.formula(numRows + 1, 5, "=AVERAGE(" + ws.range(1, 5, numRows, 5).toString() + ")");
                    ws.style(numRows + 1, 5).format("yyyy-MM-dd").horizontalAlignment("center").verticalAlignment("top").wrapText(true).set();
                    ws.range(1, 0, numRows, numCols - 1).style().borderColor(Color.RED).borderStyle("thick").shadeAlternateRows(Color.RED).set();
                });
                cfs[i] = cf;
            }
            try {
                CompletableFuture.allOf(cfs).get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });

        // Check generated workbook with Apache POI
        XSSFWorkbook xwb = new XSSFWorkbook(new ByteArrayInputStream(data));
        assertThat(xwb.getActiveSheetIndex()).isEqualTo(0);
        assertThat(xwb.getNumberOfSheets()).isEqualTo(numWs);
        for (int i = 0; i < numWs; ++i) {
            assertThat(xwb.getSheetName(i)).isEqualTo("Sheet " + i);
            XSSFSheet xws = xwb.getSheetAt(i);
            assertThat(xws.getLastRowNum()).isEqualTo(numRows + 1);
            for (int j = 1; j <= numRows; ++j) {
                assertThat(xws.getRow(j).getCell(0).getStringCellValue()).isEqualTo("String value " + j);
            }
        }

    }
}
