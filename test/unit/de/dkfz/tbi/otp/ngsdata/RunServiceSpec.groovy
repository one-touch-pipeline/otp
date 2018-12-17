package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.TestCase

@Mock([
        DataFile,
        FileType,
        Individual,
        MetaDataFile,
        Project,
        ProjectCategory,
        Realm,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
])
class RunServiceSpec extends Specification {

    RunService runService = new RunService()

    void 'retrieveMetaDataFiles finds correct MetaDataFiles'() {
        given:
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup(seqPlatformGroups: null)

        Run runWithoutDataFile = DomainFactory.createRun(seqPlatform: seqPlatform)

        Run run1 = DomainFactory.createRun(seqPlatform: seqPlatform)
        MetaDataFile run1MetaDataFileA = DomainFactory.createMetaDataFile()
        DomainFactory.createDataFile(run: run1, runSegment: run1MetaDataFileA.runSegment)
        MetaDataFile run1MetaDataFileB = DomainFactory.createMetaDataFile()
        MetaDataFile run1MetaDataFileC = DomainFactory.createMetaDataFile(runSegment: run1MetaDataFileB.runSegment)
        DomainFactory.createDataFile(run: run1, runSegment: run1MetaDataFileB.runSegment)

        Run run2 = DomainFactory.createRun(seqPlatform: seqPlatform)
        MetaDataFile run2MetaDataFile = DomainFactory.createMetaDataFile()
        DomainFactory.createDataFile(run: run2, runSegment: run2MetaDataFile.runSegment)
        DomainFactory.createDataFile(run: run2, runSegment: run2MetaDataFile.runSegment)

        expect:
        runService.retrieveMetaDataFiles(runWithoutDataFile).isEmpty()
        TestCase.assertContainSame(runService.retrieveMetaDataFiles(run1), [run1MetaDataFileA, run1MetaDataFileB, run1MetaDataFileC])
        TestCase.assertContainSame(runService.retrieveMetaDataFiles(run2), [run2MetaDataFile])
    }
}
