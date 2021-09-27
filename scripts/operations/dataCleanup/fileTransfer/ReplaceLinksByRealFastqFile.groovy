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
// change to true if you want to check all md5sum after copy (this will take a long time)
boolean validateMd5 = false

/*
script part
 */
LsdfFilesService lsdfFilesService = ctx.lsdfFilesService

def transferDataAndCorrectDB(File finalPath, File orginialPathResolved, DataFile df, def script, boolean  validateMd5, def md5Check) {
    File md5 = new File(orginialPathResolved.parent, "${orginialPathResolved.name}.md5sum")
    Long size = orginialPathResolved.size().toLong()
    script << "cp ${orginialPathResolved} ${finalPath}.tmp && mv ${finalPath}.tmp ${finalPath} && chgrp ${df.project.unixGroup} ${finalPath} && chmod 440 ${finalPath}"
    df.fileSize = size

    if (md5.exists()) {
        script << "cp ${orginialPathResolved}.md5sum ${finalPath}.md5sum.tmp && mv ${finalPath}.md5sum.tmp ${finalPath}.md5sum && chgrp ${df.project.unixGroup} ${finalPath}.md5sum && chmod 440 ${finalPath}.md5sum"
        String md5summe = md5.text.split(" ")[0]
        df.md5sum = md5summe
        if(validateMd5) {
            md5Check << "md5sum -c ${finalPath}.md5sum"
        }
    }

    script << ""
    assert df.save()
}

assert ilseNumbers : "Please provide at least one ilse number"

def notRegisteredIlseNumbers = []
def submissions = []
def script = []
def md5Check = []
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
                            transferDataAndCorrectDB(finalPath, orginialPathResolved, df, script, validateMd5, md5Check)
                        }
                    } else {
                        if (finalPath.exists() && orginialPathResolved.exists() && orginialPathResolved.canRead()) {
                            File md5sum = new File(orginialPathResolved.parent, "${orginialPathResolved.name}.md5sum")
                            if (!md5sum.exists()) {
                                if (overview) {
                                    println "SKIP SINCE FILE EXISTS with unknown md5sum: copy ${orginialPathResolved}* to ${finalPath.parent}"
                                }
                            } else if (md5sum.text.contains(df.md5sum)) {
                                if (overview) {
                                    println "SKIP SINCE FILE EXISTS with correct md5sum: copy ${orginialPathResolved}* to ${finalPath.parent}"
                                }
                            } else {
                                if (overview) {
                                    println "FILE EXISTS already but MD5 is different: skip copy ${orginialPathResolved}* to ${finalPath.parent} " +
                                            "(or change the flag to true)"
                                } else {
                                    if (checked) {
                                        transferDataAndCorrectDB(finalPath, orginialPathResolved, df, script, validateMd5, md5Check)
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
        println "umask 027"
        println "set -v"
        println "set -e"
        println "\n\n"
        println script.join("\n")
        if (md5Check) {
            println md5Check.join("\n")
        }
    } else if (!overview){
        println "nothing to copy"
    } else {
        println "only overview modus so no copy script"
    }
}

""
