/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.base.Splitter;
import de.ii.ogcapi.foundation.infra.json.SchemaValidatorImpl;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AutoMultiBind
public interface ParameterExtension extends ApiExtension {

    Schema SCHEMA = new StringSchema();

    default String getId() { return getName(); }
    default String getId(String collectionId) { return getId(); }
    default String getId(Optional<String> collectionId) {
        return collectionId.isPresent() ? getId(collectionId.get()) : getId();
    }
    String getName();
    String getDescription();
    default boolean getRequired(OgcApiDataV2 apiData) { return false; }
    default boolean getRequired(OgcApiDataV2 apiData, String collectionId) { return getRequired(apiData); }
    default boolean getRequired(OgcApiDataV2 apiData, Optional<String> collectionId) {
        return collectionId.isPresent() ? getRequired(apiData,collectionId.get()) : getRequired(apiData);
    }
    default Schema getSchema(OgcApiDataV2 apiData) { return SCHEMA; }
    default Schema getSchema(OgcApiDataV2 apiData, String collectionId) { return getSchema(apiData); }
    default Schema getSchema(OgcApiDataV2 apiData, Optional<String> collectionId) {
        return collectionId.isPresent() ? getSchema(apiData,collectionId.get()) : getSchema(apiData);
    }
    default boolean getExplode() { return false; }

    default Optional<String> validate(OgcApiDataV2 apiData, Optional<String> collectionId, List<String> values) {
        // first validate against the schema
        Optional<String> result = validateSchema(apiData, collectionId, values);
        if (result.isPresent())
            return result;
        // if the values are schema-valid, validate against any additional parameter-specific checks
        return validateOther(apiData, collectionId, values);
    }

    default Optional<String> validateOther(OgcApiDataV2 apiData, Optional<String> collectionId, List<String> values) {
        return Optional.empty();
    }

    default Optional<String> validateSchema(OgcApiDataV2 apiData, Optional<String> collectionId, List<String> values) {
        try {
            SchemaValidator validator = new SchemaValidatorImpl();
            String schemaContent = Json.mapper().writeValueAsString(getSchema(apiData, collectionId));
            Optional<String> result1 = Optional.empty();
            if (values.size()==1) {
                // try non-array variant first
                result1 = validator.validate(schemaContent, "\""+values.get(0)+"\"");
                if (!result1.isPresent())
                    return Optional.empty();
                if (!getExplode() && values.get(0).contains(",")) {
                    values = Splitter.on(",")
                            .trimResults()
                            .omitEmptyStrings()
                            .splitToList(values.get(0));
                }
            }
            Optional<String> resultn = validator.validate(schemaContent, "[\"" + String.join("\",\"", values) + "\"]");
            if (resultn.isPresent()) {
                if (result1.isPresent())
                    return Optional.of(String.format("Parameter value '%s' is invalid for parameter '%s': %s", values, getName(), result1.get()));
                else
                    return Optional.of(String.format("Parameter value '%s' is invalid for parameter '%s': %s", values, getName(), resultn.get()));
            }
        } catch (IOException e) {
            // TODO log an error
            return Optional.of(String.format("An exception occurred while validating the parameter value '%s' for parameter '%s'", values, getName()));
        }

        return Optional.empty();
    };

    default Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                    Map<String, String> parameters,
                                                    OgcApiDataV2 apiData) {
        return parameters;
    }

    default ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureType,
                                                         ImmutableFeatureQuery.Builder queryBuilder,
                                                         Map<String, String> parameters,
                                                         OgcApiDataV2 apiData) {
        return queryBuilder;
    }

    default ImmutableFeatureQuery.Builder transformQuery(ImmutableFeatureQuery.Builder queryBuilder,
                                                         Map<String, String> parameters,
                                                         OgcApiDataV2 apiData) {
        return queryBuilder;
    }

    default Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                 Map<String, Object> context,
                                                 Map<String, String> parameters,
                                                 OgcApiDataV2 apiData) {
        return context;
    }
}
