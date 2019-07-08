/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceMetadata;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xtraplatform.auth.api.AuthConfig;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS_URI;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class ExtendableOpenApiDefinition {

    private static Logger LOGGER = LoggerFactory.getLogger(ExtendableOpenApiDefinition.class);

    @Context
    private BundleContext bundleContext;

    private final ObjectMapper objectMapper;
    private final String externalUrl;
    private final AuthConfig authConfig;
    private final Set<Wfs3OpenApiExtension> openApiExtensions;

    public ExtendableOpenApiDefinition(@Requires Jackson jackson, @Requires Dropwizard dropwizard, @Requires AuthConfig authConfig) {
        super();
        this.objectMapper = jackson.getDefaultObjectMapper();
        this.externalUrl = dropwizard.getExternalUrl();
        this.authConfig = authConfig;
        this.openApiExtensions = new TreeSet<>(Comparator.comparingInt(Wfs3OpenApiExtension::getSortPriority));
    }

    public Response getOpenApi(String type, URICustomizer requestUriCustomizer, Wfs3ServiceData serviceData) {

        boolean pretty = true;

        try {
            OpenAPI openAPI = Json.mapper()
                                  .readerFor(OpenAPI.class)
                                  //.with(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                                  .readValue(Resources.asByteSource(Resources.getResource(ExtendableOpenApiDefinition.class, "/wfs3-api.json"))
                                                      .openBufferedStream());

            //TODO
            if (serviceData.isSecured() && authConfig.isJwt()) {
                openAPI.getComponents()
                       .addSecuritySchemes("JWT", new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                                                      .scheme("bearer")
                                                                      .bearerFormat("JWT"));
                openAPI.addSecurityItem(new SecurityRequirement().addList("JWT"));
            }

            openAPI.getInfo()
                   .version("1.0.0");


            openAPI.servers(ImmutableList.of(new Server().url(requestUriCustomizer.copy()
                                                                                  .clearParameters()
                                                                                  .removeLastPathSegment("api")
                                                                                  .toString())));

            if (serviceData != null) {
                /*WFSOperation operation = new GetCapabilities();

                WFSCapabilitiesAnalyzer analyzer = new MultiWfsCapabilitiesAnalyzer(
                        new GetCapabilities2OpenApi(openAPI)
                );

                WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, serviceData.staxFactory);

                wfsParser.parse(serviceData.getWfsAdapter()
                                           .request(operation));*/

                openAPI.getInfo()
                       .title(serviceData.getLabel())
                       .description(serviceData.getDescription()
                                               .orElse(""));

                if (serviceData.getMetadata()
                               .isPresent()) {
                    Wfs3ServiceMetadata md = serviceData.getMetadata()
                                                        .get();
                    openAPI.getInfo()
                           .contact(new Contact().name(md.getContactName()
                                                         .orElse(null))
                                                 .url(md.getContactUrl()
                                                        .orElse(null))
                                                 .email(md.getContactEmail()
                                                          .orElse(null)))
                           .license(new License().name(md.getLicenseName()
                                                         .orElse(null))
                                                 .url(md.getLicenseUrl()
                                                        .orElse(null)));
                }
            }

            openApiExtensions.forEach(openApiExtension -> openApiExtension.process(openAPI, serviceData));



            if (StringUtils.isNotBlank(type) && type.trim()
                                                    .equalsIgnoreCase("yaml")) {
                return Response.status(Response.Status.OK)
                               .entity(pretty ? Yaml.pretty(openAPI) : Yaml.mapper()
                                                                           .writeValueAsString(openAPI))
                               .type("application/yaml")
                               .build();
            } else {
                return Response.status(Response.Status.OK)
                               .entity(pretty ? Json.pretty(openAPI) : Json.mapper()
                                                                           .writeValueAsString(openAPI))
                               .type("application/openapi+json;version=3.0"/*MediaTypeCharset.APPLICATION_JSON_UTF8*/)
                               .build();
            }
        } catch (IOException e) {
            // ignore
            LOGGER.debug("ERROR", e);
        }
        return Response.noContent()
                       .build();
    }

    private synchronized void onArrival(ServiceReference<Wfs3OpenApiExtension> ref) {
        try {
            final Wfs3OpenApiExtension wfs3OpenApiExtension = bundleContext.getService(ref);

            openApiExtensions.add(wfs3OpenApiExtension);
        } catch (Throwable e) {
            LOGGER.error("E", e);
        }
    }

    private synchronized void onDeparture(ServiceReference<Wfs3OpenApiExtension> ref) {
        final Wfs3OpenApiExtension wfs3OpenApiExtension = bundleContext.getService(ref);

        if (Objects.nonNull(wfs3OpenApiExtension)) {
            openApiExtensions.remove(wfs3OpenApiExtension);
        }
    }


}
