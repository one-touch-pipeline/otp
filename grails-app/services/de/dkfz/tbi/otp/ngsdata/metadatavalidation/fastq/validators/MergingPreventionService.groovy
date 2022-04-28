/*
 * Copyright 2011-2021 The OTP authors
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

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.SqlUtil
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 * A helper service for {@link MergingPreventionValidator}. It should not be used elsewhere.
 */
@Transactional(readOnly = true)
class MergingPreventionService {

    static final String ALREADY_DATA_EXIST = "At least for one sample otp contains already data"
    static final String ALREADY_DATA_EXIST_COMPATIBLE = "At least for one sample otp contains already compatible data for merging"
    static final String ALREADY_DATA_EXIST_INCOMPATIBLE = "At least for one sample otp contains already not compatible data for merging"
    static final String MERGING_WORK_PACKAGE_EXISTS_COMPATIBLE = "Sample would be automatically merged with existing samples."
    static final String MERGING_WORK_PACKAGE_EXISTS_INCOMPATIBLE = "Sample can not be merged with existing data, because merging criteria is incompatible."

    @Autowired
    AbstractMergingWorkPackageService abstractMergingWorkPackageService

    @Autowired
    AntibodyTargetService antibodyTargetService

    @Autowired
    LibraryPreparationKitService libraryPreparationKitService

    @Autowired
    MetadataImportService metadataImportService

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Autowired
    SeqPlatformService seqPlatformService

    @Autowired
    SeqTrackService seqTrackService

    @Autowired
    SeqTypeService seqTypeService

    /**
     * extract from the ValueTuple the values needed for {@link MergingPreventionValidator}
     */
    MergingPreventionDataDto parseMetaData(ValueTuple valueTuple) {
        MergingPreventionDataDto data = new MergingPreventionDataDto()
        data.seqType = metadataImportService.getSeqTypeFromMetadata(valueTuple)
        if (!data.seqType) {
            return data
        }

        data.sample = findExistingSampleForValueTuple(valueTuple)
        if (!data.sample) {
            return data
        }

        data.seqPlatform = findSeqPlatform(valueTuple)
        if (!data.seqPlatform) {
            return data
        }

        data.antibodyTarget = findAntibodyTarget(valueTuple, data.seqType)

        data.libraryPreparationKit = findLibraryPreparationKit(valueTuple)

        data.mergingCriteria = findMergingCriteria(data.sample.project, data.seqType)

        data.seqPlatformGroupImport = data.seqPlatform.getSeqPlatformGroupForMergingCriteria(
                data.sample.project,
                data.seqType,
        )

        data.filledCompletely = true
        return data
    }

    /**
     * helper for {@link MergingPreventionValidator} to check, whether new data corresponds to existing Lanes
     */
    void checkForSeqTracks(MetadataValidationContext context, ValueTuple valueTuple, MergingPreventionDataDto data) {
        List<SeqTrack> seqTracks = seqTrackService.findAllBySampleAndSeqTypeAndAntibodyTarget(data.sample, data.seqType, data.antibodyTarget)
        if (!seqTracks) {
            return
        }

        seqTracks.each { SeqTrack seqTrack ->
            SeqPlatformGroup seqPlatformGroup = seqTrack.seqPlatformGroup

            if (data.seqType.singleCell) {
                context.addProblem(valueTuple.cells, LogLevel.WARNING,
                        "For ${data.createMessagePrefix(true)} already data are registered in OTP.",
                        ALREADY_DATA_EXIST)
            } else {
                boolean mergeableSeqPlatform = data.ignoreSeqPlatformGroupForMerging() || seqPlatformGroup == data.seqPlatformGroupImport
                boolean mergeableLibPrepKit = data.checkLibPrepKit() ? seqTrack.libraryPreparationKit == data.libraryPreparationKit : true
                if (mergeableSeqPlatform && mergeableLibPrepKit) {
                    context.addProblem(valueTuple.cells, LogLevel.WARNING,
                            "For ${data.createMessagePrefix(true)} already data are registered in OTP, with are compatble to merge.",
                            ALREADY_DATA_EXIST_COMPATIBLE)
                } else {
                    List<String> warnings = []
                    if (!mergeableSeqPlatform) {
                        warnings << "the new seq platform ${data.seqPlatform} is part of another seq platform group then the existing"
                    }
                    if (!mergeableLibPrepKit) {
                        warnings << "the new library preparation kit ${data.libraryPreparationKit} differs from the old library preparation kit " +
                                "${seqTrack.libraryPreparationKit}"
                    }
                    context.addProblem(valueTuple.cells, LogLevel.WARNING,
                            "For ${data.createMessagePrefix(true)} already data are registered in OTP, with are not compatble to merge, " +
                                    "since ${warnings.join(' and ')}.",
                            ALREADY_DATA_EXIST_INCOMPATIBLE)
                }
            }
        }
    }

