package com.gateway.paymentgateway.entity;

public enum PaymentStatus {

    CREATED,            // Order created
    PAYMENT_INITIATED,  // Checkout opened
    AUTHORIZED,         // Razorpay authorized
    CAPTURED,           // Amount captured
    SUCCESS,            // Business success
    FAILED,             // Payment failed / cancelled

    REFUND_REQUESTED,   // User requested refund
    REFUNDED            // Refund completed
}
