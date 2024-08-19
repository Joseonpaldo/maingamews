package org.example.websockettest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ws")
public class healthCheck {

    @GetMapping("/health")
    public String getMethodName() {
        return "ok";
    }
}
