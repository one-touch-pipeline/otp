/*
 * Copyright 2011-2019 The OTP authors
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

import java.nio.charset.MalformedInputException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

@CompileStatic
class LicenseCheck {

    static List<String> requiredLicense = [
            "Copyright",
            "Permission is hereby granted, free of charge, to any person obtaining a copy",
            "of this software and associated documentation files (the \"Software\"), to deal",
            "in the Software without restriction, including without limitation the rights",
            "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell",
            "copies of the Software, and to permit persons to whom the Software is",
            "furnished to do so, subject to the following conditions:",
            "The above copyright notice and this permission notice shall be included in all",
            "copies or substantial portions of the Software.",
            "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR",
            "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,",
            "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE",
            "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER",
            "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,",
            "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE",
            "SOFTWARE.",
    ]

    static List<Path> ignoredDirs = [
            "migrations/changelogs",
            "src/docs",

            "grails-app/assets/lib",
            "grails-app/assets/images",

            "misc/fonts",
            "misc/logo",
            "scripts/gitchangelog",

            ".git",
            ".gradle",
            ".groovy",
            ".idea",
            ".settings",
            ".slcache",

            "build",
            "gradle",
            "logs",
            "out",
            "target",

            "workflows/cwl/dataInstallation/testInput",
    ].collect { Paths.get(it) }

    static List<Path> ignoredFiles = [
            ".gitignore",
            "NOTICE.txt",
            "scripts/ReferenceGenome/README.txt",
            "scripts/rollout/readme.txt",

            ".project",
            ".classpath",
            "otp-grailsPlugins.iml",
            "otp.iml",

            "gradle.properties",
            "settings.gradle",
            "src/main/webapp/WEB-INF/web.xml",

            "gradlew.bat",
            "gradlew",
            "grailsw.bat",
            "grailsw",

            "src/main/resources/wes-api/workflow_execution_service.swagger.config.json",
            "src/main/resources/wes-api/workflow_execution_service.swagger.yaml",

            "workflows/cwl/dataInstallation/cromwell/config.conf",
            "workflows/cwl/dataInstallation/wes-service-with-cromwell/config.conf",
    ].collect { Paths.get(it) }


    static void checkLicense(Path path, List<String> problems) {
        List<String> code = []
        try {
            code = path.readLines()
        } catch (MalformedInputException ignored) {
            // likely binary file
        }

        int i = 0
        boolean licenseOk = true

        for (String codeLine : code) {
            if (codeLine.contains(requiredLicense[i])) {
                licenseOk = true
                i++
                if (i >= requiredLicense.size()) {
                    break
                }
            } else {
                licenseOk = false
            }
        }

        if (!licenseOk) {
            problems.add("${path.normalize()} does not have the correct license" as String)
        }
    }

    static void checkDir(Path path, List<String> problems) {
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isSymbolicLink(file) && !ignoredFiles.any { file.normalize() == it } ) {
                        checkLicense(file, problems)
                    }
                    return FileVisitResult.CONTINUE
                }
                @Override
                FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
                    if (ignoredDirs.any { directory.normalize().startsWith(it) } ) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        }
    }

    // passing a single file to walkFileTree does not use preVisitDirectory
    static void checkFile(Path path, List<String> problems) {
        if (Files.isDirectory(path)) {
            checkDir(path, problems)
        } else {
            if (ignoredDirs.any { path.startsWith(it) } ) {
                return
            }
            if (!ignoredFiles.any { path == it } ) {
                if (Files.exists(path)) {
                    checkLicense(path, problems)
                }
            }
        }
    }


    static void main(String... args) {
        InputStreamReader stdinReader = new InputStreamReader(System.in)
        BufferedReader reader = new BufferedReader(stdinReader)

        List<String> problems = []

        try {
            if (args.size() == 0) {
                checkDir(new File(".").toPath(), problems)
            } else {
                args.each { String file ->
                    checkFile(new File(file).toPath().normalize(), problems)
                }
            }
            if (!problems.empty) {
                println problems.join("\n")
                System.exit(1)
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err)
            System.exit(1)
        } finally {
            reader.close()
            stdinReader.close()
        }
    }
}
