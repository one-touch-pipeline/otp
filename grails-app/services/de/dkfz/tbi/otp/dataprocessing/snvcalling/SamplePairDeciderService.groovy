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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@GrailsCompileStatic
@Transactional
class SamplePairDeciderService {

    AbstractMergingWorkPackageService abstractMergingWorkPackageService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SamplePair> findOrCreateSamplePairsForProject(Project project) {
        assert project != null

        final String hql = """
            select
                mwp
            from
                AbstractMergingWorkPackage mwp,
                SampleTypePerProject sampleTypePerProject
            where
                mwp.sample.individual.project = :project
                and mwp.seqType in (:seqTypes)
                and sampleTypePerProject.category in (:categories)
                and sampleTypePerProject.project = mwp.sample.individual.project
                and sampleTypePerProject.sampleType = mwp.sample.sampleType
            """

        List<AbstractMergingWorkPackage> mergingWorkPackages = AbstractMergingWorkPackage.executeQuery(hql, [
                project   : project,
                seqTypes  : SeqTypeService.allAnalysableSeqTypes,
                categories: SampleTypePerProject.Category.values().findAll { it.correspondingCategory() },
        ])
        return findOrCreateSamplePairs(mergingWorkPackages)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SamplePair> createSamplePairs(Project project, Set<SampleType> sampleTypes) {
        if (!sampleTypes) {
            return []
        }

        final String hql = """
            select
                mwp
            from
                AbstractMergingWorkPackage mwp
            where
                mwp.sample.individual.project = :project
                and mwp.seqType in :seqTypes
                and mwp.sample.sampleType in :sampleTypes
            """

        List<AbstractMergingWorkPackage> mergingWorkPackages = AbstractMergingWorkPackage.executeQuery(hql, [
                project    : project,
                seqTypes   : SeqTypeService.allAnalysableSeqTypes,
                sampleTypes: sampleTypes,
        ])
        return findOrCreateSamplePairs(mergingWorkPackages)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SamplePair> findOrCreateSamplePairs(Collection<AbstractMergingWorkPackage> mergingWorkPackages) {
        assert mergingWorkPackages != null

        return mergingWorkPackages.collectMany { AbstractMergingWorkPackage workPackage ->
            findOrCreateSamplePairs(workPackage)
        }.unique() as List<SamplePair>
    }

    @SuppressWarnings(['Instanceof', 'ConstantAssertExpression']) //Instanceof: The method needs to do a check depending on MergingWorkPackage
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SamplePair> findOrCreateSamplePairs(AbstractMergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage

        if (!SeqTypeService.allAnalysableSeqTypes.contains(mergingWorkPackage.seqType)) {
            return []
        }

        SampleTypePerProject.Category category = SampleTypeService.getCategory(mergingWorkPackage.project, mergingWorkPackage.sampleType)
        if (!category) {
            return []
        }

        SampleTypePerProject.Category correspondingCategory = category.correspondingCategory()
        if (!correspondingCategory) {
            return []
        }

        List<AbstractMergingWorkPackage> otherMergingWorkPackages = abstractMergingWorkPackageService.findMergingWorkPackage(
                mergingWorkPackage.individual,
                mergingWorkPackage.seqType,
                mergingWorkPackage.antibodyTarget
        )

        otherMergingWorkPackages = abstractMergingWorkPackageService.filterByCategory(otherMergingWorkPackages,
                correspondingCategory)

        switch (category) {
            case SampleTypePerProject.Category.DISEASE:
                return findOrCreateSamplePairs(mergingWorkPackage, otherMergingWorkPackages)
            case SampleTypePerProject.Category.CONTROL:
                return findOrCreateSamplePairs(otherMergingWorkPackages, mergingWorkPackage)
            default:
                assert false: 'should never reached'
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SamplePair> findOrCreateSamplePairs(List<AbstractMergingWorkPackage> diseases, AbstractMergingWorkPackage control) {
        diseases.collect { AbstractMergingWorkPackage disease ->
            return findOrCreateSamplePair(disease, control)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SamplePair> findOrCreateSamplePairs(AbstractMergingWorkPackage disease, List<AbstractMergingWorkPackage> controls) {
        controls.collect { AbstractMergingWorkPackage control ->
            return findOrCreateSamplePair(disease, control)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SamplePair findOrCreateSamplePair(AbstractMergingWorkPackage disease, AbstractMergingWorkPackage control) {
        assert disease
        assert control
        SamplePair samplePair = CollectionUtils.atMostOneElement(SamplePair.createCriteria().list {
            eq('mergingWorkPackage1', disease)
            eq('mergingWorkPackage2', control)
        } as List<SamplePair>)
        if (!samplePair) {
            samplePair = new SamplePair(
                    mergingWorkPackage1: disease,
                    mergingWorkPackage2: control,
            )
            samplePair.save(flush: true)
        }
        return samplePair
    }
}
