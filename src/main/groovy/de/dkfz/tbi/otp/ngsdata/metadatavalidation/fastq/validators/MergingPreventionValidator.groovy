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

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import de.dkfz.tbi.util.spreadsheet.validation.AbstractValueTuplesValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

/**
 * Validator to check, if some merging would be happen.
 *
 * The following checks are done:
 * - prechecks: Ensure that necessary information are provided in the metadata file. If something miss, the check can not be done
 *   - seqType
 *   - sample: given via registered sample name or via parsable sample name
 *   - seqPlatform
 * - check for MergingWorkPackages:
 *   - Check in OTP, if there already exist an MergingWorkPackage
 *   - if yes,
 *     - if it is a single cell seq type: create a warning
 *     - if it is a bulk seq type: create an warning
 *       - if it is mergeable the warning should say, that it will be merged into the existing
 *       - otherwise: the warning should say, that it won't be aligned and merged because of the incompatible {@link SeqPlatformGroup} or {@link LibraryPreparationKit}
 * - check for lanes of the same sample
 *   - Check in OTP, if there already exist lanes for the sample seqType combination
 *   - if yes,
 *     - if it is a single cell seq type: create an warning
 *     - if it is a bulk seq type: create an warning
 *       - if it is mergeable the warning should say, that it will be merged into the existing
 *       - otherwise: the warning should say, that it won't be aligned and merged because of the incompatible {@link SeqPlatformGroup} or {@link LibraryPreparationKit}
 *
 *  Which data can be merged is defined over the {@link MergingCriteria} using {@link SeqPlatformGroup} and {@link LibraryPreparationKit}.
 *  - if {@link MergingCriteria#useLibPrepKit} is set, the {@link LibraryPreparationKit} have to be match
 *  - if {@link MergingCriteria#useSeqPlatformGroup} is:
 *    - {@link MergingCriteria.SpecificSeqPlatformGroups#IGNORE_FOR_MERGING}: all seqPlatform can be merged
 *    - {@link MergingCriteria.SpecificSeqPlatformGroups#USE_OTP_DEFAULT}: seqPlatform can be merged according the default OTP definition of compatible seqPlatforms
 *    - {@link MergingCriteria.SpecificSeqPlatformGroups#USE_PROJECT_SEQ_TYPE_SPECIFIC}: seqPlatform can be merged according the project and seqType specific definition
 *  - if no MergingCriteria is defined yet, then assumes the default settings for MergingCriteria:
 *    - {@link MergingCriteria#useLibPrepKit} is for wgbs seq types false, otherwise true
 *    - {@link MergingCriteria#useSeqPlatformGroup} is {@link MergingCriteria.SpecificSeqPlatformGroups#USE_OTP_DEFAULT}
 */
@Component
class MergingPreventionValidator extends AbstractValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    MergingPreventionService mergingPreventionService

    @Override
    Collection<String> getDescriptions() {
        return [
                "Check whether a new data would be merged with existing bam files",
                "Check whether a new data could not be merged because of incompatible merging criteria (seqPlatform, library preparation kit)",
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
        return [BASE_MATERIAL, ANTIBODY_TARGET, SEQUENCING_KIT, LIB_PREP_KIT]*.name()
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

    protected void validateValueTuple(MetadataValidationContext context, ValueTuple valueTuple) {
        MergingPreventionDataDto data = mergingPreventionService.parseMetaData(valueTuple)
        if (!data.filledCompletely) {
            return
        }
        mergingPreventionService.checkForMergingWorkPackage(context, valueTuple, data)
        mergingPreventionService.checkForSeqTracks(context, valueTuple, data)
    }
}
