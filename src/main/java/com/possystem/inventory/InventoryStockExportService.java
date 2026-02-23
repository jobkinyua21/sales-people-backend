package com.possystem.inventory;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryStockExportService {

    private final InventoryStockRepository inventoryStockRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ==================== EXCEL EXPORT ====================

    public byte[] exportExcel(String search, UUID categoryId, String stockStatus) {
        List<StockExportRow> rows = fetchExportData(search, categoryId, stockStatus);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Inventory");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Headers
            String[] headers = {
                    "Product Name", "Variant Name", "SKU", "Category",
                    "Cost Price", "Retail Price", "Current Stock",
                    "Reorder Level", "Reorder Qty", "Stock Status", "Last Restock"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (StockExportRow row : rows) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(row.productName != null ? row.productName : "");
                dataRow.createCell(1).setCellValue(row.variantName != null ? row.variantName : "");
                dataRow.createCell(2).setCellValue(row.sku != null ? row.sku : "");
                dataRow.createCell(3).setCellValue(row.categoryName != null ? row.categoryName : "");
                if (row.costPrice != null) dataRow.createCell(4).setCellValue(row.costPrice.doubleValue());
                if (row.price != null) dataRow.createCell(5).setCellValue(row.price.doubleValue());
                if (row.currentQuantity != null) dataRow.createCell(6).setCellValue(row.currentQuantity.doubleValue());
                if (row.reorderLevel != null) dataRow.createCell(7).setCellValue(row.reorderLevel.doubleValue());
                if (row.reorderQuantity != null) dataRow.createCell(8).setCellValue(row.reorderQuantity.doubleValue());
                dataRow.createCell(9).setCellValue(row.stockStatus != null ? row.stockStatus : "");
                dataRow.createCell(10).setCellValue(row.lastRestockedAt != null ? row.lastRestockedAt : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate inventory Excel export", e);
        }
    }

    // ==================== PDF EXPORT ====================

    public byte[] exportPdf(String search, UUID categoryId, String stockStatus) {
        List<StockExportRow> rows = fetchExportData(search, categoryId, stockStatus);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
            Document document = new Document(pdfDoc);
            document.setMargins(20, 20, 20, 20);

            PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont normal = PdfFontFactory.createFont("Helvetica");

            // Title
            document.add(new Paragraph("Inventory Report")
                    .setFont(bold).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // Table
            float[] columnWidths = {3f, 2f, 2f, 2f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 2f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths))
                    .useAllAvailableWidth();

            // Headers
            String[] headers = {
                    "Product Name", "Variant", "SKU", "Category",
                    "Cost", "Retail", "Stock",
                    "Reorder Lvl", "Reorder Qty", "Status", "Last Restock"
            };
            for (String header : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(header).setFont(bold).setFontSize(8))
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER));
            }

            // Data rows
            for (StockExportRow row : rows) {
                table.addCell(pdfCell(row.productName, normal));
                table.addCell(pdfCell(row.variantName, normal));
                table.addCell(pdfCell(row.sku, normal));
                table.addCell(pdfCell(row.categoryName, normal));
                table.addCell(pdfCell(formatDecimal(row.costPrice), normal));
                table.addCell(pdfCell(formatDecimal(row.price), normal));
                table.addCell(pdfCell(formatDecimal(row.currentQuantity), normal));
                table.addCell(pdfCell(formatDecimal(row.reorderLevel), normal));
                table.addCell(pdfCell(formatDecimal(row.reorderQuantity), normal));
                table.addCell(pdfCell(row.stockStatus, normal));
                table.addCell(pdfCell(row.lastRestockedAt, normal));
            }

            document.add(table);

            // Footer
            document.add(new Paragraph("Total items: " + rows.size())
                    .setFont(normal).setFontSize(8)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(10));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate inventory PDF export", e);
        }
    }

    // ==================== HELPERS ====================

    private List<StockExportRow> fetchExportData(String search, UUID categoryId, String stockStatus) {
        UUID shopId = getCurrentShopId();

        List<InventoryStock> stocks = inventoryStockRepository
                .searchFiltered(shopId, search, categoryId, stockStatus);

        return stocks.stream().map(stock -> {
            StockExportRow row = new StockExportRow();
            row.currentQuantity = stock.getCurrentQuantity();
            row.reorderLevel = stock.getReorderLevel();
            row.reorderQuantity = stock.getReorderQuantity();
            row.stockStatus = computeStockStatus(stock);
            row.lastRestockedAt = stock.getLastRestockedAt() != null
                    ? stock.getLastRestockedAt().format(DATE_FMT) : null;

            ProductVariant variant = productVariantRepository
                    .findByIdAndShopIdAndIsActiveTrue(stock.getVariantId(), stock.getShopId())
                    .orElse(null);
            if (variant != null) {
                row.sku = variant.getSku();
                row.variantName = variant.getVariantName();
                row.costPrice = variant.getCostPrice();
                row.price = variant.getPrice();

                Product product = productRepository
                        .findByIdAndShopIdAndIsActiveTrue(variant.getProductId(), stock.getShopId())
                        .orElse(null);
                if (product != null) {
                    row.productName = product.getProductName();
                    if (product.getCategoryId() != null) {
                        row.categoryName = categoryRepository
                                .findByIdAndShopIdAndIsActiveTrue(product.getCategoryId(), stock.getShopId())
                                .map(Category::getCategoryName)
                                .orElse(null);
                    }
                }
            }
            return row;
        }).toList();
    }

    private String computeStockStatus(InventoryStock stock) {
        BigDecimal qty = stock.getCurrentQuantity();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) return "OUT_OF_STOCK";
        if (stock.getReorderLevel() != null && qty.compareTo(stock.getReorderLevel()) <= 0) return "LOW_STOCK";
        return "IN_STOCK";
    }

    private Cell pdfCell(String value, PdfFont font) {
        return new Cell()
                .add(new Paragraph(value != null ? value : "").setFont(font).setFontSize(7))
                .setTextAlignment(TextAlignment.LEFT);
    }

    private String formatDecimal(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "";
    }

    private UUID getCurrentShopId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        UUID shopId = principal.getShopId();
        if (shopId == null) {
            throw new IllegalArgumentException("Shop context is required");
        }
        return shopId;
    }

    private static class StockExportRow {
        String productName;
        String variantName;
        String sku;
        String categoryName;
        BigDecimal costPrice;
        BigDecimal price;
        BigDecimal currentQuantity;
        BigDecimal reorderLevel;
        BigDecimal reorderQuantity;
        String stockStatus;
        String lastRestockedAt;
    }
}
