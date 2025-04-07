package com.nour.ali.java_learning_backend.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.checkout.successUrl}")
    private String successUrl;

    @Value("${stripe.checkout.cancelUrl}")
    private String cancelUrl;

    public String generateCheckoutUrl(String studentId) throws StripeException {
        Stripe.apiKey = stripeApiKey;

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
                                                                .setDescription("One-time payment for 12 months of course access")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("studentId", studentId)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}
