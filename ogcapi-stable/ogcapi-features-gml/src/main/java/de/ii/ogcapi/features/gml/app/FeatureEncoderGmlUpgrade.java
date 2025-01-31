/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.biConsumerMayThrow;
import static de.ii.xtraplatform.base.domain.util.LambdaWithException.consumerMayThrow;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;
import de.ii.ogcapi.features.gml.domain.EncodingAwareContextGml;
import de.ii.ogcapi.features.gml.domain.ModifiableEncodingAwareContextGml;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoderDefault;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.gml.domain.XMLNamespaceNormalizer;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class FeatureEncoderGmlUpgrade extends
    FeatureTokenEncoderDefault<EncodingAwareContextGml> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderGmlUpgrade.class);
    private static final List<String> GEOMETRY_COORDINATES = new ImmutableList.Builder<String>()
            .add("posList")
            .add("pos")
            .add("coordinates")
            .add("lowerCorner")
            .add("upperCorner")
            .build();;

    private final FeatureTransformationContextGml transformationContext;
    private final OutputStream outputStream;
    private final boolean isFeatureCollection;
    private final OutputStreamWriter writer;
    private final XMLNamespaceNormalizer namespaces;
    private final CrsTransformer crsTransformer;
    private final List<Link> links;
    private final Escaper escaper;
    private final int pageSize;
    private double maxAllowableOffset;

    private boolean inCurrentStart;
    private boolean inCurrentFeatureStart;
    private boolean inCurrentPropertyStart;
    private boolean inCurrentPropertyText;
    private boolean inCoordinates;
    private ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder;
    private Integer currentDimension = null;
    private boolean isLastPage;
    private String locations;

    public FeatureEncoderGmlUpgrade(FeatureTransformationContextGml transformationContext) {
        this.transformationContext = transformationContext;
        this.outputStream = transformationContext.getOutputStream();
        this.isFeatureCollection = transformationContext.isFeatureCollection();
        this.writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        this.namespaces = new XMLNamespaceNormalizer(transformationContext.getNamespaces());
        this.crsTransformer = transformationContext.getCrsTransformer().orElse(null);
        this.links = transformationContext.getLinks();
        this.escaper = XmlEscapers.xmlAttributeEscaper();
        this.pageSize  = transformationContext.getLimit();
        this.maxAllowableOffset = transformationContext.getMaxAllowableOffset();
        this.namespaces.addNamespace("sf", "http://www.opengis.net/ogcapi-features-1/1.0/sf", true);
        this.namespaces.addNamespace("ogcapi", "http://www.opengis.net/ogcapi-features-1/1.0", true);
        this.namespaces.addNamespace("atom", "http://www.w3.org/2005/Atom", true);
    }

    @Override
    public void onStart(EncodingAwareContextGml context) {
        //TODO: more elegant solution
        if (transformationContext.getOutputStream() instanceof OutputStreamToByteConsumer) {
            ((OutputStreamToByteConsumer) transformationContext.getOutputStream()).setByteConsumer(this::push);
        }
        try {
            writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            if (isFeatureCollection) {
                writer.append("\n<sf:FeatureCollection");
                namespaces.getNamespaces()
                          .keySet()
                          .forEach(consumerMayThrow(prefix -> {
                              if (!Strings.isNullOrEmpty(prefix)) {
                                  writer.append(" ");
                                  writer.append(namespaces.generateNamespaceDeclaration(prefix));
                              }
                          }));

                isLastPage = context.metadata().getNumberReturned().orElse(0) < pageSize;
                inCurrentStart = true;

                context.additionalInfo().forEach(biConsumerMayThrow(this::onGmlAttribute));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnd(EncodingAwareContextGml context) {
        try {
            if (isFeatureCollection) {
                if (inCurrentStart) {
                    writer.append(">");

                    inCurrentStart = false;
                }
                writer.append("\n</sf:FeatureCollection>");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onFeatureStart(EncodingAwareContextGml context) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}", context.path());
        }
        try {
            if (inCurrentStart) {
                writer.append(">");

                inCurrentStart = false;
            }

            if (isFeatureCollection) {
                writer.append("\n<sf:featureMember>");
            }
            writer.append("\n<");
            writer.append(getNamespaceUri(context.path()));
            writer.append(":");
            writer.append(getLocalName(context.path()));

            if (!isFeatureCollection) {
                namespaces.getNamespaces()
                          .keySet()
                          .forEach(consumerMayThrow(prefix -> {
                              writer.append(" ");
                              writer.append(namespaces.generateNamespaceDeclaration(prefix));
                          }));
                if (!Strings.isNullOrEmpty(locations)) {
                    writer.append(" ");
                    writer.append(namespaces.getNamespacePrefix("http://www.w3.org/2001/XMLSchema-instance"));
                    writer.append(":schemaLocation");
                    writer.append("=\"");
                    writer.append(locations);
                    writer.append("\"");
                }
            }

            inCurrentFeatureStart = true;

            context.additionalInfo().forEach(biConsumerMayThrow(this::onGmlAttribute));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onFeatureEnd(EncodingAwareContextGml context) {
        try {
            writer.append("\n</");
            writer.append(getNamespaceUri(context.path()));
            writer.append(":");
            writer.append(getLocalName(context.path()));
            writer.append(">");
            if (isFeatureCollection) {
                writer.append("\n</sf:featureMember>");
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onObjectStart(EncodingAwareContextGml context) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("START {} {} {} {}", context.path(), getLocalName(context.path()), context.inGeometry(), context.schema().map(
                SchemaBase::isSpatial).isPresent());
        }
        try {
            if (inCurrentFeatureStart) {
                writer.append(">");
                inCurrentFeatureStart = false;
            }
            if (inCurrentPropertyStart) {
                writer.append(">");
            }

            writer.append("\n<");
            writer.append(getNamespaceUri(context.path()));
            writer.append(":");
            writer.append(getLocalName(context.path()));

            inCurrentPropertyStart = true;
            if (GEOMETRY_COORDINATES.contains(getLocalName(context.path()))) {
                inCoordinates = true;
            }

            context.additionalInfo().forEach(biConsumerMayThrow(this::onGmlAttribute));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onObjectEnd(EncodingAwareContextGml context) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("END {} {}", context.path(), getLocalName(context.path()));
        }
        try {


            if (inCurrentPropertyStart) {
                writer.append("/>");
                inCurrentPropertyStart = false;
            } else {
                if (!inCurrentPropertyText) {
                    writer.append("\n");
                } else {
                    inCurrentPropertyText = false;
                }

                writer.append("</");
                writer.append(getNamespaceUri(context.path()));
                writer.append(":");
                writer.append(getLocalName(context.path()));
                writer.append(">");
            }
            inCoordinates = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onArrayStart(EncodingAwareContextGml context) {
        onObjectStart(context);
    }

    @Override
    public void onArrayEnd(EncodingAwareContextGml context) {
        onObjectEnd(context);
    }

    private void onGmlAttribute(String name, String value) throws Exception {
        onGmlAttribute(getNamespaceUri(name), getLocalName(name), ImmutableList.of(), value, ImmutableList.of());
    }

    //@Override
    public void onGmlAttribute(String namespace, String localName, List<String> path, String value, List<Integer> multiplicities) throws Exception {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("ATTR {} {} {}", path, localName, value);
        }

        String newValue = value;

        if (!isFeatureCollection && localName.equals("schemaLocation")) {
            locations = adjustSchemaLocation(value);
        }

        if (inCurrentStart) {
            if (localName.equals("schemaLocation")) {
                newValue = adjustSchemaLocation(value);
            } else {
                return;
            }
        }
        if (inCurrentPropertyStart && localName.equals("srsName")) {
            if (Objects.nonNull(crsTransformer)) {
                newValue = crsTransformer.getTargetCrs().toUriString();
            }
        }
        if (inCurrentPropertyStart && localName.equals("srsDimension")) {
            try {
                currentDimension = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                currentDimension = null;
            }
        }

        if (isFeatureCollection || inCurrentFeatureStart || inCurrentPropertyStart) {
            writer.append(" ");
            if (!Strings.isNullOrEmpty(namespace)) {
                writer.append(namespace);
                writer.append(":");
            }
            writer.append(localName);
            writer.append("=\"");
            writer.append(XmlEscapers.xmlAttributeEscaper().escape(newValue));
            writer.append("\"");
        }
    }

    @Override
    public void onValue(EncodingAwareContextGml context) {
        try {
            if (inCurrentPropertyStart) {
                writer.append(">");
                inCurrentPropertyStart = false;
            }

            if (inCoordinates) {
                coordinatesTransformerBuilder = ImmutableCoordinatesTransformer.builder();
                coordinatesTransformerBuilder.coordinatesWriter(ImmutableCoordinatesWriterGml.of(writer, Optional.ofNullable(currentDimension).orElse(2)));

                if (crsTransformer != null) {
                    coordinatesTransformerBuilder.crsTransformer(crsTransformer);
                }

                if (currentDimension != null) {
                    coordinatesTransformerBuilder.sourceDimension(currentDimension);
                    coordinatesTransformerBuilder.targetDimension(currentDimension);
                } else {
                    coordinatesTransformerBuilder.sourceDimension(2);
                    coordinatesTransformerBuilder.targetDimension(2);
                }

                if (maxAllowableOffset > 0) {
                    coordinatesTransformerBuilder.maxAllowableOffset(maxAllowableOffset);
                }

                Writer coordinatesWriter = coordinatesTransformerBuilder.build();
                // TODO: coalesce
                coordinatesWriter.write(context.value());
                coordinatesWriter.close();
            } else {
                writer.append(XmlEscapers.xmlContentEscaper().escape(context.value()));
            }

            inCurrentPropertyText = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String adjustSchemaLocation(String schemaLocation) {
        List<String> split = Splitter.on(' ').splitToList(schemaLocation);
        Map<String,String> locations = new LinkedHashMap<>();

        for (int i = 0; i < split.size()-1; i += 2) {
            if (!split.get(i).startsWith("http://www.opengis.net/ogcapi-features-1")) {
                locations.put(split.get(i), split.get(i + 1));
            }
        }

        locations.put("http://www.opengis.net/ogcapi-features-1/1.0/sf", "http://schemas.opengis.net/ogcapi/features/part1/1.0/xml/core-sf.xsd");
        locations.put("http://www.opengis.net/ogcapi-features-1/1.0", "http://schemas.opengis.net/ogcapi/features/part1/1.0/xml/core.xsd");
        locations.put("http://www.w3.org/2005/Atom", "http://schemas.opengis.net/kml/2.3/atom-author-link.xsd");


        return locations.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining(" "));
    }


    private String getLocalName(List<String> path) {
        return path.isEmpty() ? null : getLocalName(path.get(path.size()-1));
    }

    private String getLocalName(String name) {
        return name.substring(name.lastIndexOf(":") + 1);
    }

    private String getNamespaceUri(List<String> path) {
        return path.isEmpty() ? null : getNamespaceUri(path.get(path.size()-1));
    }

    private String getNamespaceUri(String name) {
        return name.substring(0, name.lastIndexOf(":"));
    }

    @Override
    public Class<? extends EncodingAwareContextGml> getContextInterface() {
        return EncodingAwareContextGml.class;
    }

    @Override
    public EncodingAwareContextGml createContext() {
        return ModifiableEncodingAwareContextGml.create().setEncoding(transformationContext);
    }
}
