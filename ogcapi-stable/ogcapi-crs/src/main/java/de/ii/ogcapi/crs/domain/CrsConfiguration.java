/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCrsConfiguration.Builder.class)
public interface CrsConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    Set<EpsgCrs> getAdditionalCrs();

    @Override
    default Builder getBuilder() {
        return new ImmutableCrsConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableCrsConfiguration.Builder builder = getBuilder().from(source)
                                                                .from(this);

        getAdditionalCrs().forEach(epsgCrs -> {
            if (!((CrsConfiguration) source).getAdditionalCrs()
                                            .contains(epsgCrs)) {
                builder.addAdditionalCrs(epsgCrs);
            }
        });

        return builder.build();
    }
}
