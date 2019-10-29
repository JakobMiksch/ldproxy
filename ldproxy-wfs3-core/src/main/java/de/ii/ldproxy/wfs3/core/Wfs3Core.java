/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableWfs3Collection;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.Wfs3Collection;
import de.ii.ldproxy.ogcapi.domain.Wfs3CollectionMetadataExtension;
import de.ii.ldproxy.ogcapi.domain.Wfs3Extent;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import de.ii.ldproxy.wfs3.api.ImmutableFeatureTransformationContextGeneric;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * @author zahnen
 */
@Component
@Provides(specifications = {Wfs3Core.class})
@Instantiate
public class Wfs3Core implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3Core.class);

    private final OgcApiExtensionRegistry extensionRegistry;
    private final MetricRegistry metricRegistry;

    public Wfs3Core(@Requires OgcApiExtensionRegistry extensionRegistry, @Requires Dropwizard dropwizard) {
        this.extensionRegistry = extensionRegistry;
        this.metricRegistry = dropwizard.getEnvironment()
                                        .metrics();
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core";
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        //TODO: config, config dependencies
        return true;
    }

    public void checkCollectionName(OgcApiDatasetData datasetData, String collectionName) {
        if (!datasetData.isFeatureTypeEnabled(collectionName)) {
            throw new NotFoundException();
        }
    }

    private List<Wfs3CollectionMetadataExtension> getCollectionExtenders() {
        return extensionRegistry.getExtensionsForType(Wfs3CollectionMetadataExtension.class);
    }

    public Wfs3Collection createCollection(FeatureTypeConfigurationOgcApi featureType, OgcApiDatasetData datasetData,
                                           OgcApiMediaType mediaType, List<OgcApiMediaType> alternativeMediaTypes,
                                           URICustomizer uriCustomizer, boolean isNested) {
        Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();

        /*final String qn = featureType.getLabel()/*service.getWfsAdapter()
                                                               .getNsStore()
                                                               .getNamespacePrefix(featureType.getNamespace()) + ":" + featureType.getName()*/
        ;

        ImmutableWfs3Collection.Builder collection = ImmutableWfs3Collection.builder()
                                                                            .id(featureType.getId())
                                                                            .title(featureType.getLabel())
                                                                            .description(featureType.getDescription())
                                                                            //.prefixedName(qn)
                                                                            .links(wfs3LinksGenerator.generateDatasetCollectionLinks(uriCustomizer.copy(), featureType.getId(), featureType.getLabel(), Optional.empty() /* new WFSRequest(service.getWfsAdapter(), new DescribeFeatureType(ImmutableMap.of(featureType.getNamespace(), ImmutableList.of(featureType.getName())))).getAsUrl()*/, mediaType, alternativeMediaTypes));

        if (datasetData.getFilterableFieldsForFeatureType(featureType.getId())
                       .containsKey("time")) {
            FeatureTypeConfigurationOgcApi.TemporalExtent temporal = featureType.getExtent()
                                                                                .getTemporal();
            collection.extent(new Wfs3Extent(
                    temporal.getStart() == 0 ? -1 : temporal.getStart(),
                    temporal.getEnd() == 0 ? -1 : temporal.getComputedEnd(),
                    featureType.getExtent()
                               .getSpatial()
                               .getXmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getXmax(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmax()));
        } else {
            collection.extent(new Wfs3Extent(
                    featureType.getExtent()
                               .getSpatial()
                               .getXmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmin(),
                    featureType.getExtent()
                               .getSpatial()
                               .getXmax(),
                    featureType.getExtent()
                               .getSpatial()
                               .getYmax()));
        }

        //TODO: to crs extension
        if (isNested) {
            collection.crs(
                    Stream.concat(
                            Stream.of(
                                    datasetData.getFeatureProvider()
                                               .getNativeCrs()
                                               .getAsUri(),
                                    OgcApiDatasetData.DEFAULT_CRS_URI
                            ),
                            datasetData.getAdditionalCrs()
                                       .stream()
                                       .map(EpsgCrs::getAsUri)
                    )
                          .distinct()
                          .collect(ImmutableList.toImmutableList())
            );
        }

        for (Wfs3CollectionMetadataExtension wfs3CollectionMetadataExtension : getCollectionExtenders()) {
            collection = wfs3CollectionMetadataExtension.process(collection, featureType, uriCustomizer.copy(), isNested, datasetData);
        }

        return collection.build();
    }

    public Response getItemsResponse(OgcApiDataset dataset, OgcApiRequestContext wfs3Request, String collectionName,
                                     FeatureQuery query, Wfs3OutputFormatExtension outputFormat,
                                     List<OgcApiMediaType> alternativeMediaTypes, boolean onlyHitsIfMore) {
        return getItemsResponse(dataset, wfs3Request, collectionName, query, true, outputFormat, alternativeMediaTypes, onlyHitsIfMore);
    }

    public Response getItemResponse(OgcApiDataset dataset, OgcApiRequestContext wfs3Request, String collectionName,
                                    FeatureQuery query, Wfs3OutputFormatExtension outputFormat,
                                    List<OgcApiMediaType> alternativeMediaTypes) {
        return getItemsResponse(dataset, wfs3Request, collectionName, query, false, outputFormat, alternativeMediaTypes, false);
    }

    private Response getItemsResponse(OgcApiDataset dataset, OgcApiRequestContext wfs3Request, String collectionName,
                                      FeatureQuery query,
                                      boolean isCollection, Wfs3OutputFormatExtension outputFormat,
                                      List<OgcApiMediaType> alternativeMediaTypes, boolean onlyHitsIfMore) {
        //Wfs3MediaType wfs3MediaType = checkMediaType(mediaType);
        checkCollectionName(dataset.getData(), collectionName);
        Optional<CrsTransformer> crsTransformer = dataset.getCrsTransformer(query.getCrs());

        boolean swapCoordinates = crsTransformer.isPresent() ? crsTransformer.get().needsCoordinateSwap() : dataset.getFeatureProvider()
                                                                        .shouldSwapCoordinates(query.getCrs());

        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        int pageSize = query.getLimit();
        int page = pageSize > 0 ? (pageSize + query.getOffset()) / pageSize : 0;

        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(wfs3Request.getUriCustomizer(), isCollection, page, pageSize, wfs3Request.getMediaType(), alternativeMediaTypes);

        ImmutableFeatureTransformationContextGeneric.Builder transformationContext = new ImmutableFeatureTransformationContextGeneric.Builder()
                .serviceData(dataset.getData())
                .collectionName(collectionName)
                .wfs3Request(wfs3Request)
                .crsTransformer(crsTransformer)
                .links(links)
                .isFeatureCollection(isCollection)
                .isHitsOnly(query.hitsOnly())
                .isPropertyOnly(query.propertyOnly())
                .fields(query.getFields())
                .limit(query.getLimit())
                .offset(query.getOffset())
                .maxAllowableOffset(query.getMaxAllowableOffset())
                .geometryPrecision(query.getGeometryPrecision())
                .shouldSwapCoordinates(swapCoordinates)
                .isHitsOnlyIfMore(onlyHitsIfMore);

        StreamingOutput streamingOutput;
        if (wfs3Request.getMediaType()
                       .matches(MediaType.valueOf(dataset.getFeatureProvider()
                                                         .getSourceFormat()))
                && outputFormat.canPassThroughFeatures()) {
            FeatureStream<GmlConsumer> featureStream = dataset.getFeatureProvider()
                                                              .getFeatureStream(query);

            streamingOutput = stream2(featureStream, outputStream -> outputFormat.getFeatureConsumer(transformationContext.outputStream(outputStream)
                                                                                                                          .build())
                                                                                 .get());
        } else if (outputFormat.canTransformFeatures()) {
            FeatureStream<FeatureTransformer> featureTransformStream = dataset.getFeatureProvider()
                                                                              .getFeatureTransformStream(query);

            streamingOutput = stream(featureTransformStream, outputStream -> outputFormat.getFeatureTransformer(transformationContext.outputStream(outputStream)
                                                                                                                                     .build())
                                                                                         .get());
        } else {
            throw new NotAcceptableException();
        }

        return response(streamingOutput, wfs3Request.getMediaType()
                                                    .main()
                                                    .toString());

        //return outputFormat
        //                        .getItemsResponse(getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), collectionName, query, featureTransformStream, crsTransformer, wfs3Request.getStaticUrlPrefix(), featureStream);
    }

    private Response response(Object entity, String type) {
        Response.ResponseBuilder response = Response.ok()
                                                    .entity(entity);
        if (type != null) {
            response.type(type);
        }

        return response.build();
    }

    private StreamingOutput stream(FeatureStream<FeatureTransformer> featureTransformStream,
                                   final Function<OutputStream, FeatureTransformer> featureTransformer) {
        Timer.Context timer = metricRegistry.timer(name(Wfs3Core.class, "stream"))
                                            .time();
        Timer.Context timer2 = metricRegistry.timer(name(Wfs3Core.class, "wait"))
                                             .time();

        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream), timer2)
                                      .toCompletableFuture()
                                      .join();
                timer.stop();
            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                //throw new IllegalStateException("Feature stream error", e.getCause());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Feature stream error, client gone? ({})", Throwables.getRootCause(e).getMessage());
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Exception", e.getCause());
                }
            }
        };
    }

    private StreamingOutput stream2(FeatureStream<GmlConsumer> featureTransformStream,
                                    final Function<OutputStream, GmlConsumer> featureTransformer) {
        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream), null)
                                      .toCompletableFuture()
                                      .join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                //throw new IllegalStateException("Feature stream error", e.getCause());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Feature stream error, client gone? ({})", Throwables.getRootCause(e).getMessage());
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Exception", e.getCause());
                }
            }
        };
    }
}
