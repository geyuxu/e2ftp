package org.eftp.ftpserver.business.hooks.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eftp.events.Command;
import org.eftp.events.FtpEvent;
import org.eftp.events.FtpEventName;

/**
 *
 * @author adam-bien.com
 */
@Path("events")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class EventBroadcastRessource {

    private final ConcurrentHashMap<FtpEventName, List<AsyncResponse>> listeners = new ConcurrentHashMap<>();
    private final static int TIMEOUT_IN_SECONDS = 20;

    @Inject
    Logger LOG;

    @GET
    @Path("{event-name}")
    public void registerForNotifications(@PathParam("event-name") String name,
            @Suspended AsyncResponse response) {
        FtpEventName commandName = FtpEventName.valueOf(name.toUpperCase());
        registerListener(commandName, response);
        setupTimeout(commandName, response);

    }

    public void onFtpEventArrival(@Observes @Command FtpEvent event) {
        List<AsyncResponse> commandListeners = findListenersForCommand(event);
        final FtpEventName command = event.getCommand();
        LOG.info("Received listeners " + commandListeners + " for command: " + command);
        JsonObject jsonEvent = event.asJson();
        for (AsyncResponse asyncResponse : commandListeners) {
            asyncResponse.resume(jsonEvent);
            cleanup(command, asyncResponse);
        }
    }

    List<AsyncResponse> findListenersForCommand(FtpEvent event) {
        List<AsyncResponse> retVal = new ArrayList<>();
        List<AsyncResponse> jokers = listeners.get(FtpEventName.EVERYTHING);
        List<AsyncResponse> specific = listeners.get(event.getCommand());
        if (jokers != null) {
            retVal.addAll(jokers);
        }
        if (specific != null) {
            retVal.addAll(specific);
        }
        return retVal;
    }

    void setupTimeout(final FtpEventName commandName, AsyncResponse response) {
        response.setTimeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        response.setTimeoutHandler(new TimeoutHandler() {

            @Override
            public void handleTimeout(AsyncResponse asyncResponse) {
                asyncResponse.resume(Response.status(Response.Status.NO_CONTENT).build());
                cleanup(commandName, asyncResponse);
            }
        });
    }

    void registerListener(FtpEventName commandName, AsyncResponse response) {
        List<AsyncResponse> commandListeners = listeners.get(commandName);
        if (commandListeners == null) {
            commandListeners = new ArrayList<>();
            listeners.put(commandName, commandListeners);
        }
        LOG.info("Registering listener for: " + commandName);
        commandListeners.add(response);
    }

    void cleanup(FtpEventName commandName, AsyncResponse response) {
        List<AsyncResponse> commandListeners = listeners.get(commandName);
        if (commandListeners != null) {
            commandListeners.remove(response);
        }
    }
}
