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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.*

@Rollback
@Integration
class SnvCallingServiceIntegrationSpec extends Specification implements DomainFactoryCore, DomainFactoryProcessingPriority {

    @Rule
    public TemporaryFolder temporaryFolder

    final static String ARBITRARY_INSTANCE_NAME = '2014-08-25_15h32'
    final static double COVERAGE_TOO_LOW = 20.0

    SamplePair samplePair1

    ConfigPerProjectAndSeqType roddyConfig1
    AbstractMergedBamFile bamFile1
    AbstractMergedBamFile bamFile2

    SnvCallingService snvCallingService

    void setupData() {
        Map map = DomainFactory.createProcessableSamplePair()

        samplePair1 = map.samplePair
        bamFile1 = map.bamFile1
        bamFile2 = map.bamFile2
        roddyConfig1 = map.roddyConfig

        DomainFactory.createAllAnalysableSeqTypes()

        samplePair1.project.processingPriority.priority = 0
        samplePair1.project.processingPriority.save(flush: true)
    }

    @Unroll
    void "samplePairForProcessing when config has wrong #property"() {
        given:
        setupData()

        if (property == "project") {
            roddyConfig1.project = DomainFactory.createProject(name: "otherProject", dirName: "tmp")
        } else {
            roddyConfig1.seqType = DomainFactory.createExomeSeqType()
        }
        assert roddyConfig1.save(flush: true)

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null

        where:
        property << ["project", "seqType"]
    }

    void "samplePairForProcessing when config is obsolete"() {
        given:
        setupData()

        roddyConfig1.obsoleteDate = new Date()
        assert roddyConfig1.save(flush: true)

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null
    }

