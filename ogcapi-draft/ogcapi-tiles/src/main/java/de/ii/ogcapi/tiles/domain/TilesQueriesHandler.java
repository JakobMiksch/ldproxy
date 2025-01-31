/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.features.core.domain.processing.FeatureProcessChain;
import de.ii.ogcapi.tiles.app.TileProviderMbtiles;
import de.ii.ogcapi.tiles.app.TileProviderTileServer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import org.immutables.value.Value;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TilesQueriesHandler extends QueriesHandler<TilesQueriesHandler.Query> {

    @Override
    Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers();

    enum Query implements QueryIdentifier {TILE_SETS, TILE_SET, SINGLE_LAYER_TILE, MULTI_LAYER_TILE, TILE_STREAM, EMPTY_TILE, MBTILES_TILE, TILESERVER_TILE}

    @Value.Immutable
    interface QueryInputTileEmpty extends QueryInput {

        Tile getTile();
    }

    @Value.Immutable
    interface QueryInputTileStream extends QueryInput {

        Tile getTile();
        InputStream getTileContent();
    }

    @Value.Immutable
    interface QueryInputTileMbtilesTile extends QueryInput {

        Tile getTile();
        TileProviderMbtiles getProvider();
    }

    @Value.Immutable
    interface QueryInputTileTileServerTile extends QueryInput {

        Tile getTile();
        TileProviderTileServer getProvider();
    }

    @Value.Immutable
    interface QueryInputTileMultiLayer extends QueryInput {

        Tile getTile();
        Map<String, Tile> getSingleLayerTileMap();
        Map<String, FeatureQuery> getQueryMap();
        EpsgCrs getDefaultCrs();

        // the processing
        Optional<OutputStream> getOutputStream();
        Optional<FeatureProcessChain> getProcesses();
        Map<String, Object> getProcessingParameters();
    }

    @Value.Immutable
    interface QueryInputTileSingleLayer extends QueryInput {

        Tile getTile();
        FeatureQuery getQuery();
        EpsgCrs getDefaultCrs();

        // the processing
        Optional<OutputStream> getOutputStream();
        Optional<FeatureProcessChain> getProcesses();
        Map<String, Object> getProcessingParameters();
    }

    @Value.Immutable
    interface QueryInputTileSets extends QueryInput {

        Optional<String> getCollectionId();
        List<Double> getCenter();
        Map<String, MinMax> getTileMatrixSetZoomLevels();
        String getPath();
        boolean getOnlyWebMercatorQuad();
        List<String> getTileEncodings();
    }

    @Value.Immutable
    interface QueryInputTileSet extends QueryInput {

        Optional<String> getCollectionId();
        String getTileMatrixSetId();
        List<Double> getCenter();
        MinMax getZoomLevels();
        String getPath();
    }

}
