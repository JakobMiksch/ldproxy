/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.Link;
import java.util.List;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCollectionsConfiguration.Builder.class)
public interface CollectionsConfiguration extends ExtensionConfiguration, CachingConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {

    }

  @JsonMerge(OptBoolean.FALSE)
    List<Link> getAdditionalLinks();

    Optional<Boolean> getCollectionIdAsParameter();

    Optional<Boolean> getCollectionDefinitionsAreIdentical();

    @Override
    default Builder getBuilder() {
        return new ImmutableCollectionsConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableCollectionsConfiguration.Builder builder = new ImmutableCollectionsConfiguration.Builder()
        .from(source)
        .from(this);

    List<Link> links = Lists.newArrayList(((CollectionsConfiguration) source).getAdditionalLinks());
    getAdditionalLinks().forEach(link -> {
      if (!links.contains(link)) {
        links.add(link);
      }
    });
    builder.additionalLinks(links);

    return builder.build();
    }
}