    void "samplePairForProcessing when the snvCallingInstance is already in progress"() {
        given:
        setupData()

        DomainFactory.createRoddySnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: roddyConfig1,
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFile2
        )

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null
    }

    void "samplePairForProcessing when a snvCallingInstance already finished"() {
        given:
        setupData()

        DomainFactory.createRoddySnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: roddyConfig1,
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFile2,
                processingState: AnalysisProcessingStates.FINISHED
        )

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL)
    }

    void "samplePairForProcessing when other samplePair inProcess"() {
        given:
        setupData()

        Map map2 = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair2 = map2.samplePair

        DomainFactory.createRoddySnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair2,
                config: roddyConfig1,
                sampleType1BamFile: map2.bamFile1,
                sampleType2BamFile: map2.bamFile2
        )

        expect:
        samplePair1.individual != samplePair2.individual
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL)
    }

    @Unroll
    void "samplePairForProcessing when bamFile#number does not contain all seqTracks"() {
        given:
        setupData()

        if (number == 1) {
            DomainFactory.createSeqTrackWithDataFiles(bamFile1.mergingWorkPackage)
        } else {
            DomainFactory.createSeqTrackWithDataFiles(bamFile2.mergingWorkPackage)
        }

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL)

        where:
        number << [1, 2]
    }

    @Unroll
    void "samplePairForProcessing when no samplepair for bamFile#number exists"() {
        given:
        setupData()

        MergingWorkPackage withSamplePair
        MergingWorkPackage withoutSamplePair
        if (number == 1) {
            withSamplePair = samplePair1.mergingWorkPackage2
            withoutSamplePair = samplePair1.mergingWorkPackage1
        } else {
            withSamplePair = samplePair1.mergingWorkPackage1
            withoutSamplePair = samplePair1.mergingWorkPackage2
        }

        MergingWorkPackage otherMwp = DomainFactory.createMergingWorkPackage(withSamplePair)
        DomainFactory.createSampleTypePerProject(project: samplePair1.project, sampleType: otherMwp.sampleType, category: SampleType.Category.DISEASE)
        DomainFactory.createSamplePair(otherMwp, withoutSamplePair)
        samplePair1.delete(flush: true)

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null

        where:
        number << [1, 2]
    }

    @Unroll
    void "samplePairForProcessing when bamFile#number is still in progress"() {
        given:
        setupData()

        AbstractMergedBamFile bamFileInProgress = (number == 1) ? bamFile1 : bamFile2

        bamFileInProgress.md5sum = null
        bamFileInProgress.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.INPROGRESS
        assert bamFileInProgress.save(flush: true)

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null

        where:
        number << [1, 2]
    }

    @Unroll
    void "samplePairForProcessing when for bamFile#number the coverage is too low"() {
        given:
        setupData()

        AbstractMergedBamFile problematicBamFile = (number == 1) ? bamFile1 : bamFile2
        problematicBamFile.coverage = COVERAGE_TOO_LOW
        assert problematicBamFile.save(flush: true)

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null

        where:
        number << [1, 2]
    }

    @Unroll
    void "samplePairForProcessing when for bamFile#number the number of lanes is too low"() {
        given:
        setupData()

        AbstractMergedBamFile problematicBamFile = (number == 1) ? bamFile1 : bamFile2
        ProcessingThresholds thresholds = ProcessingThresholds.findBySeqType(problematicBamFile.seqType)
        thresholds.numberOfLanes = 5
        assert thresholds.save(flush: true)

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null

        where:
        number << [1, 2]
    }

    void "samplePairForProcessing when for both bam Files the number of lanes is too low"() {
        given:
        setupData()

        ProcessingThresholds.findByProject(samplePair1.project).each {
            it.numberOfLanes = 5
            it.save(flush: true)
        }

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null
    }

    void "samplePairForProcessing when for both bam Files the coverage is too low"() {
        given:
        setupData()

        [bamFile1, bamFile2].each {
            it.coverage = COVERAGE_TOO_LOW
            it.save(flush: true)
        }

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null
    }

    @Unroll
    void "samplePairForProcessing when for bamFile#number no threshold exists"() {
        given:
        setupData()

        Project otherProject = DomainFactory.createProject()
        AbstractMergedBamFile problematicBamFile = (number == 1) ? bamFile1 : bamFile2
        ProcessingThresholds thresholds = ProcessingThresholds.findBySeqType(problematicBamFile.seqType)
        thresholds.project = otherProject
        assert thresholds.save(flush: true)

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null

        where:
        number << [1, 2]
    }

    @Unroll
    void "samplePairForProcessing when for bamFile#number the processing threshold #property is null"() {
        given:
        setupData()

        AbstractMergedBamFile bamFile = (number == 1) ? bamFile1 : bamFile2
        ProcessingThresholds thresholds = ProcessingThresholds.findBySeqType(bamFile.seqType)
        thresholds[property] = null
        if (property == "coverage") {
            thresholds.numberOfLanes = 1
        }
        assert thresholds.save(flush: true)

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL)

        where:
        number | property
        1      | "coverage"
        2      | "coverage"
        1      | "numberOfLanes"
        2      | "numberOfLanes"
    }

    @Unroll
    void "samplePairForProcessing when bamFile#number is withdrawn"() {
        given:
        setupData()

        AbstractMergedBamFile problematicBamFile = (number == 1) ? bamFile1 : bamFile2
        problematicBamFile.withdrawn = true
        assert problematicBamFile.save(flush: true)

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL) == null

        where:
        number << [1, 2]
    }

    void "samplePairForProcessing when check if the order correct"() {
        given:
        setupData()

        SamplePair samplePair = DomainFactory.createProcessableSamplePair().samplePair
        samplePair.project.processingPriority = samplePair1.project.processingPriority
        samplePair.project.save(flush: true)

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL)
    }

    void "samplePairForProcessing ensure that FastTrack is processed first"() {
        given:
        setupData()

        SamplePair samplePairFastTrack = DomainFactory.createProcessableSamplePair().samplePair
        Project project = samplePairFastTrack.project
        project.processingPriority = findOrCreateProcessingPriorityFastrack()
        assert project.save(flush: true)

        expect:
        samplePairFastTrack == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL)
    }

    void "samplePairForProcessing, make sure that min processing priority is taken into account"() {
        given:
        setupData()

        expect:
        snvCallingService.samplePairForProcessing(ProcessingPriority.FAST_TRACK) == null
    }

    void "validateInputBamFiles, when all okay, return without exception"() {
        given:
        setupData()

        RoddySnvCallingInstance snvCallingInstance = DomainFactory.createRoddySnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: roddyConfig1,
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFile2
        )
        snvCallingInstance.save(flush: true)

        snvCallingService.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            2 * getExistingBamFilePath(_) >> TestCase.uniqueNonExistentPath
        }

        when:
        snvCallingService.validateInputBamFiles(snvCallingInstance)

        then:
        noExceptionThrown()
    }

    void "validateInputBamFiles, when path throw an exception, throw a new runtime exception"() {
        given:
        setupData()

        RoddySnvCallingInstance instance = new RoddySnvCallingInstance([
                sampleType1BamFile: new RoddyBamFile(),
                sampleType2BamFile: new RoddyBamFile(),
        ])
        snvCallingService.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            2 * getExistingBamFilePath(_) >> TestCase.uniqueNonExistentPath >> { assert false }
        }

        when:
        snvCallingService.validateInputBamFiles(instance)

        then:
        RuntimeException e = thrown()
        e.message.contains('The input BAM files have changed on the file system while this job processed them.')
    }

    void "getResultRequiredForRunYapsaAndEnsureIsReadableAndNotEmpty, should find the file"() {
        given:
        setupData()

        SnvCallingService snvCallingService = new SnvCallingService(fileService: new FileService())
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])

        RoddySnvCallingInstance instance = DomainFactory.createRoddySnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: roddyConfig1,
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFile2,
                processingState: AnalysisProcessingStates.FINISHED,
        )

        Path resultDir = Paths.get(instance.workDirectory.absolutePath)
        Path expected = Files.createFile(Files.createDirectories(resultDir).resolve("snvs_${instance.individual.pid}_somatic_snvs_conf_6_to_10.vcf"))
        expected.text = "SOME_CONTENT"

        FileSystem fileSystem = new TestFileSystemService().filesystemForProcessingForRealm

        when:
        Path result = snvCallingService.getResultRequiredForRunYapsaAndEnsureIsReadableAndNotEmpty(instance, fileSystem)

        then:
        noExceptionThrown()
        result == expected

        cleanup:
        configService.clean()
    }
}
