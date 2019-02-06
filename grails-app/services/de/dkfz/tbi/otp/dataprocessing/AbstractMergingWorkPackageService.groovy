package de.dkfz.tbi.otp.dataprocessing

import grails.compiler.GrailsCompileStatic
import grails.compiler.GrailsTypeChecked
import groovy.transform.TypeCheckingMode
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.*

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

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<AbstractMergingWorkPackage> filterByCategory(List<AbstractMergingWorkPackage> mergingWorkPackages, SampleType.Category category) {
        return mergingWorkPackages.findAll {
            it.sampleType.getCategory(it.project) == category
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<AbstractMergingWorkPackage> filterBySequencingPlatformGroupIfAvailable(
            List<AbstractMergingWorkPackage> mergingWorkPackages, SeqPlatformGroup seqPlatformGroup) {
        assert seqPlatformGroup
        return mergingWorkPackages.findAll {
            if (it instanceof MergingWorkPackage) {
                it.seqPlatformGroup == seqPlatformGroup
            } else {
                true
            }
        }
    }
}
