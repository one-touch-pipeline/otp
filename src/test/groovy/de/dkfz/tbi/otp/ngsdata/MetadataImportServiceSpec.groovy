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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import groovy.transform.TupleConstructor
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.context.ApplicationContext
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesInGpcfSpecificStructure
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrainService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationInitializationService
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.*
import java.time.LocalDate
import java.util.stream.Collectors

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class MetadataImportServiceSpec extends Specification implements DomainFactoryCore, DataTest, TaxonomyFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AntibodyTarget,
                DataFile,
                FileType,
                IlseSubmission,
                Individual,
                LibraryPreparationKit,
                MetaDataEntry,
                MetaDataFile,
                MetaDataKey,
                OtrsTicket,
                Pipeline,
                ProcessingOption,
                Project,
                Realm,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
                Sample,
                SampleIdentifier,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                SoftwareToolIdentifier,
        ]
    }

    final static String TICKET_NUMBER = "2000010112345678"
    final static String WGBS_T = SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName
    final static String WG = SeqTypeNames.WHOLE_GENOME.seqTypeName
    final static String EXON = SeqTypeNames.EXOME.seqTypeName
    final static String CHIP_SEQ = SeqTypeNames.CHIP_SEQ.seqTypeName
    final static String SC_EXON = "SC_" + SeqTypeNames.EXOME.seqTypeName

    @Rule
    TemporaryFolder temporaryFolder

    void "getImplementedValidations returns descriptions of validations"() {
        given:
        MetadataImportService service = new MetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(MetadataValidator) >>
                    [
                            'validator1': [
                                    getDescriptions: { ['description1', 'description2'] }
                            ] as MetadataValidator,
                            'validator2': [
                                    getDescriptions: { ['description3'] }
                            ] as MetadataValidator,
                    ]
        }

        expect:
        containSame(service.implementedValidations, ['description1', 'description2', 'description3'])
    }

    void "validate creates context and calls validators"() {
        given:
        DirectoryStructure directoryStructure = [:] as DirectoryStructure
        DirectoryStructureBeanName directoryStructureName = DirectoryStructureBeanName.SAME_DIRECTORY

        File testDirectory = TestCase.createEmptyTestDirectory()
        Path metadataFile = testDirectory.toPath().resolve('metadata.tsv')
        metadataFile.bytes = 'Header\nI am metadata!'.getBytes(MetadataValidationContext.CHARSET)

        MetadataImportService service = new MetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(MetadataValidator) >>
                    [
                            'validator1': [
                                    validate: { MetadataValidationContext ctx ->
                                        ctx.addProblem(Collections.emptySet(), LogLevel.ERROR, 'message1')
                                        ctx.addProblem(Collections.emptySet(), LogLevel.ERROR, 'message2')
                                    }
                            ] as MetadataValidator,
                            'validator2': [
                                    validate: { MetadataValidationContext ctx ->
                                        ctx.addProblem(Collections.emptySet(), LogLevel.ERROR, 'message3')
                                    }
                            ] as MetadataValidator,
                    ]
            getBean(directoryStructureName.beanName, DirectoryStructure) >> directoryStructure
        }

        when:
        MetadataValidationContext context = service.validate(metadataFile, directoryStructureName)

        then:
        containSame(context.problems*.message, ['message1', 'message2', 'message3'])
        context.directoryStructure == directoryStructure
        context.metadataFile == metadataFile
        context.spreadsheet.header.cells[0].text == 'Header'
        context.spreadsheet.dataRows[0].cells[0].text == 'I am metadata!'

        cleanup:
        testDirectory.deleteDir()
    }

    @Unroll
    void "validateAndImportWithAuth, when there are no problems, calls importMetadataFile and returns created MetaDataFile"() {
        given:
        SeqCenter seqCenter = createSeqCenter(autoImportable: true, autoImportDir: "/auto-import-dir")
        DirectoryStructure directoryStructure = [:] as DirectoryStructure
        DirectoryStructureBeanName directoryStructureName = DirectoryStructureBeanName.SAME_DIRECTORY

        DomainFactory.createDefaultRealmWithProcessingOption()

        File testDirectory = TestCase.createEmptyTestDirectory()
        File runDirectory = new File(testDirectory, 'run')
        assert runDirectory.mkdir()
        Path metadataFile = testDirectory.toPath().resolve('metadata.tsv')
        metadataFile.bytes = ("${RUN_ID}\t${INSTRUMENT_PLATFORM}\t${INSTRUMENT_MODEL}\t${CENTER_NAME}\n" +
                ("run\tplatform\tmodelAlias\t${seqCenter.name}\n" * 2)).getBytes(MetadataValidationContext.CHARSET)

        MetaDataFile metadataFileObject = new MetaDataFile()
        MetadataImportService service = Spy(MetadataImportService) {
            1 * importMetadataFile(_, align, FastqImportInstance.ImportMode.MANUAL, TICKET_NUMBER, null, automaticNotification) >> metadataFileObject
            1 * copyMetadataFileIfRequested(_)
        }
        service.configService = new TestConfigService()
        service.configService.processingOptionService = new ProcessingOptionService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(MetadataValidator) >> [:]
            getBean(directoryStructureName.beanName, DirectoryStructure) >> directoryStructure
        }
        MetadataImportService.PathWithMd5sum pathWithMd5sum = new MetadataImportService.PathWithMd5sum(metadataFile, null)

        when:
        List<ValidateAndImportResult> results = service.validateAndImportWithAuth(
                [pathWithMd5sum], directoryStructureName, align, false, TICKET_NUMBER, null, automaticNotification)

        then:
        results[0].context.metadataFile == metadataFile
        results[0].metadataFile == metadataFileObject

        cleanup:
        testDirectory.deleteDir()

        where:
        align << [true, false]
        automaticNotification << [true, false]
    }

    void "validateAndImportWithAuth, when there are errors, returns null as metadataFile"() {
        given:
        DirectoryStructure directoryStructure = [:] as DirectoryStructure
        DirectoryStructureBeanName directoryStructureName = DirectoryStructureBeanName.SAME_DIRECTORY

        Path metadataFile = Paths.get('relative_path')
        MetadataImportService.PathWithMd5sum pathWithMd5sum = new MetadataImportService.PathWithMd5sum(metadataFile, null)

        MetadataImportService service = new MetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBean(directoryStructureName.beanName, DirectoryStructure) >> directoryStructure
        }

        when:
        List<ValidateAndImportResult> results = service.validateAndImportWithAuth(
                [pathWithMd5sum], directoryStructureName, false, false, TICKET_NUMBER, null, true)

        then:
        results[0].context.metadataFile == metadataFile
        results[0].metadataFile == null
    }

    void "validateAndImportWithAuth, validate all before import the first"() {
        given:
        DirectoryStructure directoryStructure = [:] as DirectoryStructure
        DirectoryStructureBeanName directoryStructureName = DirectoryStructureBeanName.SAME_DIRECTORY
        createSeqCenter(autoImportable: false)
        MetadataValidationContext context1 = MetadataValidationContextFactory.createContext([metadataFile: Paths.get("import1_meta.tsv")])
        MetaDataFile metadataFile1 = DomainFactory.createMetaDataFile()
        MetadataValidationContext context2 = MetadataValidationContextFactory.createContext([metadataFile: Paths.get("import2_meta.tsv")])
        MetaDataFile metadataFile2 = DomainFactory.createMetaDataFile()

        DomainFactory.createDefaultRealmWithProcessingOption()

        List<MetadataImportService.PathWithMd5sum> pathWithMd5sums = [
                new MetadataImportService.PathWithMd5sum(context1.metadataFile, HelperUtils.randomMd5sum),
                new MetadataImportService.PathWithMd5sum(context2.metadataFile, HelperUtils.randomMd5sum),
        ]

        int imported = 0

        MetadataImportService service = Spy(MetadataImportService) {
            1 * validate(context1.metadataFile, directoryStructureName, false) >> { assert imported == 0; context1 }
            1 * validate(context2.metadataFile, directoryStructureName, false) >> { assert imported == 0; context2 }
            1 * importMetadataFile(context1, true, FastqImportInstance.ImportMode.MANUAL, TICKET_NUMBER, null, true) >> {
                imported++
                metadataFile1
            }
            1 * importMetadataFile(context2, true, FastqImportInstance.ImportMode.MANUAL, TICKET_NUMBER, null, true) >> {
                imported++
                metadataFile2
            }
            0 * importMetadataFile(*_)
        }
        service.applicationContext = Mock(ApplicationContext) {
            getBean(directoryStructureName.beanName, DirectoryStructure) >> directoryStructure
        }
        service.fileSystemService = new TestFileSystemService()
        service.configService = new TestConfigService()
        service.configService.processingOptionService = new ProcessingOptionService()

        when:
        List<ValidateAndImportResult> validateAndImportResults = service.validateAndImportWithAuth(
                pathWithMd5sums, directoryStructureName, true, false, TICKET_NUMBER, null, true)

        then:
        validateAndImportResults*.context == [context1, context2]
        validateAndImportResults*.metadataFile == [metadataFile1, metadataFile2]
        imported == 2
    }

    void "validateAndImportMultiple when all are valid, returns import results"() {
        given:
        SeqCenter seqCenter = createSeqCenter(autoImportable: true, autoImportDir: "/auto-import-dir")
        MetadataValidationContext context1 = MetadataValidationContextFactory.createContext(
                [metadataFile: Paths.get("${seqCenter.autoImportDir}/001111/data/1111_meta.tsv")])
        MetaDataFile metadataFile1 = DomainFactory.createMetaDataFile()
        MetadataValidationContext context2 = MetadataValidationContextFactory.createContext(
                [metadataFile: Paths.get("${seqCenter.autoImportDir}/002222/data/2222_meta.tsv")])
        MetaDataFile metadataFile2 = DomainFactory.createMetaDataFile()

        DomainFactory.createDefaultRealmWithProcessingOption()

        MetadataImportService service = Spy(MetadataImportService) {
            1 * validate(context1.metadataFile, DirectoryStructureBeanName.GPCF_SPECIFIC, _) >> context1
            1 * validate(context2.metadataFile, DirectoryStructureBeanName.GPCF_SPECIFIC, _) >> context2
            1 * importMetadataFile(context1, true, FastqImportInstance.ImportMode.AUTOMATIC, TICKET_NUMBER, null, true) >> metadataFile1
            1 * importMetadataFile(context2, true, FastqImportInstance.ImportMode.AUTOMATIC, TICKET_NUMBER, null, true) >> metadataFile2
        }
        service.fileSystemService = new TestFileSystemService()
        service.configService = new TestConfigService()
        service.configService.processingOptionService = new ProcessingOptionService()

        expect:
        List<ValidateAndImportResult> validateAndImportResults = service.validateAndImportMultiple(TICKET_NUMBER, '1111+2222', false)

        validateAndImportResults*.context == [context1, context2]
        validateAndImportResults*.metadataFile == [metadataFile1, metadataFile2]
    }

    void "validateAndImportMultiple when import first partly and then all, the return correct imports"() {
        given:
        String runName = 'run'
        Date date = TimeUtils.toDate(LocalDate.of(2000, 1, 1))
        String dateString = TimeFormats.DATE.getFormattedDate(date)
        String fastq11 = "fastq1_1.gz"
        String fastq12 = "fastq1_2.gz"
        String fastq21 = "fastq2_1.gz"
        String fastq22 = "fastq2_2.gz"
        String md5sum11 = HelperUtils.randomMd5sum
        String md5sum12 = HelperUtils.randomMd5sum
        String md5sum21 = HelperUtils.randomMd5sum
        String md5sum22 = HelperUtils.randomMd5sum

        DomainFactory.createAllAnalysableSeqTypes()

        SeqCenter seqCenter = createSeqCenter()
        SeqPlatform seqPlatform = createSeqPlatform()
        String speciesImportAlias = "SpeciesImportAlias"
        SpeciesWithStrain speciesWithStrain = createSpeciesWithStrain([importAlias: [speciesImportAlias] as Set])
        SampleIdentifier sampleIdentifier1 = DomainFactory.createSampleIdentifier()
        SampleIdentifier sampleIdentifier2 = DomainFactory.createSampleIdentifier()
        SoftwareToolIdentifier softwareToolIdentifier = createSoftwareToolIdentifier([
                softwareTool: createSoftwareTool([
                        type: SoftwareTool.Type.BASECALLING,
                ]),
        ])
        SeqType seqTypeWithAntibodyTarget = createSeqType([
                libraryLayout    : SequencingReadType.PAIRED,
                hasAntibodyTarget: true,
        ])
        AntibodyTarget antibodyTarget = createAntibodyTarget()
        createFileType(
                type: FileType.Type.SEQUENCE,
                signature: '_',
        )
        OtrsTicket otrsTicket = createOtrsTicket()

        String metadata1 = """
${FASTQ_FILE}                   ${fastq11}                                  ${fastq12}
${MD5}                          ${md5sum11}                                 ${md5sum12}
${RUN_ID}                       ${runName}                                  ${runName}
${CENTER_NAME}                  ${seqCenter}                                ${seqCenter}
${INSTRUMENT_PLATFORM}          ${seqPlatform.name}                         ${seqPlatform.name}
${INSTRUMENT_MODEL}             ${seqPlatform.seqPlatformModelLabel.name}   ${seqPlatform.seqPlatformModelLabel.name}
${RUN_DATE}                     ${dateString}                               ${dateString}
${LANE_NO}                      1                                           1
${SEQUENCING_TYPE}              ${seqTypeWithAntibodyTarget.name}           ${seqTypeWithAntibodyTarget.name}
${SEQUENCING_READ_TYPE}         ${seqTypeWithAntibodyTarget.libraryLayout}  ${seqTypeWithAntibodyTarget.libraryLayout}
${READ}                         1                                           2
${SAMPLE_NAME}                  ${sampleIdentifier1.name}                   ${sampleIdentifier1.name}
${ANTIBODY_TARGET}              ${antibodyTarget.name}                      ${antibodyTarget.name}
${ANTIBODY}                     -                                           -
${FASTQ_GENERATOR}              ${softwareToolIdentifier.name}              ${softwareToolIdentifier.name}
${SPECIES}                      ${speciesImportAlias}                       ${speciesImportAlias}
"""

        String metadata2 = """
${FASTQ_FILE}                   ${fastq11}                                  ${fastq12}                                  ${fastq21}                                  ${fastq22}
${MD5}                          ${md5sum11}                                 ${md5sum12}                                 ${md5sum21}                                 ${md5sum22}
${RUN_ID}                       ${runName}                                  ${runName}                                  ${runName}                                  ${runName}
${CENTER_NAME}                  ${seqCenter}                                ${seqCenter}                                ${seqCenter}                                ${seqCenter}
${INSTRUMENT_PLATFORM}          ${seqPlatform.name}                         ${seqPlatform.name}                         ${seqPlatform.name}                         ${seqPlatform.name}
${INSTRUMENT_MODEL}             ${seqPlatform.seqPlatformModelLabel.name}   ${seqPlatform.seqPlatformModelLabel.name}   ${seqPlatform.seqPlatformModelLabel.name}   ${seqPlatform.seqPlatformModelLabel.name}
${RUN_DATE}                     ${dateString}                               ${dateString}                               ${dateString}                               ${dateString}
${LANE_NO}                      1                                           1                                           2                                           2
${SEQUENCING_TYPE}              ${seqTypeWithAntibodyTarget.name}           ${seqTypeWithAntibodyTarget.name}           ${seqTypeWithAntibodyTarget.name}           ${seqTypeWithAntibodyTarget.name}
${SEQUENCING_READ_TYPE}         ${seqTypeWithAntibodyTarget.libraryLayout}  ${seqTypeWithAntibodyTarget.libraryLayout}  ${seqTypeWithAntibodyTarget.libraryLayout}  ${seqTypeWithAntibodyTarget.libraryLayout}
${READ}                         1                                           2                                           1                                           2
${SAMPLE_NAME}                  ${sampleIdentifier1.name}                   ${sampleIdentifier1.name}                   ${sampleIdentifier2.name}                   ${sampleIdentifier2.name}
${ANTIBODY_TARGET}              ${antibodyTarget.name}                      ${antibodyTarget.name}                      ${antibodyTarget.name}                      ${antibodyTarget.name}
${ANTIBODY}                     -                                           -                                           -                                           -
${FASTQ_GENERATOR}              ${softwareToolIdentifier.name}              ${softwareToolIdentifier.name}              ${softwareToolIdentifier.name}              ${softwareToolIdentifier.name}
${SPECIES}                      ${speciesImportAlias}                       ${speciesImportAlias}                       ${speciesImportAlias}                       ${speciesImportAlias}
"""

        List<List<String>> lines1 = metadata1.readLines().findAll()*.split(/ {2,}/).transpose()
        List<List<String>> lines2 = metadata2.readLines().findAll()*.split(/ {2,}/).transpose()

        String content1 = lines1.collect { it*.replaceFirst(/^-$/, '').join('\t') }.join('\n')
        String content2 = lines2.collect { it*.replaceFirst(/^-$/, '').join('\t') }.join('\n')

        Path file1 = temporaryFolder.newFile("metaData1.tsv").toPath()
        file1.text = content1
        Path file2 = temporaryFolder.newFile("metaData2.tsv").toPath()
        file2.text = content2

        DirectoryStructure directoryStructure = Mock(DirectoryStructure) {
            _ * getRequiredColumnTitles() >> [FASTQ_FILE.name()]
            _ * getDataFilePath(_, _) >> { MetadataValidationContext context, Row row ->
                return context.metadataFile.resolveSibling(row.getCellByColumnTitle(FASTQ_FILE.name()).text)
            }
        }
        MetadataImportService service = Spy(MetadataImportService) {
            2 * getMetadataFilePathForIlseNumber(_, _) >> file1 >> file2
            _ * notifyAboutUnsetConfig(_, _, _) >> null
            _ * getDirectoryStructure(_) >> directoryStructure
        }
        service.sampleIdentifierService = Mock(SampleIdentifierService) {
            0 * _
        }
        service.seqTrackService = Mock(SeqTrackService) {
            2 * decideAndPrepareForAlignment(_) >> []
            2 * determineAndStoreIfFastqFilesHaveToBeLinked(!null, false)
        }
        service.seqPlatformService = Mock(SeqPlatformService) {
            2 * findSeqPlatform(seqPlatform.name, seqPlatform.seqPlatformModelLabel.name, null) >> seqPlatform
            0 * _
        }
        service.speciesWithStrainService = Mock(SpeciesWithStrainService) {
            2 * getByAlias(speciesImportAlias) >> speciesWithStrain
        }
        service.seqTypeService = Mock(SeqTypeService) {
            2 * findByNameOrImportAlias(seqTypeWithAntibodyTarget.name, [
                    libraryLayout: seqTypeWithAntibodyTarget.libraryLayout,
                    singleCell   : seqTypeWithAntibodyTarget.singleCell,
            ]) >> seqTypeWithAntibodyTarget
            0 * _
        }
        service.applicationContext = Mock(ApplicationContext) {
            4 * getBeansOfType(MetadataValidator) >> [:]
        }
        service.otrsTicketService = Mock(OtrsTicketService) {
            2 * createOrResetOtrsTicket(_, _, _) >> otrsTicket
        }
        service.antibodyTargetService = Mock(AntibodyTargetService) {
            2 * findByNameOrImportAlias(_) >> antibodyTarget
        }
        service.samplePairDeciderService = Mock(SamplePairDeciderService) {
            2 * findOrCreateSamplePairs([]) >> []
            0 * _
        }
        service.mergingCriteriaService = Mock(MergingCriteriaService) {
            2 * createDefaultMergingCriteria(_, _)
        }
        service.dataInstallationInitializationService = Mock(DataInstallationInitializationService) {
            _ * createWorkflowRuns(_) >> []
        }
        service.allDecider = Mock(AllDecider) {
            _ * decide(_, _) >> []
        }
        service.processingThresholdsService = Mock(ProcessingThresholdsService)
        service.fileSystemService = new TestFileSystemService()
        service.configService = new TestConfigService()
        service.configService.processingOptionService = new ProcessingOptionService()

        when:
        List<ValidateAndImportResult> validateAndImportResults1 = service.validateAndImportMultiple(TICKET_NUMBER, '1111', true)

        then:
        validateAndImportResults1.size() == 1
        validateAndImportResults1[0].metadataFile.fileName == file1.fileName.toString()
        validateAndImportResults1[0].context.spreadsheet.dataRows.size() == 2
        validateAndImportResults1[0].context.spreadsheet.dataRows[0].cells[0].text == fastq11
        validateAndImportResults1[0].context.spreadsheet.dataRows[1].cells[0].text == fastq12

        when:
        List<ValidateAndImportResult> validateAndImportResults2 = service.validateAndImportMultiple(TICKET_NUMBER, '1111', true)

        then:
        validateAndImportResults2.size() == 1
        validateAndImportResults2[0].metadataFile.fileName == file2.fileName.toString()
        validateAndImportResults2[0].context.spreadsheet.dataRows.size() == 2
        validateAndImportResults2[0].context.spreadsheet.dataRows[0].rowIndex == 3
        validateAndImportResults2[0].context.spreadsheet.dataRows[1].rowIndex == 4
        validateAndImportResults2[0].context.spreadsheet.dataRows[0].cells[0].text == fastq21
        validateAndImportResults2[0].context.spreadsheet.dataRows[1].cells[0].text == fastq22
    }

    void "validateAndImportMultiple when some are invalid, throws AutoImportFailedException"() {
        given:
        SeqCenter seqCenter = createSeqCenter(autoImportable: true, autoImportDir: "/auto-import-dir")
        DomainFactory.createDefaultRealmWithProcessingOption()

        Problems problems = new Problems()
        problems.addProblem([] as Set, LogLevel.ERROR, "An Error occurred")
        MetadataValidationContext context1 = MetadataValidationContextFactory.createContext(
                [metadataFile: Paths.get("${seqCenter.autoImportDir}/001111/data/1111_meta.tsv"), problems: problems])
        MetadataValidationContext context2 = MetadataValidationContextFactory.createContext(
                [metadataFile: Paths.get("${seqCenter.autoImportDir}/002222/data/2222_meta.tsv")])
        MetadataValidationContext context3 = MetadataValidationContextFactory.createContext(
                [metadataFile: Paths.get("${seqCenter.autoImportDir}/003333/data/3333_meta.tsv"), problems: problems])

        MetadataImportService service = Spy(MetadataImportService) {
            1 * validate(context1.metadataFile, DirectoryStructureBeanName.GPCF_SPECIFIC, _) >> context1
            1 * validate(context2.metadataFile, DirectoryStructureBeanName.GPCF_SPECIFIC, _) >> context2
            1 * validate(context3.metadataFile, DirectoryStructureBeanName.GPCF_SPECIFIC, _) >> context3
            1 * importMetadataFile(context2, true, FastqImportInstance.ImportMode.AUTOMATIC, TICKET_NUMBER, null, true) >> DomainFactory.createMetaDataFile()
        }

        service.fileSystemService = new TestFileSystemService()
        service.configService = new TestConfigService()
        service.configService.processingOptionService = new ProcessingOptionService()

        when:
        service.validateAndImportMultiple(TICKET_NUMBER, '1111+2222+3333', false)

        then:
        MultiImportFailedException e = thrown()
        e.failedValidations == [context1, context3]
        e.allPaths == [context1, context2, context3]*.metadataFile
    }

    @Unroll
    void "parseIlseNumbers returns correct collection of ILSe numbers"(String string, List<Integer> numbers) {
        expect:
        MetadataImportService.parseIlseNumbers(string) == numbers

        where:
        string                          || numbers
        '6072'                          || [6072]
        '6160+6061'                     || [6160, 6061]
        '6083+6085'                     || [6083, 6085]
        '6127-6128'                     || [6127, 6128]
        '6005-6009'                     || [6005, 6006, 6007, 6008, 6009]
        '6108,6136-6138'                || [6108, 6136, 6137, 6138]
        '6012,6013,6015-6019,6021-6022' || [6012, 6013, 6015, 6016, 6017, 6018, 6019, 6021, 6022]
        '6160 & 6061'                   || [6160, 6061]
        '6160 + 6061'                   || [6160, 6061]
        '6127 - 6128'                   || [6127, 6128]
    }

    @Unroll
    void "parseIlseNumbers throws exception when input is invalid"(String string, String exceptionMessage) {
        when:
        MetadataImportService.parseIlseNumbers(string)

        then:
        IllegalArgumentException e = thrown()
        e.message == exceptionMessage

        where:
        string      || exceptionMessage
        '123'       || "Cannot parse '123' as an ILSe number or a range of ILSe numbers."
        '1234567'   || "Cannot parse '1234567' as an ILSe number or a range of ILSe numbers."
        '1111-2222' || "Range of ILSe numbers is too large: '1111-2222'"
    }

    void "mayImport, when maximumProblemLevel is less than WARNING, returns true"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        assert context.maximumProblemLevel.intValue() < LogLevel.WARNING.intValue()

        expect:
        MetadataImportService.mayImport(context, false, HelperUtils.randomMd5sum)
    }

    void "mayImport, when maximumProblemLevel is greater than WARNING, returns false"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        context.addProblem(Collections.emptySet(), LogLevel.ERROR, 'my message')
        assert context.maximumProblemLevel.intValue() > LogLevel.WARNING.intValue()

        expect:
        !MetadataImportService.mayImport(context, true, context.metadataFileMd5sum)
    }

    @Unroll
    void "mayImport, when maximumProblemLevel is WARNING and MD5 sum matches, returns ignoreWarnings"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        context.addProblem(Collections.emptySet(), LogLevel.WARNING, 'my message')
        assert context.maximumProblemLevel == LogLevel.WARNING

        expect:
        MetadataImportService.mayImport(context, ignoreWarnings, context.metadataFileMd5sum) == ignoreWarnings

        where:
        ignoreWarnings << [true, false]
    }

    void "mayImport, when maximumProblemLevel is WARNING and ignoreWarnings is false and MD5 sum does not match, returns false"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        context.addProblem(Collections.emptySet(), LogLevel.WARNING, 'my message')
        assert context.maximumProblemLevel == LogLevel.WARNING

        expect:
        !MetadataImportService.mayImport(context, false, HelperUtils.randomMd5sum)
        exactlyOneElement(context.problems).message == 'my message'
    }

    void "mayImport, when maximumProblemLevel is WARNING and ignoreWarnings is true and MD5 sum does not match, adds problem and returns false"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        context.addProblem(Collections.emptySet(), LogLevel.WARNING, HelperUtils.uniqueString)
        assert context.maximumProblemLevel == LogLevel.WARNING

        expect:
        !MetadataImportService.mayImport(context, true, HelperUtils.randomMd5sum)
        context.problems.find {
            it.level == LogLevel.INFO && it.message == 'Not ignoring warnings, because the metadata file has changed since the previous validation.'
        }
    }

    @Unroll
    void "importMetadataFile imports correctly"(boolean runExists, boolean includeOptional, boolean align, FastqImportInstance.ImportMode importMode) {
        given:
        DomainFactory.createAllAnalysableSeqTypes()

        def (fastq1, fastq2, fastq3, fastq4, fastq5, fastq6, fastq7, fastq8, fastq9) =
        ["fastq_a", "s_1_1_", "s_1_2_", "s_2_1_", "s_2_2_", "s_3_1_", "fastq_g", "fastq_b", "fastq_sc"]

        def (String runName1, String runName2) = ["run1", "run2"]
        def (String center1, String center2) = ["center1", "center2"]
        def (String platform1, String platform2) = ["platform1", "platform2"]
        def (String model1, String model2) = ["model1", "model2"]
        def (String kit1, String kit2) = ["kit1", "kit2"]
        def (String human, String mouse, String chicken) = ["human", "mouse", "chicken"]
        def (String target1, String target2) = ["target1", "target2"]
        def (String single, String paired) = [SequencingReadType.SINGLE, SequencingReadType.PAIRED]
        def (String parse, String scParse, String get) = ["parse_me", "sc_parse_me", "in_db"]

        def (Date run1Date, Date run2Date) = [[2016, 4, 13], [2016, 6, 6]].collect { TimeUtils.toDate(LocalDate.of(it[0], it[1], it[2])) }
        def (String date1, String date2) = [run1Date, run2Date].collect { TimeFormats.DATE.getFormattedDate(it) }

        def (md5a, md5b, md5c, md5d, md5e, md5f, md5g, md5h, md5i) = (1..9).collect { HelperUtils.randomMd5sum }

        int seqTrackCount = includeOptional ? 7 : 1
        int dataFileCount = includeOptional ? 9 : 1

        String scMaterial = SeqType.SINGLE_CELL_DNA

        SeqCenter seqCenter = createSeqCenter(name: center1)
        SeqCenter seqCenter2 = createSeqCenter(name: center2)
        SeqPlatform seqPlatform = createSeqPlatformWithSeqPlatformGroup(
                name: platform1,
                seqPlatformModelLabel: createSeqPlatformModelLabel(name: model1),
        )
        SeqPlatform seqPlatform2 = createSeqPlatformWithSeqPlatformGroup(
                name: platform2,
                seqPlatformModelLabel: createSeqPlatformModelLabel(name: model2),
        )

        if (runExists) {
            createRun(
                    name: runName1,
                    dateExecuted: run1Date,
                    seqCenter: seqCenter,
                    seqPlatform: seqPlatform,
            )
        }
        OtrsTicket otrsTicket = createOtrsTicket(automaticNotification: true)
        SpeciesWithStrain humanSpecies = createSpeciesWithStrain(importAlias: ['human'] as Set)
        SpeciesWithStrain mouseSpecies = createSpeciesWithStrain(importAlias: ['mouse'] as Set)
        SpeciesWithStrain chickenSpecies = createSpeciesWithStrain(importAlias: ['chicken'] as Set)
        SeqType mySeqType = DomainFactory.createWholeGenomeSeqType(SequencingReadType.SINGLE)
        SeqType mySeqTypeTag = createSeqType(name: SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION, libraryLayout: SequencingReadType.SINGLE)
        SeqType exomeSingle = DomainFactory.createExomeSeqType(SequencingReadType.SINGLE)
        SeqType exomePaired = DomainFactory.createExomeSeqType(SequencingReadType.PAIRED)
        SeqType chipSeqSingle = DomainFactory.createChipSeqType(SequencingReadType.SINGLE)
        SeqType chipSeqPaired = DomainFactory.createChipSeqType(SequencingReadType.PAIRED)
        SeqType scExomeSingle = DomainFactory.createExomeSeqType(SequencingReadType.SINGLE)
        Sample sample1 = DomainFactory.createSampleIdentifier(name: 'in_db').sample
        Sample sample2
        def (SoftwareTool pipeline1, SoftwareTool pipeline2, SoftwareTool unknownPipeline) =
        ['pipeline1', 'pipeline2', 'unknown'].collect {
            createSoftwareToolIdentifier(
                    name: it,
                    softwareTool: createSoftwareTool(
                            type: SoftwareTool.Type.BASECALLING,
                    ),
            ).softwareTool
        }
        LibraryPreparationKit libraryPreparationKit1 = createLibraryPreparationKit(name: kit1)
        LibraryPreparationKit libraryPreparationKit2 = createLibraryPreparationKit(name: kit2)
        AntibodyTarget antibodyTarget1 = createAntibodyTarget(name: target1)
        AntibodyTarget antibodyTarget2 = createAntibodyTarget(name: target2)
        FileType fileType = createFileType(
                type: FileType.Type.SEQUENCE,
                signature: '_',
        )

        Closure<SampleIdentifier> createSampleIdentifierForSample2 = { String identifierName ->
            if (sample2 == null) {
                sample2 = createSample()
            }
            SampleIdentifier identifier = DomainFactory.createSampleIdentifier(name: identifierName, sample: sample2)
            return identifier
        }

        MetadataImportService service = Spy(MetadataImportService) {
            notifyAboutUnsetConfig(_, _, _) >> null
        }
        service.sampleIdentifierService = Mock(SampleIdentifierService) {
            parseAndFindOrSaveSampleIdentifier(parse, _) >> createSampleIdentifierForSample2(parse)
            parseAndFindOrSaveSampleIdentifier(scParse, _) >> createSampleIdentifierForSample2(scParse)
            parseSingleCellWellLabel(scParse, _) >> { return scParse }
        }

        service.seqTrackService = Mock(SeqTrackService)
        service.speciesWithStrainService = Mock(SpeciesWithStrainService) {
            getByAlias(human) >> humanSpecies
            getByAlias(mouse) >> mouseSpecies
            getByAlias(chicken) >> chickenSpecies
        }
        service.seqPlatformService = Mock(SeqPlatformService) {
            findSeqPlatform(seqPlatform.name, seqPlatform.seqPlatformModelLabel.name, null) >> seqPlatform
            findSeqPlatform(seqPlatform2.name, seqPlatform2.seqPlatformModelLabel.name, null) >> seqPlatform2
        }
        service.otrsTicketService = Mock(OtrsTicketService) {
            createOrResetOtrsTicket(otrsTicket.ticketNumber, null, true) >> otrsTicket
        }
        service.libraryPreparationKitService = Mock(LibraryPreparationKitService) {
            findByNameOrImportAlias(libraryPreparationKit1.name) >> libraryPreparationKit1
            findByNameOrImportAlias(libraryPreparationKit2.name) >> libraryPreparationKit2
        }
        service.seqTypeService = Mock(SeqTypeService) {
            findByNameOrImportAlias(WG, [libraryLayout: SequencingReadType.SINGLE, singleCell: false]) >> mySeqType
            findByNameOrImportAlias(WGBS_T, [libraryLayout: SequencingReadType.SINGLE, singleCell: false]) >> mySeqTypeTag
            findByNameOrImportAlias(EXON, [libraryLayout: SequencingReadType.SINGLE, singleCell: false]) >> exomeSingle
            findByNameOrImportAlias(EXON, [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) >> exomePaired
            findByNameOrImportAlias(CHIP_SEQ, [libraryLayout: SequencingReadType.SINGLE, singleCell: false]) >> chipSeqSingle
            findByNameOrImportAlias(CHIP_SEQ, [libraryLayout: SequencingReadType.PAIRED, singleCell: false]) >> chipSeqPaired
            findByNameOrImportAlias(SC_EXON, [libraryLayout: SequencingReadType.SINGLE, singleCell: true]) >> scExomeSingle
        }
        service.antibodyTargetService = Mock(AntibodyTargetService) {
            findByNameOrImportAlias(target1) >> antibodyTarget1
            findByNameOrImportAlias(target2) >> antibodyTarget2
        }

        mockAdditionalServices(service, seqTrackCount)

        File file = new File(new File(TestCase.uniqueNonExistentPath, runName1), 'metadata.tsv')
        DirectoryStructure directoryStructure = new MockDirectoryStructure(file: file)

        String metadata = """
${FASTQ_FILE}                   ${fastq1}                   ${fastq2}     ${fastq3}     ${fastq4}                   ${fastq5}                   ${fastq6}     ${fastq7}                 ${fastq8}                   ${fastq9}
${MD5}                          ${md5a}                     ${md5b}       ${md5c}       ${md5d}                     ${md5e}                     ${md5f}       ${md5g}                   ${md5h}                     ${md5i}
${RUN_ID}                       ${runName1}                 ${runName1}   ${runName1}   ${runName1}                 ${runName1}                 ${runName1}   ${runName1}               ${runName2}                 ${runName1}
${CENTER_NAME}                  ${center1}                  ${center1}    ${center1}    ${center1}                  ${center1}                  ${center1}    ${center1}                ${center2}                  ${center1}
${INSTRUMENT_PLATFORM}          ${platform1}                ${platform1}  ${platform1}  ${platform1}                ${platform1}                ${platform1}  ${platform1}              ${platform2}                ${platform1}
${INSTRUMENT_MODEL}             ${model1}                   ${model1}     ${model1}     ${model1}                   ${model1}                   ${model1}     ${model1}                 ${model2}                   ${model1}
${RUN_DATE}                     ${date1}                    ${date1}      ${date1}      ${date1}                    ${date1}                    ${date1}      ${date1}                  ${date2}                    ${date1}
${LANE_NO}                      4                           1             1             2                           2                           2             3                         5                           1
${INDEX}                        -                           barcode8      barcode8      barcode7                    barcode7                    barcode6      -                         -                           -
${SEQUENCING_TYPE}              ${WG}                       ${EXON}       ${EXON}       ${CHIP_SEQ}                 ${CHIP_SEQ}                 ${CHIP_SEQ}   ${EXON}                   ${WGBS_T}                   ${SC_EXON}
${SEQUENCING_READ_TYPE}         ${single}                   ${paired}     ${paired}     ${paired}                   ${paired}                   ${single}     ${single}                 ${single}                   ${single}
${READ}                         1                           1             2             1                           2                           1             1                         1                           1
${SAMPLE_NAME}                  ${parse}                    ${get}        ${get}        ${parse}                    ${parse}                    ${get}        ${parse}                  ${parse}                    ${scParse}
${SPECIES}                      ${human}+${mouse}           ${mouse}      ${mouse}      ${human}+${mouse}           ${human}+${mouse}           ${mouse}      ${human}+${mouse}         ${human}+${mouse}           ${human}+${mouse}
${BASE_MATERIAL}                -                           -             -             -                           -                           -             -                         -                           ${scMaterial}
"""
        if (includeOptional) {
            metadata += """
${FRAGMENT_SIZE}                -                           -             -             234                         234                         -             456                       -                           -
${FASTQ_GENERATOR}              -                           pipeline1     pipeline1     -                           -                           -             pipeline2                 -                           -
${LIB_PREP_KIT}                 -                           ${kit1}       ${kit1}       ${kit2}                     ${kit2}                     UNKNOWN       UNKNOWN                   -                           ${kit2}
${ANTIBODY_TARGET}              -                           -             -             target1                     target1                     target2       -                         -                           -
${ANTIBODY}                     -                           -             -             antibody1                   antibody1                   -             -                         -                           -
${ILSE_NO}                      -                           1234          1234          -                           -                           2345          -                         -                           -
"""
        }
        List<List<String>> lines = metadata.readLines().findAll()*.split(/ {2,}/).transpose()
        if (!includeOptional) {
            lines = lines.subList(0, 2)
        }
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                lines.collect { it*.replaceFirst(/^-$/, '').join('\t') }.join('\n'),
                [metadataFile: file.toPath(), directoryStructure: directoryStructure,]
        )

        when:
        MetaDataFile result = service.importMetadataFile(context, align, importMode, otrsTicket.ticketNumber, null, otrsTicket.automaticNotification)

        then:

        // runs
        Run.count == (includeOptional ? 2 : 1)
        Run run1 = CollectionUtils.atMostOneElement(Run.findAllWhere(
                name: runName1,
                dateExecuted: run1Date,
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
        ))
        run1 != null
        Run run2 = CollectionUtils.atMostOneElement(Run.findAllWhere(
                name: runName2,
                dateExecuted: run2Date,
                seqCenter: seqCenter2,
                seqPlatform: seqPlatform2,
        ))
        (run2 != null) == includeOptional

        // fastqImportInstance
        FastqImportInstance.count == 1
        FastqImportInstance fastqImportInstance = CollectionUtils.atMostOneElement(FastqImportInstance.findAllWhere(
                otrsTicket: otrsTicket,
                importMode: importMode,
        ))
        fastqImportInstance != null

        // metadataFile
        MetaDataFile.count == 1
        MetaDataFile metadataFile = CollectionUtils.atMostOneElement(MetaDataFile.findAllWhere(
                fileName: file.name,
                filePath: file.parent,
                md5sum: context.metadataFileMd5sum,
                fastqImportInstance: fastqImportInstance,
        ))
        result == metadataFile

        SeqTrack.count == seqTrackCount
        DataFile.count == dataFileCount
        MetaDataKey.count == context.spreadsheet.header.cells.size()
        MetaDataEntry.count == context.spreadsheet.header.cells.size() * dataFileCount

        Map commonDataFileProperties = [
                pathName           : '',
                used               : true,
                fastqImportInstance: fastqImportInstance,
                fileType           : fileType,
        ]
        Map commonRun1DataFileProperties = commonDataFileProperties + [
                dateExecuted: run1Date,
                run         : run1,
        ]
        Map commonRun2DataFileProperties = commonDataFileProperties + [
                dateExecuted: run2Date,
                run         : run2,
        ]

        // seqTrack1
        SeqTrack seqTrack1 = CollectionUtils.atMostOneElement(SeqTrack.findAllWhere(
                laneId: '4',
                insertSize: 0,
                run: run1,
                sample: sample2,
                seqType: mySeqType,
                pipelineVersion: unknownPipeline,
                kitInfoReliability: InformationReliability.UNKNOWN_UNVERIFIED,
                libraryPreparationKit: null,
        ))
        seqTrack1.ilseId == null
        seqTrack1.individual.species == humanSpecies
        seqTrack1.sample.mixedInSpecies.size() == 1
        seqTrack1.sample.mixedInSpecies.first() == mouseSpecies
        DataFile dataFile1 = CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRun1DataFileProperties + [
                fileName   : fastq1,
                vbpFileName: fastq1,
                md5sum     : md5a,
                project    : sample2.project,
                mateNumber : 1,
                indexFile  : false,
                seqTrack   : seqTrack1,
        ]))
        dataFile1 != null
        MetaDataKey key = CollectionUtils.atMostOneElement(MetaDataKey.findAllWhere(name: MD5.name()))
        key != null
        CollectionUtils.atMostOneElement(MetaDataEntry.findAllWhere(
                dataFile: dataFile1,
                key: key,
                value: md5a,
        ))

        if (includeOptional) {
            // seqTrack2
            SeqTrack seqTrack2 = CollectionUtils.atMostOneElement(SeqTrack.findAllWhere(
                    laneId: '1_barcode8',
                    insertSize: 0,
                    run: run1,
                    sample: sample1,
                    seqType: exomePaired,
                    pipelineVersion: pipeline1,
                    kitInfoReliability: InformationReliability.KNOWN,
                    libraryPreparationKit: libraryPreparationKit1,
            ))
            assert seqTrack2.ilseId == 1234
            assert seqTrack2.individual.species == mouseSpecies
            assert seqTrack2.sample.mixedInSpecies.size() == 0
            assert CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRun1DataFileProperties + [
                    fileName   : fastq2,
                    vbpFileName: fastq2,
                    md5sum     : md5b,
                    project    : sample1.project,
                    mateNumber : 1,
                    indexFile  : false,
                    seqTrack   : seqTrack2,
            ]))
            assert CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRun1DataFileProperties + [
                    fileName   : fastq3,
                    vbpFileName: fastq3,
                    md5sum     : md5c,
                    project    : sample1.project,
                    mateNumber : 2,
                    indexFile  : false,
                    seqTrack   : seqTrack2,
            ]))

            // seqTrack3
            SeqTrack seqTrack3 = CollectionUtils.atMostOneElement(SeqTrack.findAllWhere(
                    laneId: '2_barcode7',
                    insertSize: 234,
                    run: run1,
                    sample: sample2,
                    seqType: chipSeqPaired,
                    pipelineVersion: unknownPipeline,
                    kitInfoReliability: InformationReliability.KNOWN,
                    libraryPreparationKit: libraryPreparationKit2,
                    antibodyTarget: antibodyTarget1,
                    antibody: 'antibody1',
            ))
            assert seqTrack3.ilseId == null
            assert seqTrack3.individual.species == humanSpecies
            assert seqTrack3.sample.mixedInSpecies.size() == 1
            assert seqTrack3.sample.mixedInSpecies.first() == mouseSpecies
            assert CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRun1DataFileProperties + [
                    fileName   : fastq4,
                    vbpFileName: fastq4,
                    md5sum     : md5d,
                    project    : sample2.project,
                    mateNumber : 1,
                    indexFile  : false,
                    seqTrack   : seqTrack3,
            ]))
            assert CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRun1DataFileProperties + [
                    fileName   : fastq5,
                    vbpFileName: fastq5,
                    md5sum     : md5e,
                    project    : sample2.project,
                    mateNumber : 2,
                    indexFile  : false,
                    seqTrack   : seqTrack3,
            ]))

            // seqTrack4
            SeqTrack seqTrack4 = CollectionUtils.atMostOneElement(SeqTrack.findAllWhere(
                    laneId: '2_barcode6',
                    insertSize: 0,
                    run: run1,
                    sample: sample1,
                    seqType: chipSeqSingle,
                    pipelineVersion: unknownPipeline,
                    kitInfoReliability: InformationReliability.UNKNOWN_VERIFIED,
                    libraryPreparationKit: null,
                    antibodyTarget: antibodyTarget2,
                    antibody: null,
            ))
            assert seqTrack4.ilseId == 2345
            assert seqTrack4.individual.species == mouseSpecies
            assert seqTrack4.sample.mixedInSpecies.size() == 0
            assert CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRun1DataFileProperties + [
                    fileName   : fastq6,
                    vbpFileName: fastq6,
                    md5sum     : md5f,
                    project    : sample1.project,
                    mateNumber : 1,
                    indexFile  : false,
                    seqTrack   : seqTrack4,
            ]))

            // seqTrack5
            SeqTrack seqTrack5 = CollectionUtils.atMostOneElement(SeqTrack.findAllWhere(
                    laneId: '3',
                    insertSize: 456,
                    run: run1,
                    sample: sample2,
                    seqType: exomeSingle,
                    pipelineVersion: pipeline2,
                    kitInfoReliability: InformationReliability.UNKNOWN_VERIFIED,
                    libraryPreparationKit: null,
            ))
            assert seqTrack5.ilseId == null
            assert seqTrack5.individual.species == humanSpecies
            assert seqTrack5.sample.mixedInSpecies.size() == 1
            assert seqTrack5.sample.mixedInSpecies.first() == mouseSpecies
            assert CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRun1DataFileProperties + [
                    fileName   : fastq7,
                    vbpFileName: fastq7,
                    md5sum     : md5g,
                    project    : sample2.project,
                    mateNumber : 1,
                    indexFile  : false,
                    seqTrack   : seqTrack5,
            ]))

            // seqTrack6
            SeqTrack seqTrack6 = CollectionUtils.atMostOneElement(SeqTrack.findAllWhere(
                    laneId: '5',
                    insertSize: 0,
                    run: run2,
                    sample: sample2,
                    seqType: mySeqTypeTag,
                    pipelineVersion: unknownPipeline,
                    kitInfoReliability: InformationReliability.UNKNOWN_UNVERIFIED,
                    libraryPreparationKit: null,
            ))
            assert seqTrack6.ilseId == null
            assert seqTrack6.individual.species == humanSpecies
            assert seqTrack6.sample.mixedInSpecies.size() == 1
            assert seqTrack6.sample.mixedInSpecies.first() == mouseSpecies
            assert CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRun2DataFileProperties + [
                    fileName   : fastq8,
                    vbpFileName: fastq8,
                    md5sum     : md5h,
                    project    : sample2.project,
                    mateNumber : 1,
                    indexFile  : false,
                    seqTrack   : seqTrack6,
            ]))

            // seqTrack7
            SeqTrack seqTrack7 = CollectionUtils.atMostOneElement(SeqTrack.findAllWhere(
                    laneId: '1',
                    insertSize: 0,
                    run: run1,
                    sample: sample2,
                    seqType: scExomeSingle,
                    pipelineVersion: unknownPipeline,
                    kitInfoReliability: InformationReliability.KNOWN,
                    libraryPreparationKit: libraryPreparationKit2,
            ))
            assert seqTrack7.ilseId == null
            assert seqTrack7.individual.species == humanSpecies
            assert seqTrack7.sample.mixedInSpecies.size() == 1
            assert seqTrack7.sample.mixedInSpecies.first() == mouseSpecies
            assert CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRun1DataFileProperties + [
                    fileName   : fastq9,
                    vbpFileName: fastq9,
                    md5sum     : md5i,
                    project    : sample2.project,
                    mateNumber : 1,
                    indexFile  : false,
                    seqTrack   : seqTrack7,
            ]))
        }

        (align ? 1 : 0 * seqTrackCount) * service.seqTrackService.decideAndPrepareForAlignment(!null) >> []
        seqTrackCount * service.seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(!null, false)

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(SamplePair)

        where:
        runExists | includeOptional | align | importMode
        false     | true            | false | FastqImportInstance.ImportMode.AUTOMATIC
        true      | false           | true  | FastqImportInstance.ImportMode.MANUAL
    }

    @TupleConstructor
    class MockDirectoryStructure implements DirectoryStructure {
        File file

        @Override
        List<String> getRequiredColumnTitles() {
            return [FASTQ_FILE.name()]
        }

        @Override
        Path getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple) {
            return Paths.get(file.parent, valueTuple.getValue(FASTQ_FILE.name()))
        }
    }

    void "importMetadataFile imports correctly data with AntibodyTarget"() {
        given:
        String runName = 'run'
        Date date = TimeUtils.toDate(LocalDate.of(2000, 1, 1))
        String dateString = TimeFormats.DATE.getFormattedDate(date)
        String fastq1 = "fastq_1.gz"
        String fastq2 = "fastq_2.gz"
        String human = "human"
        String mouse = "mouse"
        String chicken = "chicken"
        String md5sum1 = HelperUtils.randomMd5sum
        String md5sum2 = HelperUtils.randomMd5sum

        DomainFactory.createAllAnalysableSeqTypes()
        SpeciesWithStrain humanSpecies = createSpeciesWithStrain(importAlias: [human] as Set)
        SpeciesWithStrain mouseSpecies = createSpeciesWithStrain(importAlias: [mouse] as Set)
        SpeciesWithStrain chickenSpecies = createSpeciesWithStrain(importAlias: [chicken] as Set)
        SeqType seqTypeWithAntibodyTarget = createSeqType([
                libraryLayout    : SequencingReadType.PAIRED,
                hasAntibodyTarget: true,
        ])
        SeqCenter seqCenter = createSeqCenter()
        SeqPlatform seqPlatform = createSeqPlatform()
        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier()
        AntibodyTarget antibodyTarget = createAntibodyTarget()
        SoftwareToolIdentifier softwareToolIdentifier = createSoftwareToolIdentifier([
                softwareTool: createSoftwareTool([
                        type: SoftwareTool.Type.BASECALLING,
                ]),
        ])
        FileType fileType = createFileType(
                type: FileType.Type.SEQUENCE,
                signature: '_',
        )

        MetadataImportService service = Spy(MetadataImportService) {
            notifyAboutUnsetConfig(_, _, _) >> null
        }
        service.sampleIdentifierService = Mock(SampleIdentifierService) {
            0 * _
        }
        service.seqTrackService = Mock(SeqTrackService) {
            0 * decideAndPrepareForAlignment(!null) >> []
            1 * determineAndStoreIfFastqFilesHaveToBeLinked(!null, false)
        }
        service.seqPlatformService = Mock(SeqPlatformService) {
            1 * findSeqPlatform(seqPlatform.name, seqPlatform.seqPlatformModelLabel.name, null) >> seqPlatform
            0 * _
        }
        service.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias(seqTypeWithAntibodyTarget.name, [
                    libraryLayout: seqTypeWithAntibodyTarget.libraryLayout,
                    singleCell   : seqTypeWithAntibodyTarget.singleCell,
            ]) >> seqTypeWithAntibodyTarget
            0 * _
        }
        service.antibodyTargetService = Mock(AntibodyTargetService) {
            findByNameOrImportAlias(antibodyTarget.name) >> antibodyTarget
        }
        service.speciesWithStrainService = Mock(SpeciesWithStrainService) {
            getByAlias(human) >> humanSpecies
            getByAlias(mouse) >> mouseSpecies
            getByAlias(chicken) >> chickenSpecies
        }
        mockAdditionalServices(service)

        File file = new File(new File(TestCase.uniqueNonExistentPath, runName), 'metadata.tsv')
        DirectoryStructure directoryStructure = Mock(DirectoryStructure) {
            _ * getRequiredColumnTitles() >> [FASTQ_FILE.name()]
            _ * getDataFilePath(_, _) >> { MetadataValidationContext context, Row row ->
                return Paths.get(file.parent, row.getCellByColumnTitle(FASTQ_FILE.name()).text)
            }
        }

        String metadata = """
${FASTQ_FILE}                   ${fastq1}                                   ${fastq2}
${MD5}                          ${md5sum1}                                  ${md5sum2}
${RUN_ID}                       ${runName}                                  ${runName}
${CENTER_NAME}                  ${seqCenter}                                ${seqCenter}
${INSTRUMENT_PLATFORM}          ${seqPlatform.name}                         ${seqPlatform.name}
${INSTRUMENT_MODEL}             ${seqPlatform.seqPlatformModelLabel.name}   ${seqPlatform.seqPlatformModelLabel.name}
${RUN_DATE}                     ${dateString}                               ${dateString}
${LANE_NO}                      1                                           1
${SEQUENCING_TYPE}              ${seqTypeWithAntibodyTarget.name}           ${seqTypeWithAntibodyTarget.name}
${SEQUENCING_READ_TYPE}         ${seqTypeWithAntibodyTarget.libraryLayout}  ${seqTypeWithAntibodyTarget.libraryLayout}
${READ}                         1                                           2
${SAMPLE_NAME}                  ${sampleIdentifier.name}                    ${sampleIdentifier.name}
${ANTIBODY_TARGET}              ${antibodyTarget.name}                      ${antibodyTarget.name}
${ANTIBODY}                     -                                           -
${FASTQ_GENERATOR}              ${softwareToolIdentifier.name}              ${softwareToolIdentifier.name}
${SPECIES}                      ${human}+${mouse}+${chicken}                ${human}+${mouse}+${chicken}
"""

        List<List<String>> lines = metadata.readLines().findAll()*.split(/ {2,}/).transpose()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                lines.collect { it*.replaceFirst(/^-$/, '').join('\t') }.join('\n'),
                [
                        metadataFile      : file.toPath(),
                        directoryStructure: directoryStructure,
                ]
        )

        when:
        MetaDataFile result = service.importMetadataFile(context, false, FastqImportInstance.ImportMode.MANUAL, null, null, false)

        then:
        // runs
        Run.count == 1
        Run run = CollectionUtils.atMostOneElement(Run.findAllWhere(
                name: runName,
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
        ))
        run != null
        run.dateExecuted == date

        // fastqImportInstance
        FastqImportInstance.count == 1
        FastqImportInstance fastqImportInstance = CollectionUtils.atMostOneElement(FastqImportInstance.findAllWhere(
                otrsTicket: null,
                importMode: FastqImportInstance.ImportMode.MANUAL
        ))
        fastqImportInstance != null

        // metadataFile
        MetaDataFile.count == 1
        MetaDataFile metadataFile = CollectionUtils.atMostOneElement(MetaDataFile.findAllWhere(
                fileName: file.name,
                filePath: file.parent,
                md5sum: context.metadataFileMd5sum,
                fastqImportInstance: fastqImportInstance,
        ))
        result == metadataFile

        SeqTrack.count == 1
        DataFile.count == 2
        MetaDataKey.count == context.spreadsheet.header.cells.size()
        MetaDataEntry.count == context.spreadsheet.header.cells.size() * 2

        // seqTrack1
        SeqTrack seqTrack = CollectionUtils.atMostOneElement(SeqTrack.findAllWhere(
                laneId: '1',
                insertSize: 0,
                run: run,
                sample: sampleIdentifier.sample,
                seqType: seqTypeWithAntibodyTarget,
                pipelineVersion: softwareToolIdentifier.softwareTool,
                kitInfoReliability: InformationReliability.UNKNOWN_UNVERIFIED,
                libraryPreparationKit: null,
        ))
        seqTrack.ilseId == null
        seqTrack.antibodyTarget == antibodyTarget
        seqTrack.individual.species == humanSpecies
        seqTrack.sample.mixedInSpecies.size() == 2

        Map commonRunDataFileProperties = [
                pathName           : '',
                used               : true,
                fastqImportInstance: fastqImportInstance,
                fileType           : fileType,
                dateExecuted       : date,
                run                : run,
                project            : sampleIdentifier.project,
                seqTrack           : seqTrack,
        ]

        DataFile dataFile1 = CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRunDataFileProperties + [
                fileName   : fastq1,
                vbpFileName: fastq1,
                md5sum     : md5sum1,
                mateNumber : 1,
                indexFile  : false,
        ]))
        dataFile1 != null

        DataFile dataFile2 = CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRunDataFileProperties + [
                fileName   : fastq2,
                vbpFileName: fastq2,
                md5sum     : md5sum2,
                mateNumber : 2,
                indexFile  : false,
        ]))
        dataFile2 != null
    }

    void "importMetadataFile imports correctly data index files"() {
        given:
        String runName = 'run'
        Date date = TimeUtils.toDate(LocalDate.of(2000, 1, 1))
        String dateString = TimeFormats.DATE.getFormattedDate(date)
        String fastq1 = "fastq_r1.gz"
        String fastq2 = "fastq_r2.gz"
        String fastqIndex1 = "fastq_i1.gz"
        String fastqIndex2 = "fastq_i2.gz"
        String human = "human"
        String mouse = "mouse"
        String md5sum1 = HelperUtils.randomMd5sum
        String md5sum2 = HelperUtils.randomMd5sum
        String md5sumIndex1 = HelperUtils.randomMd5sum
        String md5sumIndex2 = HelperUtils.randomMd5sum
        SpeciesWithStrain humanSpecies = createSpeciesWithStrain(importAlias: [human] as Set)
        SpeciesWithStrain mouseSpecies = createSpeciesWithStrain(importAlias: [mouse] as Set)
        DomainFactory.createAllAnalysableSeqTypes()
        SeqType seqType = DomainFactory.createSeqTypePaired()
        SeqCenter seqCenter = createSeqCenter()
        SeqPlatform seqPlatform = createSeqPlatform()
        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier()
        SoftwareToolIdentifier softwareToolIdentifier = createSoftwareToolIdentifier([
                softwareTool: createSoftwareTool([
                        type: SoftwareTool.Type.BASECALLING,
                ]),
        ])
        FileType fileType = createFileType(
                type: FileType.Type.SEQUENCE,
                signature: '_',
        )

        MetadataImportService service = Spy(MetadataImportService) {
            notifyAboutUnsetConfig(_, _, _) >> null
        }
        service.sampleIdentifierService = Mock(SampleIdentifierService) {
            0 * _
        }
        service.seqTrackService = Mock(SeqTrackService) {
            0 * decideAndPrepareForAlignment(!null) >> []
            1 * determineAndStoreIfFastqFilesHaveToBeLinked(!null, false)
        }
        service.seqPlatformService = Mock(SeqPlatformService) {
            1 * findSeqPlatform(seqPlatform.name, seqPlatform.seqPlatformModelLabel.name, null) >> seqPlatform
            0 * _
        }
        service.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias(seqType.name, [
                    libraryLayout: seqType.libraryLayout,
                    singleCell   : seqType.singleCell,
            ]) >> seqType
            0 * _
        }
        service.speciesWithStrainService = Mock(SpeciesWithStrainService) {
            getByAlias(human) >> humanSpecies
            getByAlias(mouse) >> mouseSpecies
        }
        mockAdditionalServices(service)

        File file = new File(new File(TestCase.uniqueNonExistentPath, runName), 'metadata.tsv')
        DirectoryStructure directoryStructure = Mock(DirectoryStructure) {
            _ * getRequiredColumnTitles() >> [FASTQ_FILE.name()]
            _ * getDataFilePath(_, _) >> { MetadataValidationContext context, Row row ->
                return Paths.get(file.parent, row.getCellByColumnTitle(FASTQ_FILE.name()).text)
            }
        }

        Map baseData = [
                (RUN_ID)              : runName,
                (CENTER_NAME)         : seqCenter,
                (INSTRUMENT_PLATFORM) : seqPlatform.name,
                (INSTRUMENT_MODEL)    : seqPlatform.seqPlatformModelLabel.name,
                (RUN_DATE)            : dateString,
                (LANE_NO)             : '1',
                (SEQUENCING_TYPE)     : seqType.name,
                (SEQUENCING_READ_TYPE): seqType.libraryLayout,
                (SAMPLE_NAME)         : sampleIdentifier.name,
                (FASTQ_GENERATOR)     : softwareToolIdentifier.name,
                (SPECIES)             : human + '+' + mouse,
        ].asImmutable()

        Map fastqData1 = [
                (FASTQ_FILE): fastq1,
                (MD5)       : md5sum1,
                (READ)      : '1',
        ] + baseData

        Map fastqData2 = [
                (FASTQ_FILE): fastq2,
                (MD5)       : md5sum2,
                (READ)      : '2',
        ] + baseData

        Map fastqIndexData1 = [
                (FASTQ_FILE): fastqIndex1,
                (MD5)       : md5sumIndex1,
                (READ)      : 'i1',
        ] + baseData

        Map fastqIndexData2 = [
                (FASTQ_FILE): fastqIndex2,
                (MD5)       : md5sumIndex2,
                (READ)      : 'i2',
        ] + baseData

        List<String> keys = fastqData1.keySet().toList()
        StringBuilder builder = new StringBuilder()
        builder << keys.join('\t') << '\n'
        [
                fastqData1,
                fastqData2,
                fastqIndexData1,
                fastqIndexData2,
        ].each { Map map ->
            builder << keys.collect {
                map[it]
            }.join('\t') << '\n'
        }

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                builder.toString(),
                [
                        metadataFile      : file.toPath(),
                        directoryStructure: directoryStructure,
                ]
        )

        when:
        MetaDataFile result = service.importMetadataFile(context, false, FastqImportInstance.ImportMode.MANUAL, null, null, false)

        then:
        // runs
        Run.count == 1
        Run run = CollectionUtils.atMostOneElement(Run.findAllWhere(
                name: runName,
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
        ))
        run != null
        run.dateExecuted == date

        // fastqImportInstance
        FastqImportInstance.count == 1
        FastqImportInstance fastqImportInstance = CollectionUtils.atMostOneElement(FastqImportInstance.findAllWhere(
                otrsTicket: null,
                importMode: FastqImportInstance.ImportMode.MANUAL
        ))
        fastqImportInstance != null

        // metadataFile
        MetaDataFile.count == 1
        MetaDataFile metadataFile = CollectionUtils.atMostOneElement(MetaDataFile.findAllWhere(
                fileName: file.name,
                filePath: file.parent,
                md5sum: context.metadataFileMd5sum,
                fastqImportInstance: fastqImportInstance,
        ))
        result == metadataFile

        SeqTrack.count == 1
        DataFile.count == 4
        MetaDataKey.count == context.spreadsheet.header.cells.size()
        MetaDataEntry.count == context.spreadsheet.header.cells.size() * DataFile.count

        // seqTrack1
        SeqTrack seqTrack = CollectionUtils.atMostOneElement(SeqTrack.findAllWhere(
                laneId: '1',
                insertSize: 0,
                run: run,
                sample: sampleIdentifier.sample,
                seqType: seqType,
                pipelineVersion: softwareToolIdentifier.softwareTool,
                kitInfoReliability: InformationReliability.UNKNOWN_UNVERIFIED,
                libraryPreparationKit: null,
        ))
        seqTrack.ilseId == null
        seqTrack.class == SeqTrack
        seqTrack.individual.species == humanSpecies
        seqTrack.sample.mixedInSpecies.size() == 1
        seqTrack.sample.mixedInSpecies.first() == mouseSpecies

        Map commonRunDataFileProperties = [
                pathName           : '',
                used               : true,
                fastqImportInstance: fastqImportInstance,
                fileType           : fileType,
                dateExecuted       : date,
                run                : run,
                project            : sampleIdentifier.project,
                seqTrack           : seqTrack,
        ]

        DataFile dataFile1 = CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRunDataFileProperties + [
                fileName   : fastq1,
                vbpFileName: fastq1,
                md5sum     : md5sum1,
                mateNumber : 1,
                indexFile  : false,
        ]))
        dataFile1 != null

        DataFile dataFile2 = CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRunDataFileProperties + [
                fileName   : fastq2,
                vbpFileName: fastq2,
                md5sum     : md5sum2,
                mateNumber : 2,
                indexFile  : false,
        ]))
        dataFile2 != null

        DataFile dataFileIndex1 = CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRunDataFileProperties + [
                fileName   : fastqIndex1,
                vbpFileName: fastqIndex1,
                md5sum     : md5sumIndex1,
                mateNumber : 1,
                indexFile  : true,
        ]))
        dataFileIndex1 != null

        DataFile dataFileIndex2 = CollectionUtils.atMostOneElement(DataFile.findAllWhere(commonRunDataFileProperties + [
                fileName   : fastqIndex2,
                vbpFileName: fastqIndex2,
                md5sum     : md5sumIndex2,
                mateNumber : 2,
                indexFile  : true,
        ]))
        dataFileIndex2 != null
    }

    void "extractBarcode, when both INDEX and FASTQ_FILE columns are missing, returns null"() {
        given:
        Row row = MetadataValidationContextFactory.createContext().spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result == null
    }

    void "extractBarcode, when INDEX column is missing and filename contains no barcode, returns null extracted from FASTQ_FILE cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\nfile.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == null
        result.cells == [row.getCellByColumnTitle(FASTQ_FILE.name())] as Set
    }

    void "extractBarcode, when INDEX column is missing and filename contains barcode, returns barcode extracted from FASTQ_FILE cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE.name()}\nfile_ACGTACGT_.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(FASTQ_FILE.name())] as Set
    }

    void "extractBarcode, when no entry in INDEX column and FASTQ_FILE column is missing, returns null extracted from INDEX cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("SOME_COLUMN\t${INDEX}\nsome_value").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == null
        result.cells == [row.getCellByColumnTitle(INDEX.name())] as Set
    }

    void "extractBarcode, when no entry in INDEX column and filename contains no barcode, returns null extracted from INDEX cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${INDEX}\nfile.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == null
        result.cells == [row.getCellByColumnTitle(INDEX.name())] as Set
    }

    void "extractBarcode, when no entry in INDEX column and filename contains barcode, returns null extracted from INDEX cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${INDEX}\nfile_ACGTACGT_.fastq.gz").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == null
        result.cells == [row.getCellByColumnTitle(INDEX.name())] as Set
    }

    void "extractBarcode, when INDEX column contains entry and FASTQ_FILE column is missing, returns barcode extracted from INDEX cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${INDEX}\nACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(INDEX.name())] as Set
    }

    void "extractBarcode, when INDEX column contains entry with a comma and FASTQ_FILE column is missing, returns barcode extracted from INDEX cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${INDEX}\nACGTACGT,ACGTACGT,ACGTACGT,ACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT-ACGTACGT-ACGTACGT-ACGTACGT'
        result.cells == [row.getCellByColumnTitle(INDEX.name())] as Set
    }

    void "extractBarcode, when INDEX column contains entry and filename contains no barcode, returns barcode extracted from INDEX cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${INDEX}\nfile.fastq.gz\tACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(INDEX.name())] as Set
    }

    void "extractBarcode, when INDEX column contains entry and filename contains same barcode, returns barcode extracted from INDEX cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${INDEX}\nfile_ACGTACGT_.fastq.gz\tACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(INDEX.name())] as Set
    }

    void "extractBarcode, when INDEX column contains entry and filename contains barcode which is substring, returns barcode extracted from INDEX cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${INDEX}\nfile_CGTACG_.fastq.gz\tACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(INDEX.name())] as Set
    }

    void "extractBarcode, when INDEX column contains entry and filename contains different barcode, returns barcode extracted from INDEX cell"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\t${INDEX}\nfile_TGCATGCA_.fastq.gz\tACGTACGT").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractBarcode(row)

        then:
        result.value == 'ACGTACGT'
        result.cells == [row.getCellByColumnTitle(INDEX.name())] as Set
    }

    void "extractMateNumber, when READ columns is missing, returns null"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("something\nsomething").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result == null
    }

    @Unroll
    void "extractMateNumber, when READ value is '#value', then return ExtractedValue with that value"() {
        given:
        Row row = MetadataValidationContextFactory.createContext("${READ}\tsomething\n${value}\tsomething").spreadsheet.dataRows[0]

        when:
        ExtractedValue result = MetadataImportService.extractMateNumber(row)

        then:
        result.value == value

        where:
        value << [
                '',
                '1',
                '2',
                'i1',
                'i2',
                'something',
        ]
    }

    void "copyMetaDataFileIfRequested, if data not on midterm, do nothing"() {
        given:
        DomainFactory.createDefaultRealmWithProcessingOption()

        MetadataImportService service = new MetadataImportService(
                lsdfFilesService: Mock(LsdfFilesService) {
                    0 * _
                }
        )
        service.configService = new TestConfigService()
        service.configService.processingOptionService = new ProcessingOptionService()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        expect:
        service.copyMetadataFileIfRequested(context)
    }

    private Map setupForCopyMetaDataFileIfRequested(String contextContent, boolean returnValidPath = true) {
        String ilseId = '1234'
        String fileName = 'metadataFile.tsv'
        SeqCenter seqCenter = createSeqCenter(copyMetadataFile: true)
        String content = """\
                ${ILSE_NO.name()}\t${CENTER_NAME.name()}
                ${ilseId}\t${seqCenter.name}
                """.stripIndent()
        Path file = temporaryFolder.newFile(fileName).toPath()
        file.text = content
        Path targetDirectory = temporaryFolder.newFolder('target').toPath()
        ProcessingOptionService processingOptionService = new ProcessingOptionService()
        DomainFactory.createDefaultRealmWithProcessingOption()
        MetadataImportService service = Spy(MetadataImportService) {
            1 * getIlseFolder(_, _) >> (returnValidPath ? targetDirectory : null)
        }
        service.configService = new ConfigService()
        service.fileService = new FileService()
        service.fileSystemService = new TestFileSystemService()
        service.configService.processingOptionService = processingOptionService
        service.fileService.configService = service.configService

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                content,
                [
                        directoryStructure: new DataFilesInGpcfSpecificStructure(),
                        content           : (contextContent ?: content).bytes,
                        metadataFile      : file,
                ]
        )
        return [
                context        : context,
                service        : service,
                targetDirectory: targetDirectory,
                fileName       : fileName,
        ]
    }

    void "copyMetaDataFileIfRequested, if metadata does not exist and copying failed, should fail"() {
        given:
        Map data = setupForCopyMetaDataFileIfRequested('something')

        data.service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(_, _)
        }
        data.service.fileService = Mock(FileService) {
            1 * createFileWithContentOnDefaultRealm(_, _)
        }

        when:
        data.service.copyMetadataFileIfRequested(data.context)

        then:
        RuntimeException e = thrown()
        e.message.contains('Copying of metadata file')
        e.cause.message.contains('target/metadataFile')
    }

    void "copyMetaDataFileIfRequested, if metadata does not exist and copying fine, all fine"() {
        given:
        Map data = setupForCopyMetaDataFileIfRequested(null)

        when:
        data.service.copyMetadataFileIfRequested(data.context)

        then:
        Files.exists(data.targetDirectory)
        List<Path> paths = Files.list(data.targetDirectory).collect(Collectors.toList())
        paths.size() == 1
        Files.size(paths[0])
        paths[0][-1].toString() ==~ /metadataFile-.*\.tsv/
    }

    void "copyMetaDataFileIfRequested, if metadata does not exist and IlseNumber is wrong, should fail"() {
        given:
        Map data = setupForCopyMetaDataFileIfRequested(null, false)

        data.service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(_, _)
        }

        when:
        data.service.copyMetadataFileIfRequested(data.context)

        then:
        RuntimeException e = thrown()
        e.message.contains('Copying of metadata file')
        e.cause.message.contains('Cannot invoke method resolve() on null object')
    }

    void "copyMetaDataFileIfRequested, if metadata is copied, check the permission string must be 2770"() {
        given:
        Map data = setupForCopyMetaDataFileIfRequested(null)

        data.service.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String command ->
                assert command.contains("chmod 2770")
                return new ProcessOutput(command, '', 0)
            }
        }

        //remove the folder where the metadata should be copied (part of the setup)
        //if the directory exists, nothing should be done. Permission won't be changed
        Path targetDirectory = data.targetDirectory
        if (Files.exists(targetDirectory)) {
            targetDirectory.deleteDir()
        }

        when:
        data.service.copyMetadataFileIfRequested(data.context)

        then:
        Files.exists(targetDirectory)
    }

    void "test getIlseFolder, invalid input, should return null"() {
        given:
        SeqCenter seqCenter = createSeqCenter()
        Path output

        when:
        output = new MetadataImportService().getIlseFolder(input, seqCenter)

        then:
        output == null

        where:
        input << [null, '', '1234567', '12a3']
    }

    void "test getIlseFolder, valid input"() {
        given:
        SeqCenter seqCenter = createSeqCenter(dirName: "SEQ_FACILITY")
        MetadataImportService service = new MetadataImportService()
        service.configService = new TestConfigService((OtpProperty.PATH_SEQ_CENTER_INBOX): "/test")
        service.configService.metaClass.getDefaultRealm = {
            return null
        }
        service.fileSystemService = new TestFileSystemService()

        expect:
        output == service.getIlseFolder(input, seqCenter)

        where:
        input    || output
        '123'    || FileSystems.default.getPath("/test/SEQ_FACILITY/000/000123")
        '1234'   || FileSystems.default.getPath("/test/SEQ_FACILITY/001/001234")
        '123456' || FileSystems.default.getPath("/test/SEQ_FACILITY/123/123456")
    }

    void "test getConfiguredSeqTracks"() {
        given:
        MetadataImportService service = new MetadataImportService()
        SeqTrack seqTrackWithWorkflowConfig = createSeqTrack()
        SeqTrack seqTrackWithoutWorkflowConfig = createSeqTrack()

        DomainFactory.createRoddyWorkflowConfig(
                project: seqTrackWithWorkflowConfig.project,
                seqType: seqTrackWithWorkflowConfig.seqType,
        )

        expect:
        [seqTrackWithWorkflowConfig] == service.getSeqTracksWithConfiguredAlignment([seqTrackWithWorkflowConfig, seqTrackWithoutWorkflowConfig])
    }

    class DataForGetOrCreateRun implements DomainFactoryCore {
        Run run = createRun()
        SeqCenter seqCenter = run.seqCenter
        SeqPlatform seqPlatform = run.seqPlatform
        Date dateExecuted = run.dateExecuted
        String dateExecutedString = TimeFormats.DATE.getFormattedDate(dateExecuted)

        MetadataImportService service = new MetadataImportService([
                seqPlatformService: new SeqPlatformService([
                        seqPlatformModelLabelService: new SeqPlatformModelLabelService(),
                        sequencingKitLabelService   : new SequencingKitLabelService(),
                ]),
        ])

        String fileContent

        List<Row> rows

        DataForGetOrCreateRun(Map parameterMap = [:]) {
            Map map = [
                    (RUN_ID)             : run.name,
                    (RUN_DATE)           : dateExecutedString,
                    (CENTER_NAME)        : seqCenter.name,
                    (INSTRUMENT_PLATFORM): seqPlatform.name,
                    (INSTRUMENT_MODEL)   : seqPlatform.seqPlatformModelLabel,
                    (SEQUENCING_KIT)     : seqPlatform.sequencingKitLabel ?: '',
            ] + parameterMap

            fileContent = map.collect { key, value ->
                [
                        key,
                        value,
                ]
            }.transpose()*.join('\t').join('\n')

            rows = MetadataValidationContextFactory.createContext(fileContent).spreadsheet.dataRows
        }
    }

    void "getOrCreateRun, when not exist, create it"() {
        given:
        String runName = 'newRun'

        DataForGetOrCreateRun data = new DataForGetOrCreateRun([
                (RUN_ID): runName
        ])

        when:
        Run run = data.service.getOrCreateRun(runName, data.rows)

        then:
        run
        run.name == runName
        run.seqCenter == data.seqCenter
        run.seqPlatform == data.seqPlatform
        run.dateExecuted == data.dateExecuted
    }

    void "getOrCreateRun, when run already exist and is correct,  then return it"() {
        given:
        DataForGetOrCreateRun data = new DataForGetOrCreateRun()

        when:
        Run run = data.service.getOrCreateRun(data.run.name, data.rows)

        then:
        run
        run.id == data.run.id
    }

    void "getOrCreateRun, when run already exist but is connected to another center,  then throw exception"() {
        given:
        SeqCenter seqCenter = createSeqCenter()
        DataForGetOrCreateRun data = new DataForGetOrCreateRun([
                (CENTER_NAME): seqCenter.name,
        ])

        when:
        data.service.getOrCreateRun(data.run.name, data.rows)

        then:
        AssertionError e = thrown()
        e.message.contains(seqCenter.name)
        e.message.contains(data.seqCenter.name)
    }

    void "getOrCreateRun, when run already exist but is connected to another seqPlatform,  then throw exception"() {
        given:
        SeqPlatform seqPlatform = createSeqPlatform()
        DataForGetOrCreateRun data = new DataForGetOrCreateRun([
                (INSTRUMENT_PLATFORM): seqPlatform.name,
                (INSTRUMENT_MODEL)   : seqPlatform.seqPlatformModelLabel,
                (SEQUENCING_KIT)     : seqPlatform.sequencingKitLabel ?: '',
        ])

        when:
        data.service.getOrCreateRun(data.run.name, data.rows)

        then:
        AssertionError e = thrown()
        e.message.contains(seqPlatform.name)
        e.message.contains(data.seqPlatform.name)
    }

    void "getOrCreateRun, when run already exist but has an date and the sheet has none,  then throw exception"() {
        given:
        String dateExecutedString = ''
        DataForGetOrCreateRun data = new DataForGetOrCreateRun([
                (RUN_DATE): dateExecutedString,
        ])

        when:
        data.service.getOrCreateRun(data.run.name, data.rows)

        then:
        AssertionError e = thrown()
        e.message.contains('(null)')
        e.message.contains(data.dateExecuted.toString())
    }

    void "getOrCreateRun, when run already exist but has no date and the sheet has one,  then throw exception"() {
        given:
        DataForGetOrCreateRun data = new DataForGetOrCreateRun()
        data.run.dateExecuted = null
        data.run.save(flush: true)

        when:
        data.service.getOrCreateRun(data.run.name, data.rows)

        then:
        AssertionError e = thrown()
        e.message.contains('(null)')
        e.message.contains(data.dateExecuted.toString())
    }

    void "getOrCreateRun, when run already exist without date and is correct,  then return it"() {
        given:
        DataForGetOrCreateRun data = new DataForGetOrCreateRun()

        when:
        Run run = data.service.getOrCreateRun(data.run.name, data.rows)

        then:
        run
        run.id == data.run.id
    }

    private void mockAdditionalServices(MetadataImportService service, int count = 1) {
        service.samplePairDeciderService = Mock(SamplePairDeciderService) {
            count * findOrCreateSamplePairs([]) >> []
            0 * _
        }
        service.mergingCriteriaService = Mock(MergingCriteriaService) {
            count * createDefaultMergingCriteria(_, _)
        }
        service.dataInstallationInitializationService = Mock(DataInstallationInitializationService) {
            _ * createWorkflowRuns(_) >> []
        }
        service.allDecider = Mock(AllDecider) {
            _ * decide(_, _) >> []
        }
        service.processingThresholdsService = Mock(ProcessingThresholdsService)
    }
}
