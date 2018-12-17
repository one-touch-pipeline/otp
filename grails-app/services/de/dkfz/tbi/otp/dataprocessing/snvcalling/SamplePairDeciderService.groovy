package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.compiler.GrailsCompileStatic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.utils.CollectionUtils

@GrailsCompileStatic
class SamplePairDeciderService {

    AbstractMergingWorkPackageService abstractMergingWorkPackageService


    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SamplePair> findOrCreateSamplePairs(Collection<AbstractMergingWorkPackage> mergingWorkPackages) {
        assert mergingWorkPackages != null

        return mergingWorkPackages.collect { AbstractMergingWorkPackage workPackage ->
            findOrCreateSamplePairs(workPackage)
        }.flatten().unique()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SamplePair> findOrCreateSamplePairs(AbstractMergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage

        if (!SeqTypeService.allAlignableSeqTypes.contains(mergingWorkPackage.seqType)) {
            return []
        }

        SampleType.Category category = mergingWorkPackage.sampleType.getCategory(mergingWorkPackage.project)
        if (!category) {
            return []
        }

        SampleType.Category correspondingCategory = category.correspondingCategory()
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

        if (mergingWorkPackage instanceof MergingWorkPackage) {
            otherMergingWorkPackages = abstractMergingWorkPackageService.filterBySequencingPlatformGroupIfAvailable(otherMergingWorkPackages, mergingWorkPackage.seqPlatformGroup)
        }

        switch (category) {
            case SampleType.Category.DISEASE:
                return findOrCreateSamplePairs(mergingWorkPackage, otherMergingWorkPackages)
            case SampleType.Category.CONTROL:
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
            samplePair = SamplePair.createInstance(
                    mergingWorkPackage1: disease,
                    mergingWorkPackage2: control,
            )
            samplePair.save(flush: true)
        }
        return samplePair
    }

}
