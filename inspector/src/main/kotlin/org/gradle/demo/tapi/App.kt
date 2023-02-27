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

package org.gradle.demo.tapi

import org.gradle.demo.model.OutgoingArtifactsModel
import org.gradle.demo.plugin.Beacon
import org.gradle.tooling.GradleConnector

import java.io.File
import java.nio.file.Files

import kotlin.io.path.writeText
import kotlin.reflect.KClass

fun main(vararg args: String) {
    val connector = GradleConnector.newConnector().forProjectDirectory(findProjectPath(args))

    connector.connect().use { connection ->
        val customModelBuilder = connection.model(OutgoingArtifactsModel::class.java)
        customModelBuilder.withArguments("--init-script", copyInitScript().absolutePath)
        val model = customModelBuilder.get()
        model.getArtifacts().forEach { artifact ->
            println("artifact = $artifact")
        }
    }
}

private fun copyInitScript(): File {
    val pluginJar = lookupJar(Beacon::class)
    val modelJar = lookupJar(OutgoingArtifactsModel::class)

    val initGradleResource = object {}.javaClass.getResourceAsStream("/init.gradle")
    val initGradleContents = initGradleResource.bufferedReader().use {
        buildString {
            it.readLines().forEach { line ->
                var repl = line
                    .replace("%%PLUGIN_JAR%%", pluginJar.absolutePath)
                    .replace("%%MODEL_JAR%%", modelJar.absolutePath)

                // fix paths if we're on Windows
                if (File.separatorChar == '\\') {
                    repl = repl.replace('\\', '/')
                }

                appendLine(repl)
            }
        }
    }

    val initGradlePath = Files.createTempFile("init", ".gradle")
    initGradlePath.writeText(initGradleContents)

    return initGradlePath.toFile()
}

private fun lookupJar(beaconClass: KClass<*>): File {
    val codeSource = beaconClass.java.protectionDomain.codeSource
    return File(codeSource.location.toURI())
}

private fun findProjectPath(args: Array<out String>): File {
    if (args.isEmpty()) {
        return File(".").absoluteFile
    }
    return File(args[0])
}
