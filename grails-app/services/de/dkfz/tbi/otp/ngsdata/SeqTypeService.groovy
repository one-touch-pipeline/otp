package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

class SeqTypeService {

    static List<SeqType> alignableSeqTypes() {
        final List<String> alignableSeqTypeNames = [
            SeqTypeNames.EXOME,
            SeqTypeNames.WHOLE_GENOME,
        ]*.seqTypeName
        List<SeqType> seqTypes = SeqType.findAllByNameInListAndLibraryLayout(alignableSeqTypeNames, SeqType.LIBRARYLAYOUT_PAIRED)
        assert alignableSeqTypeNames.size() == seqTypes.size()
        return seqTypes
    }



    @PreAuthorize("hasPermission(#project, read) or hasRole('ROLE_OPERATOR')")
    List<SeqType> alignableSeqTypesByProject(Project project) {
        return SeqTrack.createCriteria().listDistinct {
            projections {
                groupProperty('seqType')
                'in'('seqType', alignableSeqTypes())
                sample {
                    individual {
                        eq("project", project)
                    }
                }
            }
        }.sort { it.name }
    }

}
