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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.SqlUtil
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
class MergingPreventionValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

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
    SeqTypeService seqTypeService

    @Override
    Collection<String> getDescriptions() {
        return [
                "Check whether a new sample would be merged with existing samples",
                "Check whether a new sample could not be merged because of incompatible merging criteria (seqPlatform, library preparation kit)",
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        /** This content is used externally. Please discuss a change in the team */
        return [SAMPLE_NAME, SEQUENCING_TYPE, SEQUENCING_READ_TYPE, PROJECT, INSTRUMENT_PLATFORM, INSTRUMENT_MODEL]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        /** This content is used externally. Please discuss a change in the team */
        return [BASE_MATERIAL, ANTIBODY_TARGET, TAGMENTATION, SEQUENCING_KIT, LIB_PREP_KIT]*.name()
    }

    @Override
    void checkMissingRequiredColumn(MetadataValidationContext context, String columnTitle) {
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) {
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            validateValueTuple(context, valueTuple)
        }
    }

    private List<SeqType> getSingleCellSeqTypes() {
        SeqTypeService.cellRangerAlignableSeqTypes
    }

    private List<SeqType> getBulkSeqTypes() {
        SeqTypeService.roddyAlignableSeqTypes
    }

    void validateValueTuple(MetadataValidationContext context, ValueTuple valueTuple) {
        SeqType seqType = metadataImportService.getSeqTypeFromMetadata(valueTuple)
        if (!seqType) {
            return
        }

        AntibodyTarget antibodyTarget = findAntibodyTarget(valueTuple, seqType)

        Sample sample = findExistingSampleForValueTuple(valueTuple)
        if (!sample) {
            return
        }

        SeqPlatform seqPlatform = findSeqPlatform(valueTuple)
        if (!seqPlatform) {
            return
        }

        String libraryPreparationKitName = valueTuple.getValue(LIB_PREP_KIT.name())
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitName ?
                libraryPreparationKitService.findByNameOrImportAlias(libraryPreparationKitName) : null

        List<MergingWorkPackage> mergingWorkPackages = MergingWorkPackage.findAllWhere(
                sample: sample,
                seqType: seqType,
                antibodyTarget: antibodyTarget,
        )

        if (mergingWorkPackages) {
            String messagePrefix = "Sample ${sample.displayName} with sequencing type ${seqType.displayNameWithLibraryLayout}"
            if (seqType in singleCellSeqTypes) {
                if (mergingWorkPackages.any { hasNonWithdrawnSeqTracks(it) }) {
                    context.addProblem(valueTuple.cells, Level.ERROR,
                            "${messagePrefix} would be automatically merged with existing samples.",
                            "Sample would be automatically merged with existing samples.")
                }
            } else if (seqType in bulkSeqTypes && sample.project.alignmentDeciderBeanName != AlignmentDeciderBeanName.NO_ALIGNMENT) {
                mergingWorkPackages.each { MergingWorkPackage mergingWorkPackage ->
                    MergingCriteria mergingCriteria = atMostOneElement(MergingCriteria.findAllByProjectAndSeqType(mergingWorkPackage.project, seqType))
                    boolean useLibPrepKit = mergingCriteria ? mergingCriteria.useLibPrepKit : true
                    boolean mergeableSeqPlatform = mergingWorkPackage.seqPlatformGroup.seqPlatforms.contains(seqPlatform)
                    boolean mergeableLibPrepKit = useLibPrepKit ? mergingWorkPackage.libraryPreparationKit == libraryPreparationKit : true

                    if (mergeableSeqPlatform && mergeableLibPrepKit) {
                        if (hasNonWithdrawnSeqTracks(mergingWorkPackage)) {
                            context.addProblem(valueTuple.cells, Level.WARNING,
                                    "${messagePrefix} would be automatically merged with existing samples.",
                                    "Sample would be automatically merged with existing samples.")
                        }
                    } else {
                        List<String> warnings = []
                        if (!mergeableSeqPlatform) {
                            warnings << "new seq platform ${seqPlatform} is not compatible with seq platform group ${mergingWorkPackage.seqPlatformGroup}"
                        }
                        if (!mergeableLibPrepKit) {
                            warnings << "new library preparation kit ${libraryPreparationKit} differs from old library preparation kit ${mergingWorkPackage.libraryPreparationKit}"
                        }
                        context.addProblem(valueTuple.cells, Level.WARNING,
                                "${messagePrefix} can not be merged with the existing bam file, since ${warnings.join(' and ')}",
                                "Sample can not be merged with existing data, because merging criteria is incompatible.")
                    }
                }
            }
        }
    }

    private boolean hasNonWithdrawnSeqTracks(MergingWorkPackage mergingWorkPackage) {
        return mergingWorkPackage.seqTracks.find {
            !it.withdrawn
        }
    }

    protected AntibodyTarget findAntibodyTarget(ValueTuple valueTuple, SeqType seqType) {
        String antibodyTargetName = valueTuple.getValue(ANTIBODY_TARGET.name()) ?: ""
        if (seqType.hasAntibodyTarget && antibodyTargetName) {
            return antibodyTargetService.findByNameOrImportAlias(antibodyTargetName)
        }
        return null
    }

    protected SeqPlatform findSeqPlatform(ValueTuple valueTuple) {
        return seqPlatformService.findSeqPlatform(
                valueTuple.getValue(INSTRUMENT_PLATFORM.name()),
                valueTuple.getValue(INSTRUMENT_MODEL.name()),
                valueTuple.getValue(SEQUENCING_KIT.name())
        )
    }

    private Sample findExistingSampleForValueTuple(ValueTuple valueTuple) {
        String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
        if (sampleIdentifier) {
            return sampleIdentifier.sample
        }

        Project project = metadataImportService.getProjectFromMetadata(valueTuple)
        if (!project) {
            return
        }

        ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
        if (!parsedSampleIdentifier) {
            return
        }

        return Sample.createCriteria().get {
            individual {
                eq("pid", parsedSampleIdentifier.pid)
            }
            sampleType {
                or {
                    ilike("name", SqlUtil.replaceWildcardCharactersInLikeExpression(parsedSampleIdentifier.sampleTypeDbName))
                    ilike("name", SqlUtil.replaceWildcardCharactersInLikeExpression(parsedSampleIdentifier.sampleTypeDbName.replace('_', '-')))
                }
            }
        } as Sample
    }
}
