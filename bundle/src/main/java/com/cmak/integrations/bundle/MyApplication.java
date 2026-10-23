/*
 * Copyright (c) 2001-2015 BMC Software, Inc. All rights reserved. This software is the confidential
 * and proprietary information of BMC Software, Inc ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in accordance with the terms of the
 * license agreement between you and BMC Software, Inc.
 */

package com.cmak.integrations.bundle;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.bmc.arsys.rx.services.common.RxBundle;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.config.IntegrationConfigService;
import com.cmak.integrations.bundle.dawiyat.config.DawiyatConfigService;
import com.cmak.integrations.bundle.dawiyat.resource.DawiyatResource;
import com.cmak.integrations.bundle.dawiyat.service.DawiyatService;
import com.cmak.integrations.bundle.lbs.config.LbsConfigService;
import com.cmak.integrations.bundle.lbs.service.LbsService;
import com.cmak.integrations.bundle.sms.config.SmsConfigService;
import com.cmak.integrations.bundle.sms.service.SmsService;
import com.cmak.integrations.bundle.salam.resolve.ResolveService;
import com.cmak.integrations.bundle.salam.statusupdate.StatusUpdateService;
import com.cmak.integrations.bundle.salam.workinfo.WorkInfoService;

/**
 * Rx Web Activator class.
 *
 * register() runs ONCE when the bundle activates. The logging here is the
 * first thing to check after any deployment:
 *
 *   - if you see no [BOOT] lines at all, the bundle did not activate
 *   - if BUILD_VERSION is not the one you just built, the container is still
 *     running the old jar
 *   - if a component is missing from the list, its actions or endpoint will
 *     not be available
 *
 * Nothing here touches the configuration record. Configuration is read lazily
 * on the first call, so a bad config row cannot stop the bundle from starting
 * - it shows up as a [CONFIG] error at call time instead.
 *
 * OUTBOUND components (SMS, LBS) register a service whose @Action methods
 * appear in the Process designer. The INBOUND component (Dawiyat) registers a
 * service AND a RestfulResource, which is what publishes its URL.
 */
public class MyApplication extends RxBundle {

    @Override
    protected void register() {
        long startedAt = System.currentTimeMillis();

        ServiceLocator.getLogger().info(
                "[BOOT] ==========================================================");
        ServiceLocator.getLogger().info("[BOOT] Starting CyberMak integrations bundle");
        ServiceLocator.getLogger().info("[BOOT] Build " + Constants.BUILD_VERSION
                + " (" + Constants.BUILD_DATE + ")");
        ServiceLocator.getLogger().info("[BOOT] Java " + System.getProperty("java.version")
                + " | OS " + System.getProperty("os.name"));
        ServiceLocator.getLogger().info("[BOOT] Configuration record: "
                + Constants.CONFIG_RECORD_DEF);
        ServiceLocator.getLogger().info("[BOOT] Config cache TTL: "
                + (Constants.CACHE_TTL_MS / 1000) + "s");

        try {
            // ---- shared configuration reader ----
            IntegrationConfigService configService = new IntegrationConfigService();
            registerService(configService);
            ServiceLocator.getLogger().info("[BOOT] Registered IntegrationConfigService "
                    + "(shared by every integration)");

            // ---- SMS integration - OUTBOUND ----
            SmsConfigService smsConfigService = new SmsConfigService(configService);
            registerService(smsConfigService);
            registerService(new SmsService(smsConfigService));
            ServiceLocator.getLogger().info("[BOOT] Registered SMS integration [key="
                    + Constants.INTEGRATION_SMS + "] action: sendZainSms");

            // ---- LBS integration - OUTBOUND ----
            LbsConfigService lbsConfigService = new LbsConfigService(configService);
            registerService(lbsConfigService);
            registerService(new LbsService(lbsConfigService));
            ServiceLocator.getLogger().info("[BOOT] Registered LBS integration [key="
                    + Constants.INTEGRATION_LBS + "] action: locateSubscriber");

            // ---- Dawiyat FTTH integration - INBOUND ----
            DawiyatConfigService dawiyatConfigService = new DawiyatConfigService(configService);
            registerService(dawiyatConfigService);
            registerService(new DawiyatService(dawiyatConfigService));
            registerRestfulResource(new DawiyatResource());
            ServiceLocator.getLogger().info("[BOOT] Registered Dawiyat integration [key="
                    + Constants.INTEGRATION_DAWIYAT
                    + "] endpoint: PUT dawiyat/{partner}/tickets/{troubleTicketId}/notification");
            ServiceLocator.getLogger().info("[BOOT] Dawiyat staging record: "
                    + Constants.DawiyatConstants.STAGING_RECORD_DEF);

            ServiceLocator.getLogger().info("[BOOT] Startup complete in "
                    + (System.currentTimeMillis() - startedAt) + "ms");

        } catch (Throwable t) {
            // Without this the failure can vanish into the container log and
            // the actions simply never appear, with no explanation.
            ServiceLocator.getLogger().error(
                    "[BOOT] FAILED to register services - actions and endpoints will NOT "
                            + "be available: " + t);
            throw t;
        }

        ServiceLocator.getLogger().info(
                "[BOOT] ==========================================================");

        registerService(new ResolveService());
        registerService(new WorkInfoService());
        registerService(new StatusUpdateService());

        registerStaticWebResource(String.format("/%s", getId()), "/webapp");
    }
}