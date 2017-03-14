package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

@Mock([
        AdapterFile,
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingWorkPackage,
        Pipeline,
        ProcessingOption,
        Project,
        ProjectCategory,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SequencingKitLabel,
        SeqTrack,
        SeqType,
        SnvConfig,
        SoftwareTool,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        Realm,
        RnaRoddyBamFile,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
])
class AbstractRoddyAlignmentJobSpec extends Specification {


    @Rule
    TemporaryFolder temporaryFolder


    void "prepareAndReturnAlignmentCValues, when RoddyBamFile is null, throw assert"() {
        given:
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob)

        when:
        abstractRoddyAlignmentJob.prepareAndReturnAlignmentCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains("assert roddyBamFile")
    }

    @Unroll
    void "prepareAndReturnAlignmentCValues, when all fine, return list of cvalues"() {
        given:
        AbstractRoddyAlignmentJob job = Spy(AbstractRoddyAlignmentJob)
        File referenceGenomeFilePath = CreateFileHelper.createFile(temporaryFolder.newFile())
        File chromosomeStatSizeFilePath = CreateFileHelper.createFile(temporaryFolder.newFile())

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()

        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            1 * fastaFilePath(_) >> referenceGenomeFilePath
            1 * chromosomeStatSizeFile(_) >> chromosomeStatSizeFilePath
        }

        List<String> expected = [
                "INDEX_PREFIX:${referenceGenomeFilePath.path}",
                "CHROM_SIZES_FILE:${chromosomeStatSizeFilePath.path}",
                "possibleControlSampleNamePrefixes:${roddyBamFile.getSampleType().dirName}",
                "possibleTumorSampleNamePrefixes:",
        ]

        if (adapter == "YES" ) {
            File adapterFilePath = CreateFileHelper.createFile(temporaryFolder.newFile())
            AdapterFile adapterFile = DomainFactory.createAdapterFile()
            roddyBamFile.seqTracks.each {
                it.adapterFile = adapterFile
                it.save(flush: true)
            }

            job.adapterFileService = Mock(AdapterFileService) {
                1* fullPath(_) >> adapterFilePath
            }

            expected.addAll("CLIP_INDEX:${adapterFilePath}", "useAdaptorTrimming:true")
        }

        expected.add("runFingerprinting:false",)

        when:
        List<String> value = job.prepareAndReturnAlignmentCValues(roddyBamFile)

        then:
        expected == value

        where:
        adapter << ["YES", "NO"]
    }


    void "validate, when RoddyBamFile is null, throw assert"() {
        given:
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob)

        when:
        abstractRoddyAlignmentJob.validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains("Input roddyBamFile must not be null")
    }


    void "validate, when changing permissions fail, throw assert"() {
        given:
        String errorMessage = HelperUtils.uniqueString
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> {
                    throw new AssertionError(errorMessage)
                }
            }
            getConfigService() >> Mock(ConfigService) {
                getRealmDataProcessing(_) >> new Realm()
            }
            0 * ensureCorrectBaseBamFileIsOnFileSystem(_)

        }
        RoddyBamFile roddyBamFile = Mock(RoddyBamFile) {
            getProject() >> new Project()
        }

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        AssertionError e = thrown()
        errorMessage == e.message
    }


    void "validate, when baseBamFile change, throw assert"() {
        given:
        String errorMessage = HelperUtils.uniqueString
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> {}
            }
            getConfigService() >> Mock(ConfigService) {
                getRealmDataProcessing(_) >> new Realm()
            }
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> {
                throw new AssertionError(errorMessage)
            }

        }
        RoddyBamFile roddyBamFile = Mock(RoddyBamFile) {
            getProject() >> new Project()
        }

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        RuntimeException e = thrown()
        e.message.contains('The input BAM file seems to have changed ')
        errorMessage == e.cause.message
    }


    @Unroll
    void "validate, when #file not exist, throw assert"() {
        given:
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> {}
            }
            getConfigService() >> new ConfigService()
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> {}
            validateReadGroups(_) >> {}
            workflowSpecificValidation(_) >> {}

        }

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        CreateFileHelper.createFile(new File(roddyBamFile.workMergedQAJsonFile.parent, 'OtherFile.txt"'))
        CreateFileHelper.createFile(new File(roddyBamFile.workExecutionStoreDirectory.parent, 'OtherFile.txt"'))

        File deletedFile
        if (file == 'workSingleQaJson') {
            deletedFile = roddyBamFile.workSingleLaneQADirectories.values().first()
        } else {
            deletedFile = roddyBamFile."${file}"
        }
        assert deletedFile.delete() || deletedFile.deleteDir()

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains(deletedFile.path)

        where:
        file << [
                'workBamFile',
                'workBaiFile',
                'workMd5sumFile',
                'workMergedQADirectory',
                'workMergedQAJsonFile',
                'workSingleQaJson',
                'workExecutionStoreDirectory',
        ]
    }


    void "validate, when wrong operation status, throw assert"() {
        given:
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> {}
            }
            getConfigService() >> new ConfigService()
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> {}
            validateReadGroups(_) >> {}
            workflowSpecificValidation(_) >> {}

        }

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                md5sum             : null,
        ])
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains(AbstractMergedBamFile.FileOperationStatus.INPROGRESS.name())
    }


    void "validate, when all fine, return without exception"() {
        given:
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> {}
            }
            getConfigService() >> new ConfigService()
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> {}
            1 * validateReadGroups(_) >> {}
            1 * workflowSpecificValidation(_) >> {}

        }

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                md5sum             : null,
        ])
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        noExceptionThrown()
    }


    void "validate, when all fine and seqtype is RNA, return without exception"() {
        given:
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> {}
            }
            getConfigService() >> new ConfigService()
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> {}
            1 * validateReadGroups(_) >> {}
            1 * workflowSpecificValidation(_) >> {}

        }

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                md5sum             : null,
        ], RnaRoddyBamFile)
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        roddyBamFile.workSingleLaneQAJsonFiles.values().each { File file ->
            file.delete()
        }

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        noExceptionThrown()
    }


    void "validateReadGroups, when read groups are not as expected, throw an exception"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])
        DomainFactory.createRealmDataProcessing(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])

        String readGroupHeaders = roddyBamFile.containedSeqTracks.collect {
            "@RG     ID:${RoddyBamFile.getReadGroupName(it)}        LB:tumor_123    PL:ILLUMINA     SM:sample_tumor_123"
        }.join('\n') + '\n'

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles(roddyBamFile.mergingWorkPackage)
        roddyBamFile.seqTracks.add(seqTrack)
        roddyBamFile.numberOfMergedLanes++
        roddyBamFile.save()

        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getConfigService() >> new ConfigService()
        }

        GroovyMock(ProcessHelperService, global: true) {
            ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(_) >> { String command ->
                String expectedCommand = "set -o pipefail; samtools view -H ${roddyBamFile.workBamFile} | grep ^@RG\\\\s"
                assert command == expectedCommand
                return readGroupHeaders
            }
        }

        String expectedErrorMessage = """Read groups in BAM file are not as expected.
Read groups in ${roddyBamFile.workBamFile}:
${(roddyBamFile.containedSeqTracks - seqTrack).collect { RoddyBamFile.getReadGroupName(it) }.join('\n')}
Expected read groups:
${roddyBamFile.containedSeqTracks.collect { RoddyBamFile.getReadGroupName(it) }.join('\n')}"""

        when:
        abstractRoddyAlignmentJob.validateReadGroups(roddyBamFile)

        then:
        true
        RuntimeException e = thrown()
        e.message.contains(expectedErrorMessage)

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
    }


    void "validateReadGroups, when read groups are fine, return without exception"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])
        DomainFactory.createRealmDataProcessing(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles(roddyBamFile.mergingWorkPackage)
        roddyBamFile.seqTracks.add(seqTrack)
        roddyBamFile.numberOfMergedLanes++
        roddyBamFile.save()

        String readGroupHeaders = roddyBamFile.containedSeqTracks.collect {
            "@RG     ID:${RoddyBamFile.getReadGroupName(it)}        LB:tumor_123    PL:ILLUMINA     SM:sample_tumor_123"
        }.join('\n') + '\n'

        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getConfigService() >> new ConfigService()
        }

        GroovyMock(ProcessHelperService, global: true) {
            ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(_) >> { String command ->
                String expectedCommand = "set -o pipefail; samtools view -H ${roddyBamFile.workBamFile} | grep ^@RG\\\\s"
                assert command == expectedCommand
                return readGroupHeaders
            }
        }


        when:
        abstractRoddyAlignmentJob.validateReadGroups(roddyBamFile)

        then:
        noExceptionThrown()

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
    }


    void "ensureCorrectBaseBamFileIsOnFileSystem, if no base bam file, all fine"() {
        when:
        Spy(AbstractRoddyAlignmentJob).ensureCorrectBaseBamFileIsOnFileSystem(null)

        then:
        noExceptionThrown()
    }


    void "ensureCorrectBaseBamFileIsOnFileSystem, base bam file exist but not on file system, throw assert"() {
        given:
        RoddyBamFile baseRoddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: baseRoddyBamFile.project.realmName])

        baseRoddyBamFile.mergingWorkPackage.bamFileInProjectFolder = baseRoddyBamFile
        assert baseRoddyBamFile.mergingWorkPackage.save(flush: true)

        AbstractRoddyAlignmentJob job = Spy(AbstractRoddyAlignmentJob)

        when:
        job.ensureCorrectBaseBamFileIsOnFileSystem(baseRoddyBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains('assert bamFilePath.exists()')
    }


    void "ensureCorrectBaseBamFileIsOnFileSystem, base bam file exist and is correct, return without exception"() {
        given:
        RoddyBamFile baseRoddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: baseRoddyBamFile.project.realmName])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(baseRoddyBamFile)

        baseRoddyBamFile.mergingWorkPackage.bamFileInProjectFolder = baseRoddyBamFile
        assert baseRoddyBamFile.mergingWorkPackage.save(flush: true)

        baseRoddyBamFile.fileSize = baseRoddyBamFile.workBamFile.length()
        assert baseRoddyBamFile.save(flush: true)

        AbstractRoddyAlignmentJob job = Spy(AbstractRoddyAlignmentJob)

        when:
        job.ensureCorrectBaseBamFileIsOnFileSystem(baseRoddyBamFile)

        then:
        noExceptionThrown()
    }

}
