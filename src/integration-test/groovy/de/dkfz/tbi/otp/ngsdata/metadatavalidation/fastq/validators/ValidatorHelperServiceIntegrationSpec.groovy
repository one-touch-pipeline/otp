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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.ValidatorHelperService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrainService
import de.dkfz.tbi.otp.utils.spreadsheet.Cell
import de.dkfz.tbi.otp.utils.spreadsheet.validation.ValueTuple

@Rollback
@Integration
class ValidatorHelperServiceIntegrationSpec extends Specification implements TaxonomyFactory {

    void "test getSpeciesFromMetadata() when species is known to OTP"() {
        given:
        SpeciesWithStrain speciesWithStrainHuman = findOrCreateHumanSpecies()
        SpeciesWithStrain speciesWithStrainMouse = findOrCreateMouseSpecies()
        SpeciesWithStrainService speciesWithStrainService = Mock(SpeciesWithStrainService) {
            _ * it.getByAlias("mouse") >> speciesWithStrainMouse
            _ * it.getByAlias("human") >> speciesWithStrainHuman
        }
        ValidatorHelperService validatorHelperService = new ValidatorHelperService([speciesWithStrainService: speciesWithStrainService])
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SPECIES}\n" +
                        "human+mouse\n",
        )
        Set<Cell> cells = [context.spreadsheet.dataRows.get(0).cells.get(0)] as Set
        ValueTuple valueTuple = new ValueTuple(
                [(MetaDataColumn.SPECIES.name()): 'human+mouse'], cells)

        expect:
        validatorHelperService.getSpeciesFromMetadata(valueTuple) == [speciesWithStrainHuman, speciesWithStrainMouse]
    }

    void "test getSpeciesFromMetadata() when some species is not known to OTP"() {
        given:
        SpeciesWithStrain speciesWithStrainHuman = findOrCreateHumanSpecies()
        SpeciesWithStrainService speciesWithStrainService = Mock(SpeciesWithStrainService) {
            _ * it.getByAlias("human") >> speciesWithStrainHuman
        }
        ValidatorHelperService validatorHelperService = new ValidatorHelperService([speciesWithStrainService: speciesWithStrainService])
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.SPECIES}\n" +
                        "human+bat\n",
        )
        Set<Cell> cells = [context.spreadsheet.dataRows.get(0).cells.get(0)] as Set
        ValueTuple valueTuple = new ValueTuple(
                [(MetaDataColumn.SPECIES.name()): 'human+bat'], cells)

        expect:
        validatorHelperService.getSpeciesFromMetadata(valueTuple) == []
    }
}
