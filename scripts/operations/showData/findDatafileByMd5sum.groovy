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

import de.dkfz.tbi.otp.ngsdata.*

/**
 * This script looks up DataFiles for the given md5sums. It lists Project, PID, SeqType, SampleType and SAMPLE_NAME.
 * Simply list the md5sums in the input section below.
 */

// Input

String md5sums = """
# md5sum 1
# md5sum 2

"""

// Code

Closure<List<String>> nameStringToList = { String nameString ->
    List<String> list = []
    nameString.eachLine {
        if (it.trim().isEmpty() || it.trim().startsWith("#")) {
            return
        }
        list << it.trim()
    }
    return list
}

List<String> md5sumList = nameStringToList(md5sums)

md5sumList.each { String md5sum ->
    List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAllByFastqMd5sum(md5sum)

    int numberOfFoundFiles = rawSequenceFiles.size()
    println "${md5sum} ${numberOfFoundFiles > 1 ? "[found ${numberOfFoundFiles} files with the given md5!]" : ''}"
    rawSequenceFiles.each { RawSequenceFile rawSequenceFile ->
        SeqTrack dfSeqTrack = rawSequenceFile.seqTrack
        println """\
        ---> ${rawSequenceFile.fileName}
             ${ctx.lsdfFilesService.getFileFinalPath(rawSequenceFile)}
             Project:     ${dfSeqTrack.project}
             PID:         ${dfSeqTrack.individual.pid}
             SeqType:     ${dfSeqTrack.seqType}
             SampleType:  ${dfSeqTrack.sample.sampleType}
             SAMPLE_NAME: ${dfSeqTrack.sampleIdentifier}
        """.stripIndent()
    }
}

''
