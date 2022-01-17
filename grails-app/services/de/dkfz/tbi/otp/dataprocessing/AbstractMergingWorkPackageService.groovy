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
package de.dkfz.tbi.otp.dataprocessing

import grails.compiler.GrailsCompileStatic
import grails.compiler.GrailsTypeChecked
import grails.gorm.transactions.Transactional
import groovy.transform.TypeCheckingMode
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.*

@Transactional
@GrailsCompileStatic
class AbstractMergingWorkPackageService {

    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    @GrailsTypeChecked
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<AbstractMergingWorkPackage> findMergingWorkPackage(Individual individual, SeqType seqType, AntibodyTarget antibodyTarget = null) {
        assert individual
        assert seqType
        return AbstractMergingWorkPackage.createCriteria().list {
            sample {
                eq('individual', individual)
            }
            eq('seqType', seqType)
            if (antibodyTarget) {
                eq('antibodyTarget', antibodyTarget)
            } else {
                isNull('antibodyTarget')
            }
        }
    }

    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    @GrailsTypeChecked
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<AbstractMergingWorkPackage> findAllBySampleAndSeqTypeAndAntibodyTarget(Sample sample, SeqType seqType, AntibodyTarget antibodyTarget = null) {
        return AbstractMergingWorkPackage.findAllWhere(
                sample: sample,
                seqType: seqType,
                antibodyTarget: antibodyTarget,
        )
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<AbstractMergingWorkPackage> filterByCategory(List<AbstractMergingWorkPackage> mergingWorkPackages, SampleTypePerProject.Category category) {
        return mergingWorkPackages.findAll {
            SampleTypeService.getCategory(it.project, it.sampleType) == category
        }
    }
}
