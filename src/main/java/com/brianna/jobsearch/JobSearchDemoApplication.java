package com.brianna.jobsearch;

import org.springframework.boot.SpringApplication;

/**
 * Convenience launcher for a sanitized, screenshot-friendly demo environment.
 *
 * <p>The demo profile uses a separate SQLite database and reseeds it with
 * synthetic data on every startup, so the user's real {@code jobsearch.db}
 * is never read or modified.</p>
 */
public class JobSearchDemoApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(JobSearchDashboardApplication.class);
        application.setAdditionalProfiles("demo");
        application.run(args);
    }
}
