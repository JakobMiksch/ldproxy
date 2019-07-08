/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3MediaType;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Collections;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClass;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClasses;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.provider.wfs.FeatureProviderDataWfs;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OutputFormatGml implements Wfs3ConformanceClass, Wfs3OutputFormatExtension {

    private static final Wfs3MediaType MEDIA_TYPE = ImmutableWfs3MediaType.builder()
                                                                          .main(new MediaType("application", "gml+xml", ImmutableMap.of("version", "3.2", "profile", "http://www.opengis.net/def/profile/ogc/2.0/gml-sf2")))
                                                                          .label("GML")
                                                                          .metadata(MediaType.APPLICATION_XML_TYPE)
                                                                          .build();

    @Requires
    private GmlConfig gmlConfig;

    @ServiceController(value = false)
    private boolean enable;

    @Validate
    private void onStart() {
        this.enable = gmlConfig.isEnabled();
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/wfs-1/3.0/req/gmlsf2";
    }

    @Override
    public Wfs3MediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Response getConformanceResponse(List<Wfs3ConformanceClass> wfs3ConformanceClasses, String serviceLabel, Wfs3MediaType wfs3MediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String staticUrlPrefix) {
        return response(new Wfs3ConformanceClassesXml(new Wfs3ConformanceClasses(wfs3ConformanceClasses.stream().map(Wfs3ConformanceClass::getConformanceClass).collect(Collectors.toList()))));
    }

    @Override
    public Response getDatasetResponse(Wfs3Collections wfs3Collections, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String staticUrlPrefix, boolean isCollections) {
        if (isCollections) {
            return response(new Wfs3CollectionsXml(wfs3Collections));
        }

        return response(new LandingPage(uriCustomizer, serviceData, mediaType, alternativeMediaTypes));
    }

    @Override
    public Response getCollectionResponse(Wfs3Collection wfs3Collection, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String collectionName) {
        return response(new Wfs3CollectionXml(wfs3Collection));
    }

    @Override
    public Response getItemsResponse(Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String collectionName, FeatureQuery query, FeatureStream<FeatureTransformer> featureTransformStream, CrsTransformer crsTransformer, String staticUrlPrefix, FeatureStream<GmlConsumer> featureStream) {
        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        int pageSize = query.getLimit();
        int page = pageSize > 0 ? (pageSize + query.getOffset()) / pageSize : 0;
        boolean isCollection = uriCustomizer.isLastPathSegment("items");

        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(uriCustomizer, isCollection, page, pageSize, mediaType, alternativeMediaTypes);


        return response(stream(featureStream, outputStream -> new FeatureTransformerGmlUpgrade(outputStream, isCollection, ((FeatureProviderDataWfs)serviceData.getFeatureProvider()).getConnectionInfo().getNamespaces(), crsTransformer, links, pageSize, query.getMaxAllowableOffset())));
    }

    @Override
    public Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.empty();
    }

    private Response response(Object entity) {
        return response(entity, null);
    }

    private Response response(Object entity, String type) {
        Response.ResponseBuilder response = Response.ok()
                                                    .entity(entity);
        if (type != null) {
            response.type(type);
        }

        return response.build();
    }

    // TODO: same for every Wfs3OutputFormat, extract
    private StreamingOutput stream(FeatureStream<GmlConsumer> featureTransformStream, final Function<OutputStream, GmlConsumer> featureTransformer) {
        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream))
                                      .toCompletableFuture()
                                      .join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error", e.getCause());
            }
        };
    }
}
