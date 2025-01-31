/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain.processing;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.Example;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = ImmutableProcessDocumentation.Builder.class)
public abstract class ProcessDocumentation {

    public abstract Optional<String> getSummary();
    public abstract Optional<String> getDescription();
    public abstract Optional<ExternalDocumentation> getExternalDocs();
    public abstract Map<String, List<Example>> getExamples();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
