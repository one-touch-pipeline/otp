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


import org.joda.time.LocalDate

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement


/**
 * Script to remove the mark as withdrawn.
 *
 * It fetches all seqTracks for all samples given by PID, SAMPLE_TYPE SEQ_TYPE and LIBRARY_LOAYOUT and remove the withdrawn flag.
 * It does not consider alignment or analysis, since they could also for other reason marked as withdrawn.
 *
 * Also the withdraw MetadataEntries are adapted.
 */


String comment = """
"""

//PID SAMPLE_TYPE SEQ_TYPE LIBRARY_LAYOUT SINGLE_CELL
List<SeqTrack> seqTracks = """



""".split('\n').findAll().collect {
    String[] split = it.split(' ')
    println split as List
    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(split[0]))
    SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[1]))
    SeqType seqType = CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayoutAndSingleCell(split[2], split[3], Boolean.valueOf(split[4])))
    Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType))
    SeqTrack.findAllBySampleAndSeqType(sample, seqType)
}.flatten()




assert comment: 'Please provide a comment why the data are set to withdrawn'
SeqTrack.withTransaction {
    UnWithdrawer.ctx = ctx
    UnWithdrawer.comment = comment
    seqTracks.each {
        UnWithdrawer.unwithdraw(it)
    }
    assert false: 'Fail for debug reason, remove if the output is okay'
}

class UnWithdrawer {

    static ctx

    static String comment

    static void unwithdraw(final SeqTrack seqTrack) {
        println "\n\nunwithdraw $seqTrack"

        DataFile.findAllBySeqTrack(seqTrack).each { unwithdraw(it) }
    }

    static void unwithdraw(final DataFile dataFile) {
        final MetaDataEntry withdrawnEntry = atMostOneElement(MetaDataEntry.findAllWhere(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.WITHDRAWN.toString()))))
        if (!withdrawnEntry) {
            withdrawnEntry = new MetaDataEntry(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.WITHDRAWN.toString())), value: '', source: MetaDataEntry.Source.MANUAL)
            withdrawnEntry.save(flush: true)
        }
        if (withdrawnEntry.value != '0') {
            ctx.metaDataService.updateMetaDataEntry(withdrawnEntry, '0')
        }

        final MetaDataEntry unwithdrawnDateEntry = atMostOneElement(MetaDataEntry.findAllWhere(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName("UNWITHDRAWN_DATE"))))
        if (!unwithdrawnDateEntry) {
            unwithdrawnDateEntry = new MetaDataEntry(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName("UNWITHDRAWN_DATE")), value: '', source: MetaDataEntry.Source.MANUAL)
            unwithdrawnDateEntry.save(flush: true)
        }
        if (!(unwithdrawnDateEntry.value ==~ /^[0-9]{4}-[0-1][0-9]-[0-3][0-9]$/)) {
            ctx.metaDataService.updateMetaDataEntry(unwithdrawnDateEntry, LocalDate.now().toString())
        }

        final MetaDataEntry withdrawnCommentEntry = atMostOneElement(MetaDataEntry.findAllWhere(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName("WITHDRAWN_COMMENT"))))
        if (!withdrawnCommentEntry) {
            withdrawnCommentEntry = new MetaDataEntry(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName("WITHDRAWN_COMMENT")), value: '', source: MetaDataEntry.Source.MANUAL)
            withdrawnCommentEntry.save(flush: true)
        }
        if (!(withdrawnCommentEntry.value.contains(comment))) {
            def c = withdrawnCommentEntry.value ? "${withdrawnCommentEntry.value}, ${comment}" : comment
            ctx.metaDataService.updateMetaDataEntry(withdrawnCommentEntry, c)
        }

        println "Unwithdrawing DataFile ${dataFile}"
        dataFile.fileWithdrawn = false
        assert dataFile.save(flush: true)
    }
}
