package com.possystem.purchasing.payment;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.possystem.purchasing.invoice.SupplierInvoice;
import com.possystem.security.SecurityContextUtil;
import com.possystem.shop.Shop;
import com.possystem.shop.ShopRepository;
import com.possystem.supplier.Supplier;
import com.possystem.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchasePaymentVoucherService {

    private final PurchasePaymentRepository purchasePaymentRepository;
    private final ShopRepository shopRepository;
    private final SupplierRepository supplierRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(33, 37, 41);
    private static final DeviceRgb HEADER_BG = new DeviceRgb(52, 58, 64);
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(248, 249, 250);
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(222, 226, 230);

    public byte[] generateVoucher(UUID paymentId) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        PurchasePayment payment = purchasePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!payment.getShopId().equals(shopId)) {
            throw new IllegalArgumentException("Payment not found");
        }

        SupplierInvoice invoice = payment.getSupplierInvoice();

        Shop shop = shopRepository.findById(shopId).orElse(null);
        Supplier supplier = supplierRepository.findByIdAndShopIdAndIsActiveTrue(invoice.getSupplierId(), shopId).orElse(null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A5);
            Document doc = new Document(pdfDoc);
            doc.setMargins(25, 25, 25, 25);

            PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont normal = PdfFontFactory.createFont("Helvetica");

            // === HEADER: Company info + PAYMENT VOUCHER title ===
            Table header = new Table(UnitValue.createPercentArray(new float[]{3f, 2f}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            // Left: company info
            Cell companyCell = new Cell().setBorder(Border.NO_BORDER);
            if (shop != null) {
                companyCell.add(new Paragraph(shop.getShopName())
                        .setFont(bold).setFontSize(14).setFontColor(PRIMARY_COLOR));
                if (shop.getAddress() != null) {
                    companyCell.add(new Paragraph(shop.getAddress())
                            .setFont(normal).setFontSize(8).setFontColor(ColorConstants.GRAY));
                }
                StringBuilder contact = new StringBuilder();
                if (shop.getPhone() != null) contact.append(shop.getPhone());
                if (shop.getEmail() != null) {
                    if (!contact.isEmpty()) contact.append(" | ");
                    contact.append(shop.getEmail());
                }
                if (!contact.isEmpty()) {
                    companyCell.add(new Paragraph(contact.toString())
                            .setFont(normal).setFontSize(8).setFontColor(ColorConstants.GRAY));
                }
            }
            header.addCell(companyCell);

            // Right: PAYMENT VOUCHER label + voucher number
            Cell titleCell = new Cell().setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);
            titleCell.add(new Paragraph("PAYMENT VOUCHER")
                    .setFont(bold).setFontSize(14).setFontColor(new DeviceRgb(0, 123, 255)));
            titleCell.add(new Paragraph(payment.getVoucherNumber())
                    .setFont(bold).setFontSize(11).setFontColor(PRIMARY_COLOR));
            header.addCell(titleCell);

            doc.add(header);

            // Divider
            doc.add(new Paragraph("")
                    .setBorderBottom(new SolidBorder(BORDER_COLOR, 1))
                    .setMarginBottom(10).setMarginTop(5));

            // === DETAILS SECTION ===
            Table details = new Table(UnitValue.createPercentArray(new float[]{1f, 1f}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER)
                    .setMarginBottom(10);

            // Left column: Pay To & Invoice
            Cell leftCol = new Cell().setBorder(Border.NO_BORDER);
            leftCol.add(detailLabel("Pay To:", bold));
            leftCol.add(detailValue(supplier != null ? supplier.getSupplierName() : "-", bold));
            if (supplier != null && supplier.getCompanyName() != null) {
                leftCol.add(new Paragraph(supplier.getCompanyName())
                        .setFont(normal).setFontSize(8).setFontColor(ColorConstants.GRAY)
                        .setMarginTop(-2));
            }
            leftCol.add(detailLabel("Invoice #:", bold).setMarginTop(8));
            leftCol.add(detailValue(invoice.getInvoiceNumber() + " (" + invoice.getReferenceCode() + ")", normal));
            details.addCell(leftCol);

            // Right column: Date & Payment Method
            Cell rightCol = new Cell().setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);
            rightCol.add(detailLabel("Date:", bold).setTextAlignment(TextAlignment.RIGHT));
            rightCol.add(detailValue(
                    payment.getPaidAt() != null ? payment.getPaidAt().format(DATETIME_FMT) : "-", normal)
                    .setTextAlignment(TextAlignment.RIGHT));
            rightCol.add(detailLabel("Payment Method:", bold).setMarginTop(8)
                    .setTextAlignment(TextAlignment.RIGHT));
            rightCol.add(detailValue(formatPaymentMethod(payment.getPaymentMethod().name()), normal)
                    .setTextAlignment(TextAlignment.RIGHT));
            if (payment.getReferenceNumber() != null) {
                rightCol.add(detailLabel("Reference #:", bold).setMarginTop(8)
                        .setTextAlignment(TextAlignment.RIGHT));
                rightCol.add(detailValue(payment.getReferenceNumber(), normal)
                        .setTextAlignment(TextAlignment.RIGHT));
            }
            details.addCell(rightCol);

            doc.add(details);

            // === AMOUNT TABLE ===
            Table amountTable = new Table(UnitValue.createPercentArray(new float[]{3f, 2f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(10);

            // Header row
            amountTable.addHeaderCell(new Cell()
                    .add(new Paragraph("Description").setFont(bold).setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(HEADER_BG)
                    .setPadding(6));
            amountTable.addHeaderCell(new Cell()
                    .add(new Paragraph("Amount").setFont(bold).setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(HEADER_BG)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(6));

            // Invoice total row
            amountTable.addCell(tableCell("Invoice Total (" + invoice.getReferenceCode() + ")", normal, LIGHT_BG));
            amountTable.addCell(tableCellRight(formatAmount(invoice.getTotalAmount()), normal, LIGHT_BG));

            // Previously paid
            BigDecimal previouslyPaid = invoice.getAmountPaid().subtract(payment.getAmount());
            if (previouslyPaid.compareTo(BigDecimal.ZERO) > 0) {
                amountTable.addCell(tableCell("Previously Paid", normal, null));
                amountTable.addCell(tableCellRight("(" + formatAmount(previouslyPaid) + ")", normal, null));
            }

            // This payment (highlighted)
            amountTable.addCell(new Cell()
                    .add(new Paragraph("This Payment").setFont(bold).setFontSize(9))
                    .setBackgroundColor(new DeviceRgb(232, 245, 233))
                    .setPadding(6));
            amountTable.addCell(new Cell()
                    .add(new Paragraph(formatAmount(payment.getAmount())).setFont(bold).setFontSize(11))
                    .setBackgroundColor(new DeviceRgb(232, 245, 233))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(6));

            // Balance due
            BigDecimal balanceAfter = invoice.getTotalAmount().subtract(invoice.getAmountPaid());
            amountTable.addCell(tableCell("Balance Due", normal, LIGHT_BG));
            amountTable.addCell(tableCellRight(formatAmount(balanceAfter), bold, LIGHT_BG));

            doc.add(amountTable);

            // === NOTES ===
            if (payment.getNotes() != null && !payment.getNotes().isBlank()) {
                doc.add(new Paragraph("Notes:").setFont(bold).setFontSize(8)
                        .setFontColor(ColorConstants.GRAY).setMarginBottom(2));
                doc.add(new Paragraph(payment.getNotes()).setFont(normal).setFontSize(8)
                        .setMarginBottom(10));
            }

            // === SIGNATURE LINES ===
            doc.add(new Paragraph("").setMarginTop(20));
            Table sigTable = new Table(UnitValue.createPercentArray(new float[]{1f, 1f}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            sigTable.addCell(signatureCell("Prepared By", bold, normal));
            sigTable.addCell(signatureCell("Approved By", bold, normal));

            doc.add(sigTable);

            // === FOOTER ===
            doc.add(new Paragraph("This is a system-generated payment voucher.")
                    .setFont(normal).setFontSize(7).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(15));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payment voucher PDF", e);
        }
    }

    private Paragraph detailLabel(String text, PdfFont font) {
        return new Paragraph(text).setFont(font).setFontSize(8)
                .setFontColor(ColorConstants.GRAY).setMarginBottom(1);
    }

    private Paragraph detailValue(String text, PdfFont font) {
        return new Paragraph(text).setFont(font).setFontSize(10)
                .setFontColor(PRIMARY_COLOR).setMarginBottom(0);
    }

    private Cell tableCell(String text, PdfFont font, DeviceRgb bg) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9))
                .setPadding(6);
        if (bg != null) cell.setBackgroundColor(bg);
        return cell;
    }

    private Cell tableCellRight(String text, PdfFont font, DeviceRgb bg) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(6);
        if (bg != null) cell.setBackgroundColor(bg);
        return cell;
    }

    private Cell signatureCell(String label, PdfFont bold, PdfFont normal) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(10).setPaddingRight(10);
        cell.add(new Paragraph("_________________________")
                .setFont(normal).setFontSize(9).setTextAlignment(TextAlignment.CENTER));
        cell.add(new Paragraph(label)
                .setFont(bold).setFontSize(8).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));
        return cell;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }

    private String formatPaymentMethod(String method) {
        return method.replace("_", " ");
    }
}
