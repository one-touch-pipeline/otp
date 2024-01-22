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
package de.dkfz.tbi.otp.domainFactory.pipelines

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*

trait RoddyPanCancerFactory implements IsAlignment, IsRoddy {

    @Override
    RoddyBamFile createBamFile(Map properties = [:]) {
        return createRoddyBamFile(properties, RoddyBamFile)
    }

    @Override
    Map getSeqTypeProperties() {
        return [
                name         : SeqTypeNames.WHOLE_GENOME.seqTypeName,
                displayName  : 'WGS',
                dirName      : 'whole_genome_sequencing',
                roddyName    : 'WGS',
                libraryLayout: SequencingReadType.PAIRED,
                singleCell   : false,
        ]
    }

    @Override
    Map getConfigProperties(Map properties) {
        return DomainFactory.createRoddyWorkflowConfigMapHelper(properties)
    }

    @SuppressWarnings('GetterMethodCouldBeProperty')
    @Override
    Class getConfigPerProjectAndSeqTypeClass() {
        return RoddyWorkflowConfig
    }

    @SuppressWarnings('GetterMethodCouldBeProperty')
    @Override
    Class getQaClass() {
        return RoddyMergedBamQa
    }

    @Override
    Map getQaValuesProperties() {
        return qaValuesPropertiesMultipleChromosomes[RoddyQualityAssessment.ALL]
    }

    Map<String, Map<String, Number>> getQaValuesPropertiesMultipleChromosomes(Map properties = [:]) {
        Map map = [
                chromosome8QcBasesMapped     : 1866013,
                percentageMatesOnDifferentChr: 1.551,
        ] + properties
        return [
                "8"  : [
                        "genomeWithoutNCoverageQcBases": 0.011,
                        "referenceLength"              : 14636402211,
                        "chromosome"                   : '8',
                        "qcBasesMapped"                : map.chromosome8QcBasesMapped,
                ],
                (RoddyQualityAssessment.ALL): [
                        "pairedRead1"                    : 2091461,
                        "pairedInSequencing"             : 4213091,
                        "withMateMappedToDifferentChr"   : 336351,
                        "qcFailedReads"                  : 10,
                        "totalReadCounter"               : 4213091,
                        "totalMappedReadCounter"         : 4203691,
                        "genomeWithoutNCoverageQcBases"  : 0.011,
                        "singletons"                     : 10801,
                        "withMateMappedToDifferentChrMaq": 61611,
                        "insertSizeMedian"               : 3991,
                        "insertSizeSD"                   : 931,
                        "pairedRead2"                    : 2121631,
                        "percentageMatesOnDifferentChr"  : map.percentageMatesOnDifferentChr,
                        "chromosome"                     : RoddyQualityAssessment.ALL,
                        "withItselfAndMateMapped"        : 4192891,
                        "qcBasesMapped"                  : map.chromosome8QcBasesMapped,
                        "duplicates"                     : 8051,
                        "insertSizeCV"                   : 231,
                        "referenceLength"                : 30956774121,
                        "properlyPaired"                 : 3847661,
                ],
                "7"  : [
                        "referenceLength"              : 1591386631,
                        "genomeWithoutNCoverageQcBases": 0.011,
                        "qcBasesMapped"                : map.chromosome8QcBasesMapped,
                        "chromosome"                   : '7',
                ],
        ]
    }

    String getQaFileContent(Map properties = [:]) {
        Map map = [
                chromosome8QcBasesMapped     : '1866013',
                percentageMatesOnDifferentChr: '1.551',
        ] + properties

        return """\
{
  "8": {
    "genomeWithoutNCoverageQcBases": 0.011,
    "referenceLength": 14636402211,
    "chromosome": 8,
    "qcBasesMapped": ${map.chromosome8QcBasesMapped}
  },
  "all": {
    "pairedRead1": 2091461,
    "pairedInSequencing": 4213091,
    "withMateMappedToDifferentChr": 336351,
    "qcFailedReads": 10,
    "totalReadCounter": 4213091,
    "totalMappedReadCounter": 4203691,
    "genomeWithoutNCoverageQcBases": 0.011,
    "singletons": 10801,
    "withMateMappedToDifferentChrMaq": 61611,
    "insertSizeMedian": 3991,
    "insertSizeSD": 931,
    "pairedRead2": 2121631,
    "percentageMatesOnDifferentChr": ${map.percentageMatesOnDifferentChr},
    "chromosome": "all",
    "withItselfAndMateMapped": 4192891,
    "qcBasesMapped": ${map.chromosome8QcBasesMapped},
    "duplicates": 8051,
    "insertSizeCV": 231,
    "referenceLength": 30956774121,
    "properlyPaired": 3847661
  },
  "7": {
    "referenceLength": 1591386631,
    "genomeWithoutNCoverageQcBases": 0.011,
    "qcBasesMapped": ${map.chromosome8QcBasesMapped},
    "chromosome": 7
  }
}
"""
    }
}
