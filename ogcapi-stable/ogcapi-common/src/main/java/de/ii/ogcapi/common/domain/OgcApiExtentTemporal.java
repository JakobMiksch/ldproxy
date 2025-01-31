/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.hash.Funnel;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;

public class OgcApiExtentTemporal {

    private String[][] interval;
    private String trs;

    public OgcApiExtentTemporal(Long begin, Long end) {
        this.interval = new String[][]{{
                (begin!=null) ? Instant.ofEpochMilli(begin).truncatedTo(ChronoUnit.SECONDS).toString() : null,
                (end!=null) ? Instant.ofEpochMilli(end).truncatedTo(ChronoUnit.SECONDS).toString() : null
        }};
        this.trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
    }

    public OgcApiExtentTemporal(String begin, String end) {
        this.interval = new String[][]{{begin, end}};
        this.trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
    }

    public String[][] getInterval() {
        return interval;
    }

    @JsonIgnore
    public String getFirstIntervalIso8601() {
        return String.format("%s/%s",
                             Objects.requireNonNullElse(interval[0][0], ".."),
                             Objects.requireNonNullElse(interval[0][1], ".."));
    }

    public void setInterval(String[][] interval) {
        this.interval = interval;
    }

    public String getTrs() {
        return trs;
    }

    public void setTrs(String trs) {
        this.trs = trs;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<OgcApiExtentTemporal> FUNNEL = (from, into) -> {
        into.putString(from.getTrs(), StandardCharsets.UTF_8);
        Arrays.stream(from.getInterval())
              .forEachOrdered(arr -> Arrays.stream(arr)
                                           .forEachOrdered(val -> into.putString(Objects.requireNonNullElse(val, ".."), StandardCharsets.UTF_8)));
    };
}
