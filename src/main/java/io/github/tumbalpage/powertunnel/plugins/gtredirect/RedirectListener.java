/*
 * Intercepts requests to the configured override_domains (growtopia1.com /
 * growtopia2.com) and, if the path looks like the server_data.php lookup,
 * answers directly with our own rewritten server|ip / port|port response --
 * exactly like AdBlock's RequestListener short-circuits a blocked request
 * with request.setResponse(...), except we build a real 200 OK body instead
 * of a 403.
 */
package io.github.tumbalpage.powertunnel.plugins.gtredirect;

import io.github.krlvm.powertunnel.sdk.http.ProxyRequest;
import io.github.krlvm.powertunnel.sdk.http.ProxyResponse;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class RedirectListener extends ProxyAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectListener.class);

    private final GTRedirectPlugin plugin;
    private final String targetDomain;
    private final int staticPort;
    private final String[] overrideDomains;

    public RedirectListener(GTRedirectPlugin plugin, String targetDomain, int staticPort, String[] overrideDomains) {
        this.plugin = plugin;
        this.targetDomain = targetDomain;
        this.staticPort = staticPort;
        this.overrideDomains = overrideDomains;
    }

    @Override
    public void onClientToProxyRequest(@NotNull ProxyRequest request) {
        if (request.isBlocked()) return;

        final String host = request.getHost();
        if (host == null || !matchesOverrideDomain(host)) return;

        // Only touch the server_data.php lookup -- everything else to these
        // hosts (if anything) passes through untouched.
        final String path = request.getUri();
        if (path == null || !path.contains("server_data.php")) return;

        final String ip;
        try {
            ip = InetAddress.getByName(targetDomain).getHostAddress();
        } catch (UnknownHostException ex) {
            LOGGER.error("Failed to resolve target_domain '{}': {}", targetDomain, ex.getMessage());
            return;
        }

        final String body = "server|" + ip + "\n" +
                "port|" + staticPort + "\n" +
                "type|1\n";

        LOGGER.info("GTRedirect: {} -> {}:{}", host, ip, staticPort);

        final ProxyResponse response = plugin.getServer().getProxyServer()
                .getResponseBuilder(body, 200)
                .build();
        request.setResponse(response);
    }

    private boolean matchesOverrideDomain(String host) {
        final String h = host.toLowerCase();
        for (String d : overrideDomains) {
            if (h.equals(d) || h.endsWith("." + d)) return true;
        }
        return false;
    }
}
