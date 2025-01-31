import {
  Viewer,
  Ion,
  Camera,
  Credit,
  WebMapTileServiceImageryProvider,
  Rectangle,
  Color,
  Cartesian3,
} from "c137.js";

import "./style.css";

const Cesium = ({
  container,
  backgroundUrl,
  attribution,
  getExtent,
  addEntities,
}) => {
  Ion.defaultAccessToken = null;
  Camera.DEFAULT_VIEW_RECTANGLE = getExtent(Rectangle);
  Camera.DEFAULT_VIEW_FACTOR = 0;

  const viewer = new Viewer(container, {
    imageryProvider: new WebMapTileServiceImageryProvider({
      url: backgroundUrl,
      layer: "Base",
      style: "default",
      tileMatrixSetID: "WebMercatorQuad",
    }),
    animation: false,
    baseLayerPicker: false,
    homeButton: true,
    geocoder: false,
    timeline: false,
    scene3DOnly: true,
    fullscreenButton: false,
    navigationInstructionsInitiallyVisible: false,
  });

  if (attribution) {
    const credit = new Credit(attribution);
    viewer.scene.frameState.creditDisplay.addDefaultCredit(credit);
  }

  if (addEntities) {
    addEntities(viewer, Color, Cartesian3);
  }
};

export default Cesium;
