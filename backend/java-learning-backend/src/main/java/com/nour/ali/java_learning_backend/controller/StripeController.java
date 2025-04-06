// controller/StripeController.java
package com.nour.ali.java_learning_backend.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    @Value("${stripe.checkout.successUrl}")
    private String successUrl;

    @Value("${stripe.checkout.cancelUrl}")
    private String cancelUrl;

    @PostMapping("/create-checkout-session")
    public Map<String, Object> createCheckoutSession(@RequestParam String studentId) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?studentId=" + studentId)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(3500L) // $35.00 in cents
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Course Access Fee")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("checkoutUrl", session.getUrl());
        return responseData;
    }
}
