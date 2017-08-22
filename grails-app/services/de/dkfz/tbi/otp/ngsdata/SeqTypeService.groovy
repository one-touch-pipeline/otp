package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

class SeqTypeService {

    @Deprecated
    /** @deprecated Use {@link SeqType#getAllAlignableSeqTypes()}, {@link SeqType#getDefaultOtpAlignableSeqTypes()} or {@link SeqType#getRoddyAlignableSeqTypes()} instead. */
    static List<SeqType> alignableSeqTypes() {
        final List<String> alignableSeqTypeNames = [
            SeqTypeNames.EXOME,
            SeqTypeNames.WHOLE_GENOME,
        ]*.seqTypeName
        List<SeqType> seqTypes = SeqType.findAllByNameInListAndLibraryLayout(alignableSeqTypeNames, SeqType.LIBRARYLAYOUT_PAIRED)
        assert alignableSeqTypeNames.size() == seqTypes.size()
        return seqTypes
    }



    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, read)")
    List<SeqType> alignableSeqTypesByProject(Project project) {
        return SeqTrack.createCriteria().listDistinct {
            projections {
                groupProperty('seqType')
                'in'('seqType', SeqType.getAllAlignableSeqTypes())
                sample {
                    individual {
                        eq("project", project)
                    }
                }
            }
        }.sort { it.name }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public SeqType createSeqType(String name, String dirName, String displayName, String layout){
        assert name : "the input name '${name}' must not be null"
        assert dirName  : "the input dirname '${dirName}' must not be null"
        assert layout  : "the input layout '${layout}' must not be null"
        assert displayName  : "the input displayName '${displayName}' must not be null"
        assert !SeqType.findByNameAndLibraryLayout(name, layout) : "the SeqType name '${name}' and layout '${layout}' already exists"
            SeqType seqType = new SeqType(
                    name: name,
                    dirName: dirName,
                    displayName: displayName,
                    libraryLayout: layout
            )
        assert seqType.save(flush: true, failOnError: true)
        return seqType
    }

    public static boolean hasSeqTypeByNameOrDisplayName(String nameOrDisplayName) {
        assert nameOrDisplayName: "the input nameoralias '${nameOrDisplayName}' is null"
        return SeqType.findByNameOrDisplayName(nameOrDisplayName, nameOrDisplayName)
    }

}
