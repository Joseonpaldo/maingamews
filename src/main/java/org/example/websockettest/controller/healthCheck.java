package org.example.websockettest.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor
public class healthCheck {

    @GetMapping("/health")
    public String getMethodName() {
        return "ok";
    }
}
