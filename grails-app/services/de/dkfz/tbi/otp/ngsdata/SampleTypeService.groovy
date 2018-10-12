package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import org.springframework.security.access.prepost.*


class SampleTypeService {

    /**
     * return the used sample types of the project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List findUsedSampleTypesForProject(Project project) {
        List<SeqType> allAnalysableSeqTypes = SeqTypeService.allAnalysableSeqTypes

        List<SampleType> sampleTypes = SeqTrack.createCriteria().list{
            projections {
                sample {
                    individual {
                        eq('project', project)
                    }
                    groupProperty("sampleType")
                }
            }
            'in'("seqType", allAnalysableSeqTypes)
        }

        sampleTypes.addAll(AbstractMergingWorkPackage.createCriteria().list{
            projections {
                sample {
                    individual { eq('project', project) }
                    groupProperty("sampleType")
                }
            }
            'in'("seqType", allAnalysableSeqTypes)
        })

        return sampleTypes.unique().sort { it.name }
    }
}

