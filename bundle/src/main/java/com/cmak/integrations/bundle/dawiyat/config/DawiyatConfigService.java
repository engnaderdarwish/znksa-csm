package com.cmak.integrations.bundle.dawiyat.config;

import java.util.Map;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.bmc.arsys.rx.services.common.Service;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.config.IntegrationConfigService;

/**
 * Maps the "DAWIYAT" row of the centralized configuration record onto
 * DawiyatConfig.
 *
 *   Integration Status -> enabled
 *
 * That is the only mapping. See DawiyatConfig for why there is nothing else.
 *
 * A MISSING row is treated as ENABLED, not disabled. An inbound endpoint that
 * silently starts rejecting a partner because nobody created a config row
 * would be a worse failure than one that keeps working.
 */
public class DawiyatConfigService implements Service {

    private final IntegrationConfigService configService;

    public DawiyatConfigService(IntegrationConfigService configService) {
        this.configService = configService;
    }

    public DawiyatConfig getConfig() {
        Map<String, Object> row = configService.getRow(Constants.INTEGRATION_DAWIYAT);

        DawiyatConfig config = new DawiyatConfig();

        if (row == null) {
            ServiceLocator.getLogger().warn("[DAWIYAT-CONFIG] No configuration row named ["
                    + Constants.INTEGRATION_DAWIYAT + "] - defaulting to ENABLED. "
                    + "Create the row if you need an off switch.");
            config.setEnabled(true);
            return config;
        }

        config.setEnabled(IntegrationConfigService.isEnabled(row));

        ServiceLocator.getLogger().info("[DAWIYAT-CONFIG] Mapped configuration: " + config);
        return config;
    }

    public void invalidate() {
        configService.invalidate();
    }
}