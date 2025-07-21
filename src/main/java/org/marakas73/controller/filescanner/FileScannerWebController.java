package org.marakas73.controller.filescanner;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/file-scanner")
public class FileScannerWebController {
    @GetMapping()
    public String mainPage() {
        return "file-scanner";
    }
}
