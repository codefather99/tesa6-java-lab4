package com.ledger.settlement.web;

import java.net.URI;

import com.ledger.settlement.domain.PaymentEntity;
import com.ledger.settlement.service.SettlementService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Constructor injection only - SettlementService arrives as a constructor parameter, not
 * via field @Autowired. See SettlementService's own javadoc and the Agent Review in
 * research.md for why that matters beyond style.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final SettlementService settlementService;

    public PaymentController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * Records a payment. @Valid triggers bean validation on RecordPaymentRequest before
     * this method body runs at all; a blank merchantId or a non-positive amountMinor
     * never reaches SettlementService - MethodArgumentNotValidException is thrown first
     * and ProblemHandler turns it into a 400 problem detail.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> record(@Valid @RequestBody RecordPaymentRequest request) {
        PaymentEntity saved = settlementService.record(
                request.merchantId(), request.amountMinor(), request.currency());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(PaymentResponse.from(saved));
    }

    /**
     * A single payment by id. Added for the Agent Review step in this lab - see
     * research.md for the four defects checked and fixed against the AI agent's first
     * attempt at this exact endpoint.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> findById(@PathVariable String id) {
        PaymentEntity entity = settlementService.findById(id);
        return ResponseEntity.ok(PaymentResponse.from(entity));
    }

    @GetMapping("/settlement")
    public ResponseEntity<SettlementResponse> settlement(@RequestParam String merchantId) {
        long amountOwed = settlementService.settlementFor(merchantId);
        return ResponseEntity.ok(new SettlementResponse(merchantId, amountOwed));
    }
}
