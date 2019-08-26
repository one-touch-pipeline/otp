/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.CollectionUtils

class SeqTypeService extends MetadataFieldsService<SeqType> {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<SeqType> alignableSeqTypesByProject(Project project) {
        return SeqTrack.createCriteria().listDistinct {
            projections {
                groupProperty('seqType')
                'in'('seqType', getAllAlignableSeqTypes())
                sample {
                    individual {
                        eq("project", project)
                    }
                }
            }
        }.sort { it.name }
    }

    static boolean hasSeqTypeByNameOrDisplayName(String nameOrDisplayName) {
        assert nameOrDisplayName: "the input nameOrDisplayName '${nameOrDisplayName}' is null"
        return SeqType.findByNameOrDisplayName(nameOrDisplayName, nameOrDisplayName)
    }

    static boolean isSingleCell(String baseMaterial) {
        return baseMaterial in [SeqType.SINGLE_CELL_DNA, SeqType.SINGLE_CELL_RNA]
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createMultiple(String name, List<LibraryLayout> libraryLayouts, Map properties = [:], List<String> importAliases = []) {
        assert libraryLayouts: "the input libraryLayout must not be empty"
        assert !libraryLayouts.findAll({
            it != LibraryLayout.SINGLE && it != LibraryLayout.PAIRED && it != LibraryLayout.MATE_PAIR
        }): "the input libraryLayout has invalid values"

        if (libraryLayouts.contains(LibraryLayout.SINGLE)) {
            create(name, properties + [libraryLayout: LibraryLayout.SINGLE], importAliases)

        }
        if (libraryLayouts.contains(LibraryLayout.PAIRED)) {
            create(name, properties + [libraryLayout: LibraryLayout.PAIRED], importAliases)

        }
        if (libraryLayouts.contains(LibraryLayout.MATE_PAIR)) {
            create(name, properties + [libraryLayout: LibraryLayout.MATE_PAIR], importAliases)

        }
    }


    @Override
    void getTAndAddAlias(String name, String importAlias) {
        List<SeqType> seqTypes = []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: LibraryLayout.SINGLE, singleCell: true]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: LibraryLayout.PAIRED, singleCell: true]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: LibraryLayout.MATE_PAIR, singleCell: false]) ?: []
        seqTypes += findByNameOrImportAlias(name, [libraryLayout: LibraryLayout.MATE_PAIR, singleCell: true]) ?: []
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
            return CollectionUtils.<SeqType> atMostOneElement(
                    clazz.findAllByNameIlikeAndLibraryLayoutAndSingleCell(name, properties.libraryLayout, properties.singleCell)) ?:
                    CollectionUtils.<SeqType> atMostOneElement(
                            clazz.findAllByDisplayNameIlikeAndLibraryLayoutAndSingleCell(name, properties.libraryLayout, properties.singleCell))
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
                it.importAlias*.toLowerCase()?.contains(importAlias.toLowerCase()) &&
                        it.libraryLayout == properties.libraryLayout &&
                        it.singleCell == properties.singleCell
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
        assert properties.libraryLayout instanceof LibraryLayout: "the input libraryLayout has to be of class 'LibraryLayout' but is '${properties.libraryLayout.class}"
        assert properties.displayName: "the input displayName must not be null"
        assert properties.singleCell != null: "the input singleCell must not be null"
        assert !findByNameOrImportAlias(properties.displayName, properties): "The SeqType with displayname'${properties.displayName}' exists already"
    }

    @Override
    protected Class getClazz() {
        return SeqType
    }


    static SeqType getWholeGenomePairedSeqType() {
        return getSingleSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName, LibraryLayout.PAIRED, 'WGS PAIRED not found')
    }

    static SeqType getExomePairedSeqType() {
        return getSingleSeqType(SeqTypeNames.EXOME.seqTypeName, LibraryLayout.PAIRED, 'WES PAIRED not found')
    }

    static SeqType getWholeGenomeBisulfitePairedSeqType() {
        return getSingleSeqType(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, LibraryLayout.PAIRED, 'WGBS PAIRED not found')
    }

    static SeqType getWholeGenomeBisulfiteTagmentationPairedSeqType() {
        return getSingleSeqType(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName, LibraryLayout.PAIRED,
                'WGBS_TAG PAIRED not found')
    }

    static SeqType getRnaPairedSeqType() {
        return getSingleSeqType(SeqTypeNames.RNA.seqTypeName, LibraryLayout.PAIRED, 'RNA PAIRED not found')
    }

    static SeqType getChipSeqPairedSeqType() {
        return getSingleSeqType(SeqTypeNames.CHIP_SEQ.seqTypeName, LibraryLayout.PAIRED, 'CHIP_SEQ PAIRED not found')
    }

    static SeqType getRnaSingleSeqType() {
        return getSingleSeqType(SeqTypeNames.RNA.seqTypeName, LibraryLayout.SINGLE, 'RNA SINGLE not found')
    }

    static SeqType get10xSingleCellRnaSeqType() {
        return getSingleSeqType(SeqTypeNames._10X_SCRNA.seqTypeName, LibraryLayout.PAIRED, true, '10x_scRNA PAIRED not found')
    }

    private static SeqType getSingleSeqType(String seqTypeName, LibraryLayout layout, boolean singleCell = false, String customErrorMessage) {
        CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayoutAndSingleCell(seqTypeName, layout, singleCell), customErrorMessage)
    }

    static List<SeqType> getDefaultOtpAlignableSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getPanCanAlignableSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
                getWholeGenomeBisulfitePairedSeqType(),
                getWholeGenomeBisulfiteTagmentationPairedSeqType(),
                getChipSeqPairedSeqType(),
        ]
    }

    static List<SeqType> getRnaAlignableSeqTypes() {
        return [
                getRnaPairedSeqType(),
                getRnaSingleSeqType(),
        ]
    }

    static List<SeqType> getRoddyAlignableSeqTypes() {
        return [
                getPanCanAlignableSeqTypes(),
                getRnaAlignableSeqTypes(),
        ].flatten()
    }

    static List<SeqType> getCellRangerAlignableSeqTypes() {
        return [
                get10xSingleCellRnaSeqType(),
        ].flatten()
    }

    static List<SeqType> getAllAlignableSeqTypes() {
        return [
                getDefaultOtpAlignableSeqTypes(),
                getRoddyAlignableSeqTypes(),
                getCellRangerAlignableSeqTypes(),
        ].flatten().unique()
    }

    static List<SeqType> getSeqTypesRequiredLibPrepKit() {
        return [
                exomePairedSeqType,
                wholeGenomeBisulfitePairedSeqType,
                wholeGenomeBisulfiteTagmentationPairedSeqType,
                chipSeqPairedSeqType,
                rnaPairedSeqType,
                rnaSingleSeqType,
        ]
    }

    static List<SeqType> getSnvPipelineSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getIndelPipelineSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getSophiaPipelineSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getAceseqPipelineSeqTypes() {
        return [
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getRunYapsaPipelineSeqTypes() {
        return [
                getExomePairedSeqType(),
                getWholeGenomePairedSeqType(),
        ]
    }

    static List<SeqType> getAllAnalysableSeqTypes() {
        return [
                getSnvPipelineSeqTypes(),
                getIndelPipelineSeqTypes(),
                getSophiaPipelineSeqTypes(),
                getAceseqPipelineSeqTypes(),
                getRunYapsaPipelineSeqTypes(),
        ].flatten().unique()
    }

    static List<SeqType> getAllProcessableSeqTypes() {
        return [
                getAllAlignableSeqTypes(),
                getAllAnalysableSeqTypes(),
        ].flatten().unique()
    }

    static List<SeqType> getSeqTypesIgnoringLibraryPreparationKitForMerging() {
        return [
                getWholeGenomeBisulfitePairedSeqType(),
                getWholeGenomeBisulfiteTagmentationPairedSeqType(),
        ].flatten().unique()
    }

    static List<SeqType> getSeqTypesWithAntibodyTarget() {
        return SeqType.findAllByHasAntibodyTarget(true)
    }
}
