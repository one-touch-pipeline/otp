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

import de.dkfz.tbi.otp.dataprocessing.AlignmentDeciderBeanName
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.extractData.ExtractProjectSampleType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.extractData.ProjectSampleType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeProjectSeqTypeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class BedFileValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator, ExtractProjectSampleType {

    @Autowired
    LibraryPreparationKitService libraryPreparationKitService

    @Autowired
    ValidatorHelperService validatorHelperService

    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}' and the sequencing layout is '${SequencingReadType.PAIRED}', the correct BED file for the used library preparation kit should be configured in OTP."]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE, SEQUENCING_READ_TYPE, LIB_PREP_KIT, SAMPLE_NAME, PROJECT]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [BASE_MATERIAL.name()]
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

    //LibraryLayout.findByName is not a gorm find, so no findAll available
    @SuppressWarnings("AvoidFindWithoutAll")
    void validateValueTuple(MetadataValidationContext context, ValueTuple valueTuple) {
        String seqType = validatorHelperService.getSeqTypeNameFromMetadata(valueTuple)

        boolean singleCell = SeqTypeService.isSingleCell(valueTuple.getValue(BASE_MATERIAL.name()))

        SequencingReadType libraryLayout = SequencingReadType.getByName(valueTuple.getValue(SEQUENCING_READ_TYPE.name()))

        if (seqType != SeqTypeNames.EXOME.seqTypeName || libraryLayout != SequencingReadType.PAIRED || singleCell) {
            return
        }

        String libraryPreparationKitName = valueTuple.getValue(LIB_PREP_KIT.name())
        if (!libraryPreparationKitName) {
            return
        }
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitService.findByNameOrImportAlias(libraryPreparationKitName)
        if (!libraryPreparationKit) {
            return
        }

        ProjectSampleType projectSampleType = getProjectAndSampleTypeFromMetadata(valueTuple)
        if (!projectSampleType) {
            return
        }

        Project project = projectSampleType.project
        SampleType sampleType = projectSampleType.sampleType

        if (project.alignmentDeciderBeanName == AlignmentDeciderBeanName.NO_ALIGNMENT) {
            return
        }

        ReferenceGenome referenceGenome = ReferenceGenomeProjectSeqTypeService.getConfiguredReferenceGenomeProjectSeqType(
                project,
                SeqTypeService.exomePairedSeqType,
                sampleType,
        )?.referenceGenome
        if (!referenceGenome) {
            return
        }

        BedFile bedFile = CollectionUtils.atMostOneElement(
                BedFile.findAllWhere(
                        libraryPreparationKit: libraryPreparationKit,
                        referenceGenome: referenceGenome,
                )
        )
        if (!bedFile) {
            String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
            context.addProblem(valueTuple.cells, LogLevel.WARNING, "No BED file is configured for sample '${sampleName}' " +
                    "(reference genome '${referenceGenome.name}') with library preparation kit '${libraryPreparationKitName}'.",
                    "No BED file is configured for at least on sample.")
        }
    }
}
