/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3MediaType;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Collections;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClass;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.MOVED_PERMANENTLY;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OutputFormatHtml implements Wfs3ConformanceClass, Wfs3OutputFormatExtension {

    static final Wfs3MediaType MEDIA_TYPE = ImmutableWfs3MediaType.builder()
                                                                  .main(MediaType.TEXT_HTML_TYPE)
                                                                  //.qs(900)
                                                                  .build();

    @Context
    private BundleContext bc;

    @Requires
    private HtmlConfig htmlConfig;

    @Requires
    private Dropwizard dropwizard;

    //@Requires
    //private CodelistStore codelistStore;

    @Requires(optional = true)
    private Codelist[] codelists;

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/wfs-1/3.0/req/html";
    }

    @Override
    public Wfs3MediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Response getConformanceResponse(List<Wfs3ConformanceClass> wfs3ConformanceClasses, String serviceLabel, Wfs3MediaType wfs3MediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String staticUrlPrefix) {

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", uriCustomizer.copy()
                                                                .removeLastPathSegments(2)
                                                                .toString()))
                .add(new NavigationDTO(serviceLabel, uriCustomizer.copy()
                                                                  .removeLastPathSegments(1)
                                                                  .toString()))
                .add(new NavigationDTO("Conformance Classes"))
                .build();

        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        List<Wfs3Link> links = wfs3LinksGenerator.generateAlternateLinks(uriCustomizer.copy(), true, alternativeMediaTypes);

        Wfs3ConformanceClassesView wfs3ConformanceClassesView = new Wfs3ConformanceClassesView(wfs3ConformanceClasses.stream()
                                                                                                                     .map(Wfs3ConformanceClass::getConformanceClass)
                                                                                                                     .collect(Collectors.toList()), breadCrumbs, links, staticUrlPrefix, htmlConfig);
        return Response.ok()
                       .type(wfs3MediaType.metadata())
                       .entity(wfs3ConformanceClassesView)
                       .build();
    }

    @Override
    public Response getDatasetResponse(Wfs3Collections wfs3Collections, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String staticUrlPrefix, boolean isCollections) {

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", uriCustomizer.copy()
                                                                  .removeLastPathSegments(uriCustomizer.isLastPathSegment("collections") ? 2 : 1)
                                                                  .toString()))
                .add(new NavigationDTO(serviceData.getLabel()))
                .build();

        Wfs3DatasetView wfs3DatasetView = new Wfs3DatasetView(serviceData, wfs3Collections, breadCrumbs, staticUrlPrefix, htmlConfig);

        return Response.ok()
                       .type(mediaType.metadata())
                       .entity(wfs3DatasetView)
                       .build();
    }

    @Override
    public Response getCollectionResponse(Wfs3Collection wfs3Collection, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String collectionName) {
        return Response.status(MOVED_PERMANENTLY)
                       .header(HttpHeaders.LOCATION, uriCustomizer.copy()
                                                                  .ensureLastPathSegment("items")
                                                                  .toString())
                       .build();
    }

    @Override
    public Response getItemsResponse(Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String collectionName, FeatureQuery query, FeatureStream<FeatureTransformer> featureTransformStream, CrsTransformer crsTransformer, String staticUrlPrefix, FeatureStream<GmlConsumer> featureStream) {
        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        int pageSize = query.getLimit();
        int page = pageSize > 0 ? (pageSize + query.getOffset()) / pageSize : 0;
        boolean isCollection = uriCustomizer.isLastPathSegment("items");

        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(uriCustomizer.copy(), isCollection, page, pageSize, mediaType, alternativeMediaTypes);

        FeatureCollectionView featureTypeDataset;
        if (isCollection) {
            featureTypeDataset = createFeatureCollectionView(serviceData.getFeatureTypes()
                                                                        .get(collectionName), uriCustomizer.copy(), serviceData.getFilterableFieldsForFeatureType(collectionName, true), serviceData.getHtmlNamesForFeatureType(collectionName), staticUrlPrefix);

            addDatasetNavigation(featureTypeDataset, serviceData.getLabel(), serviceData.getFeatureTypes()
                                                                                        .get(collectionName)
                                                                                        .getLabel(), links, uriCustomizer.copy());
        } else {
            featureTypeDataset = createFeatureDetailsView(serviceData.getFeatureTypes()
                                                                     .get(collectionName), uriCustomizer.copy(), links, serviceData.getLabel(), uriCustomizer.getLastPathSegment(), staticUrlPrefix);
        }
        return response(stream(featureTransformStream, outputStream -> new FeatureTransformerHtml(new OutputStreamWriter(outputStream), isCollection, crsTransformer, page, pageSize, featureTypeDataset, codelists, dropwizard.getMustacheRenderer(), uriCustomizer.copy()
                                                                                                                                                                                                                                                                    .cutPathAfterSegments(serviceData.getId())
                                                                                                                                                                                                                                                                    .clearParameters()
                                                                                                                                                                                                                                                                    .toString())));
    }

    @Override
    public Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.of(new Gml2MicrodataMappingProvider());
    }

    public Response getFile(String file) {
        try {
            final URL url = file.endsWith("favicon.ico") ? bc.getBundle()
                                                             .getResource("img/favicon.ico") : bc.getBundle()
                                                                                                 .getResource(file);

            MediaType mediaType = file.endsWith(".css") ? new MediaType("text", "css", "utf-8") : file.endsWith(".js") ? new MediaType("application", "javascript", "utf-8") : new MediaType("image", "x-icon");

            return Response.ok((StreamingOutput) output -> Resources.asByteSource(url)
                                                                    .copyTo(output))
                           .type(mediaType)
                           .build();
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }

    private FeatureCollectionView createFeatureCollectionView(FeatureTypeConfigurationWfs3 featureType, URICustomizer uriCustomizer, Map<String, String> filterableFields, Map<String, String> htmlNames, String staticUrlPrefix) {
        URI requestUri = null;
        try {
            requestUri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            //ignore
        }
        URICustomizer uriBuilder = uriCustomizer.copy()
                                                .clearParameters()
                                                .ensureParameter("f", MEDIA_TYPE.parameter())
                                                .ensureLastPathSegment("items");

        DatasetView dataset = new DatasetView("", requestUri, null, staticUrlPrefix, htmlConfig);

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureCollection", requestUri, featureType.getId(), featureType.getLabel(), staticUrlPrefix, htmlConfig);

        //TODO featureTypeDataset.uriBuilder = uriBuilder;
        dataset.featureTypes.add(featureTypeDataset);

        featureTypeDataset.temporalExtent = featureType.getExtent()
                                                       .getTemporal();

        BoundingBox bbox = featureType.getExtent()
                                      .getSpatial();
        featureTypeDataset.bbox2 = ImmutableMap.of("minLng", Double.toString(bbox.getYmin()), "minLat", Double.toString(bbox.getXmin()), "maxLng", Double.toString(bbox.getYmax()), "maxLat", Double.toString(bbox.getXmax()));

        featureTypeDataset.filterFields = filterableFields.entrySet()
                                                          .stream()
                                                          .peek(entry -> {
                                                              if (htmlNames.containsKey(entry.getValue())) {
                                                                  entry.setValue(htmlNames.get(entry.getValue()));
                                                              }
                                                          })
                                                          .collect(Collectors.toSet());
        featureTypeDataset.uriBuilder = uriBuilder;

        return featureTypeDataset;
    }

    private FeatureCollectionView createFeatureDetailsView(FeatureTypeConfigurationWfs3 featureType, URICustomizer uriCustomizer, List<Wfs3Link> links, String serviceLabel, String featureId, String staticUrlPrefix) {
        URI requestUri = null;
        try {
            requestUri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            //ignore
        }
        URICustomizer uriBuilder = uriCustomizer.copy()
                                                .clearParameters()
                                                .ensureParameter("f", MEDIA_TYPE.parameter())
                                                .removeLastPathSegments(1);

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureDetails", requestUri, featureType.getId(), featureType.getLabel(), staticUrlPrefix, htmlConfig);
        featureTypeDataset.description = featureType.getDescription()
                                                    .orElse(featureType.getLabel());

        featureTypeDataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", uriBuilder.copy()
                                                                                       .removePathSegment("collections", -3)
                                                                                       .removeLastPathSegments(3)
                                                                                       .toString()))
                .add(new NavigationDTO(serviceLabel, uriBuilder.copy()
                                                                                         .removePathSegment("collections", -3)
                                                                                         .removeLastPathSegments(2)
                                                                                         .toString()))
                .add(new NavigationDTO(featureType.getLabel(), uriBuilder.toString()))
                .add(new NavigationDTO(featureId))
                .build();

        featureTypeDataset.formats = links.stream()
                                          .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "alternate"))
                                          .sorted(Comparator.comparing(link -> link.getTypeLabel()
                                                                                   .toUpperCase()))
                                          .map(wfs3Link -> new NavigationDTO(wfs3Link.getTypeLabel(), wfs3Link.getHref()))
                                          .collect(Collectors.toList());

        /*new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();*/

        return featureTypeDataset;
    }

    private void addDatasetNavigation(FeatureCollectionView featureCollectionView, String serviceLabel, String collectionLabel, List<Wfs3Link> links, URICustomizer uriCustomizer) {
        URICustomizer uriBuilder = uriCustomizer
                .clearParameters()
                .ensureParameter("f", MEDIA_TYPE.parameter())
                .removePathSegment("items", -1)
                .removePathSegment("collections", -2);

        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", uriBuilder.copy()
                                                                                       .removeLastPathSegments(2)
                                                                                       .toString()))
                .add(new NavigationDTO(serviceLabel, uriBuilder.copy()
                                                                                         .removeLastPathSegments(1)
                                                                                         .toString()))
                .add(new NavigationDTO(collectionLabel))
                .build();

        // TODO: only activated formats
        featureCollectionView.formats = links.stream()
                                             .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "alternate"))
                                             .sorted(Comparator.comparing(link -> link.getTypeLabel()
                                                                                      .toUpperCase()))
                                             .map(wfs3Link -> new NavigationDTO(wfs3Link.getTypeLabel(), wfs3Link.getHref()))
                                             .collect(Collectors.toList());

        /*new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();*/


    }

    private Response response(Object entity) {
        return Response.ok()
                       .entity(entity)
                       .build();
    }

    // TODO: same for every Wfs3OutputFormat, extract
    private StreamingOutput stream(FeatureStream<FeatureTransformer> featureTransformStream, final Function<OutputStream, FeatureTransformer> featureTransformer) {
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
