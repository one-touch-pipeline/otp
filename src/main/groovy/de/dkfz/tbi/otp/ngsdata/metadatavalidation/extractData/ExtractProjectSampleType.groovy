/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.extractData

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParser
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 * Provide method to extract {@link Project} and {@link SampleType} from {@link ValueTuple} for importing.
 *
 * It check {@link SampleIdentifier} and the {@link SampleIdentifierParser}s
 */
trait ExtractProjectSampleType {

    @Autowired
    SampleIdentifierService sampleIdentifierService

    /**
     * Extract from the columns {@link MetaDataColumn#SAMPLE_NAME} and {@link MetaDataColumn#PROJECT} following values:
     * - project
     * - sampleType
     */
    ProjectSampleType getProjectAndSampleTypeFromMetadata(ValueTuple tuple) {
        String sampleName = tuple.getValue(SAMPLE_NAME.name())
        String projectName = tuple.getValue(PROJECT.name()) ?: ''
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))

        if (sampleIdentifier) {
            return new ProjectSampleType(sampleIdentifier.project, sampleIdentifier.sampleType)
        }
        Project projectFromProjectColumn = Project.getByNameOrNameInMetadataFiles(projectName)
        if (!projectFromProjectColumn) {
            return null
        }
        ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, projectFromProjectColumn)
        if (!parsedSampleIdentifier) {
            return
        }

        SampleType sampleType = SampleType.findSampleTypeByName(parsedSampleIdentifier.sampleTypeDbName)
        if (!sampleType) {
            return
        }
        return new ProjectSampleType(projectFromProjectColumn, sampleType)
    }
}
