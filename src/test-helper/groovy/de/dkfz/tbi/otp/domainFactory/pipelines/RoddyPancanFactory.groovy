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
package de.dkfz.tbi.otp.domainFactory.pipelines

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*

trait RoddyPancanFactory implements IsAlignment, IsRoddy {

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
        return [
                chromosome                   : RoddyQualityAssessment.ALL,
                insertSizeCV                 : 0,
                percentageMatesOnDifferentChr: 0,
                genomeWithoutNCoverageQcBases: 0,
        ]
    }
}
