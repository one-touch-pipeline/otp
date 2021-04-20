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
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 * Script to withdrawn a sample.
 *
 * It fetches all seqTracks for all samples given by PID, SAMPLE_TYPE SEQ_TYPE and LIBRARY_LOAYOUT and mark the
 * DataFile, the bam files and the depending objects as withdrawn.
 *
 * A Comment is added to the DataFile with the reason for withdrawing. Also it is marked in the MetaDataEntry of the datafile.
 */

String comment = """
"""

//PID SAMPLE_TYPE SEQ_TYPE LIBRARY_LAYOUT
List<SeqTrack> seqTracks = """


""".split('\n').findAll().collect {
    String[] split = it.split(' ')
    println split as List
    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(split[0]))
    SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[1]))
    SeqType seqType = CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayoutAndSingleCell(split[2], split[3], false))
    Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType))
    List<SeqTrack> seqTracks = SeqTrack.findAllBySampleAndSeqType(sample, seqType)
}.flatten()

List dirsToDelete = []
assert comment: 'Please provide a comment why the data are set to withdrawn'
SeqTrack.withTransaction {
    Withdrawer.ctx = ctx
    Withdrawer.comment = comment.trim()
    seqTracks.each {
        Withdrawer.withdraw(it, dirsToDelete)
    }
    println dirsToDelete.join("\n")
    assert false
}

class Withdrawer {

    static ctx

    static String comment
    static Date date = new Date()

    static void withdraw(final SeqTrack seqTrack, List dirsToDelete) {
        println "\n\nwithdraw $seqTrack"

        LogThreadLocal.withThreadLog(System.out) {
            RoddyBamFile.createCriteria().list {
                seqTracks {
                    eq('id', seqTrack.id)
                }
            }.each {
                it.withdraw()
            }

            ProcessedBamFile.withCriteria {
                alignmentPass {
                    'eq'("seqTrack", seqTrack)
                }
            }.each {
                it.withdraw()
            }
        }

        MergingWorkPackage.createCriteria().list {
            seqTracks {
                eq('id', seqTrack.id)
            }
        }.each { mwp ->
            mwp.seqTracks = mwp.seqTracks.findAll { it.id != seqTrack.id } as Set
            assert mwp.save(flush: true)
        }

        DataFile.findAllBySeqTrack(seqTrack).each { withdraw(it, dirsToDelete) }
    }

    static void withdraw(final DataFile dataFile, List dirsToDelete) {
        println "Withdrawing DataFile ${dataFile}"
        dirsToDelete.add("rm ${ctx.lsdfFilesService.getFileViewByPidPath(dataFile)}")
        dataFile.withdrawnDate = date
        if (!dataFile.withdrawnComment?.contains(comment)) {
            dataFile.withdrawnComment = "${dataFile.withdrawnComment ? "${dataFile.withdrawnComment}\n": ""}${comment}"
        }
        dataFile.fileWithdrawn = true
        assert dataFile.save(flush: true)
    }
}
