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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MergingPreventionServiceSpec extends Specification implements DataTest, DomainFactoryCore, AlignmentPipelineFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CellRangerMergingWorkPackage,
                DataFile,
                MergingWorkPackage,
                ProcessingOption,
                SampleIdentifier,
        ]
    }

    private Project project
    private SampleType sampleType
    private SeqType seqType
    private AntibodyTarget antibodyTarget
    private LibraryPreparationKit libraryPreparationKit
    private SeqPlatform seqPlatform
    private Individual individual
    private Sample sample
    private SampleIdentifier sampleIdentifier
    private String sampleName
    private MergingCriteria mergingCriteria
    private SeqPlatformGroup seqPlatformGroupImport
    private SeqPlatformGroup seqPlatformGroupData

    private Run run
    private SeqTrack seqTrack
    private AbstractMergingWorkPackage mergingWorkPackage

    void setupData(boolean singleCell, boolean useAntibodyTarget, boolean useLibPrepKit, boolean useSequencingKitLabel) {
        project = createProject()
        sampleType = createSampleType()
        seqType = createSeqType([
                singleCell       : singleCell,
                hasAntibodyTarget: useAntibodyTarget,
        ])
        antibodyTarget = useAntibodyTarget ? createAntibodyTarget() : null
        libraryPreparationKit = useLibPrepKit ? createLibraryPreparationKit() : null
        seqPlatform = createSeqPlatform([
                sequencingKitLabel: useSequencingKitLabel ? createSequencingKitLabel() : null,
        ])
        individual = createIndividual([
                project: project,
        ])
        sample = createSample([
                individual: individual,
                sampleType: sampleType,
        ])
        sampleName = "sampleName_${nextId}"
        sampleIdentifier = createSampleIdentifier([
                name  : sampleName,
                sample: sample,
        ])
    }

    void setupDataWithSeqTrack(boolean singleCell, boolean useAntibodyTarget, boolean useLibPrepKit, boolean useSequencingKitLabel) {
        setupData(singleCell, useAntibodyTarget, useLibPrepKit, useSequencingKitLabel)

        mergingCriteria = null
        seqPlatformGroupImport = null
        seqPlatformGroupData = null
        run = createRun([
                seqPlatform: seqPlatform,
        ])
        seqTrack = createSeqTrack([
                sample               : sample,
                seqType              : seqType,
                antibodyTarget       : antibodyTarget,
                run                  : run,
                libraryPreparationKit: libraryPreparationKit,
        ])
    }

    void setupDataWithMergingWorkPackage(boolean singleCell, boolean useAntibodyTarget, boolean useLibPrepKit, boolean useSequencingKitLabel) {
        setupDataWithSeqTrack(singleCell, useAntibodyTarget, useLibPrepKit, useSequencingKitLabel)

        seqPlatformGroupData = createSeqPlatformGroup()

        Map workPackageParameters = [
                sample               : sample,
                seqType              : seqType,
                antibodyTarget       : antibodyTarget,
                seqPlatformGroup     : seqPlatformGroupData,
                libraryPreparationKit: libraryPreparationKit,
                seqTracks            : [seqTrack] as Set,
        ]

        if (singleCell) {
            mergingWorkPackage = DomainFactory.proxyCellRanger.createMergingWorkPackage(workPackageParameters)
        } else {
            mergingWorkPackage = DomainFactory.proxyRoddy.createMergingWorkPackage(workPackageParameters)
        }
    }

    @Unroll
    void "parseMetaData, when #name, then get expected data"() {
        given:
        setupData(singleCell, useAntibodyTarget, useLibPrepKit, useSequencingKitLabel)

        ValueTuple valueTuple = createValueTuple()

        MergingPreventionService service = createServiceForParseMetaData()

        when:
        MergingPreventionDataDto data = service.parseMetaData(valueTuple)

        then:
        data
        data.filledCompletely
        data.seqType == seqType
        data.seqPlatform == seqPlatform
        data.sample == sample
        data.antibodyTarget == antibodyTarget
        data.libraryPreparationKit == libraryPreparationKit

        where:
        singleCell | useAntibodyTarget | useLibPrepKit | useSequencingKitLabel
        false      | false             | false         | false
        true       | true              | true          | true

        name = "singleCell is ${singleCell}, useAntibodyTarget is ${useAntibodyTarget}, useLibPrepKit is ${useLibPrepKit}, useSequencingKitLabel is ${useSequencingKitLabel}"
    }

    @Unroll
    void "parseMetaData, when #name, then get incomplete data"() {
        given:
        setupData(singleCell, useAntibodyTarget, useLibPrepKit, useSequencingKitLabel)

        ValueTuple valueTuple = createValueTuple([(key): value])

        MergingPreventionService service = createServiceForParseMetaData()

        when:
        MergingPreventionDataDto data = service.parseMetaData(valueTuple)

        then:
        data
        !data.filledCompletely

        where:
        singleCell | useAntibodyTarget | useLibPrepKit | useSequencingKitLabel | key                  | value
        false      | false             | false         | false                 | SEQUENCING_TYPE      | ''
        false      | false             | false         | false                 | SEQUENCING_TYPE      | 'unknown'
        false      | false             | false         | false                 | SEQUENCING_READ_TYPE | ''
        false      | false             | false         | false                 | SEQUENCING_READ_TYPE | 'unknown'
        true       | false             | false         | false                 | BASE_MATERIAL        | ''
        true       | false             | false         | false                 | BASE_MATERIAL        | 'unknown'
        false      | false             | false         | false                 | BASE_MATERIAL        | SeqType.SINGLE_CELL_RNA
        false      | false             | false         | false                 | BASE_MATERIAL        | SeqType.SINGLE_CELL_DNA
        false      | false             | false         | false                 | INSTRUMENT_PLATFORM  | ''
        false      | false             | false         | false                 | INSTRUMENT_PLATFORM  | 'unknown'
        false      | false             | false         | true                  | INSTRUMENT_MODEL     | ''
        false      | false             | false         | true                  | INSTRUMENT_MODEL     | 'unknown'
        false      | false             | false         | false                 | INSTRUMENT_MODEL     | 'unknown'
        false      | false             | false         | true                  | SEQUENCING_KIT       | ''
        false      | false             | false         | true                  | SEQUENCING_KIT       | 'unknown'
        false      | false             | false         | false                 | SEQUENCING_KIT       | 'unknown'
        false      | false             | false         | false                 | SAMPLE_NAME          | ''
        false      | false             | false         | false                 | SAMPLE_NAME          | 'unknown'

        name = "${key} is '${value}', singleCell is ${singleCell}, useAntibodyTarget is ${useAntibodyTarget}, useLibPrepKit is ${useLibPrepKit}, " +
                "useSequencingKitLabel is ${useSequencingKitLabel}"
    }

    @Unroll
    void "checkForSeqTracks, when seqType #name and has no seqTracks found, then add no warnings"() {
        given:
        setupData(false, useAntibodyTarget, true, false)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        ValueTuple valueTuple = new ValueTuple()
        MergingPreventionDataDto data = createMergingPreventionDataDto()

        MergingPreventionService service = new MergingPreventionService([
                seqTrackService: Mock(SeqTrackService) {
                    1 * findAllBySampleAndSeqTypeAndAntibodyTarget(sample, seqType, antibodyTarget) >> []
                    0 * _
                },
        ])

        when:
        service.checkForSeqTracks(context, valueTuple, data)

        then:
        context.problems.empty

        where:
        useAntibodyTarget << [
                false,
                true,
        ]

        name = (useAntibodyTarget ? "use" : "do not use") + " AntibodyTarget"
    }

    @Unroll
    void "checkForSeqTracks, when seqTracks are found and seqType is single and #name, then add expected error"() {
        given:
        setupDataWithSeqTrack(true, useAntibodyTarget, true, false)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        ValueTuple valueTuple = new ValueTuple([:], [] as Set)
        MergingPreventionDataDto data = createMergingPreventionDataDto()

        MergingPreventionService service = new MergingPreventionService([
                seqTrackService: Mock(SeqTrackService) {
                    1 * findAllBySampleAndSeqTypeAndAntibodyTarget(sample, seqType, antibodyTarget) >> [seqTrack]
                    0 * _
                },
        ])

        Set<Problem> problems = [
                new Problem([] as Set, LogLevel.WARNING,
                        "For sample ${sample.displayName} with sequencing type ${seqType.displayNameWithLibraryLayout} already data are registered in OTP.",
                        MergingPreventionService.ALREADY_DATA_EXIST),
        ] as Set

        when:
        service.checkForSeqTracks(context, valueTuple, data)

        then:
        context.problems == problems

        where:
        useAntibodyTarget << [
                false,
                true,
        ]

        name = (useAntibodyTarget ? "use" : "do not use") + " AntibodyTarget"
    }

    @Unroll
    void "checkForSeqTracks, when seqTracks are found and seqType is bulk and #name, then add expected warning #messageType"() {
        given:
        setupDataWithSeqTrack(false, false, true, false)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        ValueTuple valueTuple = new ValueTuple([:], [] as Set)

        if (mergingCriteriaSeqPlatformGroup) {
            mergingCriteria = createMergingCriteria([
                    project            : project,
                    seqType            : seqType,
                    useSeqPlatformGroup: mergingCriteriaSeqPlatformGroup,
                    useLibPrepKit      : mergingCriteriaUseLibPrepKit,
            ])
        } else {
            mergingCriteria == null
        }

        MergingCriteria mergingCriteriaForSeqPlatformGroup = mergingCriteriaSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC ?
                mergingCriteria : null

        seqPlatform = createSeqPlatform()

        switch (seqPlatformGroupCase) {
            case SeqPlatformGroupCase.SAME_GROUP:
                seqPlatformGroupImport = seqPlatformGroupData = createSeqPlatformGroup([
                        seqPlatforms   : [seqTrack.seqPlatform, seqPlatform] as Set,
                        mergingCriteria: mergingCriteriaForSeqPlatformGroup,
                ])
                break
            case SeqPlatformGroupCase.DIFFERENT_GROUP:
                seqPlatformGroupImport = createSeqPlatformGroup([
                        seqPlatforms   : [seqPlatform] as Set,
                        mergingCriteria: mergingCriteriaForSeqPlatformGroup,
                ])
                seqPlatformGroupData = createSeqPlatformGroup([
                        seqPlatforms   : [seqTrack.seqPlatform] as Set,
                        mergingCriteria: mergingCriteriaForSeqPlatformGroup,
                ])
                break
            case SeqPlatformGroupCase.NO_GROUP_IMPORT:
                seqPlatformGroupData = createSeqPlatformGroup([
                        seqPlatforms   : [seqTrack.seqPlatform] as Set,
                        mergingCriteria: mergingCriteriaForSeqPlatformGroup,
                ])
                break
            case SeqPlatformGroupCase.NO_GROUP_EXISTING:
                seqPlatformGroupImport = createSeqPlatformGroup([
                        seqPlatforms   : [seqPlatform] as Set,
                        mergingCriteria: mergingCriteriaForSeqPlatformGroup,
                ])
                break
        }

        switch (seqTrackLibPrep) {
            case LibPrepKitCase.SAME:
                //no update needed
                break
            case LibPrepKitCase.OTHER:
                libraryPreparationKit = createLibraryPreparationKit()
                break
            case LibPrepKitCase.NULL_IMPORT:
                libraryPreparationKit = null
                break
            case LibPrepKitCase.NULL_EXISTING:
                seqTrack.libraryPreparationKit = null
                seqTrack.save(flush: true)
                break
        }

        MergingPreventionDataDto data = createMergingPreventionDataDto()

        MergingPreventionService service = new MergingPreventionService([
                seqTrackService: Mock(SeqTrackService) {
                    1 * findAllBySampleAndSeqTypeAndAntibodyTarget(sample, seqType, antibodyTarget) >> [seqTrack]
                    0 * _
                },
        ])

        String message
        String summaryMessage

        switch (messageType) {
            case MessageType.MERGE:
                message = "For ${data.createMessagePrefix(true)} already data are registered in OTP, with are compatble to merge."
                summaryMessage = MergingPreventionService.ALREADY_DATA_EXIST_COMPATIBLE
                break
            case MessageType.INCOMPATIBLE_SEQ_PLATFORM:
                message = "For ${data.createMessagePrefix(true)} already data are registered in OTP, with are not compatble to merge, since " +
                        "the new seq platform ${data.seqPlatform} is part of another seq platform group then the existing."
                summaryMessage = MergingPreventionService.ALREADY_DATA_EXIST_INCOMPATIBLE
                break
            case MessageType.INCOMPATIBLE_LIB_PREP_KIT:
                message = "For ${data.createMessagePrefix(true)} already data are registered in OTP, with are not compatble to merge, since " +
                        "the new library preparation kit ${data.libraryPreparationKit} differs from the old library preparation kit ${seqTrack.libraryPreparationKit}."
                summaryMessage = MergingPreventionService.ALREADY_DATA_EXIST_INCOMPATIBLE
                break
            case MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT:
                message = "For ${data.createMessagePrefix(true)} already data are registered in OTP, with are not compatble to merge, since " +
                        "the new seq platform ${data.seqPlatform} is part of another seq platform group then the existing and " +
                        "the new library preparation kit ${data.libraryPreparationKit} differs from the old library preparation kit ${seqTrack.libraryPreparationKit}."
                summaryMessage = MergingPreventionService.ALREADY_DATA_EXIST_INCOMPATIBLE
                break
        }

        Set<Problem> problems = [
                new Problem([] as Set, LogLevel.WARNING, message, summaryMessage),
        ] as Set

        when:
        service.checkForSeqTracks(context, valueTuple, data)

        then:
        context.problems == problems

        where:
        mergingCriteriaSeqPlatformGroup                                         | mergingCriteriaUseLibPrepKit | seqPlatformGroupCase                   | seqTrackLibPrep              || messageType
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | true                         | null                                   | LibPrepKitCase.SAME          || MessageType.MERGE
        null                                                                    | null                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | false                        | null                                   | LibPrepKitCase.SAME          || MessageType.MERGE

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        null                                                                    | null                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        null                                                                    | null                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        null                                                                    | null                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | true                         | null                                   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.OTHER         || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.OTHER         || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | false                        | null                                   | LibPrepKitCase.OTHER         || MessageType.MERGE

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | true                         | null                                   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_IMPORT   || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_IMPORT   || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | false                        | null                                   | LibPrepKitCase.NULL_IMPORT   || MessageType.MERGE

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | true                         | null                                   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_EXISTING || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_EXISTING || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | false                        | null                                   | LibPrepKitCase.NULL_EXISTING || MessageType.MERGE

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        null                                                                    | null                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        name = "mergingCriteriaSeqPlatformGroup is ${mergingCriteriaSeqPlatformGroup}, mergingCriteriaUseLibPrepKit is ${mergingCriteriaUseLibPrepKit}, seqPlatformGroupCase is ${seqPlatformGroupCase}, " +
                "seqTrackLibPrep is ${seqTrackLibPrep}"
    }

    @Unroll
    void "checkForMergingWorkPackage, when seqType #name and has no seqTracks found, then add no warnings"() {
        given:
        setupData(false, useAntibodyTarget, true, false)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        ValueTuple valueTuple = new ValueTuple()
        MergingPreventionDataDto data = createMergingPreventionDataDto()

        MergingPreventionService service = new MergingPreventionService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    1 * findAllBySampleAndSeqTypeAndAntibodyTarget(sample, seqType, antibodyTarget) >> []
                    0 * _
                },
        ])

        when:
        service.checkForMergingWorkPackage(context, valueTuple, data)

        then:
        context.problems.empty

        where:
        useAntibodyTarget << [
                false,
                true,
        ]

        name = (useAntibodyTarget ? "use" : "do not use") + " AntibodyTarget"
    }

    @Unroll
    void "checkForMergingWorkPackage, when seqTracks are found and seqType is single and #name, then add expected warning"() {
        given:
        setupDataWithMergingWorkPackage(true, useAntibodyTarget, true, false)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        ValueTuple valueTuple = new ValueTuple([:], [] as Set)
        MergingPreventionDataDto data = createMergingPreventionDataDto()

        MergingPreventionService service = new MergingPreventionService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    1 * findAllBySampleAndSeqTypeAndAntibodyTarget(sample, seqType, antibodyTarget) >> [mergingWorkPackage]
                    0 * _
                },
        ])

        Set<Problem> problems = [
                new Problem([] as Set, LogLevel.WARNING,
                        "Sample ${sample.displayName} with sequencing type ${seqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        MergingPreventionService.MERGING_WORK_PACKAGE_EXISTS_COMPATIBLE),
        ] as Set

        when:
        service.checkForMergingWorkPackage(context, valueTuple, data)

        then:
        context.problems == problems

        where:
        useAntibodyTarget << [
                false,
                true,
        ]

        name = (useAntibodyTarget ? "use" : "do not use") + " AntibodyTarget"
    }

    @Unroll
    void "checkForMergingWorkPackage, when seqTracks are found and seqType is bulk and #name, then add expected warning #messageType"() {
        given:
        setupDataWithMergingWorkPackage(false, false, true, false)
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        ValueTuple valueTuple = new ValueTuple([:], [] as Set)

        mergingCriteria = createMergingCriteria([
                project            : project,
                seqType            : seqType,
                useSeqPlatformGroup: mergingCriteriaSeqPlatformGroup,
                useLibPrepKit      : mergingCriteriaUseLibPrepKit,
        ])

        MergingCriteria mergingCriteriaForSeqPlatformGroup = mergingCriteriaSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC ?
                mergingCriteria : null

        seqPlatform = createSeqPlatform()

        switch (seqPlatformGroupCase) {
            case SeqPlatformGroupCase.SAME_GROUP:
                seqPlatformGroupImport = seqPlatformGroupData
                seqPlatformGroupData.seqPlatforms.addAll([seqTrack.seqPlatform, seqPlatform])
                seqPlatformGroupData.save(flush: true)
                break
            case SeqPlatformGroupCase.DIFFERENT_GROUP:
                seqPlatformGroupImport = createSeqPlatformGroup([
                        seqPlatforms   : [seqPlatform] as Set,
                        mergingCriteria: mergingCriteriaForSeqPlatformGroup,
                ])
                seqPlatformGroupData.seqPlatforms.addAll([seqTrack.seqPlatform])
                seqPlatformGroupData.save(flush: true)
                break
            case SeqPlatformGroupCase.NO_GROUP_IMPORT:
                seqPlatformGroupData.seqPlatforms.addAll([seqTrack.seqPlatform])
                seqPlatformGroupData.save(flush: true)
                break
            case SeqPlatformGroupCase.NO_GROUP_EXISTING:
                seqPlatformGroupImport = createSeqPlatformGroup([
                        seqPlatforms   : [seqPlatform] as Set,
                        mergingCriteria: mergingCriteriaForSeqPlatformGroup,
                ])
                break
        }

        switch (seqTrackLibPrep) {
            case LibPrepKitCase.SAME:
                //no update needed
                break
            case LibPrepKitCase.OTHER:
                libraryPreparationKit = createLibraryPreparationKit()
                break
            case LibPrepKitCase.NULL_IMPORT:
                libraryPreparationKit = null
                break
            case LibPrepKitCase.NULL_EXISTING:
                mergingWorkPackage.libraryPreparationKit = null
                mergingWorkPackage.save(flush: true)
                break
        }

        MergingPreventionDataDto data = createMergingPreventionDataDto()

        MergingPreventionService service = new MergingPreventionService([
                abstractMergingWorkPackageService: Mock(AbstractMergingWorkPackageService) {
                    1 * findAllBySampleAndSeqTypeAndAntibodyTarget(sample, seqType, antibodyTarget) >> [mergingWorkPackage]
                    0 * _
                },
        ])

        String message
        String summaryMessage

        switch (messageType) {
            case MessageType.MERGE:
                message = "${data.createMessagePrefix(false)} would be automatically merged with existing samples."
                summaryMessage = MergingPreventionService.MERGING_WORK_PACKAGE_EXISTS_COMPATIBLE
                break
            case MessageType.INCOMPATIBLE_SEQ_PLATFORM:
                message = "${data.createMessagePrefix(false)} can not be merged with the existing bam file, since " +
                        "the new seq platform ${data.seqPlatform} is part of group ${data.seqPlatformGroupImport}, but existing bam file use group ${seqPlatformGroupData}."
                summaryMessage = MergingPreventionService.MERGING_WORK_PACKAGE_EXISTS_INCOMPATIBLE
                break
            case MessageType.INCOMPATIBLE_LIB_PREP_KIT:
                message = "${data.createMessagePrefix(false)} can not be merged with the existing bam file, since " +
                        "the new library preparation kit ${data.libraryPreparationKit} differs from the old library preparation kit ${mergingWorkPackage.libraryPreparationKit}."
                summaryMessage = MergingPreventionService.MERGING_WORK_PACKAGE_EXISTS_INCOMPATIBLE
                break
            case MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT:
                message = "${data.createMessagePrefix(false)} can not be merged with the existing bam file, since " +
                        "the new seq platform ${data.seqPlatform} is part of group ${data.seqPlatformGroupImport}, but existing bam file use group ${seqPlatformGroupData} and " +
                        "the new library preparation kit ${data.libraryPreparationKit} differs from the old library preparation kit ${mergingWorkPackage.libraryPreparationKit}."
                summaryMessage = MergingPreventionService.MERGING_WORK_PACKAGE_EXISTS_INCOMPATIBLE
                break
        }

        Set<Problem> problems = [
                new Problem([] as Set, LogLevel.WARNING, message, summaryMessage),
        ] as Set

        when:
        service.checkForMergingWorkPackage(context, valueTuple, data)

        then:
        context.problems == problems

        where:
        mergingCriteriaSeqPlatformGroup                                         | mergingCriteriaUseLibPrepKit | seqPlatformGroupCase                   | seqTrackLibPrep              || messageType
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | true                         | null                                   | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.SAME          || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | false                        | null                                   | LibPrepKitCase.SAME          || MessageType.MERGE

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.SAME          || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | true                         | null                                   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.OTHER         || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.OTHER         || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | false                        | null                                   | LibPrepKitCase.OTHER         || MessageType.MERGE

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | true                         | null                                   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_IMPORT   || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_IMPORT   || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | false                        | null                                   | LibPrepKitCase.NULL_IMPORT   || MessageType.MERGE

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.NULL_IMPORT   || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | true                         | null                                   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_EXISTING || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.SAME_GROUP        | LibPrepKitCase.NULL_EXISTING || MessageType.MERGE
        MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING            | false                        | null                                   | LibPrepKitCase.NULL_EXISTING || MessageType.MERGE

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.DIFFERENT_GROUP   | LibPrepKitCase.NULL_EXISTING || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_EXISTING | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | true                         | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT
        MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT               | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM
        MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC | false                        | SeqPlatformGroupCase.NO_GROUP_IMPORT   | LibPrepKitCase.OTHER         || MessageType.INCOMPATIBLE_SEQ_PLATFORM

        name = "mergingCriteriaSeqPlatformGroup is ${mergingCriteriaSeqPlatformGroup}, mergingCriteriaUseLibPrepKit is ${mergingCriteriaUseLibPrepKit}, seqPlatformGroupCase is ${seqPlatformGroupCase}, " +
                "seqTrackLibPrep is ${seqTrackLibPrep}"
    }

    private MergingPreventionService createServiceForParseMetaData() {
        return new MergingPreventionService([
                antibodyTargetService       : new AntibodyTargetService(),
                libraryPreparationKitService: new LibraryPreparationKitService(),
                metadataImportService       : new MetadataImportService([
                        seqTypeService: new SeqTypeService(),
                ]),
                sampleIdentifierService     : new SampleIdentifierService(),
                seqPlatformService          : new SeqPlatformService([
                        seqPlatformModelLabelService: new SeqPlatformModelLabelService(),
                        sequencingKitLabelService   : new SequencingKitLabelService()
                ]),
                seqTypeService              : new SeqTypeService(),
        ])
    }

    private ValueTuple createValueTuple(Map map = [:]) {
        Map data = ([
                (PROJECT)             : project.project.name,
                (SAMPLE_NAME)         : sampleName,
                (SEQUENCING_TYPE)     : seqType.name,
                (SEQUENCING_READ_TYPE): seqType.libraryLayout,
                (BASE_MATERIAL)       : seqType.singleCell ? SeqType.SINGLE_CELL_DNA : '',
                (ANTIBODY_TARGET)     : seqType.hasAntibodyTarget ? antibodyTarget.name : '',
                (INSTRUMENT_PLATFORM) : seqPlatform.name,
                (INSTRUMENT_MODEL)    : seqPlatform.seqPlatformModelLabel.name,
                (SEQUENCING_KIT)      : seqPlatform.sequencingKitLabel?.name ?: '',
                (LIB_PREP_KIT)        : libraryPreparationKit?.name ?: '',
        ] + map).collectEntries {
            [(it.key.toString()): it.value.toString()]
        }
        return new ValueTuple(data, [] as Set)
    }

    private MergingPreventionDataDto createMergingPreventionDataDto(Map map = [:]) {
        return new MergingPreventionDataDto([
                seqType               : seqType,
                seqPlatform           : seqPlatform,
                sample                : sample,
                antibodyTarget        : antibodyTarget,
                libraryPreparationKit : libraryPreparationKit,
                mergingCriteria       : mergingCriteria,
                seqPlatformGroupImport: seqPlatformGroupImport,
                filledCompletely      : true,
        ] + map)
    }

    private static enum SeqPlatformGroupCase {
        SAME_GROUP,
        DIFFERENT_GROUP,
        NO_GROUP_IMPORT,
        NO_GROUP_EXISTING,
    }

    private static enum LibPrepKitCase {
        SAME,
        OTHER,
        NULL_IMPORT,
        NULL_EXISTING,
    }

    private static enum MessageType {
        MERGE,
        INCOMPATIBLE_SEQ_PLATFORM,
        INCOMPATIBLE_LIB_PREP_KIT,
        INCOMPATIBLE_SEQ_PLATFORM_LIB_PREP_KIT,
    }
}
