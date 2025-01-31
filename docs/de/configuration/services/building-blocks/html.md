# Modul "HTML" (HTML)

Das Modul "HTML" kann für jede über ldproxy bereitgestellte API aktiviert werden und ist standardmäßig aktiviert. Soweit für eine Ressource keine speziellen Regelungen für die Ausgabeformate bestehen (wie zum Beispiel für [Features](features-html.md)) und die Ressource HTML unterstützt, können Clients das Ausgabeformat HTML anfordern.

Es werden die folgenden konfigurierbaren Optionen unterstützt:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`noIndexEnabled` |boolean |`true` |Steuert, ob in allen Seiten "noIndex" gesetzt wird und Suchmaschinen angezeigt wird, dass sie die Seiten nicht indizieren sollen.
|`schemaOrgEnabled` |boolean |`true` |Steuert, ob in die HTML-Ausgabe schema.org-Annotationen, z.B. für Suchmaschinen, eingebettet sein sollen, sofern . Die Annotationen werden im Format JSON-LD eingebettet.
|`collectionDescriptionsInOverview`  |boolean |`false` |Steuert, ob in der HTML-Ausgabe der Feature-Collections-Ressource für jede Collection die Beschreibung ausgegeben werden soll.
|`legalName` |string |"Legal notice" |Auf jeder HTML-Seite kann ein ggf. rechtlich erforderlicher Link zu einem Impressum angezeigt werden. Diese Eigenschaft spezfiziert den anzuzeigenden Text.
|`legalUrl` |string |"" |Auf jeder HTML-Seite kann ein ggf. rechtlich erforderlicher Link zu einem Impressum angezeigt werden. Diese Eigenschaft spezfiziert die URL des Links.
|`privacyName` |string |"Privacy notice" |Auf jeder HTML-Seite kann ein ggf. rechtlich erforderlicher Link zu einer Datenschutzerklärung angezeigt werden. Diese Eigenschaft spezfiziert den anzuzeigenden Text.
|`privacyUrl` |string |"" |Auf jeder HTML-Seite kann ein ggf. rechtlich erforderlicher Link zu einer Datenschutzerklärung angezeigt werden. Diese Eigenschaft spezfiziert die URL des Links.
|`basemapUrl` |string |"https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" |Das URL-Template für die Kacheln einer Hintergrundkarte.
|`basemapAttribution` |string |"&copy; <a href='http://osm.org/copyright'>OpenStreetMap</a> contributors" |Die Quellenangabe für die Hintergrundkarte.
|`leafletUrl` |string |"https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" |*Deprecated* Siehe `basemapUrl`.
|`leafletAttribution` |string |"&copy; <a href='http://osm.org/copyright'>OpenStreetMap</a> contributors" |*Deprecated* Siehe `basemapAttribution`.
|`openLayersUrl` |string |"https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png" |*Deprecated* Siehe `basemapUrl`.
|`openLayersAttribution` |string |"&copy; <a href='http://osm.org/copyright'>OpenStreetMap</a> contributors" |*Deprecated* Siehe `basemapAttribution`.
|`footerText` |string |"" |Zusätzlicher Text, der auf jeder HTML-Seite im Footer angezeigt wird.
|`defaultStyle` |string |`NONE` |Ein Style im Style-Repository, der standardmäßig in Karten mit Feature- und Tile-Ressourcen verwendet werden soll. Bei `NONE` wird ein einfacher Style mit OpenStreetMap als Basiskarte verwendet. Wenn der Wert nicht `NONE` ist, enthält die "Landing Page" bzw. die "Feature Collection" auch einen Link zu einer Webkarte mit dem Stil für den Datensatz bzw. die Feature Collection. Der Style sollte alle Daten abdecken und muss im Format Mapbox Style verfügbar sein.

Beispiel für die Angaben in der Konfigurationsdatei (aus der API für [Topographische Daten in Daraa, Syrien](https://demo.ldproxy.net/daraa)):

```yaml
- buildingBlock: HTML
  enabled: true
  noIndexEnabled: true
  schemaOrgEnabled: true
  defaultStyle: topographic
```

Beispiel für die Angaben in der Konfigurationsdatei (aus der API für [Weinlagen in Rheinland-Pfalz](https://demo.ldproxy.net/vineyards)):

```yaml
- buildingBlock: HTML
  enabled: true
  noIndexEnabled: false
  schemaOrgEnabled: true
  collectionDescriptionsInOverview: true
  legalName: Legal notice
  legalUrl: https://www.interactive-instruments.de/en/about/impressum/
  privacyName: Privacy notice
  privacyUrl: https://www.interactive-instruments.de/en/about/datenschutzerklarung/
  basemapUrl: https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png
  basemapAttribution: '&copy; <a href="https://www.bkg.bund.de" target="_new">Bundesamt f&uuml;r Kartographie und Geod&auml;sie</a> (2020), <a href="https://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf" target="_new">Datenquellen</a>'
  defaultStyle: default
```

ldproxy verwendet für die HTML-Ausgabe [Mustache-Templates](https://mustache.github.io/). Anstelle der Standardtemplates von ldproxy können auch benutzerspezifische Templates verwendet werden. Die eigenen Templates müssen als Dateien im ldproxy-Datenverzeichnis unter dem relativen Pfad `templates/html/{templateName}.mustache` liegen, wobei `{templateName}` der Name des ldproxy-Templates ist. Die Standardtemplates liegen jeweils in den Resource-Verzeichnissen der Module, die sie verwenden ([Link zur Suche in GitHub](https://github.com/search?q=repo%3Ainteractive-instruments%2Fldproxy+extension%3Amustache&type=Code)).
