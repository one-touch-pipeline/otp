package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CollectionUtils
import org.springframework.security.access.prepost.PreAuthorize

class SeqTypeService {

    @Deprecated
    /** @deprecated Use {@link SeqType#getAllAlignableSeqTypes()}, {@link SeqType#getDefaultOtpAlignableSeqTypes()} or {@link SeqType#getRoddyAlignableSeqTypes()} instead. */
    static List<SeqType> alignableSeqTypes() {
        final List<String> alignableSeqTypeNames = [
            SeqTypeNames.EXOME,
            SeqTypeNames.WHOLE_GENOME,
        ]*.seqTypeName
        List<SeqType> seqTypes = SeqType.findAllByNameInListAndLibraryLayoutAndSingleCell(alignableSeqTypeNames, SeqType.LIBRARYLAYOUT_PAIRED, false)
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
    public SeqType createSeqType(String name, String dirName, String displayName, Set<String> alias, String layout, boolean singleCell){
        assert name : "the input name '${name}' must not be null"
        assert dirName  : "the input dirname '${dirName}' must not be null"
        assert layout  : "the input layout '${layout}' must not be null"
        assert displayName  : "the input displayName '${displayName}' must not be null"
        assert !SeqType.findByNameAndLibraryLayoutAndSingleCell(name, layout, singleCell): "the SeqType name '${name}', layout '${layout}' and Single Cell '${singleCell}' already exists"
        SeqType seqType = new SeqType(
                name: name,
                dirName: dirName,
                displayName: displayName,
                libraryLayout: layout,
                alias: alias,
                singleCell: singleCell
        )
        assert seqType.save(flush: true, failOnError: true)
        return seqType
    }

    public static boolean hasSeqTypeByNameOrDisplayNameOrAliasAndSingleCell(String nameOrDisplayNameOrAlias, boolean singleCell) {
        assert nameOrDisplayNameOrAlias: "the input nameOrDisplayNameOrAlias '${nameOrDisplayNameOrAlias}' is null"
        return SeqType.createCriteria().list {
            or {
                eq("name", nameOrDisplayNameOrAlias)
                eq("displayName", nameOrDisplayNameOrAlias)
            }
            eq("singleCell", singleCell)
        } || SeqType.findAllBySingleCell(singleCell)*.alias.flatten().contains(nameOrDisplayNameOrAlias)
    }

    public static addNewAliasToSeqType(String seqTypeName, String alias) {
        assert seqTypeName : "the input seqTypeName must not be null"
        assert alias : "the input alias must not be null"

        List<SeqType> seqTypes = SeqType.findAllByName(seqTypeName)
        Set<String> allAliases = SeqType.findAll()*.alias.flatten() as Set<String>
        assert !allAliases.contains(alias) : "the alias was already created"

        seqTypes*.alias.each { it.add(alias) }
        assert seqTypes*.save(flush: true)
    }

    public static SeqType findSeqTypeByNameOrAlias(String nameOrAlias) {
        assert nameOrAlias: "the input 'nameOrAlias' is null"

        SeqType seqType = SeqType.findByName(nameOrAlias)

        if (seqType) {
            return seqType
        }

        return SeqType.list().find {
            it.alias.contains(nameOrAlias)
        }
    }

    public static SeqType findSeqTypeByNameOrAliasAndLibraryLayoutAndSingleCell(String nameOrAlias, String libraryLayout, boolean singleCell) {
        assert nameOrAlias: "the input 'nameOrAlias' is null"

        SeqType seqType = SeqType.findByNameAndLibraryLayoutAndSingleCell(nameOrAlias, libraryLayout, singleCell)

        if (seqType) {
            return seqType
        }

        return CollectionUtils.atMostOneElement(SeqType.list().findAll {
            it.alias?.contains(nameOrAlias) && it.libraryLayout == libraryLayout && it.singleCell == singleCell
        })

    }

    public static boolean isSingleCell(String baseMaterial){
        if (baseMaterial in [SeqType.SINGLE_CELL_DNA, SeqType.SINGLE_CELL_RNA]) {
            return true
        }
        return false
    }
}
