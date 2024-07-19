/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import grails.util.Environment
import grails.util.Holders

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

databaseChangeLog = {

    Path base = Environment.isWarDeployed() ?
            Paths.get(Holders.applicationContext.grailsResourceLocator.findResourceForURI('WEB-INF/classes').file.path) :
            Paths.get('migrations')
    Path dir = base.resolve("changelogs/defaultValues")

    // Default values
    List<Path> files = [
            dir.resolve("seq-types.sql"),
            dir.resolve("workflows.sql"),
            dir.resolve("workflow-api-versions.sql"),
            dir.resolve("workflow-versions.sql"),
            dir.resolve("project-roles.sql"),
            dir.resolve("species-and-strains.sql"),
            dir.resolve("tool-names-of-reference-genome-indexes.sql"),
            dir.resolve("roles.sql"),
            dir.resolve("file-types.sql"),
            dir.resolve("pipelines.sql"),
    ]

    // External workflow configs
    Path ewcDir = dir.resolve("workflow-config")

    List<Path> workflowDirs = Files.list(ewcDir).findAll {
        !it.fileName.toString().contains('test')
    }.sort()

    workflowDirs.each { workflowDir ->
        files.addAll(Files.list(workflowDir).toList())
    }

    files.each { file ->
        changeSet(author: "otp", id: file.getFileName().toString(), runOnChange: "true") {
            sql(file.text)
        }
    }
}
