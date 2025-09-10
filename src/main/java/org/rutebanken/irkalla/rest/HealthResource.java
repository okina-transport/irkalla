/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.irkalla.rest;

import org.rutebanken.irkalla.service.IrkallaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Component
@Produces("application/json")
@Path("/health")
public class HealthResource {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IrkallaConfiguration irkallaConfiguration;

    private boolean hasBeenOkOnce = false;

    /**
     *  curl http://localhost:8080/babylon/health/ready
     */
    @GET
    @Path("/ready")
    public Response isReady() {
        if ( true) { // Some relevant test here
            return Response.ok("OK").build();
        } else {
            return Response.serverError()
                       .status(Response.Status.SERVICE_UNAVAILABLE)
                       .entity("XXXX is not available. This disables this service as well.")
                       .build();
        }
    }

    @GET
    @Path("/live")
    public Response isLive() {
        if ( !hasBeenOkOnce ) {
            if ( false ) {
                return Response.status(500).entity("Something is not OK").build();
            }

            hasBeenOkOnce = true;
        }

        return Response.ok("OK").build();
    }
}

