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

import groovy.transform.Canonical
import groovy.transform.TupleConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
class MergingConflictsValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    MetadataImportService metadataImportService

    @Autowired
    MergingPreventionService mergingPreventionService

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @Override
    Collection<String> getDescriptions() {
        return [
                "Check whether a new sample could not be merged with other new samples because of incompatible sequencing platforms",
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
        valueTuples.groupBy { values ->
            SeqType seqType = metadataImportService.getSeqTypeFromMetadata(values)

            return new MwpDetermingValues(
                    getSampleIdentifier(values),
                    getSampleType(values),
                    seqType,
                    seqType ? mergingPreventionService.findAntibodyTarget(values, seqType) : null,
                    getMergingCriteria(values, seqType),
            )
        }.findAll { key, values ->
            key.individual && key.sampleType && key.seqType
        }.findAll { key, values ->
            values.collect { ValueTuple valueTuple ->
                findSeqPlatformGroup(valueTuple, key.seqType) ?: mergingPreventionService.findSeqPlatform(valueTuple)
            }.unique().size() > 1
        }.each { key, values ->
            if (key.mergingCriteria == null || key.mergingCriteria.useSeqPlatformGroup != MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING) {
                context.addProblem(values*.cells.flatten() as Set<Cell>, LogLevel.WARNING,
                        "Sample ${key.individual} ${key.sampleType} with sequencing type ${key.seqType.displayNameWithLibraryLayout} cannot be merged with itself, " +
                                "since it uses incompatible seq platforms",
                        "Sample can not be merged with itself, since it uses incompatible seq platforms."
                )
            }
        }
    }

    SeqPlatformGroup findSeqPlatformGroup(ValueTuple valueTuple, SeqType seqType) {
        SeqPlatform seqPlatform = mergingPreventionService.findSeqPlatform(valueTuple)
        return seqPlatform ? seqPlatform.getSeqPlatformGroupForMergingCriteria(
                metadataImportService.getProjectFromMetadata(valueTuple),
                seqType,
        ) : null
    }

    String getSampleIdentifier(ValueTuple valueTuple) {
        String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
        if (sampleIdentifier) {
            return sampleIdentifier.individual.pid
        }

        Project project = metadataImportService.getProjectFromMetadata(valueTuple)
        if (!project) {
            return
        }

        ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
        if (!parsedSampleIdentifier) {
            return
        }
        return parsedSampleIdentifier.pid
    }

    String getSampleType(ValueTuple valueTuple) {
        String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
        if (sampleIdentifier) {
            return sampleIdentifier.sample.sampleType.displayName
        }

        Project project = metadataImportService.getProjectFromMetadata(valueTuple)
        if (!project) {
            return
        }

        ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
        if (!parsedSampleIdentifier) {
            return
        }

        return parsedSampleIdentifier.sampleTypeDbName
    }

    MergingCriteria getMergingCriteria(ValueTuple valueTuple, SeqType seqType) {
        Project project = metadataImportService.getProjectFromMetadata(valueTuple)
        return atMostOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType))
    }

    @Canonical
    @TupleConstructor
    class MwpDetermingValues {
        String individual
        String sampleType
        SeqType seqType
        AntibodyTarget antibodyTarget
        MergingCriteria mergingCriteria
    }
}
