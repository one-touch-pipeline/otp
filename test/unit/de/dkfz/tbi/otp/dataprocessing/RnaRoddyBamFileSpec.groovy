package de.dkfz.tbi.otp.dataprocessing

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
        MergingCriteria,
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
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
        MergingWorkPackage,
])
class RnaRoddyBamFileSpec extends Specification {

    void "test method getCorrespondingWorkChimericBamFile"() {
        given:
        RnaRoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([:], RnaRoddyBamFile)
        DomainFactory.createRealmDataManagement([name: roddyBamFile.project.realmName])
        DomainFactory.createRealmDataProcessing([name: roddyBamFile.project.realmName])
        String testDir = "${roddyBamFile.individual.getViewByPidPath(roddyBamFile.seqType).absoluteDataManagementPath.path}/${roddyBamFile.sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment"
        AbstractMergedBamFileService.metaClass.static.destinationDirectory = { AbstractMergedBamFile bamFile -> return TEST_DIR }

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.sampleType.dirName}_${roddyBamFile.individual.pid}_${RnaRoddyBamFile.CHIMERIC_BAM_SUFFIX}" == roddyBamFile.correspondingWorkChimericBamFile.path
    }
}
