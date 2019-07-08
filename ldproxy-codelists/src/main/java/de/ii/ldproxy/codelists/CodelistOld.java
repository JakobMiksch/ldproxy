/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import de.ii.ldproxy.codelists.CodelistStore.IMPORT_TYPE;
import de.ii.xsf.core.api.Resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class CodelistOld implements Resource {
    private String id;
    private String name;
    private String description;
    private String sourceUrl;
    private IMPORT_TYPE sourceType;
    private Map<String, String> entries;

    public CodelistOld() {
        this.entries = new LinkedHashMap<>();
    }

    public CodelistOld(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    public CodelistOld(String sourceUrl, IMPORT_TYPE sourceType) {
        this();
        this.sourceUrl = sourceUrl;
        this.sourceType = sourceType;
    }

    @Override
    public String getResourceId() {
        return id;
    }

    @Override
    public void setResourceId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public IMPORT_TYPE getSourceType() {
        return sourceType;
    }

    public void setSourceType(IMPORT_TYPE sourceType) {
        this.sourceType = sourceType;
    }

    public Map<String, String> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, String> entries) {
        this.entries = entries;
    }
}
