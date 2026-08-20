package com.ameya.inventory.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Shared "formatted, not a raw dump" table export for every Reports
 * endpoint (Phase 1 doc §H) - one place that knows how to turn a title +
 * headers + rows into a styled .xlsx or a paginated .pdf, so each report
 * method only has to build its own data.
 */
public final class TableExporter {

    private TableExporter() {
    }

    public static byte[] toXlsx(String title, List<String> headers, List<List<Object>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(sanitizeSheetName(title));

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(headerStyle);
            }

            for (List<Object> row : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                for (int c = 0; c < row.size(); c++) {
                    Cell cell = dataRow.createCell(c);
                    setCellValue(cell, row.get(c));
                }
            }

            for (int c = 0; c < headers.size(); c++) {
                sheet.autoSizeColumn(c);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] toPdf(String title, List<String> headers, List<List<Object>> rows) {
        try (PDDocument doc = new PDDocument()) {
            float margin = 40f;
            float rowHeight = 16f;
            var titleFont = new org.apache.pdfbox.pdmodel.font.PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            var headerFont = new org.apache.pdfbox.pdmodel.font.PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            var bodyFont = new org.apache.pdfbox.pdmodel.font.PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageWidth = page.getMediaBox().getWidth() - 2 * margin;
            float colWidth = headers.isEmpty() ? pageWidth : pageWidth / headers.size();
            float y = page.getMediaBox().getHeight() - margin;

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.beginText();
            cs.setFont(titleFont, 14);
            cs.newLineAtOffset(margin, y);
            cs.showText(title);
            cs.endText();
            y -= rowHeight * 2;

            cs = writeRow(doc, page, cs, headerFont, 9, headers.toArray(), margin, y, colWidth);
            y -= rowHeight;

            for (List<Object> row : rows) {
                if (y < margin + rowHeight) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    y = page.getMediaBox().getHeight() - margin;
                    cs = new PDPageContentStream(doc, page);
                }
                String[] cells = row.stream().map(v -> v == null ? "" : v.toString()).toArray(String[]::new);
                cs = writeRow(doc, page, cs, bodyFont, 9, cells, margin, y, colWidth);
                y -= rowHeight;
            }
            cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static PDPageContentStream writeRow(PDDocument doc, PDPage page, PDPageContentStream cs,
                                                  org.apache.pdfbox.pdmodel.font.PDFont font, int size,
                                                  Object[] cells, float margin, float y, float colWidth) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        for (int i = 0; i < cells.length; i++) {
            cs.newLineAtOffset(i == 0 ? margin : colWidth, i == 0 ? y : 0);
            String text = String.valueOf(cells[i]);
            if (text.length() > 28) {
                text = text.substring(0, 25) + "...";
            }
            cs.showText(text);
        }
        cs.endText();
        return cs;
    }

    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private static String sanitizeSheetName(String name) {
        String sanitized = name.replaceAll("[\\\\/*?\\[\\]:]", " ");
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }
}
