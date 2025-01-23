/*
 * Copyright 2011-2025 The OTP authors
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

import grails.converters.JSON
import grails.validation.Validateable
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrainService

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ReferenceGenomeController implements CheckAndCall {

    static allowedMethods = [
            index                           : "GET",
            changeReferenceGenomeLegacyState: "POST",
            updateName                      : "POST",
            updateFileNamePrefix            : "POST",
            updateSpecies                   : "POST",
            updateSpeciesWithStrain         : "POST",
            updateLength                    : "POST",
            updateLengthWithoutN            : "POST",
            updatePath                      : "POST",
            updateChromosomePrefix          : "POST",
            updateChromosomeSuffix          : "POST",
            updateFingerPrintingFileName    : "POST",

    ]
    ReferenceGenomeService referenceGenomeService
    SpeciesWithStrainService speciesWithStrainService

    def index() {
        return [
                referenceGenomes    : referenceGenomeService.list(),
                allSpeciesWithStrain: speciesWithStrainService.list().sort {
                    a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.displayName, b.displayName)
                } ?: [],
                allSpecies          : speciesWithStrainService.allSpecies().sort {
                    a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
                } ?: [],
        ]
    }

    def changeReferenceGenomeLegacyState(ReferenceGenomeLegacyCommand cmd) {
        checkErrorAndCallMethodWithFlashMessage(cmd, "dataFields.legacy") {
            referenceGenomeService.changeLegacyState(cmd.referenceGenome, cmd.legacy)
        }
        redirect action: 'index'
    }

    def updateName(UpdateReferenceGenomeCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            referenceGenomeService.updateNameValue(cmd.id, cmd.value)
        }
    }

    def updateFileNamePrefix(UpdateReferenceGenomeCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            referenceGenomeService.updateFileNamePrefixValue(cmd.id, cmd.value)
        }
    }

    def updateSpecies(UpdateReferenceGenomeListCommand cmd) {
        checkErrorAndCallMethodReturns(cmd) {
            referenceGenomeService.updateSpeciesValue(cmd.id, cmd.selectedValuesList as Set)
            render([:] as JSON)
        }
    }

    def updateSpeciesWithStrain(UpdateReferenceGenomeListCommand cmd) {
        checkErrorAndCallMethodReturns(cmd) {
            referenceGenomeService.updateSpeciesWithStrainValue(cmd.id, cmd.selectedValuesList as Set)
            render([:] as JSON)
        }
    }

    def updateLength(UpdateReferenceGenomeLongCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            referenceGenomeService.updateLengthValue(cmd.id, cmd.value)
        }
    }

    def updateLengthWithoutN(UpdateReferenceGenomeLongCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            referenceGenomeService.updateLengthWithoutNValue(cmd.id, cmd.value)
        }
    }

    def updatePath(UpdateReferenceGenomeCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            referenceGenomeService.updatePathValue(cmd.id, cmd.value)
        }
    }

    def updateChromosomePrefix(UpdateReferenceGenomeCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            referenceGenomeService.updateChromosomePrefixValue(cmd.id, cmd.value)
        }
    }

    def updateChromosomeSuffix(UpdateReferenceGenomeCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            referenceGenomeService.updateChromosomeSuffixValue(cmd.id, cmd.value)
        }
    }

    def updateFingerPrintingFileName(UpdateReferenceGenomeCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            referenceGenomeService.updateFingerPrintingFileNameValue(cmd.id, cmd.value)
        }
    }
}

class ReferenceGenomeLegacyCommand extends LegacyCommand {
    ReferenceGenome referenceGenome
}

class UpdateReferenceGenomeCommand implements Validateable {
    Long id
    String value
}

class UpdateReferenceGenomeListCommand implements Validateable {
    Long id
    List<String> selectedValuesList

    void setSelectedValues(String value) {
        this.selectedValuesList = JSON.parse(value) as List<String>
    }
}

class UpdateReferenceGenomeLongCommand implements Validateable {
    Long id
    long value
}

