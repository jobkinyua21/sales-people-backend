package com.possystem.sales.receipt;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.customer.Customer;
import com.possystem.customer.CustomerRepository;
import com.possystem.sales.OrderStatus;
import com.possystem.sales.SalesOrder;
import com.possystem.sales.SalesOrderItem;
import com.possystem.sales.SalesOrderRepository;
import com.possystem.sales.SalesPayment;
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
public class ReceiptService {

    private final SalesOrderRepository salesOrderRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float RECEIPT_WIDTH = 226f; // ~80mm thermal receipt width in points
    private static final float FONT_SIZE = 8f;
    private static final float FONT_SIZE_LARGE = 10f;
    private static final float FONT_SIZE_SMALL = 7f;

    public byte[] generateReceipt(UUID orderId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        SalesOrder order = salesOrderRepository.findByIdAndShopIdAndIsActiveTrue(orderId, shopId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot generate receipt for a cancelled order");
        }

        Shop shop = shopRepository.findById(shopId).orElse(null);

        String cashierName = resolveName(order.getServedBy());
        String customerName = order.getCustomerId() != null
                ? resolveCustomerName(order.getCustomerId(), shopId)
                : null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);

            // Dynamic height — start tall, will trim
            PageSize pageSize = new PageSize(RECEIPT_WIDTH, 1000);
            Document doc = new Document(pdf, pageSize);
            doc.setMargins(10, 10, 10, 10);

            // ===== HEADER =====
            addShopHeader(doc, shop);
            addDivider(doc);

            // ===== ORDER INFO =====
            addOrderInfo(doc, order, cashierName, customerName);
            addDivider(doc);

            // ===== ITEMS TABLE =====
            addItemsTable(doc, order);
            addDivider(doc);

            // ===== TOTALS =====
            addTotals(doc, order);
            addDivider(doc);

            // ===== PAYMENTS =====
            addPayments(doc, order);
            addDivider(doc);

            // ===== FOOTER =====
            addFooter(doc);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate receipt", e);
        }
    }

    // ==================== SECTIONS ====================

    private void addShopHeader(Document doc, Shop shop) {
        if (shop == null) return;

        doc.add(new Paragraph(shop.getShopName())
                .setFontSize(FONT_SIZE_LARGE)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2));

        if (shop.getAddress() != null) {
            doc.add(new Paragraph(shop.getAddress())
                    .setFontSize(FONT_SIZE_SMALL)
                    .setTextAlignment(TextAlignment.CENTER)
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
                    .setFontSize(FONT_SIZE_SMALL)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2));
        }
    }

    private void addOrderInfo(Document doc, SalesOrder order, String cashierName, String customerName) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        addInfoRow(table, "Receipt #:", order.getOrderNumber());
        addInfoRow(table, "Date:", order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "");
        addInfoRow(table, "Cashier:", cashierName != null ? cashierName : "-");
        if (customerName != null) {
            addInfoRow(table, "Customer:", customerName);
        }

        doc.add(table);
    }

    private void addItemsTable(Document doc, SalesOrder order) {
        // Header
        Table header = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1.5f, 1.5f}))
                .useAllAvailableWidth();

        addHeaderCell(header, "Item");
        addHeaderCell(header, "Qty");
        addHeaderCell(header, "Price");
        addHeaderCell(header, "Total");

        doc.add(header);

        // Items
        for (SalesOrderItem item : order.getItems()) {
            Table row = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1.5f, 1.5f}))
                    .useAllAvailableWidth();

            addItemCell(row, item.getProductName());
            addItemCell(row, formatQty(item.getQuantity()));
            addItemCell(row, formatAmount(item.getUnitPrice()));
            addItemCell(row, formatAmount(item.getTotalPrice()));

            doc.add(row);

            // Show discount if applied
            if (item.getDiscountAmount() != null && item.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                doc.add(new Paragraph("  Discount: -" + formatAmount(item.getDiscountAmount()))
                        .setFontSize(FONT_SIZE_SMALL)
                        .setMarginBottom(0)
                        .setMarginTop(0));
            }
        }
    }

    private void addTotals(Document doc, SalesOrder order) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        addTotalRow(table, "Subtotal:", formatAmount(order.getSubtotal()), false);

        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(table, "Discount:", "-" + formatAmount(order.getDiscountAmount()), false);
        }

        if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            String taxLabel = "Tax";
            if (order.getTaxRate() != null && order.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
                taxLabel += " (" + order.getTaxRate().stripTrailingZeros().toPlainString() + "%)";
            }
            addTotalRow(table, taxLabel + ":", formatAmount(order.getTaxAmount()), false);
        }

        addTotalRow(table, "TOTAL:", formatAmount(order.getTotalAmount()), true);

        doc.add(table);
    }

    private void addPayments(Document doc, SalesOrder order) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        for (SalesPayment payment : order.getPayments()) {
            addTotalRow(table, payment.getPaymentMethod().name() + ":", formatAmount(payment.getAmount()), false);
        }

        if (order.getChangeAmount() != null && order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(table, "Change:", formatAmount(order.getChangeAmount()), true);
        }

        doc.add(table);
    }

    private void addFooter(Document doc) {
        doc.add(new Paragraph("Thank you for your purchase!")
                .setFontSize(FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(5)
                .setMarginBottom(2));

        doc.add(new Paragraph("Goods once sold are not returnable without a receipt")
                .setFontSize(FONT_SIZE_SMALL)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0));
    }

    private void addDivider(Document doc) {
        doc.add(new Paragraph("- - - - - - - - - - - - - - - - - - - - - - - -")
                .setFontSize(FONT_SIZE_SMALL)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(3)
                .setMarginBottom(3));
    }

    // ==================== CELL HELPERS ====================

    private void addInfoRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setFontSize(FONT_SIZE_SMALL).setBold())
                .setBorder(Border.NO_BORDER).setPadding(0));
        table.addCell(new Cell().add(new Paragraph(value).setFontSize(FONT_SIZE_SMALL))
                .setBorder(Border.NO_BORDER).setPadding(0)
                .setTextAlignment(TextAlignment.RIGHT));
    }

    private void addHeaderCell(Table table, String text) {
        table.addCell(new Cell().add(new Paragraph(text).setFontSize(FONT_SIZE_SMALL).setBold())
                .setBorder(Border.NO_BORDER).setPaddingBottom(2));
    }

    private void addItemCell(Table table, String text) {
        table.addCell(new Cell().add(new Paragraph(text).setFontSize(FONT_SIZE_SMALL))
                .setBorder(Border.NO_BORDER).setPadding(0));
    }

    private void addTotalRow(Table table, String label, String value, boolean bold) {
        Paragraph labelP = new Paragraph(label).setFontSize(FONT_SIZE);
        Paragraph valueP = new Paragraph(value).setFontSize(FONT_SIZE);
        if (bold) {
            labelP.setBold();
            valueP.setBold();
        }
        table.addCell(new Cell().add(labelP).setBorder(Border.NO_BORDER).setPadding(0));
        table.addCell(new Cell().add(valueP).setBorder(Border.NO_BORDER).setPadding(0)
                .setTextAlignment(TextAlignment.RIGHT));
    }

    // ==================== HELPERS ====================

    private String resolveName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse(null);
    }

    private String resolveCustomerName(UUID customerId, UUID shopId) {
        return customerRepository.findByIdAndShopIdAndIsActiveTrue(customerId, shopId)
                .map(Customer::getCustomerName)
                .orElse(null);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatQty(BigDecimal qty) {
        if (qty == null) return "0";
        return qty.stripTrailingZeros().toPlainString();
    }
}
