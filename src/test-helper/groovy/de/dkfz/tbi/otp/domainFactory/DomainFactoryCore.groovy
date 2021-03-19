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
package de.dkfz.tbi.otp.domainFactory

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.time.LocalDate
import java.time.ZoneId

trait DomainFactoryCore implements DomainFactoryHelper {

    private static final int DEFAULT_PORT = -1

    private static final int DEFAULT_TIMEOUT = -1

    ProcessingPriority createProcessingPriority(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ProcessingPriority, [
                name                       : "name_${nextId}",
                priority                   : nextId,
                errorMailPrefix            : "errorMailPrefix_${nextId}",
                queue                      : "queue_${nextId}",
                roddyConfigSuffix          : "roddyConfigSuffix${nextId}",
                allowedParallelWorkflowRuns: 1,
        ], properties, saveAndValidate)
    }

    Realm createRealm(Map realmProperties = [:]) {
        return createDomainObject(Realm, [
                name                       : "realmName_${nextId}",
                jobScheduler               : Realm.JobScheduler.PBS,
                host                       : "test.host.invalid${nextId}",
                port                       : DEFAULT_PORT,
                timeout                    : DEFAULT_TIMEOUT,
                defaultJobSubmissionOptions: '',
        ], realmProperties)
    }

    Project createProject(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Project, [
                name                          : "project_${nextId}",
                individualPrefix              : "individualPrefix_${nextId}",
                dirName                       : "projectDirName_${nextId}",
                projectType                   : Project.ProjectType.SEQUENCING,
                storageUntil                  : LocalDate.now(),
                realm                         : { createRealm() },
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling           : QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK,
                unixGroup                     : "unixGroup_${nextId}",
                processingPriority            : { createProcessingPriority() },
        ], properties, saveAndValidate)
    }

    Individual createIndividual(Map properties = [:]) {
        return createDomainObject(Individual, [
                pid         : "pid_${nextId}",
                mockPid     : "mockPid_${nextId}",
                mockFullName: "mockFullName_${nextId}",
                type        : Individual.Type.REAL,
                project     : { createProject() },
        ], properties)
    }

    SampleType createSampleType(Map properties = [:]) {
        return createDomainObject(SampleType, [
                name                   : "sample-type-name-${nextId}",
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ], properties)
    }

    Sample createSample(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Sample, [
                individual: { createIndividual() },
                sampleType: { createSampleType() },
        ], properties, saveAndValidate)
    }

    SampleIdentifier createSampleIdentifier(Map properties = [:]) {
        return createDomainObject(SampleIdentifier, [
                sample: { createSample() },
                name  : "sampleIdentifierName_${nextId}",
        ], properties)
    }

    SeqType createSeqType(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(SeqType, getCustomSeqTypeProperties(properties), properties, saveAndValidate)
    }

    private Map getCustomSeqTypeProperties(Map properties = [:]) {
        String defaultName = "seqTypeName_${nextId}"
        return [
                name         : defaultName,
                displayName  : properties.displayName ?: properties.name ?: defaultName,
                dirName      : "seqTypeDirName_${nextId}",
                roddyName    : null,
                libraryLayout: LibraryLayout.SINGLE,
                singleCell   : false,
                importAlias  : [],
        ]
    }

    LibraryPreparationKit createLibraryPreparationKit(Map properties = [:]) {
        return createDomainObject(LibraryPreparationKit, [
                name            : "library-preperation-kit-name_${nextId}",
                shortDisplayName: "library-preperation-kit-short-name_${nextId}",
                importAlias     : [],
        ], properties)
    }

    SoftwareTool createSoftwareTool(Map properties = [:]) {
        return createDomainObject(SoftwareTool, [
                programName   : "programName_${nextId}",
                programVersion: "programVersion_${nextId}",
                type          : SoftwareTool.Type.BASECALLING,
        ], properties)
    }

    SoftwareToolIdentifier createSoftwareToolIdentifier(Map properties = [:]) {
        return createDomainObject(SoftwareToolIdentifier, [
                name        : "softwareToolIdentifier_${nextId}",
                softwareTool: { createSoftwareTool() },
        ], properties)
    }

    SeqCenter createSeqCenter(Map properties = [:]) {
        return createDomainObject(SeqCenter, [
                name   : "seqCenterName_${nextId}",
                dirName: "seqCenterDirName_${nextId}",
        ], properties)
    }

    AntibodyTarget createAntibodyTarget(Map properties = [:]) {
        return createDomainObject(AntibodyTarget, [
                name       : "antibodyTargetName_${nextId}",
                importAlias: [],
        ], properties)
    }

    Run createRun(Map properties = [:]) {
        return createDomainObject(Run, [
                name        : "runName_${nextId}",
                seqCenter   : { createSeqCenter() },
                seqPlatform : { createSeqPlatformWithSeqPlatformGroup() },
                dateExecuted: { Date.from(LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()) },
        ], properties)
    }

    FastqImportInstance createFastqImportInstance(Map properties = [:]) {
        FastqImportInstance fastqImportInstance = createDomainObject(FastqImportInstance, [
                importMode: FastqImportInstance.ImportMode.AUTOMATIC,
        ], properties)
        properties.dataFiles.each { DataFile dataFile ->
            fastqImportInstance.addToDataFiles(dataFile)
        }
        fastqImportInstance.save(flush: true)
        return fastqImportInstance
    }

    FileType createFileType(Map properties = [:]) {
        return createDomainObject(FileType, [
                type   : FileType.Type.SEQUENCE,
                vbpPath: "sequence_${nextId}",
        ], properties)
    }

    DataFile createDataFile(Map properties = [:], boolean saveAndValidate = true) {
        SeqTrack seqTrack
        if (properties.containsKey('seqTrack')) {
            seqTrack = properties.seqTrack
        } else {
            seqTrack = createSeqTrack(properties.containsKey('run') ? [run: properties.run] : [:])
        }
        return createDomainObject(DataFile, [
                seqTrack           : seqTrack,
                project            : seqTrack?.project,
                run                : seqTrack?.run,
                fastqImportInstance: { createFastqImportInstance() },
                fileName           : "DataFileFileName_${nextId}_R1.gz",
                vbpFileName        : "VbpDataFileFileName_${nextId}_R1.gz",
                pathName           : "",
                initialDirectory   : TestCase.uniqueNonExistentPath.path,
                md5sum             : { HelperUtils.randomMd5sum },
                dateExecuted       : new Date(),
                dateFileSystem     : new Date(),
                dateCreated        : new Date(),
                dateLastChecked    : new Date(),
                fileWithdrawn      : false,
                fileType           : { createFileType() },
                used               : true,
                fileExists         : true,
                fileLinked         : true,
                fileSize           : nextId,
                mateNumber         : 1,
                indexFile          : false,
        ], properties, saveAndValidate)
    }

    DataFile createSequenceDataFile(final Map properties = [:]) {
        Map defaultProperties = [
                used          : true,
                sequenceLength: 100,
        ]
        if (properties.seqTrack) {
            defaultProperties.project = properties.seqTrack.project
            defaultProperties.run = properties.seqTrack.run
        }
        return createDataFile(defaultProperties + properties)
    }

    SeqTrack createSeqTrack(Map properties = [:]) {
        return createDomainObject(SeqTrack, getSeqTrackProperties(properties) + [
                seqType: { createSeqType() },
        ], properties)
    }

    SeqTrack createExomeSeqTrack(Map properties = [:]) {
        return createSeqTrack(properties + (properties.seqType ? [:] : [seqType: DomainFactory.createExomeSeqType()]))
    }

    SeqTrack createChipSeqSeqTrack(Map properties = [:]) {
        return createSeqTrack(properties + (properties.seqType ? [:] : [seqType: DomainFactory.createChipSeqType()]) +
                (properties.antibodyTarget ? [:] : [antibodyTarget: createAntibodyTarget()]))
    }

    SeqTrack createSeqTrackWithOneDataFile(Map seqTrackProperties = [:], Map dataFileProperties = [:]) {
        SeqTrack seqTrack = createSeqTrack(seqTrackProperties)
        createSequenceDataFile(dataFileProperties + [seqTrack: seqTrack])
        return seqTrack
    }

    SeqTrack createSeqTrackWithTwoDataFile(Map seqTrackProperties = [:], Map dataFile1Properties = [:], Map dataFile2Properties = [:]) {
        SeqTrack seqTrack = createSeqTrack(seqTrackProperties)
        createSequenceDataFile(dataFile1Properties + [seqTrack: seqTrack])
        createSequenceDataFile(dataFile2Properties + [seqTrack: seqTrack])
        return seqTrack
    }

    private Map getSeqTrackProperties(Map properties = [:]) {
        return [
                laneId               : "laneId_${nextId}",
                sampleIdentifier     : "sampleIdentifier_${nextId}",
                sample               : { createSample() },
                pipelineVersion      : { createSoftwareTool() },
                run                  : { createRun() },
                kitInfoReliability   : { properties.libraryPreparationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED },
                normalizedLibraryName: { SeqTrack.normalizeLibraryName(properties.libraryName) },
        ]
    }

    ReferenceGenome createReferenceGenome(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ReferenceGenome, [
                name                        : "referencegenome_name${nextId}",
                path                        : HelperUtils.uniqueString,
                fileNamePrefix              : "referencegenome-prefix_${nextId}",
                length                      : 1,
                lengthWithoutN              : 1,
                lengthRefChromosomes        : 1,
                lengthRefChromosomesWithoutN: 1,
                chromosomePrefix            : "",
                chromosomeSuffix            : "",
        ], properties, saveAndValidate)
    }

    ToolName createToolName(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ToolName, [
                name: "GENOME_STAR_INDEX_${nextId}",
                type: ToolName.Type.RNA,
                path: "path_${nextId}",
        ], properties, saveAndValidate)
    }

    ReferenceGenomeIndex createReferenceGenomeIndex(Map properties = [:], boolean saveAndValidate = true) {
        ReferenceGenome referenceGenome = properties.referenceGenome ?: createReferenceGenome()
        ReferenceGenomeIndex referenceGenomeIndex = createDomainObject(ReferenceGenomeIndex, [
                referenceGenome : referenceGenome,
                toolName        : { createToolName() },
                path            : "path_${nextId}",
                indexToolVersion: "indexToolVersion_${nextId}",
        ], properties, saveAndValidate)
        referenceGenome.addToReferenceGenomeIndexes(referenceGenomeIndex)
        referenceGenome.save(flush: true)
        return referenceGenomeIndex
    }

    SeqPlatform createSeqPlatformWithSeqPlatformGroup(Map properties = [:]) {
        Set<SeqPlatformGroup> spg = (properties.seqPlatformGroups ?: [createSeqPlatformGroup()]) as Set

        SeqPlatform sp = createSeqPlatform([seqPlatformGroups: spg] + properties)

        sp.seqPlatformGroups.each {
            sp.addToSeqPlatformGroups(it)
        }
        sp.save(flush: true)

        return sp
    }

    SeqPlatform createSeqPlatform(Map properties = [:]) {
        return createDomainObject(SeqPlatform, [
                name                 : "seqPlatform_${nextId}",
                seqPlatformModelLabel: { createSeqPlatformModelLabel() },
        ], properties)
    }

    SeqPlatformModelLabel createSeqPlatformModelLabel(Map properties = [:]) {
        return createDomainObject(SeqPlatformModelLabel, [
                name       : "seqPlatformModelLabel_${nextId}",
                importAlias: [],
        ], properties)
    }

    SequencingKitLabel createSequencingKitLabel(Map properties = [:]) {
        return createDomainObject(SequencingKitLabel, [
                name       : "SequencingKitLabel__${nextId}",
                importAlias: [],
        ], properties)
    }

    SeqPlatformGroup createSeqPlatformGroup(Map properties = [:]) {
        return createDomainObject(SeqPlatformGroup, [
                seqPlatforms: [] as Set,
        ], properties)
    }

    SeqPlatformGroup createSeqPlatformGroupWithMergingCriteria(Map properties = [:]) {
        return createDomainObject(SeqPlatformGroup, [
                mergingCriteria: createMergingCriteriaLazy(
                        useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
                )
        ], properties)
    }

    MergingCriteria createMergingCriteria(Map properties = [:]) {
        return createDomainObject(MergingCriteria, [
                project: { createProject() },
                seqType: { createSeqType() },
        ], properties)
    }

    MergingCriteria createMergingCriteriaLazy(Map properties) {
        if ((properties.get("seqType") as SeqType)?.wgbs && !properties.hasProperty("useLibPrepKit")) {
            properties.put("useLibPrepKit", false)
        }

        return findOrCreateDomainObject(MergingCriteria, [
                project: { createProject() },
                seqType: { createSeqType() },
        ], properties)
    }

    OtrsTicket createOtrsTicket(Map properties = [:]) {
        return createDomainObject(OtrsTicket, [
                ticketNumber: "20000101" + String.format("%08d", nextId),
        ], properties)
    }

    OtrsTicket createOtrsTicketWithEndDatesAndNotificationSent(Map properties = [:]) {
        return createOtrsTicket([
                installationStarted  : new Date(),
                installationFinished : new Date(),
                fastqcStarted        : new Date(),
                fastqcFinished       : new Date(),
                alignmentStarted     : new Date(),
                alignmentFinished    : new Date(),
                snvStarted           : new Date(),
                snvFinished          : new Date(),
                indelStarted         : new Date(),
                indelFinished        : new Date(),
                sophiaStarted        : new Date(),
                sophiaFinished       : new Date(),
                aceseqStarted        : new Date(),
                aceseqFinished       : new Date(),
                runYapsaStarted      : new Date(),
                runYapsaFinished     : new Date(),
                finalNotificationSent: true,
        ] + properties)
    }

    IlseSubmission createIlseSubmission(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(IlseSubmission, [
                ilseNumber: { nextId % 999999 },
                warning   : false,
        ], properties, saveAndValidate)
    }

    ProcessingOption findOrCreateProcessingOption(Map properties = [:]) {
        ProcessingOption processingOption = findOrCreateDomainObject(ProcessingOption,
                [value: properties.containsKey("value") ? properties['value'] : "processingOptionValue_${counter++}"],
                properties.findAll { it.key != "value" },
        )
        if (properties.containsKey("value")) {
            processingOption.value = properties.value
        }
        processingOption.save(flush: true)
        return processingOption
    }

    ProcessingOption findOrCreateProcessingOption(ProcessingOption.OptionName optionName, String value, String type = null) {
        return findOrCreateProcessingOption([
                name : optionName,
                value: value,
                type : type,
        ])
    }
}
