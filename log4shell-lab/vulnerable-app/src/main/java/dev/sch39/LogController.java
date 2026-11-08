package dev.sch39;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;

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

    @GetMapping("/resolve")
    public String resolve(@RequestParam String data) {
        try {
            Context ctx = new InitialContext();

            try {
                Object result = ctx.lookup(data);

                String resultType = result == null
                        ? "null"
                        : result.getClass().getName();

                boolean isReference = result instanceof Reference;

                log.info("Lookup result type: {}", resultType);
                log.info("Is Reference: {}", isReference);
                log.info("Lookup result: {}", result);

                return "Lookup successful"
                        + "\nType: " + resultType
                        + "\nIs Reference: " + isReference
                        + "\nResult: " + result;

            } finally {
                ctx.close();
            }

        } catch (NamingException e) {
            log.warn("JNDI lookup failed: {}", e.toString());
            return "Lookup failed: " + e.getClass().getSimpleName();
        } catch (Exception e) {
            log.error("Unexpected resolution error", e);
            return "Failed: " + e.getClass().getSimpleName();
        }
    }
}