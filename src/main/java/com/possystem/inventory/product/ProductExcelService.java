package com.possystem.inventory;

import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductExcelService {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    private static final int COL_PRODUCT_NAME = 0;
    private static final int COL_CATEGORY_NAME = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_PRICE = 3;
    private static final int COL_COST_PRICE = 4;
    private static final int COL_COMPARE_AT_PRICE = 5;
    private static final int COL_BARCODE = 6;
    private static final int COL_WEIGHT = 7;
    private static final int COL_UOM = 8;
    private static final int COL_TRACK_STOCK = 9;

    private static final String SHEET_PRODUCTS = "Products";
    private static final String SHEET_CATEGORIES = "Categories";

    // ==================== TEMPLATE GENERATION ====================

    public byte[] generateTemplate() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Sheet 1: Products
            XSSFSheet productsSheet = workbook.createSheet(SHEET_PRODUCTS);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Headers
            String[] headers = {
                    "Product Name *", "Category Name", "Description",
                    "Price *", "Cost Price", "Compare At Price",
                    "Barcode", "Weight", "UOM", "Track Stock"
            };
            Row headerRow = productsSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample data row
            Row sampleRow = productsSheet.createRow(1);
            sampleRow.createCell(COL_PRODUCT_NAME).setCellValue("Chicken Burger");
            sampleRow.createCell(COL_CATEGORY_NAME).setCellValue("Burgers");
            sampleRow.createCell(COL_DESCRIPTION).setCellValue("Classic chicken burger");
            sampleRow.createCell(COL_PRICE).setCellValue(12.99);
            sampleRow.createCell(COL_COST_PRICE).setCellValue(5.50);
            sampleRow.createCell(COL_COMPARE_AT_PRICE).setCellValue(14.99);
            sampleRow.createCell(COL_BARCODE).setCellValue("1234567890123");
            sampleRow.createCell(COL_WEIGHT).setCellValue(0.250);
            sampleRow.createCell(COL_UOM).setCellValue("kg");
            sampleRow.createCell(COL_TRACK_STOCK).setCellValue("YES");

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                productsSheet.autoSizeColumn(i);
            }

            // Sheet 2: Categories (reference for dropdown)
            XSSFSheet categoriesSheet = workbook.createSheet(SHEET_CATEGORIES);

            Row catHeader = categoriesSheet.createRow(0);
            Cell catHeaderCell = catHeader.createCell(0);
            catHeaderCell.setCellValue("Available Categories");
            catHeaderCell.setCellStyle(headerStyle);

            List<Category> categories = categoryRepository
                    .findByShopIdAndIsActiveTrueOrderBySortOrderAsc(shopId);

            for (int i = 0; i < categories.size(); i++) {
                categoriesSheet.createRow(i + 1)
                        .createCell(0)
                        .setCellValue(categories.get(i).getCategoryName());
            }
            categoriesSheet.autoSizeColumn(0);

            // Category Name dropdown (column B, rows 1-500)
            if (!categories.isEmpty()) {
                int lastCatRow = categories.size(); // 1-based in Categories sheet
                String formula = SHEET_CATEGORIES + "!$A$2:$A$" + (lastCatRow + 1);

                XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(productsSheet);
                DataValidationConstraint catConstraint = dvHelper.createFormulaListConstraint(formula);
                CellRangeAddressList catRange = new CellRangeAddressList(1, 500, COL_CATEGORY_NAME, COL_CATEGORY_NAME);
                DataValidation catValidation = dvHelper.createValidation(catConstraint, catRange);
                catValidation.setShowErrorBox(true);
                catValidation.createErrorBox("Invalid Category", "Please select a category from the dropdown list.");
                productsSheet.addValidationData(catValidation);
            }

            // Track Stock dropdown (column J, rows 1-500)
            XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(productsSheet);
            DataValidationConstraint trackStockConstraint = dvHelper.createExplicitListConstraint(new String[]{"YES", "NO"});
            CellRangeAddressList trackStockRange = new CellRangeAddressList(1, 500, COL_TRACK_STOCK, COL_TRACK_STOCK);
            DataValidation trackStockValidation = dvHelper.createValidation(trackStockConstraint, trackStockRange);
            trackStockValidation.setShowErrorBox(true);
            trackStockValidation.createErrorBox("Invalid Value", "Please select YES or NO.");
            productsSheet.addValidationData(trackStockValidation);

            // Write to byte array
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate product template", e);
        }
    }

    // ==================== UPLOAD PROCESSING ====================

    @Transactional
    public ProductUploadResponse processUpload(MultipartFile file) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        // Pre-load category map: lowercased name -> UUID
        List<Category> categories = categoryRepository
                .findByShopIdAndIsActiveTrueOrderBySortOrderAsc(shopId);
        Map<String, UUID> categoryMap = new HashMap<>();
        for (Category cat : categories) {
            categoryMap.put(cat.getCategoryName().toLowerCase(), cat.getId());
        }

        List<ProductUploadResponse.RowError> errors = new ArrayList<>();
        List<ProductRequest> validRequests = new ArrayList<>();
        Set<String> namesInFile = new HashSet<>();
        int totalRows = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_PRODUCTS);
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

                // Product name (required)
                String productName = getStringValue(row, COL_PRODUCT_NAME);
                if (productName == null || productName.isBlank()) {
                    errors.add(ProductUploadResponse.RowError.builder()
                            .row(excelRowNum).productName(null)
                            .error("Product name is required").build());
                    continue;
                }

                // Price (required)
                BigDecimal price = getBigDecimalValue(row, COL_PRICE);
                if (price == null) {
                    errors.add(ProductUploadResponse.RowError.builder()
                            .row(excelRowNum).productName(productName)
                            .error("Price is required").build());
                    continue;
                }

                // Duplicate name within file
                String nameLower = productName.toLowerCase();
                if (!namesInFile.add(nameLower)) {
                    errors.add(ProductUploadResponse.RowError.builder()
                            .row(excelRowNum).productName(productName)
                            .error("Duplicate product name in file").build());
                    continue;
                }

                // Duplicate name in DB
                if (productRepository.existsByShopIdAndProductNameIgnoreCaseAndIsActiveTrue(shopId, productName)) {
                    errors.add(ProductUploadResponse.RowError.builder()
                            .row(excelRowNum).productName(productName)
                            .error("A product with this name already exists").build());
                    continue;
                }

                // Category resolution (optional but must be valid)
                String categoryName = getStringValue(row, COL_CATEGORY_NAME);
                UUID categoryId = null;
                if (categoryName != null && !categoryName.isBlank()) {
                    categoryId = categoryMap.get(categoryName.toLowerCase());
                    if (categoryId == null) {
                        errors.add(ProductUploadResponse.RowError.builder()
                                .row(excelRowNum).productName(productName)
                                .error("Category '" + categoryName + "' not found").build());
                        continue;
                    }
                }

                // Optional fields
                String description = getStringValue(row, COL_DESCRIPTION);
                BigDecimal costPrice = getBigDecimalValue(row, COL_COST_PRICE);
                BigDecimal compareAtPrice = getBigDecimalValue(row, COL_COMPARE_AT_PRICE);
                String barcode = getStringValue(row, COL_BARCODE);
                BigDecimal weight = getBigDecimalValue(row, COL_WEIGHT);
                String uom = getStringValue(row, COL_UOM);
                Boolean trackStock = parseBooleanValue(row, COL_TRACK_STOCK);

                ProductVariantRequest variantRequest = ProductVariantRequest.builder()
                        .price(price)
                        .costPrice(costPrice)
                        .compareAtPrice(compareAtPrice)
                        .barcode(barcode)
                        .weight(weight)
                        .uom(uom)
                        .trackStock(trackStock)
                        .build();

                ProductRequest productRequest = ProductRequest.builder()
                        .productName(productName)
                        .categoryId(categoryId)
                        .description(description)
                        .productType(ProductType.SIMPLE)
                        .defaultVariant(variantRequest)
                        .build();

                validRequests.add(productRequest);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to process Excel file: " + e.getMessage());
        }

        // Phase 2: If any errors, reject everything
        if (!errors.isEmpty()) {
            return ProductUploadResponse.builder()
                    .totalRows(totalRows)
                    .successCount(0)
                    .failureCount(errors.size())
                    .errors(errors)
                    .build();
        }

        // Phase 3: Save all (within the @Transactional boundary)
        for (ProductRequest request : validRequests) {
            productService.save(request);
        }

        return ProductUploadResponse.builder()
                .totalRows(totalRows)
                .successCount(validRequests.size())
                .failureCount(0)
                .build();
    }

    // ==================== HELPERS ====================

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

    private Boolean parseBooleanValue(Row row, int colIndex) {
        String val = getStringValue(row, colIndex);
        if (val == null || val.isBlank()) return true;
        return val.equalsIgnoreCase("YES") || val.equalsIgnoreCase("TRUE");
    }

}
