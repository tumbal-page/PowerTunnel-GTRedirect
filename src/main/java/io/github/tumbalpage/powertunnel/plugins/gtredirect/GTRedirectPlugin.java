/*
 * GTRedirect plugin for PowerTunnel.
 *
 * Redirect growtopia1.com / growtopia2.com server_data.php requests to a
 * private server, without touching the real growtopia1.com/2.com upstream:
 * we just short-circuit the request (like the official AdBlock plugin does
 * for blocked hosts) and hand back our own crafted response.
 *
 * Network model (kept intentionally simple):
 * - target_domain (e.g. lyu.my.id) is resolved to an IP fresh on every
 *   request -- this is the part that changes (playit.gg relay IP can
 *   change on restart).
 * - static_port is a constant from preferences -- the playit.gg game
 *   tunnel's public port, which does NOT change.
 * - override_domains is the comma-separated list of hosts whose
 *   server_data.php request we intercept.
 *
 * IMPORTANT: this plugin only handles the plain-HTTP server_data.php
 * request/response. ENet (UDP) gameplay traffic is untouched by this
 * plugin -- once the client has our rewritten IP:port, it connects
 * directly, and playit.gg/your server handle the rest.
 *
 * If it turns out growtopia1.com/2.com has moved to HTTPS, this plugin
 * needs a MITM/SNI-based variant instead of a plain onClientToProxyRequest
 * short-circuit -- check with a traffic capture (mitmproxy/tcpdump) first.
 */
package io.github.tumbalpage.powertunnel.plugins.gtredirect;

import io.github.krlvm.powertunnel.sdk.configuration.Configuration;
import io.github.krlvm.powertunnel.sdk.plugin.PowerTunnelPlugin;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GTRedirectPlugin extends PowerTunnelPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GTRedirectPlugin.class);

    @Override
    public void onProxyInitialization(@NotNull ProxyServer proxy) {
        final Configuration config = readConfiguration();

        final String targetDomain = config.get("target_domain", "lyu.my.id");
        final int staticPort = parsePort(config.get("static_port", "43210"));
        final String[] overrideDomains = config.get("override_domains", "growtopia1.com,growtopia2.com")
                .split(",");
        for (int i = 0; i < overrideDomains.length; i++) {
            overrideDomains[i] = overrideDomains[i].trim().toLowerCase();
        }

        LOGGER.info(
                "GTRedirect active: target={}:{} for hosts={}",
                targetDomain, staticPort, String.join(",", overrideDomains)
        );

        registerProxyListener(new RedirectListener(this, targetDomain, staticPort, overrideDomains), -10);
    }

    private static int parsePort(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid static_port value: " + raw, ex);
        }
    }
}
