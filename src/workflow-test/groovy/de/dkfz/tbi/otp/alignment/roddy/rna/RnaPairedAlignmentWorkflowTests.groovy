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
package de.dkfz.tbi.otp.alignment.roddy.rna

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackageAlignmentProperty
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

class RnaPairedAlignmentWorkflowTests extends AbstractRnaAlignmentWorkflowTests {

    @Override
    SeqType findSeqType() {
        DomainFactory.createRnaPairedSeqType()
    }

    @Override
    void createProjectConfigRna(MergingWorkPackage workPackage, Map configOptions = [:], Map referenceGenomeConfig = [:]) {
        super.createProjectConfigRna(workPackage, configOptions, referenceGenomeConfig)

        //multithreading of FEATURE_COUNT_CORES does not work for the test data (anymore), therefore it is set to one thread
        //since in production it does not make problems, it is only changed for the test
        MergingWorkPackageAlignmentProperty mergingWorkPackageAlignmentProperty = new MergingWorkPackageAlignmentProperty(name: 'FEATURE_COUNT_CORES', value: '1', mergingWorkPackage: workPackage)
        workPackage.alignmentProperties.add(mergingWorkPackageAlignmentProperty)
        workPackage.save()
    }
}
