/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Files


/*
input area
 */
// here you can list all Ilse numbers (1, 2, 3, ...) you want to see the fastq files for
def ilseNumbers = []
// only provides an overview of what will be done per file
boolean overview = true
// change to true if you are sure that the changed md5sum is okay
boolean checked = false

/*
script part
 */
LsdfFilesService lsdfFilesService = ctx.lsdfFilesService

def transferDataAndCorrectDB(File finalPath, File orginialPathResolved, DataFile df, def script) {
    def md5 = new File(orginialPathResolved.parent, "${orginialPathResolved.name}.md5sum")
    String md5summe = md5.text.split(" ")[0]
    Long size = orginialPathResolved.size().toLong()
    script << "rm ${finalPath}*"
    script << "cp ${orginialPathResolved}* ${finalPath.parent}"
    script << "chgrp ${df.project.unixGroup} ${finalPath.parent}/*"
    script << "chmod 640 ${finalPath.parent}/*"
    script << ""
    df.md5sum = md5summe
    df.fileSize = size
    assert df.save(flush: true)
}

assert ilseNumbers : "Please provide at least one ilse number"

def notRegisteredIlseNumbers = []
def submissions = []
def script = []
SeqTrack.withTransaction {
    ilseNumbers.each { ilseNumber ->
        try {
            def submission = CollectionUtils.exactlyOneElement(IlseSubmission.findAllByIlseNumber(ilseNumber))
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

    submissions.each { submission ->
        SeqTrack.findAllByIlseSubmission(submission).each { s ->
            s.getDataFiles().each { df ->
                File finalPath = new File(lsdfFilesService.getFileFinalPath(df))
                File initialPath = new File (lsdfFilesService.getFileInitialPath(df))

                if (!initialPath.exists()) {
                    if (overview) {
                        println "SKIP SINCE ORIGIN CAN NOT BE RETRIEVED BASED ON: ${initialPath}"
                    }
                } else if (!(initialPath.toPath().toRealPath().toFile().exists())) {
                    if (overview) {
                        println "SKIP SINCE ORIGIN DOES NOT EXIST: ${initialPath.toPath().toRealPath().toFile()}"
                    }
                } else {
                    File orginialPathResolved = initialPath.toPath().toRealPath().toFile()
                    boolean isSymbolicLink = Files.isSymbolicLink(finalPath.toPath())

                    if ((!finalPath.exists() || isSymbolicLink) && orginialPathResolved.exists() && orginialPathResolved.canRead()) {
                        if (overview) {
                            println "copy ${orginialPathResolved}* to ${finalPath.parent}"
                        } else {
                            transferDataAndCorrectDB(finalPath, orginialPathResolved, df, script)
                        }
                    } else {
                        if (finalPath.exists() && orginialPathResolved.exists() && orginialPathResolved.canRead()) {
                            if (new File(orginialPathResolved.parent, "${orginialPathResolved.name}.md5sum").text.contains(df.md5sum)) {
                                if (overview) {
                                    println "SKIP SINCE FILE EXISTS: copy ${orginialPathResolved}* to ${finalPath.parent}"
                                }
                            } else {
                                if (overview) {
                                    println "FILE EXISTS already but MD5 is different: skip copy ${orginialPathResolved}* to ${finalPath.parent} " +
                                            "(or change the flag to true)"
                                } else {
                                    if (checked) {
                                        transferDataAndCorrectDB(finalPath, orginialPathResolved, df, script)
                                    }
                                }
                            }
                        } else if (!orginialPathResolved.canRead()) {
                            if (overview) {
                                println "SKIP SINCE ORIGIN IS NOT READABLE: copy ${orginialPathResolved}* to ${finalPath.parent}"
                            }
                        } else  {
                            assert false : "This case should not occur -> contact software developer"
                        }
                    }
                }
            }
        }
    }

    if (script) {
        println "#!/bin/bash"
        println "set -v"
        println "set -e"
        println "\n\n"
        println script.join("\n")
    } else if (!overview){
        println "nothing to copy"
    } else {
        println "only overview modus so no copy script"
    }
}

""