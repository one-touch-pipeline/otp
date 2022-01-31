/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class AbstractRoddyAlignmentJobSpec extends Specification implements DataTest, RoddyRnaFactory {

    @Rule
    public TemporaryFolder temporaryFolder

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                DataFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                ProcessingPriority,
                Project,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SequencingKitLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                Realm,
                RnaRoddyBamFile,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
        ]
    }

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
                "GENOME_FA:${referenceGenomeFilePath.path}",
                "CHROM_SIZES_FILE:${chromosomeStatSizeFilePath.path}",
                "possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:",
        ]

        if (adapterTrimming) {
            File adapterFilePath = CreateFileHelper.createFile(temporaryFolder.newFile())
            roddyBamFile.config.adapterTrimmingNeeded = true
            roddyBamFile.config.save(flush: true)

            roddyBamFile.containedSeqTracks*.libraryPreparationKit*.adapterFile = adapterFilePath.absolutePath
            roddyBamFile.containedSeqTracks*.libraryPreparationKit*.save(flush: true)

            expected.addAll("CLIP_INDEX:${adapterFilePath.absolutePath}", "useAdaptorTrimming:true")
        }

        expected.add("runFingerprinting:false",)

        when:
        List<String> value = job.prepareAndReturnAlignmentCValues(roddyBamFile)

        then:
        CollectionUtils.containSame(expected, value)

        where:
        adapterTrimming << [true, false]
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
            0 * ensureCorrectBaseBamFileIsOnFileSystem(_)
        }
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()

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
                1 * correctPermissions(_, _) >> { }
            }
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> {
                throw new AssertionError(errorMessage)
            }
        }
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        RuntimeException e = thrown()
        e.message.contains('The input BAM file seems to have changed ')
        errorMessage == e.cause.message
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    @Unroll
    void "validate, when #file not exist, throw assert"() {
        given:
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> { }
            }
            getConfigService() >> configService
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> { }
            validateReadGroups(_) >> { }
            workflowSpecificValidation(_) >> { }
        }

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
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

        cleanup:
        configService.clean()

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
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> { }
            }
            getConfigService() >> configService
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> { }
            validateReadGroups(_) >> { }
            workflowSpecificValidation(_) >> { }
        }

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                md5sum             : null,
        ])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains(AbstractMergedBamFile.FileOperationStatus.INPROGRESS.name())

        cleanup:
        configService.clean()
    }

    void "validate, when all fine, return without exception"() {
        given:
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> { }
            }
            getConfigService() >> configService
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> { }
            1 * validateReadGroups(_) >> { }
            1 * workflowSpecificValidation(_) >> { }
        }

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                md5sum             : null,
        ])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        noExceptionThrown()

        cleanup:
        configService.clean()
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "validate, when all fine and seqtype is RNA, return without exception"() {
        given:
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getExecuteRoddyCommandService() >> Mock(ExecuteRoddyCommandService) {
                1 * correctPermissions(_, _) >> { }
            }
            getConfigService() >> configService
            1 * ensureCorrectBaseBamFileIsOnFileSystem(_) >> { }
            1 * validateReadGroups(_) >> { }
            1 * workflowSpecificValidation(_) >> { }
        }

        RoddyBamFile roddyBamFile = RoddyRnaFactory.super.createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
        ])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        roddyBamFile.workSingleLaneQAJsonFiles.values().each { File file ->
            file.delete()
        }

        when:
        abstractRoddyAlignmentJob.validate(roddyBamFile)

        then:
        noExceptionThrown()

        cleanup:
        configService.clean()
    }

    private static String createMinimalSamFile(Collection<String> readGroup) {
        """\
        |@HD\tVN:1.5\tSO:coordinate
        |@SQ\tSN:ref\tLN:45
        |${readGroup.collect { "@RG\tID:${it}\tLB:2_TTAGGC\tSM:sample_tumor_123" }.join('\n')}
        |r001\t99\tref\t7\t30\t8M2I4M1D3M\t=\t37\t39\tTTAGATAAAGGATACTG\t*
        |r002\t0\tref\t9\t30\t3S6M1P1I4M\t*\t0\t0\tAAAAGATAAGGATA\t*
        |r003\t0\tref\t9\t30\t5S6M\t*\t0\t0\tGCCTAAGCTAA\t*\tSA:Z:ref,29,-,6H5M,17,0;
        |r004\t0\tref\t16\t30\t6M14N5M\t*\t0\t0\tATAGCTTCAGC\t*
        |r003\t2064\tref\t29\t17\t6H5M\t*\t0\t0\tTAGGC\t*\tSA:Z:ref,9,+,5S6M,30,1;
        |r001\t147\tref\t37\t30\t9M\t=\t7\t-39\tCAGCGGCAT\t*\tNM:i:1
        |""".stripMargin()
    }

    void "validateReadGroups, when read groups are not as expected, throw an exception"() {
        given:
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()

        roddyBamFile.workBamFile.parentFile.mkdirs()
        roddyBamFile.workBamFile.text = createMinimalSamFile(roddyBamFile.containedSeqTracks*.readGroupName)

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles(roddyBamFile.mergingWorkPackage)
        roddyBamFile.seqTracks.add(seqTrack)
        roddyBamFile.numberOfMergedLanes++
        roddyBamFile.save(flush: true)

        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getConfigService() >> configService
        }

        String expectedErrorMessage = """\
            |Read groups in BAM file are not as expected.
            |Read groups in ${roddyBamFile.workBamFile}:
            |${(roddyBamFile.containedSeqTracks - seqTrack)*.readGroupName.sort().join('\n')}
            |Expected read groups:
            |${roddyBamFile.containedSeqTracks*.readGroupName.sort().join('\n')}
            |""".stripMargin()

        when:
        abstractRoddyAlignmentJob.validateReadGroups(roddyBamFile)

        then:
        RuntimeException e = thrown()
        e.message.contains(expectedErrorMessage)

        cleanup:
        configService.clean()
    }

    void "validateReadGroups, when read groups are fine, return without exception"() {
        given:
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles(roddyBamFile.mergingWorkPackage)
        roddyBamFile.seqTracks.add(seqTrack)
        roddyBamFile.numberOfMergedLanes++
        roddyBamFile.save(flush: true)

        roddyBamFile.workBamFile.parentFile.mkdirs()
        roddyBamFile.workBamFile.text = createMinimalSamFile(roddyBamFile.containedSeqTracks*.readGroupName)

        AbstractRoddyAlignmentJob abstractRoddyAlignmentJob = Spy(AbstractRoddyAlignmentJob) {
            getConfigService() >> configService
        }

        when:
        abstractRoddyAlignmentJob.validateReadGroups(roddyBamFile)

        then:
        noExceptionThrown()

        cleanup:
        configService.clean()
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
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        RoddyBamFile baseRoddyBamFile = DomainFactory.createRoddyBamFile()
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

        cleanup:
        configService.clean()
    }
}
