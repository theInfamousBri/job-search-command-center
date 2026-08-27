package com.brianna.jobsearch.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.brianna.jobsearch.repository.CompanyLogoRepository;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompanyLogoServiceTest {

    @Test
    void normalizesPlainDomainsAndUrls() {
        assertThat(CompanyLogoService.normalizeDomain("mastercard.com")).isEqualTo("mastercard.com");
        assertThat(CompanyLogoService.normalizeDomain("https://www.mastercard.com/us/en.html"))
                .isEqualTo("mastercard.com");
        assertThat(CompanyLogoService.normalizeDomain("  careers.example.org  ")).isEqualTo("careers.example.org");
    }

    @Test
    void rejectsValuesThatAreNotDomains() {
        assertThat(CompanyLogoService.normalizeDomain(null)).isNull();
        assertThat(CompanyLogoService.normalizeDomain("   ")).isNull();
        assertThat(CompanyLogoService.normalizeDomain("localhost")).isNull();
        assertThat(CompanyLogoService.normalizeDomain("not a domain")).isNull();
    }

    @Test
    void discoversDeclaredIconsAndPrefersLargerModernAssets() {
        CompanyLogoService service = new CompanyLogoService((CompanyLogoRepository) null);
        String html = """
                <html><head>
                  <link rel="shortcut icon" href="/favicon-16.ico" sizes="16x16">
                  <link rel="icon" type="image/png" href="/assets/favicon-64.png" sizes="64x64">
                  <link rel="apple-touch-icon" href="/assets/touch.png" sizes="180x180">
                  <link rel="icon" type="image/svg+xml" href="https://cdn.example.com/brand/icon.svg" sizes="any">
                </head></html>
                """;

        List<URI> icons = service.discoverIconUris(html, URI.create("https://example.com/careers"));

        assertThat(icons).contains(
                URI.create("https://example.com/favicon-16.ico"),
                URI.create("https://example.com/assets/favicon-64.png"),
                URI.create("https://example.com/assets/touch.png"),
                URI.create("https://cdn.example.com/brand/icon.svg"));
        assertThat(icons.getFirst()).isEqualTo(URI.create("https://cdn.example.com/brand/icon.svg"));
    }

    @Test
    void honorsHtmlBaseHrefWhenResolvingIcons() {
        CompanyLogoService service = new CompanyLogoService((CompanyLogoRepository) null);
        String html = """
                <html><head>
                  <base href="https://static.example.com/site/">
                  <link rel="icon" href="icons/favicon.png" sizes="64x64">
                </head></html>
                """;

        assertThat(service.discoverIconUris(html, URI.create("https://example.com/careers")))
                .containsExactly(URI.create("https://static.example.com/site/icons/favicon.png"));
    }

    @Test
    void ignoresNonWebIconSchemes() {
        CompanyLogoService service = new CompanyLogoService((CompanyLogoRepository) null);
        String html = """
                <link rel="icon" href="data:image/svg+xml,%3Csvg/%3E">
                <link rel="icon" href="javascript:alert(1)">
                <link rel="icon" href="/safe.png">
                """;

        assertThat(service.discoverIconUris(html, URI.create("https://example.com/")))
                .containsExactly(URI.create("https://example.com/safe.png"));
    }
}
