/*
 * Copyright 2011-2025 The OTP authors
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

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.ValidatorHelperService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.spreadsheet.validation.AbstractValueTuplesValidator
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.ValueTuple

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.BASE_MATERIAL
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.INSTRUMENT_MODEL
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.INSTRUMENT_PLATFORM
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_KIT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_READ_TYPE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_TYPE

@Component
class GroupSeqPlatformValidator extends AbstractValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    private final ValidatorHelperService validatorHelperService

    GroupSeqPlatformValidator(final ValidatorHelperService validatorHelperService) {
        this.validatorHelperService = validatorHelperService
    }

    @Override
    Collection<String> getDescriptions() {
        return [
                "Check whether a sequencing platform belongs to a group of sequencing platforms that are allowed to be merged for a project.",
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(final MetadataValidationContext context) {
        return [SAMPLE_NAME, SEQUENCING_TYPE, SEQUENCING_READ_TYPE, PROJECT, INSTRUMENT_PLATFORM, INSTRUMENT_MODEL]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(final MetadataValidationContext context) {
        return [BASE_MATERIAL, SEQUENCING_KIT,]*.name()
    }

    @Override
    void validateValueTuples(final MetadataValidationContext context, final Collection<ValueTuple> valueTuples) {
        valueTuples.each { tuple ->
            SeqType seqType = validatorHelperService.getSeqTypeFromMetadata(tuple)
            SeqPlatform seqPlatform = validatorHelperService.findSeqPlatform(tuple)
            if (!seqType) {
                return
            }
            MergingCriteria mergingCriteria = validatorHelperService.getMergingCriteria(tuple, seqType)

            if (mergingCriteria?.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
                    || mergingCriteria?.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT) {
                SeqPlatformGroup seqPlatformGroup = validatorHelperService.findSeqPlatformGroup(tuple, seqType)
                if (!seqPlatformGroup) {
                    String platformName = seqPlatform?.name ? seqPlatform.name : "Unknown platform"
                    context.addProblem(tuple.cells,
                            LogLevel.ERROR,
                            "'${platformName}' does not belong to any sequencing platform group for the specified project and sequencing type.")
                }
            }
            // For IGNORE_FOR_MERGING, do nothing
        }
    }
}
