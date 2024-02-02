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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.project.Project

import java.nio.file.*

import static groovyx.gpars.GParsPool.withPool

FastqcDataFilesService fastqcDataFilesService = ctx.fastqcDataFilesService
ProcessingOptionService processingOptionService = ctx.processingOptionService
FileService fileService = ctx.fileService
ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService

List<Project> projects = Project.list(sort: 'name')

String withdrawnUnixGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP)

FileSystem fileSystem = fileSystemService.remoteFileSystem
Path scriptPath = fileSystem.getPath(configService.scriptOutputPath.toString())
Path basePath = scriptPath.resolve("migrationFastqcNewDirectoryLevel")

int numCores = Runtime.runtime.availableProcessors()
println "${numCores} logical CPU core(s) are available"

String header = """
            |#!/bin/bash
            |
            |set -ve
            |
            |""".stripMargin()

List<String> scriptAllProjects = [
        header
].asSynchronized()

withPool(numCores, {
    projects.makeConcurrent().each { Project project ->
        FastqcProcessedFile.withTransaction {
            Path file = basePath.resolve("${project.dirName.replaceAll('/', '_')}.sh")
            List<String> scriptPerProject = [
                    header,
                    "echo start ${file}",
            ]

            FastqcProcessedFile.withCriteria {
                dataFile {
                    seqTrack {
                        sample {
                            individual {
                                eq('project', project)
                                order('pid')
                            }
                            sampleType {
                                order('name')
                            }
                        }
                        order('id')
                    }
                    order('id')
                }
                order('id')
            }.each {
                Path newDirectory = fastqcDataFilesService.fastqcOutputDirectory(it)
                Path oldBasDirectory = newDirectory.parent

                Path newPath = fastqcDataFilesService.fastqcOutputPath(it)
                Path newPathMd5sum = fastqcDataFilesService.fastqcOutputMd5sumPath(it)
                Path newPathHtml = fastqcDataFilesService.fastqcHtmlPath(it)

                Map<Path, Path> oldToNewMap = [
                        newPath,
                        newPathMd5sum,
                        newPathHtml,
                ].collectEntries {
                    [(oldBasDirectory.resolve(it.fileName)): it]
                }.findAll {
                    Files.exists(it.key)
                }

                if (oldToNewMap) {
                    String projectUnixGroup = it.sequenceFile.project.unixGroup
                    String fileUnixGroup = it.sequenceFile.fileWithdrawn ? withdrawnUnixGroup : projectUnixGroup

                    scriptPerProject << """
                    |
                    |# ${it.sequenceFile} ${it.sequenceFile.seqTrack}
                    |mkdir -p ${newDirectory}
                    |chgrp ${projectUnixGroup} ${newDirectory}
                    |chmod 2750 ${newDirectory}
                    |""".stripMargin()
                    oldToNewMap.each {
                        scriptPerProject << """
                        |mv ${it.key} ${it.value}
                        |chgrp ${fileUnixGroup} ${it.value}
                        |chmod 440  ${it.value}
                        |""".stripMargin()
                    }
                }
            }
            scriptPerProject << "\n\necho end of script ${file}\n"
            Files.deleteIfExists(file)
            fileService.createFileWithContent(file, scriptPerProject.join(''))
            scriptAllProjects << "bash ${file}"
        }
    }
})

Path file = basePath.resolve("all.sh")
Files.deleteIfExists(file)
fileService.createFileWithContent(file, scriptAllProjects.join('\n'))

println "Script generated at: ${file}"

''
