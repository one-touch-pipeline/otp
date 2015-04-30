package scripts.updates

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase

class AddReadNumberIntoDataFilesTests extends GroovyScriptAwareTestCase {

    static final String SCRIPT_NAME = "scripts/updates/AddReadNumberIntoDataFiles.groovy"

    void testUpdateDataFile_SpecialRun() {
        Run run = Run.build(name: '140930_XY123_0815_D2748ACXX')
        DataFile dataFile = DataFile.build(run: run)
        dataFile.readNumber == null
        invokeMethod(new File(SCRIPT_NAME), 'updateDataFile', [dataFile])
        assert dataFile.readNumber == null
    }

    void testUpdateDataFile_NoSeqTrack() {
        Run run = Run.build()
        DataFile dataFile = DataFile.build(run: run)
        dataFile.seqTrack = null
        dataFile.fileName = 'SOMEPID_L001_R2.fastq.gz'
        invokeMethod(new File(SCRIPT_NAME), 'updateDataFile', [dataFile])
        assert dataFile.readNumber == 2
    }

    void testUpdateDataFile_SingleEnd() {
        SeqTrack seqTrack = SeqTrack.build()
        Run run = Run.build()
        DataFile dataFile = DataFile.build(seqTrack: seqTrack, run: run)
        dataFile.seqTrack.seqType.libraryLayout = 'SINGLE'
        invokeMethod(new File(SCRIPT_NAME), 'updateDataFile', [dataFile])
        assert dataFile.readNumber == 1
    }

    void testUpdateDataFile_PairedEnd() {
        SeqTrack seqTrack = SeqTrack.build()
        Run run = Run.build()
        DataFile dataFile = DataFile.build(seqTrack: seqTrack, run: run)
        dataFile.seqTrack.seqType.libraryLayout = 'PAIRED'
        dataFile.fileName = 'SOMEPID_L001_R2.fastq.gz'
        invokeMethod(new File(SCRIPT_NAME), 'updateDataFile', [dataFile])
        assert dataFile.readNumber == 2
    }

    void testUpdateDataFile_NoMatch() {
        SeqTrack seqTrack = SeqTrack.build()
        Run run = Run.build()
        DataFile dataFile = DataFile.build(seqTrack: seqTrack, run: run)
        dataFile.seqTrack.seqType.libraryLayout = 'PAIRED'
        dataFile.fileName = 'bla-bla.fastq.gz'
        shouldFail(RuntimeException, { invokeMethod(new File(SCRIPT_NAME), 'updateDataFile', [dataFile]) })
        assert dataFile.readNumber == null
    }
}
