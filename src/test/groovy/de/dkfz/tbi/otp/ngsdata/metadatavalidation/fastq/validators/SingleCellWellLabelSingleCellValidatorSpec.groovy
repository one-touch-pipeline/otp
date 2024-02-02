/*
 * Copyright 2011-2024 The OTP authors
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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

class SingleCellWellLabelSingleCellValidatorSpec extends Specification implements DataTest, DomainFactoryCore {

    private static final String PROJECT_NAME = 'ProjectName'

    private static final String NAME_IN_METADATA_FILES = 'NameInMetadataFiles'

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Project,
        ]
    }

    @Unroll
    void "validate, when base material is '#baseMaterial' and well is '#well', then do not show warnings"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                [
                        [
                                MetaDataColumn.BASE_MATERIAL,
                                MetaDataColumn.SINGLE_CELL_WELL_LABEL,
                        ],
                        [
                                baseMaterial,
                                well,
                        ],
                ]*.join('\t').join('\n')
        )

        when:
        new SingleCellWellLabelSingleCellValidator().validate(context)

        then:
        context.problems.empty

        where:

        baseMaterial            | well
        ''                      | ''
        ''                      | 'abc'
        'abc'                   | ''
        'abc'                   | 'abc'
        SeqType.SINGLE_CELL_DNA | 'abc'
        SeqType.SINGLE_CELL_RNA | 'abc'
    }

    @Unroll
    void "validate, when base material is '#baseMaterial' and well is given via parser, then do not show warnings"() {
        given:
        createProject([
                name               : PROJECT_NAME,
                nameInMetadataFiles: NAME_IN_METADATA_FILES,
        ])

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                [
                        [
                                MetaDataColumn.BASE_MATERIAL,
                                MetaDataColumn.SINGLE_CELL_WELL_LABEL,
                                MetaDataColumn.PROJECT,
                                MetaDataColumn.SAMPLE_NAME,
                        ],
                        [
                                baseMaterial,
                                '',
                                projectName,
                                'id'
                        ],
                ]*.join('\t').join('\n')
        )

        when:
        new SingleCellWellLabelSingleCellValidator([
                sampleIdentifierService: Mock(SampleIdentifierService) {
                    1 * parseSingleCellWellLabel(_, _) >> {
                        'WELL'
                    }
                }
        ]).validate(context)

        then:
        context.problems.empty

        where:
        baseMaterial            | projectName
        SeqType.SINGLE_CELL_DNA | PROJECT_NAME
        SeqType.SINGLE_CELL_RNA | PROJECT_NAME
        SeqType.SINGLE_CELL_DNA | NAME_IN_METADATA_FILES
        SeqType.SINGLE_CELL_RNA | NAME_IN_METADATA_FILES
    }

    @Unroll
    void "validate, when base material is '#baseMaterial' and well is '#well' and project is '#projectName' and sampleName is '#sampleName', then create warning"() {
        given:
        createProject([
                name: PROJECT_NAME,
        ])

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                [
                        [
                                MetaDataColumn.BASE_MATERIAL,
                                MetaDataColumn.SINGLE_CELL_WELL_LABEL,
                                MetaDataColumn.PROJECT,
                                MetaDataColumn.SAMPLE_NAME,
                        ],
                        [
                                baseMaterial,
                                '',
                                projectName,
                                'id'
                        ],
                ]*.join('\t').join('\n')
        )

        when:
        new SingleCellWellLabelSingleCellValidator([
                sampleIdentifierService: Mock(SampleIdentifierService) {
                    _ * parseSingleCellWellLabel(_, _) >> {
                        null
                    }
                }
        ]).validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, ['A2', 'B2', 'C2', 'D2'])
        problem.message.contains(SingleCellWellLabelSingleCellValidator.WARNING_MESSAGE)

        where:
        baseMaterial            | well | projectName  | sampleName | parseCount
        SeqType.SINGLE_CELL_DNA | ''   | ''           | ''         | 0
        SeqType.SINGLE_CELL_DNA | ''   | ''           | 'id'       | 0
        SeqType.SINGLE_CELL_DNA | ''   | 'unknown'    | 'id'       | 0
        SeqType.SINGLE_CELL_DNA | ''   | PROJECT_NAME | ''         | 0
        SeqType.SINGLE_CELL_DNA | ''   | PROJECT_NAME | 'id'       | 1
    }
}
