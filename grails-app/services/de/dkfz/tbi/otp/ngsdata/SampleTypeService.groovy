package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

/**
 *
 */
class SampleTypeService {

    /**
     * return the used whole genome and exome paired sample types of the project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
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

