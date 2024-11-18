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
package migration

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType

// input
// List of projects to update the config paths
Map<String, String> projects = [
]

Boolean dryRun = true

// script
ConfigPerProjectAndSeqType.withTransaction {
    projects.each { Map.Entry<String, String> p ->
        ConfigPerProjectAndSeqType.withCriteria() {
            project {
                eq("name", p.key)
            }
        }.each { ConfigPerProjectAndSeqType config ->
            println "Updating config path for project ${config.project.name} and seq type ${config.seqType.name}"
            config.configFilePath = config.configFilePath.replace("/omics/odcf/project/external/", "/omics/odcf/project/${p.value}/")
            config.save(flush: true)
            println "New config path: ${config.configFilePath}"
        }
    }
    assert !dryRun: "This is a dry run, w/o modification of database."
}
''
