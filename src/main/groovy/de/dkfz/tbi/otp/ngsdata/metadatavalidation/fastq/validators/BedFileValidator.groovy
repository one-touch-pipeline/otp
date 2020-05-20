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
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
class BedFileValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    LibraryPreparationKitService libraryPreparationKitService

    @Autowired
    SampleIdentifierService sampleIdentifierService


    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}' and the sequencing layout is '${LibraryLayout.PAIRED}', the correct BED file for the used library preparation kit should be configured in OTP."]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE, SEQUENCING_READ_TYPE, LIB_PREP_KIT, SAMPLE_ID, PROJECT]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [TAGMENTATION_BASED_LIBRARY, BASE_MATERIAL]*.name()
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

    void validateValueTuple(MetadataValidationContext context, ValueTuple valueTuple) {
        String seqType = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)

        boolean singleCell = SeqTypeService.isSingleCell(valueTuple.getValue(BASE_MATERIAL.name()))

        LibraryLayout libraryLayout = LibraryLayout.findByName(valueTuple.getValue(SEQUENCING_READ_TYPE.name()))

        if (seqType != SeqTypeNames.EXOME.seqTypeName || libraryLayout != LibraryLayout.PAIRED || singleCell) {
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

        String sampleId = valueTuple.getValue(SAMPLE_ID.name())
        String projectName = valueTuple.getValue(PROJECT.name())

        Project project = Project.getByNameOrNameInMetadataFiles(projectName)
        SampleType sampleType

        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleId))
        if (sampleIdentifier) {
            project = sampleIdentifier.project
            sampleType = sampleIdentifier.sampleType
        } else {
            if (!project) {
                return
            }

            ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleId, project)
            if (!parsedSampleIdentifier) {
                return
            }

            sampleType = SampleType.findSampleTypeByName(parsedSampleIdentifier.sampleTypeDbName)
            if (!sampleType) {
                return
            }
        }
        if (project.alignmentDeciderBeanName == AlignmentDeciderBeanName.NO_ALIGNMENT) {
            return
        }

        ReferenceGenome referenceGenome = ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(
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
            context.addProblem(valueTuple.cells, Level.WARNING, "No BED file is configured for sample '${sampleId}' (reference genome '${referenceGenome.name}') with library preparation kit '${libraryPreparationKitName}'.", "No BED file is configured for at least on sample.")
        }
    }
}
