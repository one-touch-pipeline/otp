/*
 * Copyright 2011-2024 The OTP authors
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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.project.ProjectFieldReferenceAble
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.MetadataField
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

/*
 * In the GUI and e-mails sent by OTP this shall be called "Sequencing Type" (or "Seq. Type" where little space is
 * available), no matter if the sequencing read type is included in the displayed value or not.
 * Decided by the OTP Product Owner on 2020-08-20.
 */
/** This table is used externally. Please discuss a change in the team */
@ManagedEntity
class SeqType implements Entity, MetadataField, ProjectFieldReferenceAble {

    final static SINGLE_CELL_DNA = "Single-cell DNA"

    final static SINGLE_CELL_RNA = "Single-cell RNA"

    static final TAGMENTATION_SUFFIX = '_TAGMENTATION'

    static final String SINGLE_CELL_TRUE = "single cell"

    static final String SINGLE_CELL_FALSE = "bulk"

    static final Collection<SeqTypeNames> WGBS_SEQ_TYPE_NAMES = [
            SeqTypeNames.WHOLE_GENOME_BISULFITE,
            SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION,
    ].asImmutable()

    /** This attribute is used externally. Please discuss a change in the team */
    SequencingReadType libraryLayout
    String dirName

    /** This attribute is used externally. Please discuss a change in the team */
    boolean singleCell

    /**
     * Display name used in the GUI.
     *
     * This attribute is used externally. Please discuss a change in the team
     */
    String displayName

    /** name used in roddy config files */
    String roddyName

    /**
     * Indicates, if the SeqType require antibodyTarget
     *
     * This attribute is used externally. Please discuss a change in the team
     */
    boolean hasAntibodyTarget = false

    boolean needsBedFile = false

    static hasMany = [importAlias: String]

    static constraints = {
        List<String> tables = ['libraryLayout', 'singleCell']
        /**
         * One of {@link SeqTypeNames#seqTypeName}.
         * Used in file system paths
         */
        name(blank: false, unique: tables)
        dirName(blank: false, unique: 'libraryLayout', validator: { String val, SeqType obj ->
            if (!OtpPathValidator.isValidPathComponent(val)) {
                return "validator.path.component"
            }
            if (SeqType.findAllByDirNameAndSingleCell(obj.dirName, !obj.singleCell)) {
                return "unique"
            }
            if (SeqType.findAllByNameAndSingleCell(obj.name, obj.singleCell)) {
                if (SeqType.findAllByNameAndSingleCell(obj.name, obj.singleCell).any { it.dirName != obj.dirName }) {
                    return "same"
                }
            }
        })
        displayName(blank: false)
        roddyName(nullable: true, blank: false, validator: {
            !it?.contains('_')  // Roddy has problems with underscores
        })
    }

    static mapping = {
        name index: 'seq_type__name_idx'
        dirName index: 'seq_type__dir_name_idx'
        displayName index: 'seq_type__display_name_idx'
        libraryLayout index: 'seq_type__library_layout_idx'
        singleCell index: 'seq_type__single_cell_idx'
        hasAntibodyTarget index: 'seq_type__has_antibody_target_idx'
    }

    /**
     * Retrieves the unique natural Id (human readable)
     * Should not be changed, since this is stored at the database.
     */
    String getNaturalId() {
        return "${name}_${libraryLayout}"
    }

    String getNameWithLibraryLayout() {
        return "${name} ${libraryLayout}"
    }

    String getDisplayNameWithLibraryLayout() {
        return "${displayName} ${libraryLayout} ${singleCellDisplayName}"
    }

    String getSingleCellDisplayName() {
        return singleCell ? SINGLE_CELL_TRUE : SINGLE_CELL_FALSE
    }

    String getLibraryLayoutDirName() {
        return libraryLayout.name().toLowerCase()
    }

    String getProcessingOptionName() {
        return displayName ?: name
    }

    SeqTypeNames getSeqTypeName() {
        return SeqTypeNames.fromSeqTypeName(name)
    }

    boolean isExome() {
        return name == SeqTypeNames.EXOME.seqTypeName
    }

    boolean isWgbs() {
        return WGBS_SEQ_TYPE_NAMES.contains(seqTypeName)
    }

    boolean isWgs() {
        return name == SeqTypeNames.WHOLE_GENOME.seqTypeName
    }

    boolean isRna() {
        return name == SeqTypeNames.RNA.seqTypeName
    }

    boolean isChipSeq() {
        return name == SeqTypeNames.CHIP_SEQ.seqTypeName
    }

    @Override
    String toString() {
        return displayNameWithLibraryLayout
    }

    @Override
    String getStringForProjectFieldDomainReference() {
        return displayNameWithLibraryLayout
    }
}
