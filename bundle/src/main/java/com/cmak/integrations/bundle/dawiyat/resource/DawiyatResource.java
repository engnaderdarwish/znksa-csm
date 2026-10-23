package com.cmak.integrations.bundle.dawiyat.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bmc.arsys.rx.application.common.ServiceLocator;
import com.bmc.arsys.rx.services.bundle.BundleService;
import com.bmc.arsys.rx.services.common.RestfulResource;
import com.bmc.arsys.rx.services.common.annotation.AccessControlledMethod;
import com.bmc.arsys.rx.services.common.annotation.RxDefinitionTransactional;
import com.cmak.integrations.bundle.config.Constants;
import com.cmak.integrations.bundle.dawiyat.domain.DawiyatResponse;
import com.cmak.integrations.bundle.dawiyat.domain.DawiyatTicketUpdate;
import com.cmak.integrations.bundle.dawiyat.service.DawiyatService;

/**
 * The inbound REST endpoint Dawiyat calls.
 *
 * This is the ONLY component of the three that is inbound. SMS and LBS expose
 * @Action methods a process calls; this exposes a URL a partner calls.
 *
 * Legacy URL (Tomcat WAR):
 *   PUT /service/webresources/generic/{partner}/tickets/{troubleTicketId}/notification
 *
 * New URL (Helix):
 *   PUT /api/<bundle-id>/dawiyat/{partner}/tickets/{troubleTicketId}/notification
 *
 * The tail of the path is kept identical so Dawiyat only has to change the
 * host and the prefix, not the shape of the call.
 *
 * The three other endpoints in the legacy WAR (GET, POST and a PATCH that
 * threw an exception when the user was literally "anjay") were NetBeans
 * wizard leftovers and are deliberately NOT ported.
 */
@Path("dawiyat")
public class DawiyatResource implements RestfulResource {

    private DawiyatService dawiyatService = null;

    /**
     * Looked up by class name, not injected - a RestfulResource is created by
     * the platform and cannot take constructor arguments. Same approach as
     * MOEResource in the MOE bundle.
     */
    private DawiyatService getDawiyatService() {
        if (dawiyatService == null) {
            BundleService bundleService = ServiceLocator.getBundleService();
            dawiyatService = (DawiyatService) bundleService.getService(
                    Constants.DawiyatConstants.SERVICE_CLASS_NAME);
        }
        return dawiyatService;
    }

    /**
     * Receives one ticket update from Dawiyat and stores it in the staging
     * record for downstream workflow to process.
     *
     * Authorization is ValidUser: Helix authenticates the caller before this
     * method runs, so there is no hand-rolled Basic auth check the way the
     * legacy SecurityFilter did (it compared against the hard-coded strings
     * "user" and "password").
     *
     * HTTP status codes, which the legacy service did not use properly -
     * it answered 200 to everything, including failures:
     *   200 - stored
     *   400 - the payload is wrong, do not retry unchanged
     *   500 - our side failed, safe to retry
     */
    @PUT
    @Path("/{partner}/tickets/{troubleTicketId}/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RxDefinitionTransactional(readOnly = true)
    @AccessControlledMethod(
            authorization = AccessControlledMethod.AuthorizationLevel.ValidUser,
            allowOperationInServerGroup = true)
    public Response notification(@PathParam("partner") String partner,
                                 @PathParam("troubleTicketId") String troubleTicketId,
                                 DawiyatTicketUpdate update) {
        try {
            DawiyatResponse response = getDawiyatService()
                    .recordTicketUpdate(partner, troubleTicketId, update);

            Response.Status status;
            if (Constants.DawiyatConstants.RESPONSE_CODE_SUCCESS.equals(response.getErrCode())) {
                status = Response.Status.OK;
            } else if (Constants.DawiyatConstants.RESPONSE_CODE_BAD_REQUEST
                    .equals(response.getErrCode())) {
                status = Response.Status.BAD_REQUEST;
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR;
            }

            return Response.status(status)
                    .entity(response)
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Throwable t) {
            // Nothing should reach here - the service catches its own errors.
            // If it does, answer in the agreed JSON shape rather than letting
            // the container return an HTML error page Dawiyat cannot parse.
            ServiceLocator.getLogger().error(
                    "[DAWIYAT] Unhandled error in the notification endpoint: " + t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(DawiyatResponse.error("Unexpected error: "
                            + t.getClass().getSimpleName()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}