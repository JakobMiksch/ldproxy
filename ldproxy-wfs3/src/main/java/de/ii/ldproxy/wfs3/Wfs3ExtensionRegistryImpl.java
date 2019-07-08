/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.google.common.collect.Lists;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClass;
import de.ii.ldproxy.wfs3.api.Wfs3Extension;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.wfs3.api.Wfs3Extension)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class Wfs3ExtensionRegistryImpl implements Wfs3ExtensionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ExtensionRegistryImpl.class);

    @Context
    private BundleContext bundleContext;

    private final List<Wfs3Extension> wfs3Extensions;
    private final List<Wfs3ConformanceClass> wfs3ConformanceClasses;
    private final Map<Wfs3MediaType, Wfs3OutputFormatExtension> wfs3OutputFormats;
    private final List<Wfs3EndpointExtension> wfs3Endpoints;

    Wfs3ExtensionRegistryImpl() {
        //TODO
        this.wfs3Extensions = new ArrayList<>();
        this.wfs3ConformanceClasses = Lists.newArrayList(() -> "http://www.opengis.net/spec/wfs-1/3.0/req/core");
        this.wfs3OutputFormats = new LinkedHashMap<>();
        this.wfs3Endpoints = new ArrayList<>();
    }

    @Override
    public List<Wfs3Extension> getExtensions() {
        return wfs3Extensions;
    }

    @Override
    public List<Wfs3ConformanceClass> getConformanceClasses() {
        return wfs3ConformanceClasses;
    }

    @Override
    public Map<Wfs3MediaType, Wfs3OutputFormatExtension> getOutputFormats() {
        return wfs3OutputFormats;
    }

    @Override
    public List<Wfs3EndpointExtension> getEndpoints() {
        return wfs3Endpoints;
    }

    private synchronized void onArrival(ServiceReference<Wfs3Extension> ref) {
        try {
            final Wfs3Extension wfs3Extension = bundleContext.getService(ref);

            wfs3Extensions.add(wfs3Extension);

            if (wfs3Extension instanceof Wfs3ConformanceClass) {
                final Wfs3ConformanceClass wfs3ConformanceClass = (Wfs3ConformanceClass) wfs3Extension;

                LOGGER.debug("WFS3 CONFORMANCE CLASS {}", wfs3ConformanceClass.getConformanceClass());

                wfs3ConformanceClasses.add(wfs3ConformanceClass);
            }

            if (wfs3Extension instanceof Wfs3OutputFormatExtension) {
                final Wfs3OutputFormatExtension wfs3OutputFormat = (Wfs3OutputFormatExtension) wfs3Extension;

                LOGGER.debug("WFS3 OUTPUT FORMAT {}", wfs3OutputFormat.getMediaType());

                wfs3OutputFormats.put(wfs3OutputFormat.getMediaType(), wfs3OutputFormat);
            }

            if (wfs3Extension instanceof Wfs3EndpointExtension) {
                final Wfs3EndpointExtension wfs3Endpoint = (Wfs3EndpointExtension) wfs3Extension;

                LOGGER.debug("WFS3 ENDPOINT {} {}", wfs3Endpoint.getPath(), wfs3Endpoint.getMethods());

                wfs3Endpoints.add(wfs3Endpoint);
            }
        } catch (Throwable e) {
            LOGGER.error("E", e);
        }
    }

    private synchronized void onDeparture(ServiceReference<Wfs3Extension> ref) {
        final Wfs3Extension wfs3Extension = bundleContext.getService(ref);

        if (Objects.nonNull(wfs3Extension)) {

            wfs3Extensions.remove(wfs3Extension);

            if (wfs3Extension instanceof Wfs3ConformanceClass) {
                wfs3ConformanceClasses.remove(wfs3Extension);
            }

            if (wfs3Extension instanceof Wfs3OutputFormatExtension) {
                wfs3OutputFormats.remove(((Wfs3OutputFormatExtension)wfs3Extension).getMediaType());
            }

            if (wfs3Extension instanceof Wfs3EndpointExtension) {
                wfs3Endpoints.remove(wfs3Extension);
            }
        }
    }
}
