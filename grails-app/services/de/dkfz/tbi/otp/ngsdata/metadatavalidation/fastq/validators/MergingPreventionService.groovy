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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackageService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

/**
 * A helper service for {@link MergingPreventionValidator}. It should not be used elsewhere.
 */
@CompileDynamic
@Transactional(readOnly = true)
class MergingPreventionService {

    static final String ALREADY_DATA_EXIST = "At least for one sample otp contains already data"
    static final String ALREADY_DATA_EXIST_COMPATIBLE = "At least for one sample otp contains already compatible data for merging"
    static final String ALREADY_DATA_EXIST_INCOMPATIBLE = "At least for one sample otp contains already not compatible data for merging"
    static final String MERGING_WORK_PACKAGE_EXISTS_COMPATIBLE = "Sample would be automatically merged with existing samples."
    static final String MERGING_WORK_PACKAGE_EXISTS_INCOMPATIBLE = "Sample can not be merged with existing data, because merging criteria is incompatible."

    MergingWorkPackageService mergingWorkPackageService

    SeqTrackService seqTrackService

    ValidatorHelperService validatorHelperService

    /**
     * extract from the ValueTuple the values needed for {@link MergingPreventionValidator}
     */
    MergingPreventionDataDto parseMetaData(ValueTuple valueTuple) {
        MergingPreventionDataDto data = new MergingPreventionDataDto()
        data.seqType = validatorHelperService.getSeqTypeFromMetadata(valueTuple)
        if (!data.seqType) {
            return data
        }

        data.sample = validatorHelperService.findExistingSampleForValueTuple(valueTuple)
        if (!data.sample) {
            return data
        }

        data.seqPlatform = validatorHelperService.findSeqPlatform(valueTuple)
        if (!data.seqPlatform) {
            return data
        }

        data.antibodyTarget = validatorHelperService.findAntibodyTarget(valueTuple, data.seqType)

        data.libraryPreparationKit = validatorHelperService.findLibraryPreparationKit(valueTuple)

        data.mergingCriteria = validatorHelperService.findMergingCriteria(data.sample.project, data.seqType)

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
        seqTracks.groupBy {
            it.seqPlatform
        }.each { SeqPlatform seqPlatform, List<SeqTrack> seqTracksPerSeqPlatform ->
            SeqPlatformGroup seqPlatformGroup = seqPlatform.getSeqPlatformGroupForMergingCriteria(data.sample.project, data.seqType)

            if (data.seqType.singleCell) {
                context.addProblem(valueTuple.cells, LogLevel.WARNING,
                        "For ${data.createMessagePrefix(true)} already data are registered in OTP.",
                        ALREADY_DATA_EXIST)
            } else {
                boolean mergeableSeqPlatform = data.ignoreSeqPlatformGroupForMerging() || seqPlatformGroup == data.seqPlatformGroupImport
                seqTracksPerSeqPlatform*.libraryPreparationKit.unique().each { LibraryPreparationKit libraryPreparationKit ->
                    boolean mergeableLibPrepKit = data.checkLibPrepKit() ? libraryPreparationKit == data.libraryPreparationKit : true
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
                                    "${libraryPreparationKit}"
                        }
                        context.addProblem(valueTuple.cells, LogLevel.WARNING,
                                "For ${data.createMessagePrefix(true)} already data are registered in OTP, with are not compatble to merge, " +
                                        "since ${warnings.join(' and ')}.",
                                ALREADY_DATA_EXIST_INCOMPATIBLE)
                    }
                }
            }
        }
    }

    /**
     * helper for {@link MergingPreventionValidator} to check, whether new data corresponds to existing {@link MergingWorkPackage}
     */
    void checkForMergingWorkPackage(MetadataValidationContext context, ValueTuple valueTuple, MergingPreventionDataDto data) {
        List<MergingWorkPackage> mergingWorkPackages = mergingWorkPackageService.findAllBySampleAndSeqTypeAndAntibodyTarget(
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

    private boolean hasNonWithdrawnSeqTracks(MergingWorkPackage mergingWorkPackage) {
        return mergingWorkPackage.seqTracks.find {
            !it.withdrawn
        }
    }
}
