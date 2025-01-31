/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Value.Immutable
@JsonDeserialize(builder = ImmutableApiCatalogEntry.Builder.class)
public abstract class ApiCatalogEntry extends PageRepresentation {

    public abstract String getId();

    public abstract URI getLandingPageUri();

    public abstract List<String> getTags();

    public abstract boolean isDataset();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
