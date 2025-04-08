package com.nour.ali.java_learning_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RouteController {

    @RequestMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
