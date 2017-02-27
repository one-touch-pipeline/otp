package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        Pipeline,
        Project,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RnaRoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqTrack,
        SeqType,
        SoftwareTool,
        MergingWorkPackage,
])
class RnaRoddyBamFileSpec extends Specification {

    final String TEST_DIR = TestCase.getUniqueNonExistentPath()

    void "test method getCorrespondingWorkChimericBamFile"() {
        given:
        RnaRoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([:], RnaRoddyBamFile)
        DomainFactory.createRealmDataManagement([name: roddyBamFile.project.realmName])
        DomainFactory.createRealmDataProcessing([name: roddyBamFile.project.realmName])
        AbstractMergedBamFileService.metaClass.static.destinationDirectory = { AbstractMergedBamFile bamFile -> return TEST_DIR }

        expect:
        "${TEST_DIR}/${roddyBamFile.workDirectoryName}/${roddyBamFile.sampleType.dirName}_${roddyBamFile.individual.pid}.${RnaRoddyBamFile.CHIMERIC_BAM_SUFFIX}" == roddyBamFile.correspondingWorkChimericBamFile.path
    }
}
