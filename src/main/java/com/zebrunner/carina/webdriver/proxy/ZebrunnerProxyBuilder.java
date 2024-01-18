package com.zebrunner.carina.webdriver.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.webdriver.proxy.mode.Mode;

/**
 * Builder for forming a command for a Zebrunner proxy.
 * All commands are intermediate, except for the build method,
 * which builds the command and puts it in the {@code proxy_zebrunner_args} parameter.
 *
 * For information about the default values, refer to the <a href="https://docs.mitmproxy.org/stable/concepts-options/">documentation</a>.
 * The builder does not add parameters that were not specified by the user.
 * 
 */
public final class ZebrunnerProxyBuilder {
    public static final String PROXY_ARGUMENTS_PARAMETER = "proxy_zebrunner_args";
    public static final String PROXY_TYPE_PARAMETER = "proxy_zebrunner_type";

    //// Supported commands ////
    private static final String MODE_ARG = "--mode";
    private static final String NO_ANTICACHE_ARG = "--no-anticache";
    private static final String ANTICACHE_ARG = " --anticache";
    private static final String NO_SHOW_HOST_ARG = "--no-showhost";
    private static final String SHOW_HOST_ARG = "--showhost";
    private static final String STICKY_COOKIE_ARG = "--stickycookie";
    private static final String STICKY_AUTH_ARG = "--stickyauth";
    private static final String NO_ANTICOMP_ARG = "--no-anticomp";
    private static final String ANTICOMP_ARG = "--anticomp";
    private static final String FLOW_DETAIL_ARG = "--flow-detail";
    private static final String LISTEN_HOST_ARG = "--listen-host";
    private static final String LISTEN_PORT_ARG = " --listen-port";
    private static final String NO_SERVER_ARG = "--no-server";
    private static final String SERVER_ARG = " --server";
    private static final String IGNORE_HOSTS_ARG = "--ignore-hosts";
    private static final String ALLOW_HOSTS_ARG = "--allow-hosts";
    private static final String TCP_HOSTS_ARG = "--tcp-hosts";
    private static final String UPSTREAM_AUTH_ARG = "--upstream-auth";
    private static final String PROXYAUTH_ARG = "--proxyauth";
    private static final String NO_RAW_TCP_ARG = "--no-rawtcp";
    private static final String RAW_TCP_ARG = "--rawtcp";
    private static final String NO_HTTP_2_ARG = "--no-http2";
    private static final String HTTP_2_ARG = "--http2";
    private static final String NO_SSL_INSECURE_ARG = "--no-ssl-insecure";
    private static final String SSL_INSECURE_ARG = "--ssl-insecure";
    private static final String MAP_REMOTE_ARG = "--map-remote";
    private static final String MODIFY_HEADERS_ARG = "--modify-headers";
    private static final String MODIFY_BODY_ARG = "--modify-body";
    private static final String INTERCEPT_ARG = "--intercept";
    private static final String VIEW_FILTER_ARG = "--view-filter";
    /*
     * Unsupported commands:
     * --rfile
     * --scripts
     * --save-stream-file
     * --client-replay
     * --server-replay
     * --map-local
     * --no-server-replay-kill-extra
     * --server-replay-kill-extra
     * --no-server-replay-nopop
     * --server-replay-nopop
     * --no-server-replay-refresh
     * --server-replay-refresh
     * --certs
     * --cert-passphrase
     */

    private final List<Mode> modes = new ArrayList<>(1);
    private Boolean antiCache = null;
    private Boolean showHost = null;
    private FilterCondition stickyCookieFilter = null;
    private FilterCondition stickyAuthFilter = null;
    private Boolean antiCompress = null;
    private FlowDetail flowDetail = null;
    private String listenHost = null;
    private Integer listenPort = null;
    private Boolean server = null;
    private final List<String> ignoredHostRegexes = new ArrayList<>(0);
    private final List<String> allowedHostRegexes = new ArrayList<>(0);
    private final List<String> tcpHostRegexes = new ArrayList<>(0);
    private String upstreamAuthUsername = null;
    private String upstreamAuthPassword = null;
    private String proxyAuth = null;
    private Boolean rawTCP = null;
    private Boolean http2 = null;
    private Boolean sslInsecure = null;
    private final List<Modification> mapRemoteModifications = new ArrayList<>(0);
    private final List<Modification> headerModifications = new ArrayList<>();
    private final List<Modification> bodyModifications = new ArrayList<>();
    private FilterCondition intercept = null;
    private FilterCondition viewFilter = null;

