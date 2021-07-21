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
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
class MergingConflictsValidator extends MergingPreventionValidator {
    @Override
    Collection<String> getDescriptions() {
        return [
                "Check whether a new sample could not be merged with other new samples because of incompatible sequencing platforms",
        ]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.groupBy { values ->
            SeqType seqType = metadataImportService.getSeqTypeFromMetadata(values)
            new MwpDetermingValues(
                    getSampleIdentifier(values),
                    getSampleType(values),
                    seqType,
                    seqType ? findAntibodyTarget(values, seqType) : null,
            )
        }.findAll { key, values ->
            key.individual && key.sampleType && key.seqType
        }.findAll { key, values ->
            values.collect { ValueTuple valueTuple ->
                findSeqPlatformGroup(valueTuple, key.seqType) ?: findSeqPlatform(valueTuple)
            }.unique().size() > 1
        }.each { key, values ->
            context.addProblem(values*.cells.flatten() as Set<Cell>, LogLevel.WARNING,
                    "Sample ${key.individual} ${key.sampleType} with sequencing type ${key.seqType.displayNameWithLibraryLayout} cannot be merged with itself, " +
                            "since it uses incompatible seq platforms",
                    "Sample can not be merged with itself, since it uses incompatible seq platforms."
            )
        }
    }

    SeqPlatformGroup findSeqPlatformGroup(ValueTuple valueTuple, SeqType seqType) {
        findSeqPlatform(valueTuple).getSeqPlatformGroupForMergingCriteria(
                metadataImportService.getProjectFromMetadata(valueTuple),
                seqType,
        )
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

    @Canonical
    @TupleConstructor
    class MwpDetermingValues {
        String individual
        String sampleType
        SeqType seqType
        AntibodyTarget antibodyTarget
    }
}
