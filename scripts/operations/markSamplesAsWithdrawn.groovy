import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import org.joda.time.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


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




assert comment: 'Please provide a comment why the data are set to withdrawn'
SeqTrack.withTransaction {
    Withdrawer.ctx = ctx
    Withdrawer.comment = comment
    seqTracks.each {
        Withdrawer.withdraw(it)
    }
    assert false
}

class Withdrawer {

    static ctx

    static String comment

    static void withdraw(final SeqTrack seqTrack) {
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

        DataFile.findAllBySeqTrack(seqTrack).each { withdraw(it) }
    }

    static void withdraw(final DataFile dataFile) {
        final MetaDataEntry withdrawnEntry = atMostOneElement(MetaDataEntry.findAllWhere(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.WITHDRAWN.toString()))))
        if (!withdrawnEntry) {
            withdrawnEntry = new MetaDataEntry(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.WITHDRAWN.toString())), value: '', source: MetaDataEntry.Source.MANUAL)
            withdrawnEntry.save(flush: true)
        }
        if (withdrawnEntry.value != '1') {
            ctx.metaDataService.updateMetaDataEntry(withdrawnEntry, '1')
        }

        final MetaDataEntry withdrawnDateEntry = atMostOneElement(MetaDataEntry.findAllWhere(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.WITHDRAWN_DATE.toString()))))
        if (!withdrawnDateEntry) {
            withdrawnDateEntry = new MetaDataEntry(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.WITHDRAWN_DATE.toString())), value: '', source: MetaDataEntry.Source.MANUAL)
            withdrawnDateEntry.save(flush: true)
        }
        if (!(withdrawnDateEntry.value ==~ /^[0-9]{4}-[0-1][0-9]-[0-3][0-9]$/)) {
            ctx.metaDataService.updateMetaDataEntry(withdrawnDateEntry, LocalDate.now().toString())
        }

        final MetaDataEntry withdrawnCommentEntry = atMostOneElement(MetaDataEntry.findAllWhere(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName("WITHDRAWN_COMMENT"))))
        if (!withdrawnCommentEntry) {
            withdrawnCommentEntry = new MetaDataEntry(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName("WITHDRAWN_COMMENT")), value: '', source: MetaDataEntry.Source.MANUAL)
            withdrawnCommentEntry.save(flush: true)
        }
        if (!(withdrawnCommentEntry.value.contains(comment))) {
            def c = withdrawnCommentEntry.value ? "${withdrawnCommentEntry.value}\n${comment}" : comment
            ctx.metaDataService.updateMetaDataEntry(withdrawnCommentEntry, c)
        }

        println "Withdrawing DataFile ${dataFile}"
        dataFile.fileWithdrawn = true
        assert dataFile.save(flush: true)
    }
}