    private String proxyType= null;

    static final class Modification {
        private static final List<String> DELIMITERS = List.of("/", ":", "|", "%", ";", "$");
        // optional parameter
        private String flowFilter = null;
        private final String original;
        private final String replacement;
        private final String delimiter;

        private Modification(String flowFilter, String original, String replacement) {
            // optional parameter
            this.flowFilter = flowFilter;
            this.original = normalize(original);
            this.replacement = normalize(replacement);
            this.delimiter = getSuitableDelimiter(flowFilter, original, replacement);
        }

        public Modification(String original, String replacement) {
            this.original = normalize(original);
            this.replacement = normalize(replacement);
            this.delimiter = getSuitableDelimiter(null, original, replacement);
        }

        public String getFlowFilter() {
            return flowFilter;
        }

        public String getOriginal() {
            return original;
        }

        public String getReplacement() {
            return replacement;
        }

        public String getDelimiter() {
            return delimiter;
        }

        private static String getSuitableDelimiter(@Nullable String flowFilter, String original, String replacement) {
            Stream<String> stream = DELIMITERS.stream();

            if (flowFilter != null) {
                stream = stream.filter(d -> !flowFilter.contains(d));
            }
            return stream.filter(d -> !original.contains(d))
                    .filter(d -> !replacement.contains(d))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(String.format(
                            "Could not find suitable delimiter for the modification.\n FlowFilter:'%s' \n"
                                    + "Regex: '%s' \n"
                                    + "Replacement: '%s'",
                            flowFilter != null ? flowFilter : "",
                            original,
                            replacement)));
        }
    }

    private static String normalize(String regex) {
        // escaping a character so it doesn't interfere with the command
        return regex.replaceAll("([\\\\]{1,10})?\"", "\\\\\"");
    }

    /**
     * Get new instance of {@link ZebrunnerProxyBuilder}
     * 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public static ZebrunnerProxyBuilder getInstance() {
        return new ZebrunnerProxyBuilder();
    }

    ////////////// MITMPROXY OPTIONS //////////////

    /**
     * Allows to specify one or more proxy modes.
     * 
     * @param mode see {@link com.zebrunner.carina.webdriver.proxy.mode.Mode} and it's implementations:
     *            {@link com.zebrunner.carina.webdriver.proxy.mode.DnsMode},
     *            {@link com.zebrunner.carina.webdriver.proxy.mode.ReverseMode} and so on.
     * @return {@link ZebrunnerProxyBuilder}.
     */
    public ZebrunnerProxyBuilder addMode(Mode mode) {
        Objects.requireNonNull(mode);
        modes.add(mode);
        return this;
    }

    /**
     * Enable anticache option. When the anticache option is set, it removes headers (if-none-match and if-modified-since) that might elicit a 304 Not
     * Modified response from the server. This is useful when you want to make sure you capture an HTTP exchange in its totality.
     * 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder enableAntiCache() {
        this.antiCache = true;
        return this;
    }

    /**
     * Disable anticache option. Opposite of {@link #enableAntiCache()}
     * 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder disableAntiCache() {
        this.antiCache = false;
        return this;
    }

    /**
     * Enable use the Host header to construct URLs for display.
     * 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder enableShowHost() {
        this.showHost = true;
        return this;
    }

    /**
     * Opposite of {@link #enableShowHost()}
     *
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder disableShowHost() {
        this.showHost = false;
        return this;
    }

    /**
     * When the stickycookie option is set, mitmproxy will add the cookie most recently set by the server to any cookie-less request.
     * Consider a service that sets a cookie to track the session after authentication. Using sticky cookies, you can fire up mitmproxy,
     * and authenticate to a service as you usually would using a browser. After authentication, you can request authenticated resources
     * through mitmproxy as if they were unauthenticated, because mitmproxy will automatically add the session tracking cookie to requests.
     * Among other things, this lets you script interactions with authenticated resources (using tools like wget or curl) without having
     * to worry about authentication.
     * 
     * @param filter see {@link FilterConditions}
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder stickyCookie(FilterCondition filter) {
        this.stickyCookieFilter = filter;
        return this;
    }

    /**
     * The stickyauth option is analogous to the sticky cookie option, in that HTTP Authorization headers are simply replayed to the
     * server once they have been seen. This is enough to allow you to access a server resource using HTTP Basic authentication through
     * the proxy. Note that mitmproxy doesn’t (yet) support replay of HTTP Digest authentication.
     * 
     * @param filter see {@link FilterConditions}
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder stickyAuth(FilterCondition filter) {
        this.stickyAuthFilter = filter;
        return this;
    }

    /**
     * Enable try to convince servers to send us un-compressed data.
     * 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder enableAntiCompress() {
        this.antiCompress = true;
        return this;
    }

    /**
     * Opposite of {@link #enableAntiCompress()}
     *
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder disableAntiCompress() {
        this.antiCompress = false;
        return this;
    }

    public enum FlowDetail {
        /**
         * No output
         */
        NO_OUTPUT(0),

        /**
         * Shortened request URL with response status code
         */
        SHORT_URL_RESPONSE_CODE(1),

        /**
         * full request URL with response status code and HTTP headers
         */
        FULL_URL_RESPONSE_CODE_HEADERS(2),

        /**
         * {@link #FULL_URL_RESPONSE_CODE_HEADERS} with truncated response content, content of WebSocket and TCP messages
         */
        FULL_URL_RESPONSE_CODE_HEADERS_TRUNCATE_CONTENT(3),

        /**
         * {@link #FULL_URL_RESPONSE_CODE_HEADERS_TRUNCATE_CONTENT} + nothing is truncated
         */
        FULL_URL_RESPONSE_CODE_HEADERS_FULL_CONTENT(4);

        private final int number;

        FlowDetail(int number) {
            this.number = number;
        }

        public int getNumber() {
            return this.number;
        }
    }

    /**
     * The display detail level
     * 
     * @param flowDetail see {@link FlowDetail}
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder flowDetail(FlowDetail flowDetail) {
        this.flowDetail = flowDetail;
        return this;
    }

    /////////////// PROXY OPTIONS ///////////////

    /**
     * Address to bind proxy server(s) to (may be overridden for individual modes).
     * 
     * @param host for example {@code 127.0.0.1}
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder listenHost(String host) {
        this.listenHost = host;
        return this;
    }

    /**
     * Port to bind proxy server(s) to (may be overridden for individual modes).
     * By default, the port is mode-specific. The default regular HTTP proxy spawns on port 8080.
     * 
     * @param port for example {@code 8080}
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder listenPort(Integer port) {
        this.listenPort = port;
        return this;
    }

    /**
     * Enable start a proxy server. Enabled by default.
     * 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder enableServer() {
        this.server = true;
        return this;
    }

    /**
     * Opposite of {@link #enableServer()}
     * 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder disableServer() {
        this.server = false;
        return this;
    }

    /**
     * Add ignored host(s) and forward all traffic without processing it. In transparent mode, it is recommended to use an IP address (range), not the
     * hostname. In regular mode, only SSL traffic is ignored and the hostname should be used. The supplied value is interpreted as a regular
     * expression and matched on the ip or the hostname. For more info, check
     * <a href="https://docs.mitmproxy.org/stable/howto-ignoredomains/">documentation</a>.
     * 
     * @param regex a regex which is matched against a host:port string (e.g. “example.com:443”) of a connection
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder addIgnoreHost(String regex) {
        this.ignoredHostRegexes.add(regex);
        return this;
    }

    /**
     * Opposite of {@link #addIgnoreHost(String)}.
     *
     * @param regex a regex which is matched against a host:port string (e.g. “example.com:443”) of a connection
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder addAllowHost(String regex) {
        this.allowedHostRegexes.add(regex);
        return this;
    }

    /**
     * Generic TCP SSL proxy mode for all hosts that match the pattern. Similar to {@link #addIgnoreHost(String)},
     * but SSL connections are intercepted. The communication contents are printed to the log
     * in verbose mode.
     * 
     * @param regex a regex which is matched against a host:port string (e.g. “example.com:443”) of a connection
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder addTCPHost(String regex) {
        this.tcpHostRegexes.add(regex);
        return this;
    }

    /**
     * Add HTTP Basic authentication to upstream proxy and reverse proxy requests.
     * 
     * @param username username
     * @param password password
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder upstreamAuth(@Nonnull String username, @Nonnull String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        this.upstreamAuthUsername = username;
        this.upstreamAuthPassword = password;
        return this;
    }

    /**
     * Require proxy authentication by username/password.
     * 
     * @param username username
     * @param password password
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder proxyAuth(@Nonnull String username, @Nonnull String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        this.proxyAuth = username + ":" + password;
        return this;
    }

    // todo add proxyAuth for ldap (ldap[s]:url_server_ldap[:port]:dn_auth:password:dn_subtree" for LDAP authentication)

    /**
     * Enable raw TCP connections. TCP connections are enabled by default.
     ** 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder enableRawTCP() {
        this.rawTCP = true;
        return this;
    }

    /**
     * Opposite of {@link #enableRawTCP()}
     *
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder disableRawTCP() {
        this.rawTCP = false;
        return this;
    }

    /**
     * Enable HTTP/2 support. HTTP/2 support is enabled by default.
     * 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder enableHttp2() {
        this.http2 = true;
        return this;
    }

    /**
     * Opposite of {@link #enableHttp2()}
     *
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder disableHttp2() {
        this.http2 = false;
        return this;
    }

    //////////////////// SSL OPTIONS ////////////////////

    /**
     * Enable do not verify upstream server SSL/TLS certificates.
     * 
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder enableSSlInsecure() {
        this.sslInsecure = true;
        return this;
    }

    /**
     * Opposite of {@link #enableSSlInsecure()}
     *
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder disableSSlInsecure() {
        this.sslInsecure = false;
        return this;
    }

    ////////////////// MAP REMOTE //////////////////

    /**
     * Map remote resources to another remote URL
     *
     * @param urlRegex regex of url
     * @param replacement replacement
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder addMapRemote(String urlRegex, String replacement) {
        this.mapRemoteModifications.add(new Modification(urlRegex, replacement));
        return this;
    }

    /**
     * Map remote resources to another remote URL
     *
     * @param flowFilter see {@link FilterConditions}
     * @param urlRegex regex
     * @param replacement replacement
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder addMapRemote(FilterCondition flowFilter, String urlRegex, String replacement) {
        Objects.requireNonNull(flowFilter);
        this.mapRemoteModifications.add(new Modification(flowFilter.toString(), urlRegex, replacement));
        return this;
    }

    ////////////////// MODIFY BODY //////////////////

    /**
     * Add body modify
     *
     * @param regex regex
     * @param replacement replacement
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder addBodyModify(String regex, String replacement) {
        this.bodyModifications.add(new Modification(regex, replacement));
        return this;
    }

    /**
     * Add body modify
     *
     * @param flowFilter see {@link FilterConditions}
     * @param regex regex
     * @param replacement replacenent
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder addBodyModify(FilterCondition flowFilter, String regex, String replacement) {
        Objects.requireNonNull(flowFilter);
        this.bodyModifications.add(new Modification(flowFilter.toString(), regex, replacement));
        return this;
    }

    ////////////////// MODIFY HEADERS //////////////////

    /**
     * Add header modify
     *
     * @param regex regex
     * @param headerValue value
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder addHeaderModify(String regex, String headerValue) {
        this.headerModifications.add(new Modification(regex, headerValue));
        return this;
    }

    /**
     * Add header modify
     *
     * @param flowFilter see {@link FilterConditions}
     * @param regex regex
     * @param headerValue value
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder addHeaderModify(FilterCondition flowFilter, String regex, String headerValue) {
        Objects.requireNonNull(flowFilter);
        this.headerModifications.add(new Modification(flowFilter.toString(), regex, headerValue));
        return this;
    }

    public ZebrunnerProxyBuilder removeHeaderModify(String regex) {
        headerModifications.removeIf((modification -> modification.getOriginal().equals(regex)));
        return this;
    }

    public ZebrunnerProxyBuilder removeHeaderModify(FilterCondition flowFilter, String regex) {
        headerModifications.removeIf((modification -> StringUtils.equals(modification.getOriginal(), regex)
                && StringUtils.equals(modification.getFlowFilter(), flowFilter.toString())));
        return this;
    }

    ////////////////// FILTERS //////////////////

    /**
     * Intercept filter expression
     *
     * @param flowFilter see {@link FilterConditions}
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder intercept(FilterCondition flowFilter) {
        this.intercept = flowFilter;
        return this;
    }

    /**
     * Limit the view to matching flows
     * 
     * @param flowFilter see {@link FilterConditions}
     * @return {@link ZebrunnerProxyBuilder}
     */
    public ZebrunnerProxyBuilder viewFilter(FilterCondition flowFilter) {
        this.viewFilter = flowFilter;
        return this;
    }

    public ZebrunnerProxyBuilder useBaseProxy() {
        this.proxyType = "simple";
        return this;
    }

    public ZebrunnerProxyBuilder useExtendedProxy() {
        this.proxyType = "full";
        return this;
    }

    /**
     * Build command and put in into the {@code proxy_zebrunner_args} parameter.
     * 
     * @param currentTestOnly true if parameter should be added only for current test method, false if it should be added globally
     */
    public void build(boolean currentTestOnly) {
        StringBuilder sb = new StringBuilder();
        modes.forEach(mode -> sb.append(StringUtils.SPACE)
                .append(MODE_ARG)
                .append(StringUtils.SPACE)
                .append(mode));

        if (antiCache != null) {
            if (antiCache) {
                sb.append(StringUtils.SPACE).append(ANTICACHE_ARG);
            } else {
                sb.append(StringUtils.SPACE).append(NO_ANTICACHE_ARG);
            }
        }
        if (showHost != null) {
            if (showHost) {
                sb.append(StringUtils.SPACE).append(SHOW_HOST_ARG);
            } else {
                sb.append(StringUtils.SPACE).append(NO_SHOW_HOST_ARG);
            }
        }
        if (stickyCookieFilter != null) {
            sb.append(StringUtils.SPACE)
                    .append(STICKY_COOKIE_ARG)
                    .append(StringUtils.SPACE)
                    .append("\"")
                    .append(stickyCookieFilter)
                    .append("\"");
        }

        if (stickyAuthFilter != null) {
            sb.append(StringUtils.SPACE)
                    .append(STICKY_AUTH_ARG)
                    .append(StringUtils.SPACE)
                    .append("\"")
                    .append(stickyAuthFilter)
                    .append("\"");
        }

        if (antiCompress != null) {
            if (antiCompress) {
                sb.append(StringUtils.SPACE).append(ANTICOMP_ARG);
            } else {
                sb.append(StringUtils.SPACE).append(NO_ANTICOMP_ARG);
            }
        }

        if (flowDetail != null) {
            sb.append(StringUtils.SPACE)
                    .append(FLOW_DETAIL_ARG)
                    .append(StringUtils.SPACE)
                    .append(flowDetail.getNumber());
        }

        if (listenHost != null) {
            sb.append(StringUtils.SPACE)
                    .append(LISTEN_HOST_ARG)
                    .append(StringUtils.SPACE)
                    .append(listenHost);
        }

        if (listenPort != null) {
            sb.append(StringUtils.SPACE)
                    .append(LISTEN_PORT_ARG)
                    .append(StringUtils.SPACE)
                    .append(listenPort);
        }

        if (server != null) {
            if (server) {
                sb.append(StringUtils.SPACE).append(SERVER_ARG);
            } else {
                sb.append(StringUtils.SPACE).append(NO_SERVER_ARG);
            }
        }

        ignoredHostRegexes.forEach(hostRegex -> sb.append(StringUtils.SPACE)
                .append(IGNORE_HOSTS_ARG)
                .append(StringUtils.SPACE)
                .append("\"")
                .append(normalize(hostRegex))
                .append("\""));
        allowedHostRegexes.forEach(hostRegex -> sb.append(StringUtils.SPACE)
                .append(ALLOW_HOSTS_ARG)
                .append(StringUtils.SPACE)
                .append("\"")
                .append(normalize(hostRegex))
                .append("\""));
        tcpHostRegexes.forEach(hostRegex -> sb.append(StringUtils.SPACE)
                .append(TCP_HOSTS_ARG)
                .append(StringUtils.SPACE)
                .append("\"")
                .append(normalize(hostRegex))
                .append("\""));

        if (upstreamAuthUsername != null && upstreamAuthPassword != null) {
            sb.append(StringUtils.SPACE)
                    .append(UPSTREAM_AUTH_ARG)
                    .append(StringUtils.SPACE)
                    .append(upstreamAuthUsername)
                    .append(":")
                    .append(upstreamAuthPassword);
        }

        if (proxyAuth != null) {
            sb.append(StringUtils.SPACE)
                    .append(PROXYAUTH_ARG)
                    .append(StringUtils.SPACE)
                    .append(proxyAuth);
        }

        if (rawTCP != null) {
            if (rawTCP) {
                sb.append(StringUtils.SPACE).append(RAW_TCP_ARG);
            } else {
                sb.append(StringUtils.SPACE).append(NO_RAW_TCP_ARG);
            }
        }

        if (http2 != null) {
            if (http2) {
                sb.append(StringUtils.SPACE).append(HTTP_2_ARG);
            } else {
                sb.append(StringUtils.SPACE).append(NO_HTTP_2_ARG);
            }
        }

        if (sslInsecure != null) {
            if (sslInsecure) {
                sb.append(StringUtils.SPACE).append(SSL_INSECURE_ARG);
            } else {
                sb.append(StringUtils.SPACE).append(NO_SSL_INSECURE_ARG);
            }
        }

        mapRemoteModifications.forEach(modification -> sb.append(StringUtils.SPACE)
                .append(MAP_REMOTE_ARG)
                .append(StringUtils.SPACE)
                .append(generateFromModification(modification)));
        headerModifications.forEach(modification -> sb.append(StringUtils.SPACE)
                .append(MODIFY_HEADERS_ARG)
                .append(StringUtils.SPACE)
                .append(generateFromModification(modification)));
        bodyModifications.forEach(modification -> sb.append(StringUtils.SPACE)
                .append(MODIFY_BODY_ARG)
                .append(StringUtils.SPACE)
                .append(generateFromModification(modification)));

        if (intercept != null) {
            sb.append(StringUtils.SPACE)
                    .append(INTERCEPT_ARG)
                    .append(StringUtils.SPACE)
                    .append("\"")
                    .append(intercept)
                    .append("\"");
        }

        if (viewFilter != null) {
            sb.append(StringUtils.SPACE)
                    .append(VIEW_FILTER_ARG)
                    .append(StringUtils.SPACE)
                    .append("\"")
                    .append(viewFilter)
                    .append("\"");
        }

        String command = sb.toString();
        if (!command.isBlank()) {
            R.CONFIG.put(PROXY_ARGUMENTS_PARAMETER, command, currentTestOnly);
        }

        if(!StringUtils.isBlank(proxyType)) {
            R.CONFIG.put(PROXY_TYPE_PARAMETER, proxyType, currentTestOnly);
        }
    }

    private static String generateFromModification(Modification modification) {
        return "\"" + (modification.getFlowFilter() != null ? modification.getDelimiter() + modification.getFlowFilter() : "") +
                modification.getDelimiter() +
                modification.getOriginal() +
                modification.getDelimiter() +
                modification.getReplacement() + "\"";
    }

}
