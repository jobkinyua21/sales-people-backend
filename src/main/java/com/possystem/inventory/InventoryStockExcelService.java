package com.possystem.inventory;

import com.possystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryStockExcelService {

    private final InventoryStockRepository inventoryStockRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // Column layout — read-only columns first, then editable columns
    private static final int COL_STOCK_ID = 0;          // read-only (hidden)
    private static final int COL_PRODUCT_NAME = 1;       // read-only
    private static final int COL_VARIANT_NAME = 2;       // read-only
    private static final int COL_SKU = 3;                // read-only
    private static final int COL_CATEGORY = 4;           // read-only
    private static final int COL_CURRENT_QTY = 5;        // editable
    private static final int COL_REORDER_LEVEL = 6;      // editable
    private static final int COL_REORDER_QTY = 7;        // editable

    private static final String SHEET_STOCK = "Stock";

    // ==================== TEMPLATE GENERATION ====================

    public byte[] generateTemplate() {
        UUID shopId = getCurrentShopId();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(SHEET_STOCK);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Read-only style (light gray background)
            CellStyle readOnlyStyle = workbook.createCellStyle();
            readOnlyStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            readOnlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            readOnlyStyle.setLocked(true);

            // Editable style (white/green background)
            CellStyle editableStyle = workbook.createCellStyle();
            editableStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            editableStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            editableStyle.setLocked(false);

            // Editable header style
            CellStyle editableHeaderStyle = workbook.createCellStyle();
            Font editableHeaderFont = workbook.createFont();
            editableHeaderFont.setBold(true);
            editableHeaderStyle.setFont(editableHeaderFont);
            editableHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            editableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Headers
            String[] headers = {
                    "Stock ID (DO NOT EDIT)", "Product Name", "Variant Name",
                    "SKU", "Category",
                    "Current Quantity *", "Reorder Level", "Reorder Quantity"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(i >= COL_CURRENT_QTY ? editableHeaderStyle : headerStyle);
            }

            // Fetch all stock records with resolved product info
            List<InventoryStock> stockRecords = inventoryStockRepository.searchFiltered(shopId, null, null, null);

            int rowNum = 1;
            for (InventoryStock stock : stockRecords) {
                Row row = sheet.createRow(rowNum++);

                // Resolve variant → product → category
                ProductVariant variant = productVariantRepository
                        .findByIdAndShopIdAndIsActiveTrue(stock.getVariantId(), shopId)
                        .orElse(null);
                if (variant == null) continue;

                Product product = productRepository
                        .findByIdAndShopIdAndIsActiveTrue(variant.getProductId(), shopId)
                        .orElse(null);
                if (product == null) continue;

                String categoryName = null;
                if (product.getCategoryId() != null) {
                    categoryName = categoryRepository
                            .findByIdAndShopIdAndIsActiveTrue(product.getCategoryId(), shopId)
                            .map(Category::getCategoryName)
                            .orElse(null);
                }

                // Read-only columns
                Cell stockIdCell = row.createCell(COL_STOCK_ID);
                stockIdCell.setCellValue(stock.getId().toString());
                stockIdCell.setCellStyle(readOnlyStyle);

                Cell productNameCell = row.createCell(COL_PRODUCT_NAME);
                productNameCell.setCellValue(product.getProductName());
                productNameCell.setCellStyle(readOnlyStyle);

                Cell variantNameCell = row.createCell(COL_VARIANT_NAME);
                variantNameCell.setCellValue(variant.getVariantName());
                variantNameCell.setCellStyle(readOnlyStyle);

                Cell skuCell = row.createCell(COL_SKU);
                skuCell.setCellValue(variant.getSku());
                skuCell.setCellStyle(readOnlyStyle);

                Cell categoryCell = row.createCell(COL_CATEGORY);
                categoryCell.setCellValue(categoryName != null ? categoryName : "");
                categoryCell.setCellStyle(readOnlyStyle);

                // Editable columns (pre-filled with current values)
                Cell currentQtyCell = row.createCell(COL_CURRENT_QTY);
                currentQtyCell.setCellValue(stock.getCurrentQuantity() != null
                        ? stock.getCurrentQuantity().doubleValue() : 0);
                currentQtyCell.setCellStyle(editableStyle);

                Cell reorderLevelCell = row.createCell(COL_REORDER_LEVEL);
                if (stock.getReorderLevel() != null) {
                    reorderLevelCell.setCellValue(stock.getReorderLevel().doubleValue());
                }
                reorderLevelCell.setCellStyle(editableStyle);

                Cell reorderQtyCell = row.createCell(COL_REORDER_QTY);
                if (stock.getReorderQuantity() != null) {
                    reorderQtyCell.setCellValue(stock.getReorderQuantity().doubleValue());
                }
                reorderQtyCell.setCellStyle(editableStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Protect sheet — read-only columns locked, editable columns unlocked
            sheet.protectSheet("");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate stock template", e);
        }
    }

    // ==================== UPLOAD PROCESSING ====================

    @Transactional
    public StockUploadResponse processUpload(MultipartFile file) {
        UUID shopId = getCurrentShopId();

        List<StockUploadResponse.RowError> errors = new ArrayList<>();
        List<StockUpdateRow> validRows = new ArrayList<>();
        int totalRows = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_STOCK);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            int lastRow = sheet.getLastRowNum();

            // Phase 1: Parse & validate ALL rows
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                totalRows++;
                int excelRowNum = i + 1;

                String productName = getStringValue(row, COL_PRODUCT_NAME);
                String sku = getStringValue(row, COL_SKU);

                // Stock ID (required)
                String stockIdStr = getStringValue(row, COL_STOCK_ID);
                if (stockIdStr == null || stockIdStr.isBlank()) {
                    errors.add(StockUploadResponse.RowError.builder()
                            .row(excelRowNum).productName(productName).sku(sku)
                            .error("Stock ID is missing").build());
                    continue;
                }

                UUID stockId;
                try {
                    stockId = UUID.fromString(stockIdStr);
                } catch (IllegalArgumentException e) {
                    errors.add(StockUploadResponse.RowError.builder()
                            .row(excelRowNum).productName(productName).sku(sku)
                            .error("Invalid Stock ID format").build());
                    continue;
                }

                // Verify stock record exists
                Optional<InventoryStock> stockOpt = inventoryStockRepository
                        .findByIdAndShopIdAndIsActiveTrue(stockId, shopId);
                if (stockOpt.isEmpty()) {
                    errors.add(StockUploadResponse.RowError.builder()
                            .row(excelRowNum).productName(productName).sku(sku)
                            .error("Stock record not found").build());
                    continue;
                }

                // Current quantity (required)
                BigDecimal currentQuantity = getBigDecimalValue(row, COL_CURRENT_QTY);
                if (currentQuantity == null) {
                    errors.add(StockUploadResponse.RowError.builder()
                            .row(excelRowNum).productName(productName).sku(sku)
                            .error("Current quantity is required").build());
                    continue;
                }

                BigDecimal reorderLevel = getBigDecimalValue(row, COL_REORDER_LEVEL);
                BigDecimal reorderQuantity = getBigDecimalValue(row, COL_REORDER_QTY);

                validRows.add(new StockUpdateRow(stockOpt.get(), currentQuantity, reorderLevel, reorderQuantity));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to process Excel file: " + e.getMessage());
        }

        // Phase 2: If any errors, reject everything
        if (!errors.isEmpty()) {
            return StockUploadResponse.builder()
                    .totalRows(totalRows)
                    .successCount(0)
                    .failureCount(errors.size())
                    .errors(errors)
                    .build();
        }

        // Phase 3: Apply all updates
        for (StockUpdateRow update : validRows) {
            InventoryStock stock = update.stock;
            BigDecimal oldQuantity = stock.getCurrentQuantity();

            stock.setCurrentQuantity(update.currentQuantity);
            if (update.reorderLevel != null) stock.setReorderLevel(update.reorderLevel);
            if (update.reorderQuantity != null) stock.setReorderQuantity(update.reorderQuantity);

            // Track restock if quantity increased
            if (stock.getCurrentQuantity().compareTo(oldQuantity) > 0) {
                stock.setLastRestockedAt(LocalDateTime.now());
            }

            inventoryStockRepository.save(stock);
        }

        return StockUploadResponse.builder()
                .totalRows(totalRows)
                .successCount(validRows.size())
                .failureCount(0)
                .build();
    }

    // ==================== HELPERS ====================

    private record StockUpdateRow(InventoryStock stock, BigDecimal currentQuantity,
                                  BigDecimal reorderLevel, BigDecimal reorderQuantity) {}

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getStringValue(row, c);
                if (val != null && !val.isBlank()) return false;
            }
        }
        return true;
    }

    private String getStringValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getStringCellValue().trim();
            default -> null;
        };
    }

    private BigDecimal getBigDecimalValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                String val = cell.getStringCellValue().trim();
                if (val.isEmpty()) yield null;
                try {
                    yield new BigDecimal(val);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
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
}
