/*
 * Copyright 2011-2023 The OTP authors
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

import groovy.transform.CompileStatic
import groovy.json.JsonSlurper

@CompileStatic
class FindNewLicenses {
    static final String REPORT_FILE_NAME = "gl-license-scanning-report.json"

    static final List<String> allowedLicenses = [
            "Apache License 1.1",
            "Apache License 2.0",
            "Apache-1.1",
            "Apache-2.0",
            "BSD",
            "BSD 2-Clause License",
            "BSD 2-Clause \"Simplified\" License",
            "BSD 3-Clause \"New\" or \"Revised\" License",
            "BSD licence",
            "BSD License 3",
            "BSD-2-Clause",
            "BSD-3-Clause",
            "CDDL+GPL License",
            "CDDL + GPLv2 with classpath exception",
            "CDDL-1.0",
            "CDDL-1.1",
            "CDDL/GPLv2+CE",
            "Common Development and Distribution License",
            "Common Development and Distribution License 1.0",
            "Common Development and Distribution License 1.1",
            "Eclipse Distribution License v. 1.0",
            "Eclipse Distribution License - v 1.0",
            "EDL 1.0",
            "Eclipse Public License (EPL)",
            "Eclipse Public License 1.0",
            "Eclipse Public License v2.0",
            "EPL-1.0",
            "EPL 2.0",
            "GNU General Public License v2.0 w/Classpath exception",
            "GNU General Public License v3.0 only",
            "GNU Lesser General Public License",
            "GNU General Public License, Version 2 with the Classpath Exception",
            "GNU Lesser General Public License v2.1 only",
            "GNU Lesser General Public License v2.1 or later",
            "GNU Library General Public License v2.1 or later",
            "Lesser General Public License, version 3 or greater",
            "GPL v2 with the Classpath exception",
            "GPL-2.0-with-classpath-exception",
            "GPL-3.0",
            "ISC License",
            "LGPL-2.1",
            "LGPL-2.1+",
            "LGPL-2.1-or-later",
            "MIT",
            "MIT-0",
            "MIT License",
            "MIT No Attribution",
            "Mozilla Public License 1.1",
            "Mozilla Public License 2.0",
            "MPL 2.0 or EPL 1.0",
            "MPL-1.1",
            "MPL-2.0",
            "NCBI License",
            "Public Domain",
            "Public Domain, per Creative Commons CC0",
            "Revised BSD",
            "The (New) BSD License",
            "Universal Permissive License, Version 1.0",
            "ISC",
            "The Unlicense",
            "Unlicense",
    ]*.toUpperCase()

    static final List<Map> allowedDependencies = [
            [name: "RoddyToolLib", version: "2.4.0"],
            [name: "bedutils", version: "0.0.8"],
            [name: "grolifant", version: "0.11"],
            [name: "jsr166y", version: "1.7.0"],
            [name: "jta", version: "1.1"],
            [name: "mail", version: "3.0.0"],
            [name: "multiverse-core", version: "0.7.0"],
            [name: "rhino", version: "1.7R5-20130223-1"],
            [name: "scaffolding-core", version: "2.1.0"],
            [name: "webdriver-binaries-gradle-plugin", version: "2.4"],
    ] as List<Map>

    /**
     * Read license report json file which is passed by the ci pipeline.
     * JSON will be parsed and returned as a map.
     */
    static Map getLicenseReport() {
        File licenseReport = new File(REPORT_FILE_NAME)
        JsonSlurper jsonSlurper = new JsonSlurper()
        return jsonSlurper.parse(licenseReport) as Map
    }

    /**
     * Get all new licenses from the direct dependencies.
     */
    static List<String> getNewDirectLicenses(Map parsedReport) {
        List<String> newDirectLicenses = []

        parsedReport.licenses.each { Map license ->
            String licenseName = license.name.toString().toUpperCase()
            if (!allowedLicenses.contains(licenseName) && licenseName != 'UNKNOWN') {
                newDirectLicenses.add(license.name as String)
            }
        }
        return newDirectLicenses.unique().sort { it.toUpperCase() }
    }

    /**
     *
     * Get all licenses that are not used in any dependencies.
     */
    static List<String> getUnusedLicences(Map parsedReport) {
        List<String> directLicenses = parsedReport.licenses.collect { Map license ->
            license.name.toString()
        }
        List<String> indirectLicenses = ((parsedReport.dependencies as List<Map>)*.licenses).flatten() as List<String>
        List<String> allLicences = (directLicenses + indirectLicenses)*.toUpperCase().unique()

        return allowedLicenses.findAll { String allowedLicense ->
            return !allLicences.contains(allowedLicense)
        }
    }

    /**
     * Get all new licenses from peer dependencies.
     */
    static List<String> getNewIndirectLicenses(Map parsedReport) {
        List<String> newIndirectLicenses = []
        List<String> indirectLicenses = ((parsedReport.dependencies as List<Map>)*.licenses as List<String>).flatten().unique() as List<String>

        indirectLicenses.each { String license ->
            if (!allowedLicenses.contains(license.toUpperCase()) && license != 'unknown') {
                newIndirectLicenses.add(license)
            }
        }
        return newIndirectLicenses.unique().sort { it.toUpperCase() }
    }

    static List<Map> getUnknownDependencies(Map parsedReport) {
        List<Map> unknownDependencies = []

        parsedReport.dependencies.each { Map dependency ->
            if ((dependency.licenses as List<String>)*.toLowerCase().contains("unknown")) {
                unknownDependencies.add([name: dependency.name, version: dependency.version])
            }
        }
        return unknownDependencies.unique()
    }

    /**
     * Retrieve unknown Dependencies not contained in whitelisted Dependencies.
     */
    static List<Map> filterUnknownDependencies(Map parsedReport) {
        List<Map> unknownDependencies = getUnknownDependencies(parsedReport)

        return unknownDependencies.findAll { Map dependency ->
            !allowedDependencies.contains([name: dependency.name, version: dependency.version])
        }
    }

    static void main(String... args) {
        Map parsedReport = getLicenseReport()
        List<String> newDirectLicenses = getNewDirectLicenses(parsedReport)
        List<String> newIndirectLicenses = getNewIndirectLicenses(parsedReport)
        List<String> unusedLicenses = getUnusedLicences(parsedReport)
        List<Map> unknownDependencies = filterUnknownDependencies(parsedReport)

        String displayUnkownDependencies = unknownDependencies.collect {
            return "[name: \"${it.name}\", version: \"${it.version}\"]"
        }.join(", \n")

        println "${unusedLicenses.size()} unused licenses:"
        println unusedLicenses ?: "-"
        println ""
        println "${newDirectLicenses.size()} new direct licenses:"
        println newDirectLicenses ?: "-"
        println ""
        println "${newIndirectLicenses.size()} new indirect licenses:"
        println newIndirectLicenses ?: "-"
        println ""
        println "${unknownDependencies.size()} dependencies with unknown licenses:"
        println displayUnkownDependencies ?: "-"

        assert newDirectLicenses == [] && newIndirectLicenses == []: "New licenses are found. Please check if they are compatible with OTP."
        assert unusedLicenses == []: "There are unused Licenses, which can be removed from the white list."
    }
}
