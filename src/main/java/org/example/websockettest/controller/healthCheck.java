package org.example.websockettest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class healthCheck {

    @GetMapping("/ws/health")
    public String getMethodName() {
        return "ok";
    }
}
