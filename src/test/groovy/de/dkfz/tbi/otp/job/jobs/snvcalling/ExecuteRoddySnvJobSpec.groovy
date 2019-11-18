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
package de.dkfz.tbi.otp.job.jobs.snvcalling


import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class ExecuteRoddySnvJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AbstractMergedBamFile,
                DataFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                Project,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SequencingKitLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                ReferenceGenome,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                Realm,
                RoddyBamFile,
                RoddySnvCallingInstance,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
        ]
    }

    TestConfigService configService

    @Rule
    public TemporaryFolder temporaryFolder


    void "prepareAndReturnWorkflowSpecificCValues, when roddySnvCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddySnvJob().prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert roddySnvCallingInstance')
    }


    void "prepareAndReturnWorkflowSpecificCValues, when all fine, return correct value list"() {
        given:
        File fasta = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "fasta.fa"))
        File chromosomeLength = temporaryFolder.newFile()

        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])

        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService         : configService,
                snvCallingService     : Mock(SnvCallingService) {
                    1 * validateInputBamFiles(_) >> { }
                },
                referenceGenomeService: Mock(ReferenceGenomeService) {
                    1 * fastaFilePath(_) >> fasta
                    1 * chromosomeLengthFile(_) >> chromosomeLength
                    0 * _
                },
        ])
        job.chromosomeIdentifierSortingService = new ChromosomeIdentifierSortingService()

        RoddySnvCallingInstance roddySnvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()

        AbstractMergedBamFile bamFileDisease = roddySnvCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = roddySnvCallingInstance.sampleType2BamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing.path
        String bamFileControlPath = bamFileControl.pathForFurtherProcessing.path

        String analysisMethodNameOnOutput = "snv_results/${roddySnvCallingInstance.seqType.libraryLayoutDirName}/" +
                "${roddySnvCallingInstance.sampleType1BamFile.sampleType.dirName}_${roddySnvCallingInstance.sampleType2BamFile.sampleType.dirName}/" +
                "${roddySnvCallingInstance.instanceName}"

        List<String> chromosomeNames = ["1", "2", "3", "4", "5", "X", "Y", "M"]
        DomainFactory.createReferenceGenomeEntries(roddySnvCallingInstance.referenceGenome, chromosomeNames)

        List<String> expectedList = [
                "bamfile_list:${bamFileControlPath};${bamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta.path}",
                "CHROMOSOME_LENGTH_FILE:${chromosomeLength.path}",
                "CHR_SUFFIX:${roddySnvCallingInstance.referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${roddySnvCallingInstance.referenceGenome.chromosomePrefix}",
                "${job.getChromosomeIndexParameterWithoutMitochondrium(roddySnvCallingInstance.referenceGenome)}",
                "analysisMethodNameOnOutput:${analysisMethodNameOnOutput}",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(roddySnvCallingInstance)

        then:
        expectedList == returnedList

        cleanup:
        configService.clean()
    }


    @Unroll
    void "prepareAndReturnWorkflowSpecificParameter, return always empty String"() {
        expect:
        new ExecuteRoddySnvJob().prepareAndReturnWorkflowSpecificParameter(value).empty

        where:
        value << [
                null,
                new RoddySnvCallingInstance(),
        ]
    }


    void "validate, when all fine, set processing state to finished"() {
        given:
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> { }
                },
                snvCallingService         : Mock(SnvCallingService) {
                    1 * validateInputBamFiles(_) >> { }
                },
        ])
        RoddySnvCallingInstance roddySnvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createRoddySnvResultFiles(roddySnvCallingInstance)

        when:
        job.validate(roddySnvCallingInstance)

        then:
        roddySnvCallingInstance.processingState == AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }


    void "validate, when roddySnvCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddySnvJob().validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains('The input roddyResult must not be null. Expression')
    }


    void "validate, when correctPermissionsAndGroups fail, throw assert"() {
        given:
        String md5sum = HelperUtils.uniqueString
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])
        RoddySnvCallingInstance roddySnvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createRoddySnvResultFiles(roddySnvCallingInstance)

        when:
        job.validate(roddySnvCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(md5sum)
        roddySnvCallingInstance.processingState != AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }


    @Unroll
    void "validate, when file not exist, throw assert"() {
        given:
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> { }
                },
        ])
        RoddySnvCallingInstance roddySnvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createRoddySnvResultFiles(roddySnvCallingInstance)

        File fileToDelete = fileClousure(roddySnvCallingInstance)
        assert fileToDelete.delete() || fileToDelete.deleteDir()

        when:
        job.validate(roddySnvCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(fileToDelete.path)
        roddySnvCallingInstance.processingState != AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()

        where:
        fileClousure << [
                { RoddySnvCallingInstance it ->
                    it.workExecutionStoreDirectory
                },
                { RoddySnvCallingInstance it ->
                    it.workExecutionDirectories.first()
                },
                { RoddySnvCallingInstance it ->
                    it.getCombinedPlotPath()
                },
                { RoddySnvCallingInstance it ->
                    it.getSnvCallingResult()
                },
                { RoddySnvCallingInstance it ->
                    it.getSnvDeepAnnotationResult()
                },
        ]
    }
}
