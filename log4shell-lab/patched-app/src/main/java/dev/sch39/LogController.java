package dev.sch39;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogController {

    private static final Logger log = LogManager.getLogger(LogController.class);

    @GetMapping("/log")
    public String log(@RequestParam String data) {
        log.info("User submitted: {}", data);
        return "logged";
    }
}