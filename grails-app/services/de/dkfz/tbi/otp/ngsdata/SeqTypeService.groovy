package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CollectionUtils
import org.springframework.security.access.prepost.PreAuthorize

class SeqTypeService extends MetadataFieldsService<SeqType> {

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

    public static boolean hasSeqTypeByNameOrDisplayName(String nameOrDisplayName) {
        assert nameOrDisplayName: "the input nameOrDisplayName '${nameOrDisplayName}' is null"
        return SeqType.findByNameOrDisplayName(nameOrDisplayName, nameOrDisplayName)
    }

    public static boolean isSingleCell(String baseMaterial) {
        if (baseMaterial in [SeqType.SINGLE_CELL_DNA, SeqType.SINGLE_CELL_RNA]) {
            return true
        }
        return false
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void createMultiple(String name, List<String> libraryLayouts, Map properties = [:], List<String> importAliases = []) {
        assert libraryLayouts: "the input libraryLayout must not be empty"
        assert !libraryLayouts.findAll({
            it != SeqType.LIBRARYLAYOUT_SINGLE && it != SeqType.LIBRARYLAYOUT_PAIRED && it != SeqType.LIBRARYLAYOUT_MATE_PAIR
        }): "the input libraryLayout has invalid values"

        if (libraryLayouts.contains(SeqType.LIBRARYLAYOUT_SINGLE)) {
            create(name, properties + [libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE], importAliases)

        }
        if (libraryLayouts.contains(SeqType.LIBRARYLAYOUT_PAIRED)) {
            create(name, properties + [libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED], importAliases)

        }
        if (libraryLayouts.contains(SeqType.LIBRARYLAYOUT_MATE_PAIR)) {
            create(name, properties + [libraryLayout: SeqType.LIBRARYLAYOUT_MATE_PAIR], importAliases)

        }
    }


    @Override
    public void getTAndAddAlias(String name, String importAlias) {
        List<SeqType> seqTypes = []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE, singleCell: false]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE, singleCell: true]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED, singleCell: false]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED, singleCell: true]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SeqType.LIBRARYLAYOUT_MATE_PAIR, singleCell: false]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: SeqType.LIBRARYLAYOUT_MATE_PAIR, singleCell: true]) ?: []
        assert seqTypes: "No ${clazz} with name or importAlias ${name} exists"

        seqTypes.each { SeqType seqType ->
            if (seqType) {
                assert !seqType.importAlias.contains(importAlias): "the importAlias was already created"
                seqType.importAlias.add(importAlias)
                assert seqType.save(flush: true)
            }
        }
    }

    @Override
    protected SeqType findByName(String name, Map properties = [:]) {
        if (properties.libraryLayout && properties.singleCell != null) {
            return CollectionUtils.<SeqType> atMostOneElement(clazz.findAllByNameIlikeAndLibraryLayoutAndSingleCell(name, properties.libraryLayout, properties.singleCell)) ?:
                    CollectionUtils.<SeqType> atMostOneElement(clazz.findAllByDisplayNameIlikeAndLibraryLayoutAndSingleCell(name, properties.libraryLayout, properties.singleCell))
        } else if (!properties.libraryLayout && properties.singleCell != null) {
            return clazz.findByNameIlikeAndSingleCell(name, properties.singleCell) ?:
                    clazz.findByDisplayNameIlikeAndSingleCell(name, properties.singleCell)
        } else {
            return clazz.findByName(name) ?: clazz.findByDisplayName(name)
        }
    }

    @Override
    protected SeqType findByImportAlias(String importAlias, Map properties = [:]) {
        if (properties.libraryLayout && properties.singleCell != null) {
            return CollectionUtils.<SeqType> atMostOneElement(clazz.list().findAll {
                it.importAlias*.toLowerCase()?.contains(importAlias.toLowerCase()) && it.libraryLayout == properties.libraryLayout && it.singleCell == properties.singleCell
            })
        } else {
            return clazz.list().find {
                it.importAlias*.toLowerCase()?.contains(importAlias.toLowerCase())
            }
        }
    }

    @Override
    protected void checkProperties(Map properties) {
        assert properties.dirName: "the input dirname must not be null"
        assert properties.libraryLayout: "the input libraryLayout must not be null"
        assert properties.displayName: "the input displayName must not be null"
        assert properties.singleCell != null: "the input singleCell must not be null"
        assert !findByNameOrImportAlias(properties.displayName, properties): "The SeqType with displayname'${properties.displayName}' exists already"
    }

    @Override
    protected Class getClazz() {
        return SeqType
    }
}
