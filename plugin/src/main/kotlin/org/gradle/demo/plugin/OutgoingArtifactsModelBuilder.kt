/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.demo.plugin

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.internal.ReusableAction
import org.gradle.api.specs.Specs
import org.gradle.demo.model.DefaultOutgoingArtifactsModel
import org.gradle.demo.model.OutgoingArtifactsModel
import org.gradle.tooling.provider.model.ToolingModelBuilder

import java.io.File

class OutgoingArtifactsModelBuilder : ToolingModelBuilder {
    private val MODEL_NAME = OutgoingArtifactsModel::class.java.name

    override fun canBuild(modelName: String): Boolean =
        modelName == MODEL_NAME

    override fun buildAll(modelName: String, project: Project): Any {
        val artifacts = mutableSetOf<File>()

        project.allprojects { p ->
            p.configurations.forEach { c ->
                if (c.isCanBeConsumed) {
                    artifacts += c.artifacts.files.files
                }
            }
        }

        project.allprojects { p ->
            val artifactTypeAttr = Attribute.of("artifactType", String::class.java)
            p.dependencies.attributesSchema.getMatchingStrategy(artifactTypeAttr).compatibilityRules.add(CompatibilityRule::class.java)

            p.configurations.forEach { c ->
                if (c.isCanBeResolved) {
                    val copy = c.copyRecursive()

                    copy.attributes {
                        //it.attribute(Attribute.of("artifactType", String::class.java), "")
                        it.attribute(Attribute.of("artifactType", String::class.java), "jar")
                        //it.attribute(Attribute.of("artifactType", String::class.java), "aar")
                        //it.attribute(Attribute.of("artifactType", String::class.java), "android-lint-local-aar")
                        //it.attribute(Attribute.of("com.android.build.api.attributes.BuildTypeAttr", String::class.java), "release")
                        //it.attribute(Attribute.of("com.android.build.gradle.internal.attributes.VariantAttr", String::class.java), "release")
                        //it.attribute(Attribute.of("org.gradle.jvm.environment", String::class.java), "android")
                    }

                    val resolvedArtifacts = copy.resolvedConfiguration.lenientConfiguration.getArtifacts(Specs.SATISFIES_ALL)
                    artifacts += resolvedArtifacts.map { File(it.toString()) }

                    p.logger.quiet(resolvedArtifacts.size.toString())
                    println(resolvedArtifacts.size.toString())

                    resolvedArtifacts.forEach {
                        p.logger.quiet(it.toString())
                        println(it.toString())
                    }
                }
            }
        }

        return DefaultOutgoingArtifactsModel(artifacts.toList())
    }
}

class CompatibilityRule : AttributeCompatibilityRule<String>, ReusableAction {
    override fun execute(details: CompatibilityCheckDetails<String>) {
        val consumerValue = details.consumerValue
        val producerValue = details.producerValue
        if (consumerValue == "jar" && producerValue in listOf("jar", "aar", "apk", "json", "txt")) {
            details.compatible()
            return
        }
    }
}