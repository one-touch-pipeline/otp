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
package operations.dataInstallation

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.util.TimeFormats

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * script to change md5Sums of Datafiles
 * runName = Name of {@link Run}, the {@link RawSequenceFile} belongs to as String
 * rawSequenceFileMap = Map of {@link RawSequenceFile} fileName as String and its new md5Sum as String
 *               rawSequenceFileMap = [fileName: md5Sum, fileName: md5Sum]
 */

String executingUser = ""
List<String> runNames = [""]
Map<String, String> rawSequenceFileMap = [:]

assert executingUser: "Your username is used to set a comment that informs of the changes to the RawSequenceFile"
assert runNames: "Define the Runs"
runNames.each {
    assert it: "Invalid run in list of runs"
}

List<Run> runs = Run.findAllByNameInList(runNames)
assert runs.size() == runNames.size(): "Didn't find Runs for all run names"

RawSequenceFile.withTransaction {
    rawSequenceFileMap.each { String fileName, String newMd5sum ->
        RawSequenceFile rawSequenceFile = exactlyOneElement(RawSequenceFile.findAllByRunInListAndFileName(runs, fileName))

        String fromMd5 = rawSequenceFile.fastqMd5sum
        String toMd5 = newMd5sum
        Long fromFileSize = rawSequenceFile.fileSize
        Long toFileSize = new File(ctx.lsdfFilesService.getFileViewByPidPath(rawSequenceFile)).size()

        if (fromMd5 != toMd5) {
            rawSequenceFile.fastqMd5sum = newMd5sum
            rawSequenceFile.fileSize = toFileSize
            String newComment = """${TimeFormats.DATE_TIME.getFormattedDate(new Date())}
  changed md5sum   from: ${fromMd5} to: ${toMd5}
  changed fileSize from: ${fromFileSize} to: ${toFileSize}"""
            println(newComment)
            String previous = rawSequenceFile.comment?.comment
            ctx.commentService.createOrUpdateComment(
                    rawSequenceFile,
                    previous + (previous? "\n\n" : "") + newComment,
                    executingUser
            )
            rawSequenceFile.save(flush: true)
        } else {
            println "Nothing done for ${fileName} with md5sum ${newMd5sum}"
        }
    }
    assert false: "Intentional failure for debugging"
}
