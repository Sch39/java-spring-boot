package dev.sch39.tus_library.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UploadPageController {
    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }
}