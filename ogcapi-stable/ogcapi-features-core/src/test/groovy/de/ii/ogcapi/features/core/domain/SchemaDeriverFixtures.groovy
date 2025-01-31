/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain

import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry

class SchemaDeriverFixtures {
    static final FeatureSchema FEATURE_SCHEMA =
            new ImmutableFeatureSchema.Builder()
                    .name("test-name")
                    .type(SchemaBase.Type.OBJECT)
                    .description("bar")
                    .addTransformations(new ImmutablePropertyTransformation.Builder().flatten(".").build())
                    .putPropertyMap("ID", new ImmutableFeatureSchema.Builder()
                            .name("id")
                            .type(SchemaBase.Type.INTEGER)
                            .role(SchemaBase.Role.ID)
                            .label("foo")
                            .description("bar")
                            .build())
                    .putPropertyMap("string", new ImmutableFeatureSchema.Builder()
                            .name("string")
                            .type(SchemaBase.Type.STRING)
                            .label("foo")
                            .description("bar")
                            .constraints(new ImmutableSchemaConstraints.Builder().required(true).build())
                            .build())
                    .putPropertyMap("link", new ImmutableFeatureSchema.Builder()
                            .name("link")
                            .type(SchemaBase.Type.OBJECT)
                            .objectType("Link")
                            .putPropertyMap("title", new ImmutableFeatureSchema.Builder()
                                    .name("title")
                                    .description("bar")
                                    .label("foo")
                                    .type(SchemaBase.Type.STRING)
                                    .build())
                            .putPropertyMap("href", new ImmutableFeatureSchema.Builder()
                                    .name("href")
                                    .description("bar")
                                    .label("foo")
                                    .type(SchemaBase.Type.STRING)
                                    .build())
                            .build())
                    .putPropertyMap("links", new ImmutableFeatureSchema.Builder()
                            .name("links")
                            .type(SchemaBase.Type.OBJECT_ARRAY)
                            .objectType("Link")
                            .label("foo")
                            .description("bar")
                            .constraints(new ImmutableSchemaConstraints.Builder().minOccurrence(1).maxOccurrence(5).build())
                            .putPropertyMap("title", new ImmutableFeatureSchema.Builder()
                                    .name("title")
                                    .description("bar")
                                    .label("foo")
                                    .type(SchemaBase.Type.STRING)
                                    .build())
                            .putPropertyMap("href", new ImmutableFeatureSchema.Builder()
                                    .name("href")
                                    .description("bar")
                                    .label("foo")
                                    .type(SchemaBase.Type.STRING)
                                    .build()))
                    .putPropertyMap("geometry", new ImmutableFeatureSchema.Builder()
                            .name("geometry")
                            .type(SchemaBase.Type.GEOMETRY)
                            .geometryType(SimpleFeatureGeometry.MULTI_POLYGON)
                            .role(SchemaBase.Role.PRIMARY_GEOMETRY) //TODO:
                            .label("foo")
                            .description("bar")
                            .build())
                    .putPropertyMap("datetime", new ImmutableFeatureSchema.Builder()
                            .name("datetime")
                            .type(SchemaBase.Type.DATETIME)
                            .label("foo")
                            .description("bar")
                            .build())
                    .putPropertyMap("endLifespanVersion", new ImmutableFeatureSchema.Builder()
                            .name("endLifespanVersion")
                            .type(SchemaBase.Type.DATETIME)
                            .label("foo")
                            .description("bar")
                            .build())
                    .putPropertyMap("boolean", new ImmutableFeatureSchema.Builder()
                            .name("boolean")
                            .type(SchemaBase.Type.BOOLEAN)
                            .label("foo")
                            .description("bar")
                            .build())
                    .putPropertyMap("percent", new ImmutableFeatureSchema.Builder()
                            .name("percent")
                            .type(SchemaBase.Type.FLOAT)
                            .label("foo")
                            .description("bar")
                            .constraints(new ImmutableSchemaConstraints.Builder().minOccurrence(0).maxOccurrence(100).build())
                            .build())
                    .putPropertyMap("strings", new ImmutableFeatureSchema.Builder()
                            .name("strings")
                            .type(SchemaBase.Type.VALUE_ARRAY)
                            .valueType(SchemaBase.Type.STRING)
                            .label("foo")
                            .description("bar")
                            .build())
                    .putPropertyMap("objects", new ImmutableFeatureSchema.Builder()
                            .name("objects")
                            .type(SchemaBase.Type.OBJECT_ARRAY)
                            .label("foo")
                            .description("bar")
                            .putPropertyMap("integer", new ImmutableFeatureSchema.Builder()
                                    .name("integer")
                                    .type(SchemaBase.Type.INTEGER)
                                    .label("foo")
                                    .description("bar")
                                    .build())
                            .putPropertyMap("date", new ImmutableFeatureSchema.Builder()
                                    .name("date")
                                    .type(SchemaBase.Type.DATE)
                                    .build())
                            .putPropertyMap("object2", new ImmutableFeatureSchema.Builder()
                                    .name("object2")
                                    .type(SchemaBase.Type.OBJECT)
                                    .objectType("Object2")
                                    .putPropertyMap("regex", new ImmutableFeatureSchema.Builder()
                                            .name("regex")
                                            .type(SchemaBase.Type.STRING)
                                            .constraints(new ImmutableSchemaConstraints.Builder().regex("'^_\\\\w+\$'").build())
                                            .build())
                                    .putPropertyMap("codelist", new ImmutableFeatureSchema.Builder()
                                            .name("codelist")
                                            .type(SchemaBase.Type.STRING)
                                            .constraints(new ImmutableSchemaConstraints.Builder().codelist("mycodelist").build())
                                            .build())
                                    .putPropertyMap("enum", new ImmutableFeatureSchema.Builder()
                                            .name("enum")
                                            .type(SchemaBase.Type.STRING)
                                            .constraints(new ImmutableSchemaConstraints.Builder().enumValues(Arrays.asList("foo", "bar")).build())
                                            .build())
                                    .putPropertyMap("strings", new ImmutableFeatureSchema.Builder()
                                            .name("strings")
                                            .type(SchemaBase.Type.VALUE_ARRAY)
                                            .valueType(SchemaBase.Type.STRING)
                                            .build())
                                    .build())
                            .build())
                    .build()
}
