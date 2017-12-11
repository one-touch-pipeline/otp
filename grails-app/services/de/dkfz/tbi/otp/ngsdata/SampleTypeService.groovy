package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.*

/**
 *
 */
class SampleTypeService {

    /**
     * return the used sample types of the project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'read')")
    public List findUsedSampleTypesForProject(Project project) {
        List seq = SeqTrack.createCriteria().list{
            projections {
                sample {
                    individual { eq('project', project) }
                    groupProperty("sampleType")
                }
            }
            'in'("seqType", SeqType.allAnalysableSeqTypes)
        }
        return seq
    }
}

