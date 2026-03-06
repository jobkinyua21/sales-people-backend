package com.possystem.sales.register;

import com.possystem.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashRegisterService {

    private final CashRegisterSessionRepository sessionRepository;

    // ==================== OPEN / CLOSE ====================

    @Transactional
    public CashRegisterSessionResponse openRegister(OpenRegisterRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        if (sessionRepository.existsByShopIdAndOpenedByAndStatus(shopId, userId, RegisterSessionStatus.OPEN)) {
            throw new IllegalArgumentException("You already have an open register session. Close it first.");
        }

        if (request.getOpeningBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative");
        }

        CashRegisterSession session = CashRegisterSession.builder()
                .shopId(shopId)
                .openedBy(userId)
                .openingBalance(request.getOpeningBalance())
                .expectedClosingBalance(request.getOpeningBalance())
                .status(RegisterSessionStatus.OPEN)
                .openedAt(LocalDateTime.now())
                .notes(request.getNotes())
                .build();

        CashRegisterSession saved = sessionRepository.save(session);
        return buildResponse(saved);
    }

    @Transactional
    public CashRegisterSessionResponse closeRegister(CloseRegisterRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        CashRegisterSession session = sessionRepository.findByIdAndShopId(request.getSessionId(), shopId)
                .orElseThrow(() -> new IllegalArgumentException("Register session not found"));

        if (session.getStatus() != RegisterSessionStatus.OPEN) {
            throw new IllegalArgumentException("Register session is already closed");
        }

        if (request.getActualClosingBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Closing balance cannot be negative");
        }

        // Expected = opening + cash sales + cash in - cash out - cash refunds
        BigDecimal expected = session.getOpeningBalance()
                .add(session.getTotalCashSales())
                .add(session.getTotalCashIn())
                .subtract(session.getTotalCashOut())
                .subtract(session.getTotalCashRefunds());

        session.setExpectedClosingBalance(expected);
        session.setActualClosingBalance(request.getActualClosingBalance());
        session.setDifference(request.getActualClosingBalance().subtract(expected));
        session.setClosedBy(userId);
        session.setClosedAt(LocalDateTime.now());
        session.setStatus(RegisterSessionStatus.CLOSED);

        if (request.getNotes() != null) {
            session.setNotes(request.getNotes());
        }

        CashRegisterSession saved = sessionRepository.save(session);
        return buildResponse(saved);
    }

    // ==================== CASH MOVEMENTS ====================

    @Transactional
    public CashRegisterSessionResponse addCashMovement(CashMovementRequest request) {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        CashRegisterSession session = sessionRepository
                .findByShopIdAndOpenedByAndStatus(shopId, userId, RegisterSessionStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("You don't have an open register session. Open the register first."));

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        CashMovement movement = CashMovement.builder()
                .session(session)
                .movementType(request.getMovementType())
                .amount(request.getAmount())
                .reason(request.getReason())
                .recordedBy(userId)
                .build();

        session.getMovements().add(movement);

        if (request.getMovementType() == CashMovementType.CASH_IN) {
            session.setTotalCashIn(session.getTotalCashIn().add(request.getAmount()));
        } else {
            session.setTotalCashOut(session.getTotalCashOut().add(request.getAmount()));
        }

        CashRegisterSession saved = sessionRepository.save(session);
        return buildResponse(saved);
    }

    // ==================== QUERIES ====================

    public CashRegisterSessionResponse getCurrentSession() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        Optional<CashRegisterSession> session = sessionRepository
                .findByShopIdAndOpenedByAndStatus(shopId, userId, RegisterSessionStatus.OPEN);
        return session.map(this::buildResponse).orElse(null);
    }

    public List<CashRegisterSessionResponse> getMySessionHistory() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();
        UUID userId = SecurityContextUtil.getCurrentUserId();

        return sessionRepository.findAllByShopIdAndOpenedByOrderByOpenedAtDesc(shopId, userId).stream()
                .map(this::buildResponse)
                .toList();
    }

    public List<CashRegisterSessionResponse> getAllSessionHistory() {
        UUID shopId = SecurityContextUtil.getCurrentShopId();

        return sessionRepository.findAllByShopIdOrderByOpenedAtDesc(shopId).stream()
                .map(this::buildResponse)
                .toList();
    }

    // ==================== SALES INTEGRATION ====================

    /**
     * Called by SalesOrderService when a cash payment is recorded on an order.
     * Tracks the cash amount in the cashier's open session.
     */
    @Transactional
    public void recordCashSale(UUID shopId, UUID userId, BigDecimal cashAmount) {
        sessionRepository.findByShopIdAndOpenedByAndStatus(shopId, userId, RegisterSessionStatus.OPEN)
                .ifPresent(session -> {
                    session.setTotalCashSales(session.getTotalCashSales().add(cashAmount));
                    sessionRepository.save(session);
                });
    }

    /**
     * Called by SalesOrderService when a cash refund is issued (order cancellation).
     */
    @Transactional
    public void recordCashRefund(UUID shopId, UUID userId, BigDecimal refundAmount) {
        sessionRepository.findByShopIdAndOpenedByAndStatus(shopId, userId, RegisterSessionStatus.OPEN)
                .ifPresent(session -> {
                    session.setTotalCashRefunds(session.getTotalCashRefunds().add(refundAmount));
                    sessionRepository.save(session);
                });
    }

    /**
     * Check if the given user has an open register session for the given shop.
     */
    public boolean hasOpenSession(UUID shopId, UUID userId) {
        return sessionRepository.existsByShopIdAndOpenedByAndStatus(shopId, userId, RegisterSessionStatus.OPEN);
    }

    // ==================== RESPONSE BUILDING ====================

    private CashRegisterSessionResponse buildResponse(CashRegisterSession session) {
        List<CashRegisterSessionResponse.CashMovementResponse> movements = session.getMovements().stream()
                .map(m -> CashRegisterSessionResponse.CashMovementResponse.builder()
                        .id(m.getId())
                        .movementType(m.getMovementType())
                        .amount(m.getAmount())
                        .reason(m.getReason())
                        .recordedBy(m.getRecordedBy())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();

        return CashRegisterSessionResponse.builder()
                .id(session.getId())
                .shopId(session.getShopId())
                .openedBy(session.getOpenedBy())
                .closedBy(session.getClosedBy())
                .status(session.getStatus())
                .openingBalance(session.getOpeningBalance())
                .totalCashSales(session.getTotalCashSales())
                .totalCashIn(session.getTotalCashIn())
                .totalCashOut(session.getTotalCashOut())
                .totalCashRefunds(session.getTotalCashRefunds())
                .expectedClosingBalance(session.getExpectedClosingBalance())
                .actualClosingBalance(session.getActualClosingBalance())
                .difference(session.getDifference())
                .notes(session.getNotes())
                .openedAt(session.getOpenedAt())
                .closedAt(session.getClosedAt())
                .createdAt(session.getCreatedAt())
                .movements(movements)
                .build();
    }
}
