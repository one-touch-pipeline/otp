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
package operations.workflowTriggering.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 * The script only works for the old workflow system. For the new workflow system please use the GUI: https://otp.dkfz.de/otp/triggerAlignment/index
 */

LogThreadLocal.withThreadLog(System.out, { SeqTrack.withTransaction {
    Collection<RoddyBamFile> roddyBamFiles = RoddyBamFile.withCriteria {
        'in'('id', [
                // RoddyBamFile IDs
                -1,

        ] as long[])
    }


    List<SeqType> unsupportedSeqTypes = [
            SeqTypeService.wholeGenomePairedSeqType,
            SeqTypeService.exomePairedSeqType,
            SeqTypeService.chipSeqPairedSeqType,
            SeqTypeService.wholeGenomeBisulfitePairedSeqType,
            SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
    ]

    roddyBamFiles.each {
        assert !unsupportedSeqTypes.contains(it.seqType) : "${it.seqType} is not supported by this script, since it is part of the new workflow system. Please use gui: https://otp.dkfz.de/otp/triggerAlignment/index"    }

    roddyBamFiles.each {
        println "${it}"
    }
    println roddyBamFiles.size()
    println '\n'

    /*
    roddyBamFiles.each {
        it.withdraw()
        it.workPackage.needsProcessing = true
        it.save(flush: true)
        println "restart ${it}"
    }
    //*/
}})
println ''
