package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

class SoftwareToolService {

    static SoftwareToolIdentifier getBaseCallingTool(String name) {
        return (SoftwareToolIdentifier)SoftwareToolIdentifier.createCriteria().get {
            eq('name', name)
            softwareTool {
                eq('type', SoftwareTool.Type.BASECALLING)
            }
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SoftwareTool getSoftwareTool(long id) {
        return SoftwareTool.get(id)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<String> uniqueSortedListOfSoftwareToolProgramNames() {
        return SoftwareTool.createCriteria().list {
            order("programName", "asc")
            projections { distinct("programName") }
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SoftwareTool> findSoftwareToolsByProgramNameSortedAfterVersion(String programName) {
        return SoftwareTool.findAllByProgramName(programName, [sort: "programVersion"])
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SoftwareTool updateSoftwareTool(Long id, String version) {
        SoftwareTool softwareTool = getSoftwareTool(id)
        softwareTool.programVersion = version
        return assertSave(softwareTool)
    }


    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SoftwareToolIdentifier getSoftwareToolIdentifier(long id) {
        return SoftwareToolIdentifier.get(id)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SoftwareToolIdentifier> findSoftwareToolIdentifiersBySoftwareToolSortedAfterName(SoftwareTool softwareTool) {
        return SoftwareToolIdentifier.findAllBySoftwareTool(softwareTool, [sort: "name"])
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SoftwareToolIdentifier updateSoftwareToolIdentifier(Long id, String alias) {
        SoftwareToolIdentifier softwareToolIdentifier = getSoftwareToolIdentifier(id)
        softwareToolIdentifier.name = alias;
        return assertSave(softwareToolIdentifier)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SoftwareToolIdentifier createSoftwareToolIdentifier(SoftwareTool softwareTool, String alias) {
        SoftwareToolIdentifier softwareToolIdentifier = new SoftwareToolIdentifier(
                name: alias,
                softwareTool: softwareTool
                )
        return assertSave(softwareToolIdentifier)
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
