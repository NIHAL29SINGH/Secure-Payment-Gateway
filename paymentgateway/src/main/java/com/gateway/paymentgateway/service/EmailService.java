package com.gateway.paymentgateway.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void send(String to, String subject, String body) {
        System.out.println("=================================");
        System.out.println("EMAIL TO   : " + to);
        System.out.println("SUBJECT    : " + subject);
        System.out.println("BODY:");
        System.out.println(body);
        System.out.println("=================================");
    }
}
