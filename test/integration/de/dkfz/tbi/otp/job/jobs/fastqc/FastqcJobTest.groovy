package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Test

class FastqcJobTest {

    @Test
    void testExecute_allFine_returnsCorrectCommand() {
        ProcessingOptionService processingOptionService = new ProcessingOptionService()
        processingOptionService.createOrUpdate("fastqcCommand", null, null, "fastqc-0.10.1", "command for fastqc")

        SeqTrack seqTrack = SeqTrack.build()
        Realm realm = Realm.build()
        DataFile dataFile = DataFile.build(fileExists: true, fileSize: 1L)
        FastqcJob fastqcJob = new FastqcJob()

        fastqcJob.metaClass.getProcessParameterValue { -> seqTrack.id as String }

        fastqcJob.fastqcDataFilesService = [
                fastqcOutputDirectory: {s -> 'outputDir'},
                fastqcRealm: {s -> realm},
                createFastqcProcessedFile: {DataFile d -> assert dataFile == d}
        ] as FastqcDataFilesService
        fastqcJob.seqTrackService = [ getSequenceFilesForSeqTrack: {s -> [dataFile]} ] as SeqTrackService
        fastqcJob.lsdfFilesService = [ getFileFinalPath: {s -> 'finalPath'} ] as LsdfFilesService
        fastqcJob.executionHelperService = [
                sendScript: {Realm inputRealm, String inputCommand ->
                    assert realm == inputRealm
                    assert "fastqc-0.10.1 finalPath --noextract --nogroup -o outputDir;chmod -R 440 outputDir/*.zip" == inputCommand
                    return 'pbsJobId'
                }
        ] as ExecutionHelperService
        fastqcJob.metaClass.addOutputParameter {String name, String value -> 'pbsJobId' == value || realm.id.toString() == value}

        fastqcJob.execute()
    }
}
