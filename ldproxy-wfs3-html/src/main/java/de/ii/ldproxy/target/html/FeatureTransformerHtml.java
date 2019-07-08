/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.codelists.CodelistData;
import de.ii.ldproxy.target.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.ldproxy.target.html.MicrodataMapping.MICRODATA_TYPE;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.dropwizard.views.FallbackMustacheViewRenderer;
import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.OnTheFlyMapping;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import io.dropwizard.views.ViewRenderer;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.AttributeProviderContext;
import org.commonmark.renderer.html.AttributeProviderFactory;
import org.commonmark.renderer.html.CoreHtmlNodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlNodeRendererFactory;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class FeatureTransformerHtml implements FeatureTransformer, FeatureTransformer.OnTheFly {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerHtml.class);

    private OutputStreamWriter outputStreamWriter;
    protected XMLPathTracker currentPath;
    protected FeatureDTO currentFeature;
    protected String outputFormat; // as constant somewhere
    protected FeatureTypeMapping featureTypeMapping; // reduceToOutputFormat
    protected boolean isFeatureCollection;
    protected boolean isAddress;
    protected List<String> groupings;
    protected boolean isGrouped;
    //protected String query;
    protected MustacheFactory mustacheFactory;
    protected ViewRenderer mustacheRenderer;
    protected int page;
    protected int pageSize;
    protected CrsTransformer crsTransformer;
    //protected SparqlAdapter sparqlAdapter;
    protected Codelist[] codelists;

    //public String title;
    //public List<FeatureDTO> features;
    //public List<NavigationDTO> breadCrumbs;
    //public List<NavigationDTO> pagination;
    //public List<NavigationDTO> formats;
    public FeatureCollectionView dataset;
    //public String requestUrl;

    private String serviceUrl;

    MICRODATA_GEOMETRY_TYPE currentGeometryType;
    boolean currentGeometryNested;
    CoordinatesWriterType.Builder cwBuilder;
    TargetMapping currentMapping;
    StringBuilder currentValue = new StringBuilder();
    private Writer coordinatesWriter;
    private Writer coordinatesOutput;
    private FeaturePropertyDTO currentGeometryPart;
    private int currentGeometryParts;
    private String currentFormatter;

    public FeatureTransformerHtml(OutputStreamWriter outputStreamWriter, boolean isFeatureCollection, CrsTransformer crsTransformer, int page, int pageSize, FeatureCollectionView featureTypeDataset, Codelist[] codelists, ViewRenderer mustacheRenderer, String serviceUrl) {
        this.outputStreamWriter = outputStreamWriter;
        this.currentPath = new XMLPathTracker();
        this.featureTypeMapping = featureTypeMapping;
        this.outputFormat = outputFormat;
        this.isFeatureCollection = isFeatureCollection;
        this.isAddress = false;//isAddress;
        this.groupings = new ArrayList<>();//groupings;
        this.isGrouped = false;//isGrouped;
        //this.query = query;
        this.page = page;
        this.pageSize = pageSize;
        this.mustacheFactory = new DefaultMustacheFactory() {
            @Override
            public Reader getReader(String resourceName) {
                final InputStream is = getClass().getResourceAsStream(resourceName);
                if (is == null) {
                    throw new MustacheException("Template " + resourceName + " not found");
                }
                return new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            }

            @Override
            public void encode(String value, Writer writer) {
                try {
                    writer.write(value);
                } catch (IOException e) {
                    // ignore
                }
            }
        };
        this.crsTransformer = crsTransformer;

        this.dataset = featureTypeDataset;

        /*try {
            URIBuilder urlBuilder = new URIBuilder(dataset.requestUrl);
            urlBuilder.clearParameters();
            this.wfsUrl = urlBuilder.build().toString();
            this.wfsByIdUrl = urlBuilder.addParameter("SERVICE", "WFS").addParameter("VERSION", "2.0.0").addParameter("REQUEST", "GetFeature").addParameter("STOREDQUERY_ID", "urn:ogc:def:query:OGC-WFS::GetFeatureById").addParameter("ID", "").build().toString();
        } catch (URISyntaxException e) {
            //ignore
        }*/
        this.serviceUrl = serviceUrl;

        //this.sparqlAdapter = null;//sparqlAdapter;
        this.codelists = codelists;
        this.mustacheRenderer = mustacheRenderer;
    }

    @Override
    public String getTargetFormat() {
        return Gml2MicrodataMappingProvider.MIME_TYPE;
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {

        LOGGER.debug("START");

        if (isFeatureCollection && numberReturned.isPresent()) {
            long returned = numberReturned.getAsLong();
            long matched = numberMatched.orElse(-1);

            long pages = Math.max(page, 0);
            if (returned > 0 && matched > -1) {
                pages = Math.max(pages, matched / pageSize + (matched % pageSize > 0 ? 1 : 0));
            }

            LOGGER.debug("numberMatched {}", matched);
            LOGGER.debug("numberReturned {}", returned);
            LOGGER.debug("pageSize {}", pageSize);
            LOGGER.debug("page {}", page);
            LOGGER.debug("pages {}", pages);

            ImmutableList.Builder<NavigationDTO> pagination = new ImmutableList.Builder<>();
            ImmutableList.Builder<NavigationDTO> metaPagination = new ImmutableList.Builder<>();
            if (page > 1) {
                pagination
                        .add(new NavigationDTO("«", "page=1"))
                        .add(new NavigationDTO("‹", "page=" + String.valueOf(page - 1)));
                metaPagination
                        .add(new NavigationDTO("prev", "page=" + String.valueOf(page - 1)));
            } else {
                pagination
                        .add(new NavigationDTO("«"))
                        .add(new NavigationDTO("‹"));
            }

            if (matched > -1) {
                long from = Math.max(1, page - 2);
                long to = Math.min(pages, from + 4);
                if (to == pages) {
                    from = Math.max(1, to - 4);
                }
                for (long i = from; i <= to; i++) {
                    if (i == page) {
                        pagination.add(new NavigationDTO(String.valueOf(i), true));
                    } else {
                        pagination.add(new NavigationDTO(String.valueOf(i), "page=" + String.valueOf(i)));
                    }
                }

                if (page < pages) {
                    pagination
                            .add(new NavigationDTO("›", "page=" + String.valueOf(page + 1)))
                            .add(new NavigationDTO("»", "page=" + String.valueOf(pages)));
                    metaPagination
                            .add(new NavigationDTO("next", "page=" + String.valueOf(page + 1)));
                } else {
                    pagination
                            .add(new NavigationDTO("›"))
                            .add(new NavigationDTO("»"));
                }
            } else {
                int from = Math.max(1, page - 2);
                int to = page;
                for (int i = from; i <= to; i++) {
                    if (i == page) {
                        pagination.add(new NavigationDTO(String.valueOf(i), true));
                    } else {
                        pagination.add(new NavigationDTO(String.valueOf(i), "page=" + String.valueOf(i)));
                    }
                }
                if (returned >= pageSize) {
                    pagination
                            .add(new NavigationDTO("›", "page=" + String.valueOf(page + 1)));
                    metaPagination
                            .add(new NavigationDTO("next", "page=" + String.valueOf(page + 1)));
                } else {
                    pagination
                            .add(new NavigationDTO("›"));
                }
            }

            this.dataset.pagination = pagination.build();
            this.dataset.metaPagination = metaPagination.build();

        } else if (isFeatureCollection) {
            //analyzeFailed(ex);
            LOGGER.error("Pagination not supported by feature provider");
        }
    }

    @Override
    public void onEnd() throws Exception {

            /*Mustache mustache;
            if (isFeatureCollection) {
                mustache = mustacheFactory.compile("featureCollection.mustache");
            } else {
                mustache = mustacheFactory.compile("featureDetails.mustache");
            }
            mustache.execute(outputStreamWriter, dataset).flush();*/
        ((FallbackMustacheViewRenderer) mustacheRenderer).render(dataset, outputStreamWriter);
        outputStreamWriter.flush();
    }

    @Override
    public void onFeatureStart(TargetMapping mapping) throws Exception {
        currentFeature = new FeatureDTO();
        if (!isFeatureCollection) {
            currentFeature.idAsUrl = true;
        }

        currentFeature.name = mapping.getName();
        currentFeature.itemType = ((MicrodataPropertyMapping) mapping).getItemType();
        currentFeature.itemProp = ((MicrodataPropertyMapping) mapping).getItemProp();
    }

    @Override
    public void onFeatureEnd() throws Exception {
        /*try {
            if (!isGrouped) {
                outputStreamWriter.write("</ul>");
                outputStreamWriter.write("</div>");
            }
            if (isFeatureCollection || isGrouped) {
                outputStreamWriter.write("</li>");
            }

        } catch (IOException e) {
            analyzeFailed(e);
        }*/
        if (currentFeature.name != null)
            currentFeature.name = currentFeature.name.replaceAll("\\{\\{[^}]*\\}\\}", "");
        if (!isFeatureCollection) {
            this.dataset.title = currentFeature.name;
            this.dataset.breadCrumbs.get(dataset.breadCrumbs.size() - 1).label = currentFeature.name;
        }
        dataset.features.add(currentFeature);
        currentFeature = null;
    }

    @Override
    public void onPropertyStart(TargetMapping mapping, List<Integer> multiplicities) throws Exception {
        currentMapping = mapping;
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        currentValue.append(text);
    }

    @Override
    public void onPropertyEnd() throws Exception {
        if (currentValue.length() > 0) {
            writeField((MicrodataPropertyMapping) currentMapping, currentValue.toString());
            currentValue.setLength(0);
        }
    }

    protected void writeField(MicrodataPropertyMapping mapping, String value) {

        /*if (value == null || value.isEmpty()) {
            return;
        }*/
        if (value == null) {
            value = "";
        }

        if (mapping.getType() == MICRODATA_TYPE.ID) {
            currentFeature.id = new FeaturePropertyDTO();
            currentFeature.id.value = value;
            currentFeature.id.itemProp = "url";
            if (!isFeatureCollection) {
                this.dataset.title = value;
            }
            if (currentFeature.name == null || currentFeature.name.isEmpty()) {
                currentFeature.name = value;
            }

            if (!isFeatureCollection || mapping.isShowInCollection()) {
                FeaturePropertyDTO property = new FeaturePropertyDTO();
                property.name = mapping.getName();
                property.value = value;

                currentFeature.addChild(property);
            }
        } else {
            // TODO: better way to de/serialize

            if (mapping.getItemProp() != null && !mapping.getItemProp()
                                                         .isEmpty()) {
                String[] path = mapping.getItemProp()
                                       .split("::");

                FeaturePropertyDTO lastProperty = null;

                for (int i = 0; i < path.length; i++) {
                    String itemProp = path[i];
                    String itemType = mapping.getItemType();
                    String prefix = "";

                    if (itemProp.contains("[")) {
                        String[] p = itemProp.split("\\[");
                        itemProp = p[0];
                        String[] props = p[1].split("=");
                        if (props[0].equals("itemType")) {
                            itemType = p[1].split("=")[1];
                            itemType = itemType.substring(0, itemType.indexOf(']'));
                        } else if (props[0].equals("prefix")) {
                            prefix = props[1].substring(0, props[1].length() - 1);
                        }
                    }//"itemProp": "address[itemType=http://schema.org/PostalAddress]::streetAddress"

                    FeaturePropertyDTO currentProperty = null;
                    boolean knownProperty = false;

                    if (i == 0) {
                        for (FeaturePropertyDTO p : currentFeature.childList) {
                            if (p != null && p.itemProp != null && (p.itemProp.equals(itemProp) || p.name.equals(mapping.getName()))) {
                                currentProperty = p;
                                knownProperty = true;
                                break;
                            }
                        }
                    } else if (lastProperty != null) {
                        for (FeaturePropertyDTO p : lastProperty.childList) {
                            if (p != null && (p.itemProp.equals(itemProp) || p.name.equals(mapping.getName()))) {
                                currentProperty = p;
                                knownProperty = true;
                                break;
                            }
                        }
                    }

                    if (currentProperty == null) {
                        currentProperty = new FeaturePropertyDTO();
                        currentProperty.itemProp = itemProp;
                        currentProperty.itemType = itemType;

                        if (i == 0 && !knownProperty) {
                            currentFeature.addChild(currentProperty);
                        }
                    }


                    if (i == path.length - 1) {
                        currentProperty.name = mapping.getName();
                        if (currentProperty.value != null) {
                            currentProperty.value += prefix + value;
                        } else {
                            currentProperty.value = value;
                        }

                        int pos = currentFeature.name.indexOf("{{" + currentProperty.name + "}}");
                        if (pos > -1) {
                            currentFeature.name = currentFeature.name.substring(0, pos) + prefix + value + currentFeature.name.substring(pos);
                        }

                        // TODO
                        /*if (currentProperty.name.equals("postalCode") && !isFeatureCollection) {
                            Map<String, String> reports = sparqlAdapter.request(currentProperty.value, SparqlAdapter.QUERY.POSTAL_CODE_EXACT);
                            if (!reports.isEmpty()) {
                                currentFeature.links = new FeaturePropertyDTO();
                                currentFeature.links.name = "announcements";
                                for (Map.Entry<String, String> id : reports.entrySet()) {
                                    FeaturePropertyDTO link = new FeaturePropertyDTO();
                                    link.value = id.getKey();
                                    link.name = id.getValue() + " (" + id.getKey()
                                                                         .substring(id.getKey()
                                                                                      .lastIndexOf('/') + 1) + ")";
                                    currentFeature.links.addChild(link);
                                }
                            }
                        }*/
                    }

                    if (lastProperty != null && !knownProperty) {
                        lastProperty.addChild(currentProperty);
                    }

                    lastProperty = currentProperty;
                }
            } else {
                FeaturePropertyDTO property = new FeaturePropertyDTO();
                property.name = mapping.getName();
                property.value = value;
                property.itemType = mapping.getItemType();
                property.itemProp = mapping.getItemProp();


                if (mapping.getCodelist() != null) {
                    //TODO: read into map in Wfs3OutputFormatHtml with @Bind(aggregate=true)
                    //  private void bindHello(Hello h) { m_hellos.add(h); }

                    property.value = Arrays.stream(codelists)
                                           .filter(cl -> cl.getId()
                                                           .equals(mapping.getCodelist()))
                                           .findFirst()
                                           .map(cl -> {
                                               String resolvedValue = cl.getValue(property.value);

                                               if (cl.getData().getSourceType() == CodelistData.IMPORT_TYPE.TEMPLATES) {
                                                   resolvedValue = applyFilterMarkdown(applyTemplate(property, resolvedValue));
                                                   property.isHtml = true;
                                               }

                                               return resolvedValue;
                                           })
                                           .orElse(property.value);
                }

                if (mapping.getType() == MICRODATA_TYPE.DATE) {
                    try {
                        DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss][.SSS][X]");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mapping.getFormat());
                        TemporalAccessor ta = parser.parseBest(value, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
                        property.value = formatter.format(ta);
                    } catch (Exception e) {
                        //ignore
                    }
                } else if (mapping.getType() == MICRODATA_TYPE.STRING && mapping.getFormat() != null && !mapping.getFormat()
                                                                                                                .isEmpty()) {
                    boolean more = false;
                    if (currentFormatter == null) {

                        String formattedValue = applyTemplate(property, mapping.getFormat());

                        property.value = formattedValue
                                .replace("{{serviceUrl}}", serviceUrl);
                        int subst = property.value.indexOf("}}");
                        if (subst > -1) {
                            property.value = property.value.substring(0, property.value.indexOf("{{")) + value + property.value.substring(subst + 2);
                            more = property.value.contains("}}");
                        }
                    } else {
                        int subst = currentFormatter.indexOf("}}");
                        if (subst > -1) {
                            property.value = currentFormatter.substring(0, currentFormatter.indexOf("{{")) + value + currentFormatter.substring(subst + 2);
                            more = property.value.contains("}}");
                        }
                    }
                    if (more) {
                        this.currentFormatter = property.value;
                        return;
                    } else {
                        currentFormatter = null;
                    }
                }
                if (property.value.startsWith("http://") || property.value.startsWith("https://")) {
                    if (property.value.toLowerCase()
                                      .endsWith(".png") || property.value.toLowerCase()
                                                                         .endsWith(".jpg") || property.value.toLowerCase()
                                                                                                            .endsWith(".gif")) {
                        property.isImg = true;
                    } else {
                        property.isUrl = true;
                    }
                }

                currentFeature.addChild(property);

                int pos = currentFeature.name.indexOf("{{" + property.name + "}}");
                if (pos > -1) {
                    currentFeature.name = currentFeature.name.substring(0, pos) + property.value + currentFeature.name.substring(pos);
                }

                // TODO
                /*if (property.name.equals("postalCode") && !isFeatureCollection) {
                    Map<String, String> reports = sparqlAdapter.request(property.value, SparqlAdapter.QUERY.POSTAL_CODE_EXACT);
                    if (!reports.isEmpty()) {
                        currentFeature.links = new FeaturePropertyDTO();
                        currentFeature.links.name = "announcements";
                        for (Map.Entry<String, String> id : reports.entrySet()) {
                            FeaturePropertyDTO link = new FeaturePropertyDTO();
                            link.value = id.getKey();
                            link.name = id.getValue() + " (" + id.getKey()
                                                                 .substring(id.getKey()
                                                                              .lastIndexOf('/') + 1) + ")";
                            currentFeature.links.addChild(link);
                        }
                    }
                }*/
            }
        }
    }

    static final Set<Extension> EXTENSIONS = Collections.singleton(TablesExtension.create());
    static final Parser parser = Parser.builder()
                                       .extensions(EXTENSIONS)
                                       .build();

    static final HtmlRenderer renderer = HtmlRenderer.builder()
                                                     .extensions(EXTENSIONS)
                                                     .nodeRendererFactory(context -> new CoreHtmlNodeRenderer(context) {
                                                         @Override
                                                         public void visit(Paragraph paragraph) {
                                                             this.visitChildren(paragraph);
                                                         }
                                                     })
                                                     .attributeProviderFactory(context -> (node, tagName, attributes) -> {
                                                         if (node instanceof Link) {
                                                             attributes.put("target", "_blank");
                                                         }
                                                     })
                                                     .build();

    static String applyFilterMarkdown(String value) {

        Node document = parser.parse(value);
        return renderer.render(document);
    }

    static String applyTemplate(FeaturePropertyDTO property, String template) {
        Pattern valuePattern = Pattern.compile("\\{\\{value( ?\\| ?[\\w]+(:'[^']*')*)*\\}\\}");
        Pattern filterPattern = Pattern.compile(" ?\\| ?([\\w]+)((?::'[^']*')*)");

        String formattedValue = "";
        Matcher matcher = valuePattern.matcher(template);

        int lastMatch = 0;
        while (matcher.find()) {
            String filteredValue = property.value;
            Matcher matcher2 = filterPattern.matcher(template.substring(matcher.start(), matcher.end()));
            while (matcher2.find()) {
                String filter = matcher2.group(1);
                List<String> parameters = matcher2.groupCount() < 2
                        ? ImmutableList.of()
                        : Splitter.on(':')
                                  .omitEmptyStrings()
                                  .splitToList(matcher2.group(2))
                                  .stream()
                                  .map(s -> s.substring(1, s.length() - 1))
                                  .collect(Collectors.toList());

                if (filter.equals("markdown")) {
                    filteredValue = applyFilterMarkdown(filteredValue);
                    property.isHtml = true;
                } else if (filter.equals("replace") && parameters.size() >= 2) {
                    filteredValue = filteredValue.replaceAll(parameters.get(0), parameters.get(1));
                } else if (filter.equals("prepend") && parameters.size() >= 1) {
                    filteredValue = parameters.get(0) + filteredValue;
                } else if (filter.equals("append") && parameters.size() >= 1) {
                    filteredValue = filteredValue + parameters.get(0);
                } else if (filter.equals("urlencode")) {
                    try {
                        filteredValue = URLEncoder.encode(filteredValue, Charsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        //ignore
                    }
                } else {
                    LOGGER.warn("Template filter '{}' not supported", filter);
                }
            }
            //formattedValue = formattedValue.substring(lastMatch, matcher.start()) + filteredValue + formattedValue.substring(matcher.end());
            //lastMatch = matcher.start();
            formattedValue += template.substring(lastMatch, matcher.start()) + filteredValue;
            lastMatch = matcher.end();
        }
        formattedValue += template.substring(lastMatch);

        return formattedValue;
    }


    @Override
    public void onGeometryStart(TargetMapping mapping, SimpleFeatureGeometry type, Integer dimension) throws Exception {
        if (Objects.isNull(mapping)) return;

        final MicrodataGeometryMapping geometryMapping = (MicrodataGeometryMapping) mapping;
        if (isFeatureCollection && !((MicrodataGeometryMapping) mapping).isShowInCollection()) return;

        currentGeometryType = geometryMapping.getGeometryType();
        if (currentGeometryType == MICRODATA_GEOMETRY_TYPE.GENERIC) {
            currentGeometryType = MICRODATA_GEOMETRY_TYPE.forGmlType(type);
        }

        coordinatesOutput = new StringWriter();
        coordinatesWriter = new HtmlTransformingCoordinatesWriter(coordinatesOutput, Objects.nonNull(dimension) ? dimension : 2, crsTransformer);

        currentGeometryParts = 0;
    }

    @Override
    public void onGeometryNestedStart() throws Exception {
        if (currentGeometryType == null) return;

        currentGeometryParts++;
        if (currentGeometryParts == 1) {
            currentFeature.geo = new FeaturePropertyDTO();
            currentFeature.geo.itemType = "http://schema.org/GeoShape";
            currentFeature.geo.itemProp = "geo";
            currentFeature.geo.name = "geometry";

            currentGeometryPart = new FeaturePropertyDTO();
            currentFeature.geo.addChild(currentGeometryPart);

            switch (currentGeometryType) {
                case LINE_STRING:
                    currentGeometryPart.itemProp = "line";
                    break;
                case POLYGON:
                    currentGeometryPart.itemProp = "polygon";
                    break;
            }
        }
    }

    @Override
    public void onGeometryCoordinates(String text) throws Exception {
        if (currentGeometryType == null) return;

        switch (currentGeometryType) {
            case POINT:
                currentFeature.geo = new FeaturePropertyDTO();
                currentFeature.geo.itemType = "http://schema.org/GeoCoordinates";
                currentFeature.geo.itemProp = "geo";
                currentFeature.geo.name = "geometry";

                String[] coordinates = text.split(" ");
                CoordinateTuple point = new CoordinateTuple(coordinates[0], coordinates[1]);
                if (crsTransformer != null) {
                    point = crsTransformer.transform(point);
                }

                FeaturePropertyDTO longitude = new FeaturePropertyDTO();
                longitude.name = "longitude";
                longitude.itemProp = "longitude";
                longitude.value = point.getXasString();

                FeaturePropertyDTO latitude = new FeaturePropertyDTO();
                latitude.name = "latitude";
                latitude.itemProp = "latitude";
                latitude.value = point.getYasString();

                currentFeature.geo.addChild(latitude);
                currentFeature.geo.addChild(longitude);

                break;
            case LINE_STRING:
            case POLYGON:
                if (currentGeometryParts == 1) {
                    try {
                        coordinatesWriter.append(text);
                        coordinatesWriter.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
                break;
        }
    }

    @Override
    public void onGeometryNestedEnd() throws Exception {
        if (currentGeometryType == null) return;
    }

    @Override
    public void onGeometryEnd() throws Exception {
        if (currentGeometryType == null) return;

        if (currentGeometryPart != null) {
            currentGeometryPart.value = coordinatesOutput.toString();
        }

        currentGeometryType = null;
    }

    @Override
    public OnTheFlyMapping getOnTheFlyMapping() {
        return new OnTheFlyMappingHtml();
    }

}
