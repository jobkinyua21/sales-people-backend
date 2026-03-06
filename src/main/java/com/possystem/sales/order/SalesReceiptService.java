package com.possystem.sales;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.DashedBorder;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.possystem.customer.Customer;
import com.possystem.customer.CustomerRepository;
import com.possystem.security.SecurityContextUtil;
import com.possystem.shop.Shop;
import com.possystem.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesReceiptService {

    private final SalesOrderRepository salesOrderRepository;
    private final ShopRepository shopRepository;
    private final CustomerRepository customerRepository;

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(33, 37, 41);
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(248, 249, 250);
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(222, 226, 230);

    // 80mm thermal receipt width ~ 226 points
    private static final PageSize RECEIPT_PAGE = new PageSize(226, 800);

    public byte[] generateReceipt(UUID orderId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(orderId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Receipt can only be generated for COMPLETED orders");
        }

        Shop shop = shopRepository.findById(shopId).orElse(null);
        Customer customer = order.getCustomerId() != null
                ? customerRepository.findByIdAndShopIdAndIsActiveTrue(order.getCustomerId(), shopId).orElse(null)
                : null;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(RECEIPT_PAGE);
            Document doc = new Document(pdfDoc);
            doc.setMargins(10, 10, 10, 10);

            PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont normal = PdfFontFactory.createFont("Helvetica");

            // === SHOP HEADER ===
            if (shop != null) {
                doc.add(new Paragraph(shop.getShopName())
                        .setFont(bold).setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(0));
                if (shop.getAddress() != null) {
                    doc.add(new Paragraph(shop.getAddress())
                            .setFont(normal).setFontSize(7)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontColor(ColorConstants.GRAY)
                            .setMarginBottom(0));
                }
                StringBuilder contact = new StringBuilder();
                if (shop.getPhone() != null) contact.append("Tel: ").append(shop.getPhone());
                if (shop.getEmail() != null) {
                    if (!contact.isEmpty()) contact.append(" | ");
                    contact.append(shop.getEmail());
                }
                if (!contact.isEmpty()) {
                    doc.add(new Paragraph(contact.toString())
                            .setFont(normal).setFontSize(7)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontColor(ColorConstants.GRAY)
                            .setMarginBottom(2));
                }
            }

            // === RECEIPT TITLE ===
            doc.add(new Paragraph("SALES RECEIPT")
                    .setFont(bold).setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2));

            // Dashed divider
            doc.add(dashedDivider());

            // === ORDER INFO ===
            Table infoTable = noBorderTable(new float[]{1f, 1f});

            addInfoRow(infoTable, "Receipt #:", order.getOrderNumber(), bold, normal);
            addInfoRow(infoTable, "Date:", order.getCompletedAt() != null
                    ? order.getCompletedAt().format(DATETIME_FMT)
                    : order.getCreatedAt().format(DATETIME_FMT), bold, normal);
            if (customer != null) {
                addInfoRow(infoTable, "Customer:", customer.getCustomerName(), bold, normal);
            }

            doc.add(infoTable);
            doc.add(dashedDivider());

            // === ITEMS TABLE ===
            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{0.5f, 3f, 1f, 1.5f}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER)
                    .setMarginBottom(2);

            // Header
            itemsTable.addHeaderCell(receiptHeaderCell("#", bold));
            itemsTable.addHeaderCell(receiptHeaderCell("Item", bold));
            itemsTable.addHeaderCell(receiptHeaderCell("Qty", bold).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(receiptHeaderCell("Amount", bold).setTextAlignment(TextAlignment.RIGHT));

            // Items
            int idx = 1;
            for (SalesOrderItem item : order.getItems()) {
                boolean isEven = idx % 2 == 0;
                DeviceRgb bg = isEven ? LIGHT_BG : null;

                itemsTable.addCell(receiptCell(String.valueOf(idx), normal, bg));

                // Item name + variant + unit price
                Cell nameCell = new Cell().setBorder(Border.NO_BORDER).setPadding(2);
                if (bg != null) nameCell.setBackgroundColor(bg);
                nameCell.add(new Paragraph(item.getProductName())
                        .setFont(normal).setFontSize(7).setMarginBottom(0));
                if (item.getVariantName() != null && !item.getVariantName().isBlank()) {
                    nameCell.add(new Paragraph(item.getVariantName() + " @ " + formatAmount(item.getUnitPrice()))
                            .setFont(normal).setFontSize(6)
                            .setFontColor(ColorConstants.GRAY).setMarginBottom(0));
                }
                if (item.getDiscountAmount() != null && item.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    nameCell.add(new Paragraph("Disc: -" + formatAmount(item.getDiscountAmount()))
                            .setFont(normal).setFontSize(6)
                            .setFontColor(new DeviceRgb(220, 53, 69)).setMarginBottom(0));
                }
                itemsTable.addCell(nameCell);

                itemsTable.addCell(receiptCell(item.getQuantity().stripTrailingZeros().toPlainString(), normal, bg)
                        .setTextAlignment(TextAlignment.CENTER));
                itemsTable.addCell(receiptCell(formatAmount(item.getTotalPrice()), normal, bg)
                        .setTextAlignment(TextAlignment.RIGHT));
                idx++;
            }

            doc.add(itemsTable);
            doc.add(dashedDivider());

            // === TOTALS ===
            Table totals = noBorderTable(new float[]{2f, 1.5f});

            addTotalRow(totals, "Subtotal", formatAmount(order.getSubtotal()), normal, normal);

            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                String discLabel = "Discount";
                if (order.getDiscountType() == DiscountType.PERCENTAGE && order.getDiscountValue() != null) {
                    discLabel += " (" + order.getDiscountValue().stripTrailingZeros().toPlainString() + "%)";
                }
                addTotalRow(totals, discLabel, "-" + formatAmount(order.getDiscountAmount()), normal, normal);
            }

            if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                String taxLabel = "Tax";
                if (order.getTaxRate() != null && order.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
                    taxLabel += " (" + order.getTaxRate().stripTrailingZeros().toPlainString() + "%)";
                }
                addTotalRow(totals, taxLabel, formatAmount(order.getTaxAmount()), normal, normal);
            }

            doc.add(totals);

            // Grand total (highlighted)
            Table grandTotal = noBorderTable(new float[]{2f, 1.5f});
            grandTotal.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setBorderTop(new SolidBorder(PRIMARY_COLOR, 1))
                    .add(new Paragraph("TOTAL").setFont(bold).setFontSize(10))
                    .setPadding(3));
            grandTotal.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setBorderTop(new SolidBorder(PRIMARY_COLOR, 1))
                    .add(new Paragraph(formatAmount(order.getTotalAmount())).setFont(bold).setFontSize(10))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(3));
            doc.add(grandTotal);

            doc.add(dashedDivider());

            // === PAYMENTS ===
            doc.add(new Paragraph("PAYMENT DETAILS")
                    .setFont(bold).setFontSize(8)
                    .setMarginBottom(2));

            Table paymentTable = noBorderTable(new float[]{2f, 1.5f});
            for (SalesPayment payment : order.getPayments()) {
                String method = payment.getPaymentMethod().name().replace("_", " ");
                addTotalRow(paymentTable, method, formatAmount(payment.getAmount()), normal, normal);
            }
            addTotalRow(paymentTable, "Total Paid", formatAmount(order.getAmountPaid()), bold, bold);

            if (order.getChangeAmount() != null && order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(paymentTable, "Change", formatAmount(order.getChangeAmount()), bold, bold);
            }

            doc.add(paymentTable);

            doc.add(dashedDivider());

            // === FOOTER ===
            doc.add(new Paragraph("Thank you for your purchase!")
                    .setFont(bold).setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5).setMarginBottom(2));

            doc.add(new Paragraph("Goods once sold are not returnable without receipt")
                    .setFont(normal).setFontSize(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate sales receipt PDF", e);
        }
    }

    // === HELPERS ===

    private Paragraph dashedDivider() {
        return new Paragraph("")
                .setBorderBottom(new DashedBorder(BORDER_COLOR, 0.5f))
                .setMarginTop(3).setMarginBottom(3);
    }

    private Table noBorderTable(float[] widths) {
        return new Table(UnitValue.createPercentArray(widths))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);
    }

    private void addInfoRow(Table table, String label, String value, PdfFont labelFont, PdfFont valueFont) {
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label).setFont(labelFont).setFontSize(7)
                        .setFontColor(ColorConstants.GRAY))
                .setPadding(1));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value != null ? value : "-").setFont(valueFont).setFontSize(7))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(1));
    }

    private void addTotalRow(Table table, String label, String value, PdfFont labelFont, PdfFont valueFont) {
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label).setFont(labelFont).setFontSize(8))
                .setPadding(2));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value).setFont(valueFont).setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(2));
    }

    private Cell receiptHeaderCell(String text, PdfFont font) {
        return new Cell().setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER_COLOR, 0.5f))
                .add(new Paragraph(text).setFont(font).setFontSize(7))
                .setPadding(2);
    }

    private Cell receiptCell(String text, PdfFont font, DeviceRgb bg) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(text).setFont(font).setFontSize(7))
                .setPadding(2);
        if (bg != null) cell.setBackgroundColor(bg);
        return cell;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
