/*
 *
 */
package org.eftp.ftpserver.business.configuration.boundary;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.eftp.ftpserver.business.configuration.control.ConfigurationStore;
import org.eftp.ftpserver.business.logger.boundary.Log;

/**
 *
 * @author adam-bien.com
 */
@Startup
@Singleton
public class ConfigurationStartup {

    @Inject
    ConfigurationStore store;

    @Inject
    Log LOG;

    @PostConstruct
    public void establishDefaults() {
        LOG.info("Establishing defaults");
        this.store.saveOrUpdate("SERVER_PORT", "8888");
        this.store.saveOrUpdate("IDLE_TIME", String.valueOf(30 * 1000));
        this.store.saveOrUpdate("MAX_LOGINS", String.valueOf(100));
        LOG.info("Default values are written");
    }

}