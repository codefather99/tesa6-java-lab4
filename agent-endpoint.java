// Unedited output from the AI agent.

package com.ledger.settlement.web;

import com.ledger.settlement.domain.PaymentEntity;
import com.ledger.settlement.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentByIdController {

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/payments/{id}")
    public PaymentEntity getPayment(@PathVariable String id) {
        PaymentEntity payment = paymentRepository.findById(id).orElse(null);
        if (payment == null) {
            throw new RuntimeException("Payment not found: " + id);
        }
        return payment;
    }
}
