/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.domain;

import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.ApiExtension;
import io.swagger.v3.oas.models.OpenAPI;

public interface OpenApiExtension extends ApiExtension {

    int getSortPriority();

    OpenAPI process(OpenAPI openAPI, OgcApiDataV2 apiData);
}
