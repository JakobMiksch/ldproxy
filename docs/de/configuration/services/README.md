# Die API-Konfiguration

Jede API stellt eine OGC Web API bereit.

Die Konfiguration einer API wird in einer Konfigurationsdatei in einem Objekt mit den folgenden Eigenschaften beschrieben. 

Informationen zu den einzelnen API-Modulen finden Sie [hier](building-blocks/README.md), siehe `api` in der nachfolgenden Tabelle.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`id` |string | |*REQUIRED* Eindeutiger Identifikator der API. Typischerweise identisch mit dem Identifikator des Feature-Providers. Erlaubt sind Buchstaben (A-Z, a-z), Ziffern (0-9), der Unterstrich ("_") und der Bindestrich ("-").
|`apiVersion` |integer |`null` |Ist ein Wert angegeben, dann wird eine Version im Pfad der API-URIs ergänzt. Ohne Angabe ist der Pfad zur Landing Page `/{id}`, wobei `{id}` der Identifikator der API ist. Ist der Wert angegeben, dann ist der Pfad zur Landing Page `/{id}/v{apiVersion}`, also z.B. `/{id}/v1` beim Wert 1.
|`createdAt` |integer | |Zeitpunkt in Millisekunden seit dem 1.1.1970, an dem die Datei erzeugt wurde. Der Wert wird automatisch vom Manager bei der Erzeugung gesetzt und besitzt nur informativen Charakter.
|`lastModified` |integer | |Zeitpunkt in Millisekunden seit dem 1.1.1970, an dem die Datei zuletzt geändert wurde. Der Wert wird automatisch vom Manager bei jeder Änderung gesetzt und besitzt nur informativen Charakter.
|`entityStorageVersion` |integer | |*REQUIRED* Bezeichnet die Version des API-Definition-Konfigurationsdatei-Schemas. Diese Dokumentation bezieht sich auf die Version 2 und alle Dateien nach diesem Schema müssen den Wert 2 haben. Konfigurationen zu Version 1 werden automatisch auf Version 2 aktualisiert.
|`label` |string |der Wert von `id` |Eine Bezeichnung der API, z.B. für die Präsentation zu Nutzern.
|`description` |string |`null` |Eine Beschreibung der API, z.B. für die Präsentation zu Nutzern.
|`serviceType` |enum | |*REQUIRED* Stets `OGC_API`.
|`enabled` |boolean |`true` |Steuert, ob die API mit dem Start von ldproxy aktiviert wird.
|`shouldStart` |boolean |`true` |*Deprecated* Siehe `enabled`
|`metadata` |object |`{}` |Über dieses Objekt können grundlegende Metadaten zur API (Version, Kontaktdaten, Lizenzinformationen) festgelegt werden. Erlaubt sind die folgenden Elemente (in Klammern werden die Ressourcen genannt, in denen die Angabe verwendet wird): `version` (API-Definition), `contactName` (API-Definition, HTML-Landing-Page), `contactUrl` (API-Definition, HTML-Landing-Page), `contactEmail` (API-Definition, HTML-Landing-Page), `contactPhone` (HTML-Landing-Page), `licenseName` (API-Definition, HTML-Landing-Page, Feature-Collections, Feature-Collection), `licenseUrl` (API-Definition, HTML-Landing-Page, Feature-Collections, Feature-Collection),  `keywords` (Meta-Tags und schema:Dataset in HTML-Landing-Page), `attribution` (Landing-Page, Karten), `creatorName` (schema:Dataset in HTML), `creatorUrl` (schema:Dataset in HTML), `creatorLogoUrl` (schema:Dataset in HTML), `publisherName` (schema:Dataset in HTML), `publisherUrl` (schema:Dataset in HTML), `publisherLogoUrl` (schema:Dataset in HTML). Alle Angaben sind Strings, bis auf die Keywords, die als Array von Strings angegeben werden.
|`tags` |array |`null` |Ordnet der API die aufgelisteten Tags zu. Die Tags müssen jeweils Strings ohne Leerzeichen sein. Die Tags werden im API-Katalog angezeigt und können über den Query-Parameter `tags` zur Filterung der in der API-Katalog-Antwort zurückgelieferten APIs verwendet werden, z.B. `tags=INSPIRE`.<br>_seit Version 2.1_
|`externalDocs` |object |`{}` |Es kann externes Dokument mit weiteren Informationen angegeben werden, auf das aus der API verlinkt wird. Anzugeben sind die Eigenschaften `url` und `description`.
|`defaultExtent` |object |`{'spatialComputed': true, 'temporalComputed': true}` |Es kann ein Standardwert für die räumliche (`spatial`) und/oder zeitliche (`temporal`) Ausdehnung der Daten angeben werden, die bei den Objektarten verwendet wird, wenn dort keine anderslautende Ausdehnung spezifiziert wird. Für die räumliche Ausdehnung sind die folgenden Eigenschaften anzugeben (alle Angaben in `CRS84`): `xmin`, `ymin`, `xmax`, `ymax`. Für die zeitliche Ausdehnung sind die folgenden Eigenschaften anzugeben (alle Angaben in Millisekunden seit dem 1.1.1970): `start`, `end`. Soll die räumliche Ausdehnung aus den Daten einer Objektart standardmäßig automatisch beim Start von ldproxy ermittelt werden, kann `spatialComputed` mit dem Wert `true` angegeben werden. Soll die zeitliche Ausdehnung aus den Daten einer Objektart standardmäßig automatisch beim Start von ldproxy ermittelt werden, kann `temporalComputed` mit dem Wert `true` angegeben werden. Bei großen Datenmengen verzögern diese Optionen allerdings die Zeitdauer, bis die API verfügbar ist. Hinweis: Es handelt sich hierbei nicht um die Ausdehnung des Datensatzes insgesamt, dieser wird stets automatisch aus den Ausdehnungen der einzelnen Objektarten ermittelt.
|`api` |array |`[]` |Ein Array mit der Konfiguration der [API-Module](building-blocks/README.md) für die API.
|`apiValidation` |enum |`NONE` |Steuert ob die Spezifikationen der API während des Starts der API geprüft werden. Eine grundlegende Prüfung findet bereits beim Lesen der Konfigurationsdateien statt und Syntaxfehler oder fehlende Daten-Provider werden bereits in dieser Phase behandelt. Während des Starts der API und seiner Module können weitere Prüfungen auf Konsistenz durchgeführt werden. `NONE` heißt keine Prüfung. Bei `LAX` schlägt die Prüfung fehl und der Start des Providers wird verhindert, wenn Probleme festgestellt werden, die in jedem Fall zu Laufzeitfehlern führen würden. Probleme die abhängig von den tatsächlichen Daten zu Laufzeitfehlern führen könnten, werden als Warnung geloggt. Bei `STRICT` führen alle festgestellten Probleme zu einem Fehlstart. Die API wird also nur gestartet, wenn keine Risiken für Laufzeitfehler im Zusammenhang mit der API-Konfiguration identifiziert werden.
|`collections` |object |`{}` |Ein Objekt mit der spezifischen Konfiguration zu jeder Objektart, der Name der Objektart ist der Schlüssel, der Wert ein [Collection-Objekt](#collection).
|`auto` |boolean |false |Steuert, ob die Informationen zu `collections` beim Start automatisch aus dem Feature-Provider bestimmt werden sollen (Auto-Modus). In diesem Fall sollte `collections` nicht angegeben sein.
|`autoPersist` |boolean |false |Steuert, ob die im Auto-Modus (`auto: true`) bestimmten Schemainformationen in die Konfigurationsdatei übernommen werden sollen. In diesem Fall werden `auto` und `autoPersist` beim nächsten Start automatisch aus der Datei entfernt. Liegt die Konfigurationsdatei in einem anderen Verzeichnis als unter `store/entities/services` (siehe `additionalLocations`), so wird eine neue Datei in `store/entities/services` erstellt. `autoPersist: true` setzt voraus, dass `store` sich nicht im `READ_ONLY`-Modus befindet.

<a name="collection"></a>

## Das Collection-Objekt

Jedes Collection-Objekt beschreibt eine Objektart aus einem Feature Provider (derzeit werden nur Feature Collections von ldproxy unterstützt). Es setzt sich aus den folgenden Eigenschaften zusammen:

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`id` |string | |*REQUIRED* Eindeutiger Identifikator der API. Typischerweise identisch mit dem Identifikator des Types im Feature-Provider. Erlaubt sind Buchstaben (A-Z, a-z), Ziffern (0-9), der Unterstrich ("_") und der Bindestrich ("-").
|`label` |string |der Wert von `id` |Eine Bezeichnung der API, z.B. für die Präsentation zu Nutzern.
|`description` |string |`null` |Eine Beschreibung des Schemaobjekts, z.B. für die Präsentation zu Nutzern.
|`persistentUriTemplate` |string |`null` |Über die Feature-Ressource hat jedes Feature zwar eine feste URI, die für Links verwendet werden kann, allerdings ist die URI nur so lange stabil, wie die API stabil bleibt. Um von Veränderungen in der URI unabhängig zu sein, kann es sinnvoll oder gewünscht sein, API-unabhängige URIs für die Features zu definieren und von diesen URIs auf die jeweils gültige API-URI weiterzuleiten. Diese kananosche URI kann auch in ldproxy Konfiguriert und bei den Features kodiert werden. Hierfür ist ein Muster der Feature-URI anzugeben, wobei `{{value}}` als Ersetzungspunkt für den lokalen Identifikator des Features in der API angegeben werden kann.
|`extent` |object |`{}` |Es kann die räumliche (`spatial`) und/oder zeitliche (`temporal`) Ausdehnung der Features von der Objektart angeben werden. Für die räumliche Ausdehnung sind die folgenden Eigenschaften anzugeben (alle Angaben in `CRS84`): `xmin`, `ymin`, `xmax`, `ymax`. Für die zeitliche Ausdehnung sind die folgenden Eigenschaften anzugeben (alle Angaben in Millisekunden seit dem 1.1.1970): `start`, `end`. Soll die räumliche Ausdehnung aus den Daten automatisch beim Start von ldproxy ermittelt werden, kann `spatialComputed` mit dem Wert `true` angegeben werden. Soll die zeitliche Ausdehnung aus den Daten automatisch beim Start von ldproxy ermittelt werden, kann `temporalComputed` mit dem Wert `true` angegeben werden. Bei großen Datenmengen verzögern diese Optionen allerdings die Zeitdauer, bis die API verfügbar ist. Wurde in `defaultExtent` die automatische Berechnung aktiviert, dann kann sie für die Objektart mit `false` jeweils wieder deaktiviert werden. `spatialComputed` bzw. `temporalComputed` werden nur berücksichtigt, wenn keine explizite Ausdehnung in `spatial` bzw. `temporal` angegeben ist.
|`additionalLinks` |array |`[]` |Erlaubt es, zusätzliche Links bei jeder Objektart zu ergänzen. Der Wert ist ein Array von Link-Objekten. Anzugeben sind jeweils mindestens die URI (`href`), der anzuzeigende Text (`label`) und die Link-Relation (`rel`).
|`api` |array |`[]` |Ein Array mit der Konfiguration der [API-Module](building-blocks/README.md) für die Objektart.

## API-Module

Ein Array dieser Modul-Konfigurationen steht auf der Ebene der gesamten API und für jede Collection zur Verfügung. Die jeweils gültige Konfiguration ergibt sich aus der Priorisierung:

* Ist nichts angegeben, dann gelten die im ldproxy-Code vordefinierten Standardwerte. Diese sind bei den jeweiligen [API-Modulen](building-blocks/README.md) spezifiziert.
* Diese systemseitigen Standardwerte können von den Angaben im Verzeichnis "defaults" überschrieben werden.
* Diese deploymentweiten Standardwerte können von den Angaben in der API-Definition auf Ebene der API überschrieben werden.
* Diese API-weiten Standardwerte können bei den Collection-Ressourcen und untergeordneten Ressourcen von den Angaben in der API-Definition auf Ebene der Collection überschrieben werden.
* Diese Werte können durch Angaben im Verzeichnis "overrides" überschrieben werden.

## Eine API-Beispielkonfiguration

Als Beispiel siehe die [API-Konfiguration](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/entities/services/vineyards.yml) der API [Weinlagen in Rheinland-Pfalz](https://demo.ldproxy.net/vineyards).

## Speicherung

API-Konfigurationen liegen unter dem relativen Pfad `store/entities/services/{apiId}.yml` im Datenverzeichnis.
