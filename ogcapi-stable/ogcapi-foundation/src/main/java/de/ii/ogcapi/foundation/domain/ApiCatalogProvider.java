/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.google.common.base.Splitter;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.services.domain.ServiceListingProvider;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ApiCatalogProvider implements ServiceListingProvider, ApiExtension {

    protected final URI servicesUri;
    protected final I18n i18n;
    protected final EntityDataDefaultsStore defaultsStore;
    protected final ExtensionRegistry extensionRegistry;

    public ApiCatalogProvider(ServicesContext servicesContext,
                              I18n i18n,
                              EntityDataDefaultsStore defaultsStore,
                              ExtensionRegistry extensionRegistry) {
        this.servicesUri = servicesContext.getUri();
        this.i18n = i18n;
        this.defaultsStore = defaultsStore;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Response getServiceListing(List<ServiceData> apis, URI uri) {
        try {
            return getServiceListing(apis, uri, Optional.of(Locale.ENGLISH));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not generate service overview.", e);
        }
    }

    public abstract Response getServiceListing(List<ServiceData> services, URI uri, Optional<Locale> language) throws URISyntaxException;

    public abstract ApiMediaType getApiMediaType();

    private Optional<URI> getExternalUri() {
        return Optional.of(servicesUri);
    }

    private void customizeUri(final URICustomizer uriCustomizer) {
        if (getExternalUri().isPresent()) {
            uriCustomizer.setScheme(getExternalUri().get()
                                                    .getScheme());
            uriCustomizer.replaceInPath("/rest/services", getExternalUri().get()
                                                                          .getPath());
            uriCustomizer.ensureNoTrailingSlash();
        }
    }

    private String getStaticUrlPrefix(final URICustomizer uriCustomizer) {
        if (getExternalUri().isPresent()) {
            return uriCustomizer.copy()
                                .cutPathAfterSegments("rest", "services")
                                .replaceInPath("/rest/services", getExternalUri().get()
                                                                                 .getPath())
                                .ensureLastPathSegment("___static___")
                                .ensureNoTrailingSlash()
                                .getPath();
        }

        return "";
    }

    private FoundationConfiguration getConfig() {
        //TODO: encapsulate in entities/defaults layer
        EntityDataBuilder<?> builder = defaultsStore.getBuilder(Identifier.from(EntityDataDefaultsStore.EVENT_TYPE, Service.TYPE, OgcApiDataV2.SERVICE_TYPE.toLowerCase()));
        if (builder instanceof ImmutableOgcApiDataV2.Builder) {
            ImmutableOgcApiDataV2 defaults = ((ImmutableOgcApiDataV2.Builder) builder).id("NOT_SET")
                                                                                      .build();
            return defaults.getExtension(FoundationConfiguration.class)
                           .orElse(new ImmutableFoundationConfiguration.Builder().build());
        }
        return new ImmutableFoundationConfiguration.Builder().build();
    }

    protected URI getApiUrl(URI uri, String apiId, Optional<Integer> apiVersion) throws URISyntaxException {
        return apiVersion.isPresent() ?
            new URICustomizer(uri).clearParameters().ensureLastPathSegments(apiId, "v"+apiVersion.get()).ensureNoTrailingSlash().build() :
            new URICustomizer(uri).clearParameters().ensureLastPathSegment(apiId).ensureNoTrailingSlash().build();
    }

    protected URI getApiUrl(URI uri, List<String> subPathToLandingPage) throws URISyntaxException {
        return new URICustomizer(uri).clearParameters()
                                     .ensureLastPathSegments(subPathToLandingPage.toArray(new String[0]))
                                     .ensureNoTrailingSlash()
                                     .build();
    }

    protected ApiCatalog getCatalog(List<ServiceData> services, URI uri, Optional<Locale> language) throws URISyntaxException {
        final DefaultLinksGenerator linksGenerator = new DefaultLinksGenerator();
        URICustomizer uriCustomizer = new URICustomizer(uri);
        String urlPrefix = getStaticUrlPrefix(uriCustomizer);
        List<String> tags = Splitter.on(',')
                                    .omitEmptyStrings()
                                    .trimResults()
                                    .splitToList(uriCustomizer.getQueryParams()
                                                              .stream()
                                                              .filter(param -> param.getName().equals("tags"))
                                                              .map(param -> param.getValue())
                                                              .findAny()
                                                              .orElse(""));
        Optional<String> name = uriCustomizer.getQueryParams()
                                   .stream()
                                   .filter(param -> param.getName().equals("name"))
                                   .map(param -> param.getValue())
                                   .findAny();
        customizeUri(uriCustomizer);
        try {
            uri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            // ignore
        }

        ApiMediaType mediaType = getApiMediaType();
        List<ApiMediaType> alternateMediaTypes = extensionRegistry.getExtensionsForType(ApiCatalogProvider.class)
                                                      .stream()
                                                      .map(ApiCatalogProvider::getApiMediaType)
                                                      .filter(candidate -> !candidate.label().equals(mediaType.label()))
                                                      .collect(Collectors.toList());

        FoundationConfiguration config = getConfig();

        URI finalUri = uri;
        ImmutableApiCatalog.Builder builder = new ImmutableApiCatalog.Builder()
                .title(Objects.requireNonNullElse(config.getApiCatalogLabel(), i18n.get("rootTitle", language)))
                .description(Objects.requireNonNullElse(config.getApiCatalogDescription(), i18n.get("rootDescription", language)))
                .catalogUri(new URICustomizer(finalUri).clearParameters().ensureNoTrailingSlash().build())
                .urlPrefix(urlPrefix)
                .links(linksGenerator.generateLinks(uriCustomizer, mediaType, alternateMediaTypes, i18n, language))
                .apis(services.stream()
                              .sorted(Comparator.comparing(ServiceData::getLabel))
                              .filter(api -> !name.isPresent() || api.getLabel()
                                                                     .toLowerCase(language.orElse(Locale.ENGLISH))
                                                                     .contains(name.get().toLowerCase(language.orElse(Locale.ENGLISH))))
                              .filter(api -> tags.isEmpty() || (api instanceof OgcApiDataV2 && ((OgcApiDataV2) api).getTags()
                                                                                                                   .stream()
                                                                                                                   .anyMatch(tag -> tags.contains(tag))))
                              .map(api -> {
                                  try {
                                      if (api instanceof OgcApiDataV2)
                                          return new ImmutableApiCatalogEntry.Builder()
                                                  .id(api.getId())
                                                  .title(api.getLabel())
                                                  .description(api.getDescription())
                                                  .landingPageUri(getApiUrl(finalUri, ((OgcApiDataV2)api).getSubPath()))
                                                  .tags(((OgcApiDataV2)api).getTags())
                                                  .isDataset(((OgcApiDataV2) api).isDataset())
                                                  .build();
                                      return new ImmutableApiCatalogEntry.Builder()
                                              .id(api.getId())
                                              .title(api.getLabel())
                                              .description(api.getDescription())
                                              .landingPageUri(getApiUrl(finalUri, api.getId(), api.getApiVersion()))
                                              .build();
                                  } catch (URISyntaxException e) {
                                      throw new RuntimeException(String.format("Could not create landing page URI for API '%s'.", api.getId()), e);
                                  }
                              })
                              .collect(Collectors.toList()));

        if (Objects.nonNull(config.getGoogleSiteVerification()))
            builder.googleSiteVerification(config.getGoogleSiteVerification());

        for (ApiCatalogExtension extension : extensionRegistry.getExtensionsForType(ApiCatalogExtension.class)) {
            builder = extension.process(builder,
                                        uriCustomizer.copy(),
                                        mediaType,
                                        alternateMediaTypes,
                                        language);
        }

        return builder.build();
    }
}
