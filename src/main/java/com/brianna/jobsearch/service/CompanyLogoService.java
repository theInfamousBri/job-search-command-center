package com.brianna.jobsearch.service;

import com.brianna.jobsearch.repository.CompanyLogoRepository;
import com.brianna.jobsearch.repository.CompanyLogoRepository.CompanyLogo;
import java.io.InputStream;
import java.io.StringReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyLogoService {

    private static final int MAX_LOGO_BYTES = 1_000_000;
    private static final int MAX_HTML_BYTES = 2_000_000;
    private static final int MAX_MANIFEST_BYTES = 512_000;
    private static final int MAX_REDIRECTS = 5;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; Job-Search-Command-Center/1.2.0; +local-app)";

    private static final Pattern MANIFEST_ICON_OBJECT = Pattern.compile(
            "\\{(?=[^{}]*\\\"src\\\"\\s*:)([^{}]*)}", Pattern.DOTALL);
    private static final Pattern JSON_SRC = Pattern.compile(
            "\\\"src\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"", Pattern.DOTALL);
    private static final Pattern JSON_SIZES = Pattern.compile(
            "\\\"sizes\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"", Pattern.DOTALL);
    private static final Pattern JSON_PURPOSE = Pattern.compile(
            "\\\"purpose\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"", Pattern.DOTALL);
    private static final Pattern CHARSET_PATTERN = Pattern.compile(
            "charset\\s*=\\s*[\\\"']?([^;\\\"'\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)");
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
            "\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);

    private final CompanyLogoRepository repository;
    private final HttpClient httpClient;

    public CompanyLogoService(CompanyLogoRepository repository) {
        this.repository = repository;
        // Redirects are followed manually so every target can be checked before a request is sent.
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public Optional<CompanyLogo> find(String rawDomain) {
        String domain = normalizeDomain(rawDomain);
        return domain == null ? Optional.empty() : repository.findByDomain(domain);
    }

    public boolean hasLogo(String rawDomain) {
        String domain = normalizeDomain(rawDomain);
        return domain != null && repository.exists(domain);
    }

    /**
     * Discover the site's preferred icon instead of assuming one conventional path.
     *
     * Discovery order is deliberately browser-like:
     * 1. homepage <link rel="icon"> / apple-touch-icon declarations
     * 2. icons declared by a web app manifest
     * 3. conventional favicon / touch-icon paths as a fallback
     */
    public CompanyLogo fetchAndCache(String rawDomain) {
        String domain = requireDomain(rawDomain);
        ensurePublicHost(domain);

        List<IconCandidate> candidates = new ArrayList<>();
        Optional<PageDocument> page = fetchHomepage(domain);

        if (page.isPresent()) {
            PageDocument homepage = page.get();
            candidates.addAll(discoverHtmlIcons(homepage.links(), homepage.finalUri()));
            candidates.addAll(discoverManifestIcons(homepage.links(), homepage.finalUri()));
        }

        // Keep the original simple behavior as a last resort.
        candidates.add(new IconCandidate(URI.create("https://" + domain + "/apple-touch-icon.png"), 620, "fallback"));
        candidates.add(new IconCandidate(URI.create("https://" + domain + "/favicon.ico"), 600, "fallback"));
        candidates.add(new IconCandidate(URI.create("https://" + domain + "/favicon.png"), 580, "fallback"));

        for (IconCandidate candidate : deduplicateAndRank(candidates)) {
            Optional<DownloadedImage> image = downloadImage(candidate.uri());
            if (image.isEmpty()) {
                continue;
            }

            DownloadedImage downloaded = image.get();
            repository.upsert(domain, downloaded.mimeType(), downloaded.data(), downloaded.finalUri().toString());
            return repository.findByDomain(domain).orElseThrow();
        }

        throw new IllegalArgumentException(
                "No usable site icon was found for " + domain
                        + ". The site may block automated requests; try uploading a logo manually instead.");
    }

    public void storeUpload(String rawDomain, MultipartFile file) {
        String domain = requireDomain(rawDomain);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose an image file first.");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("Logo images must be 1 MB or smaller.");
        }

        try {
            byte[] data = file.getBytes();
            String mimeType = normalizeImageMimeType(file.getContentType(), data);
            if (mimeType == null) {
                throw new IllegalArgumentException("Use a PNG, JPEG, WebP, GIF, ICO, or SVG image.");
            }
            if ("image/svg+xml".equals(mimeType) && !isSafeSvg(data)) {
                throw new IllegalArgumentException(
                        "That SVG contains unsupported active content. Use a simple logo SVG or raster image instead.");
            }
            repository.upsert(domain, mimeType, data, "manual-upload");
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not read the uploaded logo.", ex);
        }
    }

    public void delete(String rawDomain) {
        String domain = requireDomain(rawDomain);
        repository.delete(domain);
    }

    public static String normalizeDomain(String rawDomain) {
        if (rawDomain == null || rawDomain.isBlank()) {
            return null;
        }

        String value = rawDomain.trim();
        try {
            URI uri = value.contains("://") ? URI.create(value) : URI.create("https://" + value);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            if (!host.contains(".") || host.contains(" ")) {
                return null;
            }
            return host;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Package-private for parser tests without making network calls. */
    List<URI> discoverIconUris(String html, URI pageUri) {
        ParsedPageLinks links = parsePageLinks(html);
        return discoverHtmlIcons(links, pageUri).stream()
                .sorted(Comparator.comparingInt(IconCandidate::score).reversed())
                .map(IconCandidate::uri)
                .toList();
    }

    private Optional<PageDocument> fetchHomepage(String domain) {
        for (URI root : List.of(
                URI.create("https://" + domain + "/"),
                URI.create("http://" + domain + "/"))) {
            try {
                HttpResult response = fetch(root, MAX_HTML_BYTES,
                        "text/html,application/xhtml+xml;q=0.9,*/*;q=0.5");
                if (response.statusCode() < 200 || response.statusCode() >= 300 || response.data().length == 0) {
                    continue;
                }
                String contentType = response.contentType();
                if (contentType != null
                        && !contentType.toLowerCase(Locale.ROOT).contains("html")
                        && !looksLikeHtml(response.data())) {
                    continue;
                }
                String html = decodeText(response.data(), contentType);
                return Optional.of(new PageDocument(parsePageLinks(html), response.finalUri()));
            } catch (Exception ignored) {
                // Some sites reject one scheme but accept the other.
            }
        }
        return Optional.empty();
    }

    private ParsedPageLinks parsePageLinks(String html) {
        List<LinkDeclaration> links = new ArrayList<>();
        String[] baseHref = new String[1];

        try {
            new ParserDelegator().parse(
                    new StringReader(html == null ? "" : html),
                    new HTMLEditorKit.ParserCallback() {
                        @Override
                        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int pos) {
                            capture(tag, attributes);
                        }

                        @Override
                        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int pos) {
                            capture(tag, attributes);
                        }

                        private void capture(HTML.Tag tag, MutableAttributeSet attributes) {
                            if (tag == HTML.Tag.BASE && baseHref[0] == null) {
                                baseHref[0] = attribute(attributes, HTML.Attribute.HREF);
                                return;
                            }
                            if (tag != HTML.Tag.LINK) {
                                return;
                            }

                            String href = attribute(attributes, HTML.Attribute.HREF);
                            if (href == null || href.isBlank()) {
                                return;
                            }
                            links.add(new LinkDeclaration(
                                    attribute(attributes, HTML.Attribute.REL),
                                    href,
                                    attribute(attributes, HTML.Attribute.TYPE),
                                    attribute(attributes, "sizes")));
                        }
                    },
                    true);
        } catch (Exception ignored) {
            // Malformed HTML is common. Conventional favicon paths still remain as fallback candidates.
        }

        return new ParsedPageLinks(baseHref[0], links);
    }

    private String attribute(MutableAttributeSet attributes, Object key) {
        Object value = attributes.getAttribute(key);
        if (value == null && key instanceof String rawName) {
            HTML.Attribute known = HTML.getAttributeKey(rawName);
            if (known != null) {
                value = attributes.getAttribute(known);
            }
        }
        return value == null ? null : value.toString();
    }

    private List<IconCandidate> discoverHtmlIcons(ParsedPageLinks page, URI pageUri) {
        List<IconCandidate> candidates = new ArrayList<>();
        URI baseUri = effectiveBaseUri(pageUri, page.baseHref());

        for (LinkDeclaration link : page.links()) {
            String rel = Optional.ofNullable(link.rel()).orElse("").toLowerCase(Locale.ROOT).trim();
            if (!isIconRel(rel)) {
                continue;
            }

            URI uri = resolveWebUri(baseUri, link.href());
            if (uri == null) {
                continue;
            }

            int sizeScore = iconSizeScore(link.sizes());
            int score;
            if (rel.contains("apple-touch-icon")) {
                score = 1050 + sizeScore;
            } else if (rel.contains("mask-icon")) {
                score = 760 + sizeScore;
            } else {
                score = 900 + sizeScore;
            }

            String type = Optional.ofNullable(link.type()).orElse("").toLowerCase(Locale.ROOT);
            String path = Optional.ofNullable(uri.getPath()).orElse("").toLowerCase(Locale.ROOT);
            if ("image/svg+xml".equals(type) || path.endsWith(".svg")) {
                score += 220;
            }
            if (rel.contains("shortcut")) {
                score -= 25;
            }

            candidates.add(new IconCandidate(uri, score, "html"));
        }

        return candidates;
    }

    private List<IconCandidate> discoverManifestIcons(ParsedPageLinks page, URI pageUri) {
        List<IconCandidate> candidates = new ArrayList<>();
        URI baseUri = effectiveBaseUri(pageUri, page.baseHref());

        for (LinkDeclaration link : page.links()) {
            String rel = Optional.ofNullable(link.rel()).orElse("").toLowerCase(Locale.ROOT).trim();
            if (!containsRelToken(rel, "manifest")) {
                continue;
            }

            URI manifestUri = resolveWebUri(baseUri, link.href());
            if (manifestUri == null) {
                continue;
            }

            try {
                HttpResult response = fetch(manifestUri, MAX_MANIFEST_BYTES,
                        "application/manifest+json,application/json,text/plain;q=0.8,*/*;q=0.2");
                if (response.statusCode() < 200 || response.statusCode() >= 300 || response.data().length == 0) {
                    continue;
                }

                String json = decodeText(response.data(), response.contentType());
                Matcher objectMatcher = MANIFEST_ICON_OBJECT.matcher(json);
                while (objectMatcher.find()) {
                    String object = objectMatcher.group(1);
                    String src = jsonString(object, JSON_SRC);
                    if (src == null || src.isBlank()) {
                        continue;
                    }

                    URI iconUri = resolveWebUri(response.finalUri(), src);
                    if (iconUri == null) {
                        continue;
                    }

                    String sizes = jsonString(object, JSON_SIZES);
                    String purpose = Optional.ofNullable(jsonString(object, JSON_PURPOSE)).orElse("")
                            .toLowerCase(Locale.ROOT);
                    int score = 820 + iconSizeScore(sizes);
                    if (purpose.contains("any")) score += 80;
                    if (purpose.contains("maskable")) score += 35;
                    String path = Optional.ofNullable(iconUri.getPath()).orElse("").toLowerCase(Locale.ROOT);
                    if (path.endsWith(".svg")) score += 180;

                    candidates.add(new IconCandidate(iconUri, score, "manifest"));
                }
            } catch (Exception ignored) {
                // A broken or protected manifest should not prevent fallback favicon discovery.
            }
        }

        return candidates;
    }

    private Optional<DownloadedImage> downloadImage(URI uri) {
        try {
            HttpResult response = fetch(uri, MAX_LOGO_BYTES,
                    "image/avif,image/webp,image/svg+xml,image/png,image/*;q=0.9,*/*;q=0.2");
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            byte[] data = response.data();
            if (data.length == 0 || data.length > MAX_LOGO_BYTES) {
                return Optional.empty();
            }

            String mimeType = normalizeImageMimeType(response.contentType(), data);
            if (mimeType == null) {
                return Optional.empty();
            }
            if ("image/svg+xml".equals(mimeType) && !isSafeSvg(data)) {
                return Optional.empty();
            }

            return Optional.of(new DownloadedImage(response.finalUri(), mimeType, data));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private HttpResult fetch(URI start, int maxBytes, String accept) throws Exception {
        URI current = validateRemoteUri(start);

        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", accept)
                    .header("Accept-Language", "en-US,en;q=0.8")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();

            if (isRedirect(status)) {
                try (InputStream ignored = response.body()) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location == null || redirects == MAX_REDIRECTS) {
                        return new HttpResult(
                                current,
                                status,
                                response.headers().firstValue("Content-Type").orElse(null),
                                new byte[0]);
                    }
                    current = validateRemoteUri(current.resolve(location));
                    continue;
                }
            }

            byte[] data;
            try (InputStream body = response.body()) {
                data = body.readNBytes(maxBytes + 1);
            }
            if (data.length > maxBytes) {
                return new HttpResult(
                        current,
                        status,
                        response.headers().firstValue("Content-Type").orElse(null),
                        new byte[0]);
            }

            return new HttpResult(
                    current,
                    status,
                    response.headers().firstValue("Content-Type").orElse(null),
                    data);
        }

        throw new IllegalArgumentException("Too many redirects while fetching company branding.");
    }

    private URI validateRemoteUri(URI uri) {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("Logo URL is not a valid web address.");
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new IllegalArgumentException("Logo fetching only supports HTTP and HTTPS URLs.");
        }
        ensurePublicHost(uri.getHost());
        return uri;
    }

    private List<IconCandidate> deduplicateAndRank(List<IconCandidate> input) {
        Map<String, IconCandidate> unique = new LinkedHashMap<>();
        for (IconCandidate candidate : input) {
            try {
                URI validated = validateRemoteUri(candidate.uri());
                String key = validated.normalize().toString();
                IconCandidate existing = unique.get(key);
                if (existing == null || candidate.score() > existing.score()) {
                    unique.put(key, new IconCandidate(validated, candidate.score(), candidate.origin()));
                }
            } catch (IllegalArgumentException ignored) {
                // Skip malformed, local, data:, javascript:, and other non-web icon URLs.
            }
        }
        return unique.values().stream()
                .sorted(Comparator.comparingInt(IconCandidate::score).reversed())
                .toList();
    }

    private URI effectiveBaseUri(URI pageUri, String rawBaseHref) {
        URI declared = resolveWebUri(pageUri, rawBaseHref);
        return declared == null ? pageUri : declared;
    }

    private URI resolveWebUri(URI base, String rawHref) {
        if (base == null || rawHref == null || rawHref.isBlank()) {
            return null;
        }
        String value = rawHref.trim();
        if (value.regionMatches(true, 0, "data:", 0, 5)
                || value.regionMatches(true, 0, "javascript:", 0, 11)
                || value.regionMatches(true, 0, "blob:", 0, 5)) {
            return null;
        }
        try {
            URI uri = base.resolve(value);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    ? uri
                    : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isIconRel(String rel) {
        if (rel == null || rel.isBlank()) return false;
        return containsRelToken(rel, "icon")
                || containsRelToken(rel, "shortcut") && rel.contains("icon")
                || rel.contains("apple-touch-icon")
                || rel.contains("mask-icon");
    }

    private boolean containsRelToken(String rel, String token) {
        for (String value : rel.split("\\s+")) {
            if (value.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    private int iconSizeScore(String sizes) {
        if (sizes == null || sizes.isBlank()) {
            return 0;
        }
        if (sizes.toLowerCase(Locale.ROOT).contains("any")) {
            return 450;
        }

        int largest = 0;
        for (String token : sizes.toLowerCase(Locale.ROOT).split("\\s+")) {
            Matcher matcher = SIZE_PATTERN.matcher(token);
            if (matcher.matches()) {
                int width = Integer.parseInt(matcher.group(1));
                int height = Integer.parseInt(matcher.group(2));
                largest = Math.max(largest, Math.min(width, height));
            }
        }
        return Math.min(largest, 512);
    }

    private String jsonString(String object, Pattern fieldPattern) {
        Matcher matcher = fieldPattern.matcher(object);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJsonString(matcher.group(1));
    }

    private String unescapeJsonString(String value) {
        String result = value
                .replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

        Matcher unicode = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(result);
        StringBuffer buffer = new StringBuffer();
        while (unicode.find()) {
            char ch = (char) Integer.parseInt(unicode.group(1), 16);
            unicode.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(ch)));
        }
        unicode.appendTail(buffer);
        return buffer.toString();
    }

    private String requireDomain(String rawDomain) {
        String domain = normalizeDomain(rawDomain);
        if (domain == null) {
            throw new IllegalArgumentException("Add a valid company domain first, for example mastercard.com.");
        }
        return domain;
    }

    private void ensurePublicHost(String domain) {
        try {
            for (InetAddress address : InetAddress.getAllByName(domain)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || isUniqueLocalIpv6(address)) {
                    throw new IllegalArgumentException("Company logo fetching only supports public internet domains.");
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not resolve the company domain: " + domain);
        }
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte first = address.getAddress()[0];
        return (first & 0xFE) == 0xFC;
    }

    private String normalizeImageMimeType(String supplied, byte[] data) {
        String mime = supplied == null ? "" : supplied.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (List.of(
                "image/png",
                "image/jpeg",
                "image/webp",
                "image/gif",
                "image/x-icon",
                "image/vnd.microsoft.icon",
                "image/svg+xml").contains(mime)) {
            return mime;
        }

        if (startsWith(data, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47})) return "image/png";
        if (startsWith(data, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) return "image/jpeg";
        if (data.length >= 12
                && new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        if (startsWith(data, "GIF8".getBytes(StandardCharsets.US_ASCII))) return "image/gif";
        if (startsWith(data, new byte[]{0x00, 0x00, 0x01, 0x00})) return "image/x-icon";
        if (looksLikeSvg(data)) return "image/svg+xml";
        return null;
    }

    private boolean looksLikeHtml(byte[] data) {
        String prefix = new String(data, 0, Math.min(data.length, 512), StandardCharsets.UTF_8)
                .trim()
                .toLowerCase(Locale.ROOT);
        return prefix.startsWith("<!doctype html") || prefix.startsWith("<html") || prefix.contains("<head");
    }

    private boolean looksLikeSvg(byte[] data) {
        if (data == null || data.length == 0) return false;
        String prefix = new String(data, 0, Math.min(data.length, 2048), StandardCharsets.UTF_8)
                .replace("\uFEFF", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return prefix.startsWith("<svg")
                || (prefix.startsWith("<?xml") && prefix.contains("<svg"));
    }

    private boolean isSafeSvg(byte[] data) {
        if (!looksLikeSvg(data)) return false;
        String svg = new String(data, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return !svg.contains("<script")
                && !svg.contains("<foreignobject")
                && !svg.contains("<iframe")
                && !svg.contains("<object")
                && !svg.contains("<embed")
                && !svg.contains("javascript:")
                && !EVENT_HANDLER_PATTERN.matcher(svg).find();
    }

    private String decodeText(byte[] data, String contentType) {
        Charset charset = StandardCharsets.UTF_8;
        if (contentType != null) {
            Matcher matcher = CHARSET_PATTERN.matcher(contentType);
            if (matcher.find()) {
                try {
                    charset = Charset.forName(matcher.group(1));
                } catch (Exception ignored) {
                    // UTF-8 remains the sensible web default.
                }
            }
        }
        return new String(data, charset);
    }

    private boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data == null || data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private record LinkDeclaration(String rel, String href, String type, String sizes) {
    }

    private record ParsedPageLinks(String baseHref, List<LinkDeclaration> links) {
    }

    private record IconCandidate(URI uri, int score, String origin) {
    }

    private record PageDocument(ParsedPageLinks links, URI finalUri) {
    }

    private record DownloadedImage(URI finalUri, String mimeType, byte[] data) {
    }

    private record HttpResult(URI finalUri, int statusCode, String contentType, byte[] data) {
    }
}
