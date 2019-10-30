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
import de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
class MergingPreventionValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    AntibodyTargetService antibodyTargetService
    @Autowired
    SampleIdentifierService sampleIdentifierService
    @Autowired
    SeqTypeService seqTypeService

    @Override
    Collection<String> getDescriptions() {
        return ["Check whether a new sample would be merged with existing samples"]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SAMPLE_ID, SEQUENCING_TYPE, LIBRARY_LAYOUT, PROJECT]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [BASE_MATERIAL, ANTIBODY_TARGET, TAGMENTATION_BASED_LIBRARY]*.name()
    }

    @Override
    void checkMissingRequiredColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) { }

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
        String seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
        LibraryLayout libraryLayout = LibraryLayout.findByName(valueTuple.getValue(LIBRARY_LAYOUT.name()))
        if (!libraryLayout) {
            return
        }
        String baseMaterial = valueTuple.getValue(BASE_MATERIAL.name())
        boolean singleCell = SeqTypeService.isSingleCell(baseMaterial)
        SeqType seqType = seqTypeService.findByNameOrImportAlias(seqTypeName, [libraryLayout: libraryLayout, singleCell: singleCell])

        if (!seqType) {
            return
        }

        String antibodyTargetName = valueTuple.getValue(ANTIBODY_TARGET.name()) ?: ""
        AntibodyTarget antibodyTarget = null
        if (seqType.hasAntibodyTarget && antibodyTargetName) {
            antibodyTarget = antibodyTargetService.findByNameOrImportAlias(antibodyTargetName)
        }

        String sampleId = valueTuple.getValue(SAMPLE_ID.name())
        String projectName = valueTuple.getValue(PROJECT.name())
        Project project = Project.findByName(projectName)
        Sample sample
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleId))
        if (sampleIdentifier) {
            sample = sampleIdentifier.sample
            project = sampleIdentifier.project
        } else {
            if (!project) {
                return
            }
            ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleId, project)
            if (!parsedSampleIdentifier) {
                return
            }
            sample = Sample.createCriteria().get {
                individual {
                    eq("pid", parsedSampleIdentifier.pid)
                }
                sampleType {
                    ilike("name", SqlUtil.replaceWildcardCharactersInLikeExpression(parsedSampleIdentifier.sampleTypeDbName))
                }
            } as Sample

            if (!sample) {
                return
            }
        }

        List<MergingWorkPackage> mergingWorkPackages = MergingWorkPackage.findAllWhere(
                sample: sample,
                seqType: seqType,
                antibodyTarget: antibodyTarget,
        )

        if (mergingWorkPackages) {
            if (seqType in singleCellSeqTypes) {
                context.addProblem(valueTuple.cells, Level.ERROR,
                        "Sample ${sample.displayName} with sequencing type ${seqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples.")
            } else if (seqType in bulkSeqTypes && project.alignmentDeciderBeanName != AlignmentDeciderBeanName.NO_ALIGNMENT) {
                context.addProblem(valueTuple.cells, Level.WARNING,
                        "Sample ${sample.displayName} with sequencing type ${seqType.displayNameWithLibraryLayout} would be automatically merged with existing samples.",
                        "Sample would be automatically merged with existing samples.")
            }
        }
    }
}
