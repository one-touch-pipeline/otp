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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class LibPrepKitSeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqTypeService seqTypeService

    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is ${SeqTypeService.seqTypesRequiredLibPrepKit*.nameWithLibraryLayout.join(' or ')}, the library preparation kit must not be empty."]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE, LIBRARY_LAYOUT]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [LIB_PREP_KIT, TAGMENTATION_BASED_LIBRARY, BASE_MATERIAL]*.name()
    }

    @Override
    void checkMissingRequiredColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        List<SeqType> seqTypes = SeqTypeService.getSeqTypesRequiredLibPrepKit()
        valueTuples.each { ValueTuple valueTuple ->
            String seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            LibraryLayout libraryLayout = LibraryLayout.findByName(valueTuple.getValue(LIBRARY_LAYOUT.name()))
            if (!libraryLayout) {
                return
            }
            String baseMaterial = valueTuple.getValue(BASE_MATERIAL.name())
            boolean singleCell = SeqTypeService.isSingleCell(baseMaterial)
            if (seqTypeName.isEmpty()) {
                return
            }
            SeqType seqType = seqTypeService.findByNameOrImportAlias(seqTypeName, [libraryLayout: libraryLayout, singleCell: singleCell])
            if (seqType in seqTypes && !valueTuple.getValue(LIB_PREP_KIT.name())) {
                context.addProblem(valueTuple.cells, Level.ERROR, "If the sequencing type is '${seqType.nameWithLibraryLayout}'" +
                        ", the library preparation kit must be given.")
            }
        }
    }
}
