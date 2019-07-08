/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.base.Strings;
import de.ii.xsf.cfgstore.api.BundleConfigDefault;
import de.ii.xsf.cfgstore.api.ConfigPropertyDescriptor;
import de.ii.xsf.cfgstore.api.handler.LocalBundleConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import static de.ii.ldproxy.target.geojson.GeoJsonConfig.ENABLED;
import static de.ii.ldproxy.target.geojson.GeoJsonConfig.MULTIPLICITY;
import static de.ii.ldproxy.target.geojson.GeoJsonConfig.NESTED_OBJECTS;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {GeoJsonConfig.class})
@Instantiate
@LocalBundleConfig(bundleId = "ldproxy-target-geojson", category = "GeoJson Output Format", properties = {
        @ConfigPropertyDescriptor(name = ENABLED, label = "Enable GeoJson output format?", defaultValue = "true"),
        @ConfigPropertyDescriptor(name = NESTED_OBJECTS, label = "How to format nested objects?", defaultValue = "NEST"),
        @ConfigPropertyDescriptor(name = MULTIPLICITY, label = "How to format multiple values?", defaultValue = "ARRAY")
})
public class GeoJsonConfig extends BundleConfigDefault {

    static final String ENABLED = "enabled";
    static final String NESTED_OBJECTS = "nestedObjects";
    static final String MULTIPLICITY = "multiplicity";

    public boolean isEnabled() {
        return Strings.nullToEmpty(properties.get(ENABLED)).toLowerCase().equals("true");
    }

    public FeatureTransformerGeoJson.NESTED_OBJECTS getNestedObjectStrategy() {
        FeatureTransformerGeoJson.NESTED_OBJECTS nestedObjects;
        try {
            nestedObjects = FeatureTransformerGeoJson.NESTED_OBJECTS.valueOf(Strings.nullToEmpty(properties.get(NESTED_OBJECTS)));
        } catch (IllegalArgumentException e) {
            nestedObjects = FeatureTransformerGeoJson.NESTED_OBJECTS.NEST;
        }

        return nestedObjects;
    }

    public FeatureTransformerGeoJson.MULTIPLICITY getMultiplicityStrategy() {
        FeatureTransformerGeoJson.MULTIPLICITY multiplicity;
        try {
            multiplicity = FeatureTransformerGeoJson.MULTIPLICITY.valueOf(Strings.nullToEmpty(properties.get(MULTIPLICITY)));
        } catch (IllegalArgumentException e) {
            multiplicity = FeatureTransformerGeoJson.MULTIPLICITY.ARRAY;
        }

        return multiplicity;
    }
}
