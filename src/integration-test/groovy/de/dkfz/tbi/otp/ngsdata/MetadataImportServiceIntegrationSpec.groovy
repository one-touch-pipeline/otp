/*
 * Copyright 2011-2024 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.web.mapping.LinkGenerator
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholdsService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesInSameDirectory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.Md5sumFormatValidator
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.utils.TimeFormats
import de.dkfz.tbi.otp.utils.TimeUtils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.BASE_MATERIAL
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CENTER_NAME
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_GENERATOR
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.INDEX
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.INSTRUMENT_MODEL
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.INSTRUMENT_PLATFORM
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.LANE_NO
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.LIB_PREP_KIT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.MD5
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.READ
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_DATE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_KIT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_READ_TYPE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_TYPE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SINGLE_CELL_WELL_LABEL
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SPECIES

@Rollback
@Integration
class MetadataImportServiceIntegrationSpec extends Specification implements DomainFactoryCore, CellRangerFactory, WorkflowSystemDomainFactory, TaxonomyFactory, UserAndRoles {

    @Autowired
    MetadataImportService metadataImportService

    @Autowired
    TestConfigService testConfigService
    RemoteShellHelper remoteShellHelper

    @TempDir
    Path tempDir

    final static String TICKET_NUMBER = "2000010112345678"

    void 'getMetadataValidators returns MetadataValidators'() {
        expect:
        metadataImportService.metadataValidators.find { it instanceof Md5sumFormatValidator }
    }

    void 'getDirectoryStructure, when called with bean name, returns bean'() {
        expect:
        metadataImportService.getDirectoryStructure(DirectoryStructureBeanName.SAME_DIRECTORY) instanceof DataFilesInSameDirectory
    }

    void "test notifyAboutUnsetConfig"() {
        given:
        MetadataImportService service = Spy(MetadataImportService) {
            getSeqTracksWithConfiguredAlignment(_) >> null
        }
        Ticket ticket = createTicket()
        SeqTrack st1 = createSeqTrack()
        SeqTrack st2 = createSeqTrack()
        ProcessingThresholds p1 = DomainFactory.createProcessingThresholds()
        ProcessingThresholds p2 = DomainFactory.createProcessingThresholds()

        service.sampleTypeService = Mock(SampleTypeService) {
            getSeqTracksWithoutSampleCategory(_) >> [st1]
        }
        service.processingThresholdsService = Mock(ProcessingThresholdsService) {
            getSeqTracksWithoutProcessingThreshold(_) >> [st2]
        }
        service.linkGenerator = Mock(LinkGenerator) {
            2 * link(_) >> "link1" >> "link2"
        }
        service.mailHelperService = Mock(MailHelperService) {
            1 * saveErrorMailInNewTransaction(_, _) >> { String subject, String body ->
                assert subject.contains("threshold")
                assert subject.contains("category")
                assert body.contains("link1")
                assert body.contains("link2")
                assert body.contains(st1.project.displayName)
                assert body.contains(p1.project.displayName)
                assert body.contains(p1.sampleType.displayName)
                assert body.contains(p1.seqType.displayName)
                assert body.contains(p2.project.displayName)
                assert body.contains(p2.sampleType.displayName)
                assert body.contains(p2.seqType.displayName)
            }
        }
        service.ticketService = Mock(TicketService) {
            1 * getPrefixedTicketNumber(_) >> "TICKET_PREFIX"
        }

        expect:
        service.notifyAboutUnsetConfig([st1, st2], [p1, p2], ticket)
    }

    void 'validateAndImportResults, when project data is given, then validate and import data'() {
        given:
        String runName = 'run'
        Date date = TimeUtils.toDate(LocalDate.of(2000, 1, 1))
        String dateString = TimeFormats.DATE.getFormattedDate(date)
        String fastq11 = "1_R1.fastq.gz"
        String fastq12 = "1_R2.fastq.gz"

        String md5sum11 = HelperUtils.randomMd5sum
        String md5sum12 = HelperUtils.randomMd5sum

        DomainFactory.createAllAnalysableSeqTypes()
        DomainFactory.createRoddyAlignableSeqTypes()

        SeqType singleCellSeqType = DomainFactory.createCellRangerAlignableSeqTypes().first()
        SeqCenter seqCenter = createSeqCenter(autoImportable: true, autoImportDir: tempDir)

        SeqPlatform seqPlatform = createSeqPlatform()
        seqPlatform.sequencingKitLabel = createSequencingKitLabel()

        createLibraryPreparationKit(name: "kit1")
        String speciesImportAlias = "SpeciesImportAlias"
        SpeciesWithStrain speciesWithStrain = createSpeciesWithStrain([importAlias: [speciesImportAlias] as Set])

        Project project = createProject([speciesWithStrains: [speciesWithStrain] as Set])

        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier()
        sampleIdentifier.individual.project = project
        sampleIdentifier.individual.save(flush: true)

        createWorkflow(name: WgbsWorkflow.WORKFLOW)
        DomainFactory.createPanCanPipeline()
        Pipeline cellRanger = findOrCreatePipeline()
        createCellRangerConfig(project: project, seqType: singleCellSeqType, pipeline: cellRanger)

        SoftwareToolIdentifier softwareToolIdentifier = createSoftwareToolIdentifier([
                softwareTool: createSoftwareTool([
                        type: SoftwareTool.Type.BASECALLING,
                ]),
        ])
        createFileType(
                type: FileType.Type.SEQUENCE,
                signature: '_',
        )

        String metadata1 = """
${FASTQ_FILE}                   ${fastq11}                                  ${fastq12}
${MD5}                          ${md5sum11}                                 ${md5sum12}
${RUN_ID}                       ${runName}                                  ${runName}
${CENTER_NAME}                  ${seqCenter}                                ${seqCenter}
${INSTRUMENT_PLATFORM}          ${seqPlatform.name}                         ${seqPlatform.name}
${INSTRUMENT_MODEL}             ${seqPlatform.seqPlatformModelLabel.name}   ${seqPlatform.seqPlatformModelLabel.name}
${RUN_DATE}                     ${dateString}                               ${dateString}
${LANE_NO}                      1                                           1
${SEQUENCING_TYPE}              ${singleCellSeqType.name}                   ${singleCellSeqType.name}
${SEQUENCING_READ_TYPE}         ${singleCellSeqType.libraryLayout}          ${singleCellSeqType.libraryLayout}
${READ}                         1                                           2
${SAMPLE_NAME}                  ${sampleIdentifier.name}                   ${sampleIdentifier.name}
${FASTQ_GENERATOR}              ${softwareToolIdentifier.name}              ${softwareToolIdentifier.name}
${SPECIES}                      ${speciesImportAlias}                       ${speciesImportAlias}
${BASE_MATERIAL}                ${SeqType.SINGLE_CELL_DNA}                  ${SeqType.SINGLE_CELL_DNA}
${PROJECT}                      ${project.name}                             ${project.name}
${INDEX}                        A-C                                         A-C
${LIB_PREP_KIT}                 kit1                                        kit1
${SEQUENCING_KIT}               ${seqPlatform.sequencingKitLabel}           ${seqPlatform.sequencingKitLabel}
${SINGLE_CELL_WELL_LABEL}       Test                                        Test
"""

        List<List<String>> lines = metadata1.readLines().findAll()*.split(/ {2,}/).transpose()
        String content = lines.collect { it*.replaceFirst(/^-$/, '').join('\t') }.join('\n')
        Path directory = Files.createDirectories(Files.createDirectories(tempDir.resolve("001111")).resolve("data"))
        Path file = CreateFileHelper.createFile(directory.resolve("1111_meta.tsv"), content)
        Path fastq = Files.createDirectories(Files.createDirectories(Files.createDirectories(directory.resolve("run")).resolve("1")).resolve("fastq"))
        CreateFileHelper.createFile(fastq.resolve("1_R1.fastq.gz"))
        CreateFileHelper.createFile(fastq.resolve("1_R2.fastq.gz"))

        MetadataValidationContextFactory.createContext(
                [metadataFile: Paths.get("${seqCenter.autoImportDir}/1111_meta.tsv")])
        remoteShellHelper = metadataImportService.fileService.remoteShellHelper
        testConfigService.addOtpProperty(OtpProperty.PATH_METADATA_STORAGE, tempDir.resolve("metadata").toString())
        metadataImportService.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
        createUserAndRoles()

        when:
        List<ValidateAndImportResult> validateAndImportResults = doWithAuth(OPERATOR) {
            metadataImportService.validateAndImportMultiple(TICKET_NUMBER, '1111', false)
        }

        then:
        validateAndImportResults.first().context.metadataFile == file
        CollectionUtils.exactlyOneElement(SeqTrack.findAllBySeqType(singleCellSeqType))
        Ticket ticket = Ticket.findByTicketNumber(TICKET_NUMBER)
        FastqImportInstance fastqImportInstance = FastqImportInstance.findByTicket(ticket)
        Set<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAllByFastqImportInstance(fastqImportInstance)
        rawSequenceFiles.size() == 2
        rawSequenceFiles.each {
            MetaDataEntry.findAllBySequenceFile(it).size() == lines.size()
        }
    }

    void cleanup() {
        if (remoteShellHelper) {
            metadataImportService.fileService.remoteShellHelper = remoteShellHelper
            testConfigService.clean()
        }
    }
}
