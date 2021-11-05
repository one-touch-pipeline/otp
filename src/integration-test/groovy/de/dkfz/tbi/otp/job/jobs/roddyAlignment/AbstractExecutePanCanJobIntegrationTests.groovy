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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService

@Rollback
@Integration
class AbstractExecutePanCanJobIntegrationTests implements DomainFactoryCore, DomainFactoryProcessingPriority {

    AbstractExecutePanCanJob abstractExecutePanCanJob

    LsdfFilesService lsdfFilesService

    Realm realm
    TestConfigService configService
    RoddyBamFile roddyBamFile
    File configFile
    File roddyCommand
    File roddyBaseConfigsPath
    File roddyApplicationIni
    File chromosomeStatSizeFile
    File featureTogglesConfigPath
    File referenceGenomeFile

    String workflowSpecificCValues = "workflowSpecificCValues"

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    void setupData() {
        DomainFactory.createRoddyAlignableSeqTypes()

        roddyBamFile = DomainFactory.createRoddyBamFile([
                md5sum                      : null,
                fileOperationStatus         : AbstractMergedBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        roddyBamFile.workPackage.metaClass.seqTracks = SeqTrack.list()

        configService.addOtpProperties(tmpDir.root.toPath())

        realm = DomainFactory.createRealm()
        abstractExecutePanCanJob = [
                prepareAndReturnWorkflowSpecificCValues  : { RoddyBamFile bamFile -> ["${workflowSpecificCValues}"] },
                prepareAndReturnWorkflowSpecificParameter: { RoddyBamFile bamFile -> "workflowSpecificParameter" },
        ] as AbstractExecutePanCanJob

        abstractExecutePanCanJob.referenceGenomeService = new ReferenceGenomeService()
        abstractExecutePanCanJob.referenceGenomeService.configService = configService
        abstractExecutePanCanJob.referenceGenomeService.processingOptionService = new ProcessingOptionService()
        abstractExecutePanCanJob.lsdfFilesService = new LsdfFilesService()
        abstractExecutePanCanJob.executeRoddyCommandService = new ExecuteRoddyCommandService()
        abstractExecutePanCanJob.executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        abstractExecutePanCanJob.bedFileService = new BedFileService()
        abstractExecutePanCanJob.configService = configService
        abstractExecutePanCanJob.chromosomeIdentifierSortingService = new ChromosomeIdentifierSortingService()
        abstractExecutePanCanJob.processingOptionService = new ProcessingOptionService()

        File processingRootPath = configService.getProcessingRootPath()

        DomainFactory.createRoddyProcessingOptions(tmpDir.newFolder())

        File roddyPath = ProcessingOption.findByName(OptionName.RODDY_PATH).value as File
        roddyCommand = new File(roddyPath, 'roddy.sh')
        roddyBaseConfigsPath = ProcessingOption.findByName(OptionName.RODDY_BASE_CONFIGS_PATH).value as File
        roddyBaseConfigsPath.mkdirs()
        roddyApplicationIni = ProcessingOption.findByName(OptionName.RODDY_APPLICATION_INI).value as File
        roddyApplicationIni.text = "Some Text"
        featureTogglesConfigPath = ProcessingOption.findByName(OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH).value as File

        File referenceGenomeDir = new File("${processingRootPath}/reference_genomes/${roddyBamFile.referenceGenome.path}")
        assert referenceGenomeDir.mkdirs()
        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomeDir.parent)
        referenceGenomeFile = new File(referenceGenomeDir, "${roddyBamFile.referenceGenome.fileNamePrefix}.fa")
        CreateFileHelper.createFile(referenceGenomeFile)

        configFile = tmpDir.newFile()
        CreateFileHelper.createFile(configFile)
        RoddyWorkflowConfig config = roddyBamFile.config
        config.configFilePath = configFile.path
        assert config.save(flush: true)

        chromosomeStatSizeFile = abstractExecutePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage, false)
        CreateFileHelper.createFile(chromosomeStatSizeFile)

        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.correctPermissions = { RoddyBamFile bamFile, Realm realm -> }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecuteRoddyCommandService, abstractExecutePanCanJob.executeRoddyCommandService)
        TestCase.removeMetaClass(ReferenceGenomeService, abstractExecutePanCanJob.referenceGenomeService)
        configService.clean()
    }

    private String viewByPidString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.individual.getViewByPidPathBase(roddyBamFile.seqType).absoluteDataManagementPath.path
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RoddyResultIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(null, realm)
        }.contains("roddyResult must not be null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RealmIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, null)
        }.contains("realm must not be null")
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ConfigFileIsNotInFileSystem_ShouldFail() {
        setupData()
        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }
        assert configFile.delete()

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, realm)
        }.contains(roddyBamFile.config.configFilePath)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_NormalPriority_AllFine() {
        setupData()
        testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper(
                "${roddyBamFile.config.programVersion}-${roddyBamFile.seqType.roddyName.toLowerCase()}-${roddyBamFile.processingPriority.roddyConfigSuffix}")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_MinimalPriority_AllFine() {
        setupData()
        roddyBamFile.project.processingPriority = findOrCreateProcessingPriorityMinimum()
        roddyBamFile.project.save(flush: true)
        testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper(
                "${roddyBamFile.config.programVersion}-${roddyBamFile.seqType.roddyName.toLowerCase()}-${PRIORITY_NAME_MINIMUM}")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_FasttrackPriority_AllFine() {
        setupData()
        roddyBamFile.project.processingPriority = findOrCreateProcessingPriorityFastrack()
        roddyBamFile.project.save(flush: true)
        testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper(
                "${roddyBamFile.config.programVersion}-${roddyBamFile.seqType.roddyName.toLowerCase()}-${PRIORITY_NAME_FASTTRACK}")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_OverFasttrackPriority_AllFine() {
        setupData()
        roddyBamFile.project.processingPriority = findOrCreateProcessingPriorityMaximum()
        roddyBamFile.project.save(flush: true)
        testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper(
                "${roddyBamFile.config.programVersion}-${roddyBamFile.seqType.roddyName.toLowerCase()}-${PRIORITY_NAME_MAXIMUM}")
    }

    void testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper(String additionalImports) {
        DomainFactory.createProcessingOptionForInitRoddyModule()
        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        String expectedCmd = """
${roddyCommand} rerun \
${roddyBamFile.pipeline.name}_${roddyBamFile.seqType.roddyName}_${roddyBamFile.seqType.libraryLayout}_\
${roddyBamFile.config.programVersion}_${roddyBamFile.config.configVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--usefeaturetoggleconfig=${featureTogglesConfigPath} \
--usePluginVersion=${roddyBamFile.config.programVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath},\
${roddyBaseConfigsPath}/${ExecuteRoddyCommandService.RESOURCE_PATH}/${roddyBamFile.project.realm.jobScheduler.toString().toLowerCase()} \
--useiodir=${viewByPidString()},${roddyBamFile.workDirectory} \
--additionalImports=${additionalImports} \
workflowSpecificParameter \
--cvalues="${workflowSpecificCValues},sharedFilesBaseDirectory:/shared"\
"""

        String actualCmd = abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, realm)
        assert expectedCmd == actualCmd
    }

    @Test
    void testPrepareAndReturnCValues_RoddyResultIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnCValues(null)
        }.contains("roddyResult must not be null")
    }

    @Test
    void testPrepareAndReturnCValues_NoFastTrack_setUpCorrect() {
        setupData()
        String expectedCommand = """\
--cvalues="${workflowSpecificCValues},sharedFilesBaseDirectory:/shared"\
"""
        assert expectedCommand == abstractExecutePanCanJob.prepareAndReturnCValues(roddyBamFile)
    }

    @Test
    void testGetChromosomeIndexParameterWithMitochondrium_RoddyResultIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameterWithMitochondrium(null)
        }.contains("assert referenceGenome")
    }

    @Test
    void testGetChromosomeIndexParameterWithMitochondrium_NoChromosomeNamesExist_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameterWithMitochondrium(roddyBamFile.referenceGenome)
        }.contains("No chromosome names could be found for reference genome")
    }

    @Test
    void testGetChromosomeIndexParameterWithMitochondrium_AllFine() {
        setupData()
        List<String> chromosomeNames = ["1", "4", "3", "X", "5", "M", "2", "Y", "21"]
        List<String> chromosomeNamesExpected = ["1", "2", "3", "4", "5", "21", "X", "Y", "M"]
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, chromosomeNames)

        assert "CHROMOSOME_INDICES:( ${chromosomeNamesExpected.join(' ')} )" == abstractExecutePanCanJob.getChromosomeIndexParameterWithMitochondrium(roddyBamFile.referenceGenome)
    }

    @Test
    void testGetChromosomeIndexParameterWithoutMitochondrium_RoddyResultIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameterWithoutMitochondrium(null)
        }.contains("assert referenceGenome")
    }

    @Test
    void testGetChromosomeIndexParameterWithoutMitochondrium_NoChromosomeNamesExist_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameterWithoutMitochondrium(roddyBamFile.referenceGenome)
        }.contains("No chromosome names could be found for reference genome")
    }

    @Test
    void testGetChromosomeIndexParameterWithoutMitochondrium_AllFine() {
        setupData()
        List<String> chromosomeNames = ["1", "4", "3", "X", "5", "2", "Y", "21"]
        List<String> chromosomeNamesExpected = ["1", "2", "3", "4", "5", "21", "X", "Y"]
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, chromosomeNames)
        DomainFactory.createReferenceGenomeEntry(
                referenceGenome: roddyBamFile.referenceGenome,
                classification: ReferenceGenomeEntry.Classification.MITOCHONDRIAL,
                name: "M",
                alias: "M"
        )

        assert "CHROMOSOME_INDICES:( ${chromosomeNamesExpected.join(' ')} )" == abstractExecutePanCanJob.getChromosomeIndexParameterWithoutMitochondrium(roddyBamFile.referenceGenome)
    }
}
