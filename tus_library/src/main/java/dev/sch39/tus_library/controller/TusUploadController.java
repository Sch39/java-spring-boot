package dev.sch39.tus_library.controller;

import me.desair.tus.server.TusFileUploadService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@RestController
public class TusUploadController {

    private final TusFileUploadService tusFileUploadService;

    public TusUploadController(TusFileUploadService tusFileUploadService) {
        this.tusFileUploadService = tusFileUploadService;
    }

    @RequestMapping(value = { "/files", "/files/**" }, method = {
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.HEAD,
            RequestMethod.DELETE,
            RequestMethod.OPTIONS,
            RequestMethod.GET
    })
    public void processUpload(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        tusFileUploadService.process(request, response);
    }
}
