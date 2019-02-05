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

import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * removes the complete content of a project from the OTP database
 * -> but not the files in the file-system. They are needed to include again to OTP.
 * When they are in OTP again, it is easy to remove the project manually from the filesystem.
 */

// input area
//----------------------

String projectName = ""

//script area
//-----------------------------

assert projectName

// `flush: true` is intentionally left out at certain places to improve performance
Project.withTransaction {

    Project project = exactlyOneElement(Project.findAllByName(projectName))

    Individual.findAllByProject(project).each { Individual individual ->

        ClusterJob.findAllByIndividual(individual)*.delete()

        Sample.findAllByIndividual(individual).each { Sample sample ->

            SampleIdentifier.findAllBySample(sample)*.delete(flush: true)

            /*
             * Delete seqTrack and all corresponding information and processing results (fastq, fastqc, bam, qa)
             */
            SeqTrack.findAllBySample(sample).each {
                ctx.dataSwapService.deleteSeqTrack(it)
            }

            SeqScan.findAllBySample(sample).each {
                ctx.dataSwapService.deleteSeqScanAndCorrespondingInformation(it)
            }

            sample.delete(flush: true)
        }
        individual.delete(flush: true)
    }

    /*
     * There are files which are not connected to a seqTrack -> they have to be deleted, too
     */
    DataFile.findAllByProject(project).each {
        ctx.dataSwapService.deleteDataFile(it)
    }

    //just used while testing to make sure that the DB changes are rolled back -> otherwise each time a new DB dump has to be included
    assert false
}
