/*
 * Copyright 2011-2022 The OTP authors
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

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Files
import java.nio.file.Path

/*
input area
 */
// here you can list all Ilse numbers (1, 2, 3, ...) you want to see the fastq files for
List<Integer> ilseNumbers = []
// only provides an overview of what will be done per file
boolean overview = true
// change to true if you are sure that the changed md5sum is okay
boolean checked = false
// change to true if you want to check all md5sum after copy (this will take a long time)
boolean validateMd5 = false

/*
script part
 */
LsdfFilesService lsdfFilesService = ctx.lsdfFilesService
FileService fileService = ctx.fileService

static def transferDataAndCorrectDB(Path finalPath, Path originalPathResolved, DataFile df, def script, boolean validateMd5, def md5Check, FileService fileService) {
    Path md5sumPath = finalPath.parent.resolve("${finalPath.fileName}.md5sum")
    if (Files.exists(md5sumPath)) {
        Files.delete(md5sumPath)
    }
    String content = "${df.md5sum}  ${finalPath.fileName}"
    fileService.createFileWithContentOnDefaultRealm(md5sumPath, content)

    script << "rsync -uvL --group=${df.project.unixGroup} --perms=440 ${originalPathResolved} ${finalPath}"

    if (validateMd5) {
        md5Check << "cd ${md5sumPath.parent} && md5sum -c ${md5sumPath}" //check md5sum from otp
    }

    Long size = originalPathResolved.size().toLong()
    df.fileSize = size
    df.save(flush: true)
}

assert ilseNumbers: "Please provide at least one ilse number"

List<Integer> notRegisteredIlseNumbers = []
List<IlseSubmission> submissions = []
List<String> script = []
List<String> md5Check = []

SeqTrack.withTransaction {
    ilseNumbers.each { Integer ilseNumber ->
        try {
            IlseSubmission submission = CollectionUtils.exactlyOneElement(IlseSubmission.findAllByIlseNumber(ilseNumber as int))
            submissions << submission
        } catch (AssertionError e) {
            notRegisteredIlseNumbers << ilseNumber
        }
    }

    if (notRegisteredIlseNumbers) {
        println "the following ilse numbers could not be found in OTP: ${notRegisteredIlseNumbers.join(", ")}"
        assert false
    }

    if (overview && submissions) {
        println "The following files would be copied since the source still exists and the copied file is gone"
    }

    submissions.each { IlseSubmission submission ->
        SeqTrack.findAllByIlseSubmission(submission).each { SeqTrack seqTrack ->
            seqTrack.dataFiles.each { DataFile df ->
                Path finalPath = lsdfFilesService.getFileFinalPathAsPath(df)
                Path initialPath = lsdfFilesService.getFileInitialPathAsPath(df, finalPath.fileSystem)

                if (!Files.exists(initialPath)) {
                    if (overview) {
                        println "SKIP SINCE ORIGIN CAN NOT BE RETRIEVED BASED ON: ${initialPath}"
                    }
                } else if (!(Files.exists(initialPath.toRealPath()))) {
                    if (overview) {
                        println "SKIP SINCE ORIGIN DOES NOT EXIST: ${initialPath.toRealPath()}"
                    }
                } else {
                    Path originalPathResolved = initialPath.toRealPath()
                    boolean isSymbolicLink = Files.isSymbolicLink(finalPath)

                    if ((!Files.exists(finalPath) || isSymbolicLink) && Files.exists(originalPathResolved) && Files.isReadable(originalPathResolved)) {

                        if (overview) {
                            println "copy ${originalPathResolved}* to ${finalPath.parent}"
                        } else {
                            transferDataAndCorrectDB(finalPath, originalPathResolved, df, script, validateMd5, md5Check, fileService)
                        }
                    } else {
                        if (Files.exists(finalPath) && Files.exists(originalPathResolved) && Files.isReadable(originalPathResolved)) {
                            Path md5sum = originalPathResolved.resolveSibling("${originalPathResolved.fileName}.md5sum")
                            if (!Files.exists(md5sum)) {
                                if (overview) {
                                    println "SKIP SINCE FILE EXISTS with unknown md5sum: copy ${originalPathResolved} to ${finalPath.parent}"
                                }
                            } else if (md5sum.text.contains(df.md5sum)) {
                                if (overview) {
                                    println "SKIP SINCE FILE EXISTS with correct md5sum: copy ${originalPathResolved} to ${finalPath.parent}"
                                }
                            } else {
                                if (overview) {
                                    println "FILE EXISTS already but MD5 is different: skip copy ${originalPathResolved} to ${finalPath.parent} " +
                                            "(or change the flag to true)"
                                } else {
                                    if (checked) {
                                        transferDataAndCorrectDB(finalPath, originalPathResolved, df, script, validateMd5, md5Check, fileService)
                                    }
                                }
                            }
                        } else if (!Files.isReadable(originalPathResolved)) {
                            if (overview) {
                                println "SKIP SINCE ORIGIN IS NOT READABLE: copy ${originalPathResolved}* to ${finalPath.parent}"
                            }
                        } else {
                            assert false: "This case should not occur -> contact software developer"
                        }
                    }
                }
            }
        }
    }

    if (script) {
        println "#!/bin/bash"
        println "umask 027"
        println "set -v"
        println "set -e"
        println "\n\n"
        println script.join("\n")
        if (md5Check) {
            println md5Check.join("\n")
        }
    } else if (!overview) {
        println "nothing to copy"
    } else {
        println "only overview mode so no copy script"
    }
}

""
