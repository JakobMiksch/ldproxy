/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class OgcApiExtentSpatial {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiExtentSpatial.class);
    private double[][] bbox;
    private String crs;

    public OgcApiExtentSpatial(String xmin, String ymin, String xmax, String ymax) {
        double[] bbox1 = ImmutableList.of(xmin, ymin, xmax, ymax)
                .stream()
                .mapToDouble(Double::parseDouble)
                .toArray();
        this.bbox = new double[][]{ bbox1 };
        this.crs = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    }

    public OgcApiExtentSpatial(double xmin, double ymin, double xmax, double ymax) {
        this.bbox = new double[][]{{xmin, ymin, xmax, ymax}};
        this.crs = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    }

    public double[][] getBbox() {
        return bbox;
    }

    public void setBbox(double[][] bbox) {
        this.bbox = bbox;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<OgcApiExtentSpatial> FUNNEL = (from, into) -> {
        into.putString(from.getCrs(), StandardCharsets.UTF_8);
        Arrays.stream(from.getBbox())
              .forEachOrdered(arr -> Arrays.stream(arr)
                                           .forEachOrdered(val -> into.putDouble(val)));
    };
}
