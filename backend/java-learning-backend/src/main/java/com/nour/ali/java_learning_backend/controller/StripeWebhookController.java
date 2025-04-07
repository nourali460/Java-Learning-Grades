// File: controller/StripeWebhookController.java
package com.nour.ali.java_learning_backend.controller;

import com.nour.ali.java_learning_backend.model.Student;
import com.nour.ali.java_learning_backend.service.StudentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final StudentService studentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StripeWebhookController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Verify webhook signature
            com.stripe.model.Event event = Webhook.constructEvent(
                    payload, sigHeader, endpointSecret
            );

            if ("checkout.session.completed".equals(event.getType())) {
                JsonNode json = objectMapper.readTree(payload);
                JsonNode sessionObj = json.get("data").get("object");
                String successUrl = sessionObj.get("success_url").asText();

                // Extract studentId from success_url param
                String studentId = null;
                if (successUrl.contains("studentId=")) {
                    studentId = successUrl.substring(successUrl.indexOf("studentId=") + 10);
                }

                if (studentId != null) {
                    Optional<Student> optionalStudent = studentService.findById(studentId);
                    if (optionalStudent.isPresent()) {
                        Student student = optionalStudent.get();
                        student.setPaid(true);
                        student.setActive(true);
                        student.setPaymentDate(Instant.now());
                        studentService.save(student);
                        System.out.println("✅ Payment processed for: " + studentId);
                    } else {
                        System.out.println("❌ Student not found for ID in webhook: " + studentId);
                    }
                }
            }

            return ResponseEntity.ok("Webhook received");

        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("Webhook error");
        }
    }
}

