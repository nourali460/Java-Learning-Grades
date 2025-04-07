package com.nour.ali.java_learning_backend.controller;

import com.nour.ali.java_learning_backend.service.StripeService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private final StripeService stripeService;

    @Autowired
    public StripeController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/create-checkout-session")
    public Map<String, Object> createCheckoutSession(@RequestParam String studentId) throws StripeException {
        String checkoutUrl = stripeService.generateCheckoutUrl(studentId);
        return Map.of("checkoutUrl", checkoutUrl);
    }
}
