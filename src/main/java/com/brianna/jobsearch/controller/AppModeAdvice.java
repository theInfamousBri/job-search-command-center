package com.brianna.jobsearch.controller;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AppModeAdvice {

    private final boolean demoMode;
    private final String databaseFileName;

    public AppModeAdvice(
            @Value("${app.demo:false}") boolean demoMode,
            @Value("${app.database.path:jobsearch.db}") String databasePath) {
        this.demoMode = demoMode;
        this.databaseFileName = Path.of(databasePath).getFileName().toString();
    }

    @ModelAttribute("demoMode")
    public boolean demoMode() {
        return demoMode;
    }

    @ModelAttribute("databaseFileName")
    public String databaseFileName() {
        return databaseFileName;
    }
}
