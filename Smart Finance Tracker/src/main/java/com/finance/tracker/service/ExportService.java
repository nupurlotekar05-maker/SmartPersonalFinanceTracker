package com.finance.tracker.service;

import com.finance.tracker.entity.Transaction;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.util.SecurityUtils;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.DocumentException; // ✅ FIX 1: Added missing import
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── CSV Export ───────────────────────────────────────────────
    public byte[] exportCsv(LocalDate start, LocalDate end) throws IOException {
        List<Transaction> txs = getTransactions(start, end);
        log.info("[EXPORT CSV] userId={} start={} end={} transactionCount={}",
                securityUtils.getCurrentUserId(), start, end, txs.size());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out))) {
            writer.writeNext(csvHeader());
            for (Transaction t : txs) writer.writeNext(toCsvRow(t));
        }
        return out.toByteArray();
    }

    // ── Excel Export ─────────────────────────────────────────────
    public byte[] exportExcel(LocalDate start, LocalDate end) throws IOException {
        List<Transaction> txs = getTransactions(start, end);
        log.info("[EXPORT EXCEL] userId={} start={} end={} transactionCount={}",
                securityUtils.getCurrentUserId(), start, end, txs.size());

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Transactions");

            // Header row style
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] cols = csvHeader();
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Transaction t : txs) {
                Row row = sheet.createRow(rowNum++);
                String[] data = toCsvRow(t);
                for (int i = 0; i < data.length; i++) row.createCell(i).setCellValue(data[i]);
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF Report ───────────────────────────────────────────────
    public byte[] exportPdf(LocalDate start, LocalDate end) throws IOException, DocumentException {
        List<Transaction> txs = getTransactions(start, end);
        log.info("[EXPORT PDF] userId={} start={} end={} transactionCount={}",
                securityUtils.getCurrentUserId(), start, end, txs.size());
        if (txs.isEmpty()) {
            log.warn("[EXPORT PDF] No transactions found for period {} to {}", start, end);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Title
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("Finance Tracker - Transaction Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        doc.add(title);

        com.itextpdf.text.Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        Paragraph period = new Paragraph("Period: " + start.format(FMT) + " to " + end.format(FMT), subFont);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(15);
        doc.add(period);

        // Table
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 2.5f, 2f, 1.5f, 1.5f, 1.5f, 2f});

        // Table headers
        String[] headers = csvHeader();
        com.itextpdf.text.Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(new BaseColor(52, 73, 94));
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Rows
        com.itextpdf.text.Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        boolean alt = false;
        for (Transaction t : txs) {
            BaseColor bg = alt ? new BaseColor(245, 245, 245) : BaseColor.WHITE;
            for (String val : toCsvRow(t)) {
                PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", rowFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                table.addCell(cell);
            }
            alt = !alt;
        }

        doc.add(table);

        // Summary
        // ✅ FIX 2: Use .equals() instead of == for enum comparisons on stream filters
        double totalIncome = txs.stream()
                .filter(t -> Transaction.TransactionType.INCOME.equals(t.getType()))
                .mapToDouble(t -> t.getAmount().doubleValue()).sum();
        double totalExpense = txs.stream()
                .filter(t -> Transaction.TransactionType.EXPENSE.equals(t.getType()))
                .mapToDouble(t -> t.getAmount().doubleValue()).sum();

        com.itextpdf.text.Font sumFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Paragraph summary = new Paragraph(
                "\nSummary:  Total Income: ₹" + String.format("%.2f", totalIncome) +
                "   |   Total Expense: ₹" + String.format("%.2f", totalExpense) +
                "   |   Net Savings: ₹" + String.format("%.2f", totalIncome - totalExpense), sumFont);
        summary.setSpacingBefore(12);
        doc.add(summary);

        doc.close();
        return out.toByteArray();
    }

    // ── Helpers ──────────────────────────────────────────────────
    private List<Transaction> getTransactions(LocalDate start, LocalDate end) {
        return transactionRepository.findByUserIdAndTransactionDateBetween(
                securityUtils.getCurrentUserId(), start, end);
    }

    private String[] csvHeader() {
        return new String[]{"ID", "Date", "Description", "Category", "Type", "Amount", "Account"};
    }

    private String[] toCsvRow(Transaction t) {
        // ✅ FIX 3: Added null-safe guard for account to prevent NPE
        String accountName = (t.getAccount() != null) ? t.getAccount().getAccountName() : "Unknown";
        return new String[]{
                String.valueOf(t.getId()),
                t.getTransactionDate().format(FMT),
                t.getDescription(),
                t.getCategory() != null ? t.getCategory().getName() : "Uncategorized",
                t.getType().name(),
                t.getAmount().toPlainString(),
                accountName
        };
    }
}