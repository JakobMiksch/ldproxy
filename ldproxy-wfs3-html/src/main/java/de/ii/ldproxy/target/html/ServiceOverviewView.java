/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;

import java.net.URI;

/**
 * @author zahnen
 */
public class ServiceOverviewView extends DatasetView {
    public URI uri;
    public ServiceOverviewView(URI uri, Object data, String urlPrefix, HtmlConfig htmlConfig) {
        super("services", uri, data, urlPrefix, htmlConfig);
        this.uri = uri;
        this.title = "ldproxy Dataset Overview";
        this.description = "ldproxy Dataset Overview";
        this.keywords = new ImmutableList.Builder<String>().add("ldproxy", "service", "dataset", "overview").build();
        this.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", true))
                .build();
    }
}
