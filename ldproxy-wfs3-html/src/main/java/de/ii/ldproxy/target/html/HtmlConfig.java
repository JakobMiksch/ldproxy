/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Strings;
import de.ii.xsf.cfgstore.api.BundleConfigDefault;
import de.ii.xsf.cfgstore.api.ConfigPropertyDescriptor;
import de.ii.xsf.cfgstore.api.handler.LocalBundleConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import static de.ii.ldproxy.target.html.HtmlConfig.*;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {HtmlConfig.class})
@Instantiate
@LocalBundleConfig(category = "HTML Views", properties = {
        @ConfigPropertyDescriptor(name = LEGAL_NAME, label = "Label for legal notice", defaultValue = "Legal notice"),
        @ConfigPropertyDescriptor(name = LEGAL_URL, label = "URL for legal notice", defaultValue = ""),
        @ConfigPropertyDescriptor(name = PRIVACY_NAME, label = "Label for privacy notice", defaultValue = "Privacy notice"),
        @ConfigPropertyDescriptor(name = PRIVACY_URL, label = "URL for privacy notice", defaultValue = ""),
        @ConfigPropertyDescriptor(name = LEAFLET_URL, label = "URL for leaflet background tiles", defaultValue = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"),
        //TODO: single quotes do not work in javascript, but double quotes are obviously not accepted by LocalBundleConfigHandler
        @ConfigPropertyDescriptor(name = LEAFLET_ATTRIBUTION, label = "Attribution for leaflet", defaultValue = "&copy; <a href=''http://osm.org/copyright''>OpenStreetMap</a> contributors"),
        @ConfigPropertyDescriptor(name = FOOTER_TEXT, label = "Text for footer", defaultValue = "")
})
public class HtmlConfig extends BundleConfigDefault {

    static final String LEGAL_NAME = "legalName";
    static final String LEGAL_URL = "legalUrl";
    static final String PRIVACY_NAME = "privacyName";
    static final String PRIVACY_URL = "privacyUrl";
    static final String LEAFLET_URL = "leafletUrl";
    static final String LEAFLET_ATTRIBUTION = "leafletAttribution";
    static final String FOOTER_TEXT = "footerText";

    public String getLegalName() {
        return Strings.nullToEmpty(properties.get(LEGAL_NAME));
    }

    public String getLegalUrl() {
        return Strings.nullToEmpty(properties.get(LEGAL_URL));
    }

    public String getPrivacyName() {
        return Strings.nullToEmpty(properties.get(PRIVACY_NAME));
    }

    public String getPrivacyUrl() {
        return Strings.nullToEmpty(properties.get(PRIVACY_URL));
    }

    public String getLeafletUrl() {
        return Strings.nullToEmpty(properties.get(LEAFLET_URL));
    }

    public String getLeafletAttribution() {
        return Strings.nullToEmpty(properties.get(LEAFLET_ATTRIBUTION));
    }

    public String getFooterText() {
        return Strings.nullToEmpty(properties.get(FOOTER_TEXT));
    }
}