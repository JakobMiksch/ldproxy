/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableStylesConfiguration.Builder.class)
public interface StylesConfiguration extends ExtensionConfiguration, CachingConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    List<String> getStyleEncodings();

    @Nullable
    Boolean getManagerEnabled();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isManagerEnabled() { return Objects.equals(getManagerEnabled(), true); }

    @Nullable
    Boolean getValidationEnabled();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isValidationEnabled() { return Objects.equals(getValidationEnabled(), true); }

    @Nullable
    Boolean getUseIdFromStylesheet();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean shouldUseIdFromStylesheet() { return Objects.equals(getUseIdFromStylesheet(), true); }

    @Deprecated
    @Nullable
    Boolean getResourcesEnabled();

    @Deprecated
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isResourcesEnabled() { return Objects.equals(getResourcesEnabled(), true); }

    @Deprecated
    @Nullable
    Boolean getResourceManagerEnabled();

    @Deprecated
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isResourceManagerEnabled() { return Objects.equals(getResourceManagerEnabled(), true); }

    @Deprecated(since = "3.1.0")
    @Nullable
    String getDefaultStyle();

    @Nullable
    Boolean getDeriveCollectionStyles();

    @Nullable
    Boolean getWebmapWithPopup();

    @Nullable
    Boolean getWebmapWithLayerControl();

    @Nullable
    Boolean getLayerControlAllLayers();

    @Override
    default Builder getBuilder() {
        return new ImmutableStylesConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableStylesConfiguration.Builder builder = ((ImmutableStylesConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        List<String> styleEncodings = Lists.newArrayList(((StylesConfiguration) source).getStyleEncodings());
        getStyleEncodings().forEach(styleEncoding -> {
            if (!styleEncodings.contains(styleEncoding)) {
                styleEncodings.add(styleEncoding);
            }
        });
        builder.styleEncodings(styleEncodings);

        return builder.build();
    }
}