    /**
     * helper for {@link MergingPreventionValidator} to check, whether new data corresponds to existing {@link MergingWorkPackage}
     */
    void checkForMergingWorkPackage(MetadataValidationContext context, ValueTuple valueTuple, MergingPreventionDataDto data) {
        List<MergingWorkPackage> mergingWorkPackages = abstractMergingWorkPackageService.findAllBySampleAndSeqTypeAndAntibodyTarget(
                data.sample, data.seqType, data.antibodyTarget)

        if (!mergingWorkPackages) {
            return
        }

        if (data.seqType.singleCell) {
            if (mergingWorkPackages.any { hasNonWithdrawnSeqTracks(it) }) {
                context.addProblem(valueTuple.cells, LogLevel.WARNING,
                        "${data.createMessagePrefix(false)} would be automatically merged with existing samples.",
                        MERGING_WORK_PACKAGE_EXISTS_COMPATIBLE)
            }
        } else {
            mergingWorkPackages.each { MergingWorkPackage mergingWorkPackage ->
                boolean mergeableSeqPlatform = data.ignoreSeqPlatformGroupForMerging() || mergingWorkPackage.seqPlatformGroup == data.seqPlatformGroupImport
                boolean mergeableLibPrepKit = data.checkLibPrepKit() ? mergingWorkPackage.libraryPreparationKit == data.libraryPreparationKit : true

                if (mergeableSeqPlatform && mergeableLibPrepKit) {
                    if (hasNonWithdrawnSeqTracks(mergingWorkPackage)) {
                        context.addProblem(valueTuple.cells, LogLevel.WARNING,
                                "${data.createMessagePrefix(false)} would be automatically merged with existing samples.",
                                MERGING_WORK_PACKAGE_EXISTS_COMPATIBLE)
                    }
                } else {
                    List<String> warnings = []
                    if (!mergeableSeqPlatform) {
                        warnings << "the new seq platform ${data.seqPlatform} is part of group ${data.seqPlatformGroupImport}, but existing bam file use " +
                                "group ${mergingWorkPackage.seqPlatformGroup}"
                    }
                    if (!mergeableLibPrepKit) {
                        warnings << "the new library preparation kit ${data.libraryPreparationKit} differs from the old library preparation kit " +
                                "${mergingWorkPackage.libraryPreparationKit}"
                    }
                    context.addProblem(valueTuple.cells, LogLevel.WARNING,
                            "${data.createMessagePrefix(false)} can not be merged with the existing bam file, " +
                                    "since ${warnings.join(' and ')}.",
                            MERGING_WORK_PACKAGE_EXISTS_INCOMPATIBLE)
                }
            }
        }
    }

    protected Sample findExistingSampleForValueTuple(ValueTuple valueTuple) {
        String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
        if (sampleIdentifier) {
            return sampleIdentifier.sample
        }

        Project project = metadataImportService.getProjectFromMetadata(valueTuple)
        if (!project) {
            return null
        }

        ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
        if (!parsedSampleIdentifier) {
            return null
        }

        return Sample.createCriteria().get {
            individual {
                eq("pid", parsedSampleIdentifier.pid)
            }
            sampleType {
                or {
                    ilike("name", SqlUtil.replaceWildcardCharactersInLikeExpression(parsedSampleIdentifier.sampleTypeDbName))
                    ilike("name", SqlUtil.replaceWildcardCharactersInLikeExpression(
                            parsedSampleIdentifier.sampleTypeDbName.replace('_', '-')))
                }
            }
        } as Sample
    }

    protected AntibodyTarget findAntibodyTarget(ValueTuple valueTuple, SeqType seqType) {
        String antibodyTargetName = valueTuple.getValue(ANTIBODY_TARGET.name()) ?: ""
        if (seqType.hasAntibodyTarget && antibodyTargetName) {
            return antibodyTargetService.findByNameOrImportAlias(antibodyTargetName)
        }
        return null
    }

    protected LibraryPreparationKit findLibraryPreparationKit(ValueTuple valueTuple) {
        String libraryPreparationKitName = valueTuple.getValue(LIB_PREP_KIT.name())
        return libraryPreparationKitName ? libraryPreparationKitService.findByNameOrImportAlias(libraryPreparationKitName) : null
    }

    protected MergingCriteria findMergingCriteria(Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType))
    }

    protected SeqPlatform findSeqPlatform(ValueTuple valueTuple) {
        String seqPlatformName = valueTuple.getValue(INSTRUMENT_PLATFORM.name())
        String seqPlatformModel = valueTuple.getValue(INSTRUMENT_MODEL.name())
        String sequencingKit = valueTuple.getValue(SEQUENCING_KIT.name())
        return seqPlatformName ? seqPlatformService.findSeqPlatform(seqPlatformName, seqPlatformModel, sequencingKit) : null
    }

    private boolean hasNonWithdrawnSeqTracks(MergingWorkPackage mergingWorkPackage) {
        return mergingWorkPackage.seqTracks.find {
            !it.withdrawn
        }
    }
}
