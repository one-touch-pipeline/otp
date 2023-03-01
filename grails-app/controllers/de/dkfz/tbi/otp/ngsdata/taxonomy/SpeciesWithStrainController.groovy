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
package de.dkfz.tbi.otp.ngsdata.taxonomy

import grails.converters.JSON
import grails.validation.Validateable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.FlashMessage

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class SpeciesWithStrainController implements CheckAndCall {

    SpeciesWithStrainService speciesWithStrainService
    SpeciesService speciesService
    StrainService strainService

    static allowedMethods = [
            index                  : "GET",
            createSpeciesWithStrain: "POST",
            createSpecies          : "POST",
            createStrain           : "POST",
            changeLegacyState      : "POST",
            createImportAlias      : "POST",
    ]

    private Map getRedirectParams() {
        return [
                action: "index",
                params: params.helper ? [helper: params.helper] : [:],
        ]
    }

    JSON createImportAlias(CreateSpeciesImportAliasCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            speciesWithStrainService.addNewAlias(cmd.id, cmd.importAlias)
        }
    }

    def index() {
        List<Species> species = speciesWithStrainService.allSpecies()
        return [
                helperParams               : redirectParams.params,

                cachedCommonName           : flash.speciesCommonName ?: "",
                cachedScientificName       : flash.scientificName ?: "",
                cachedStrainName           : flash.strainName ?: "",

                allSpecies                 : species.sort { it.toString() },
                speciesBySpeciesCommonName : species.groupBy { it.speciesCommonName },
                strains                    : Strain.list().sort { it.toString() },
                speciesCommonNames         : SpeciesCommonName.list().sort { it.toString() },
                speciesWithStrainsBySpecies: SpeciesWithStrain.list().groupBy { it.species },
        ]
    }

    def changeLegacyState(SpeciesLegacyCommand cmd) {
        checkErrorAndCallMethodWithFlashMessage(cmd, "dataFields.legacy") {
            speciesWithStrainService.changeLegacyState(cmd.species, cmd.legacy)
        }
        redirect action: 'index'
    }

    def createSpeciesWithStrain(CreateSpeciesWithStrainCommand cmd) {
        Errors errors = speciesWithStrainService.createSpeciesWithStrain(
                Species.get(cmd.speciesId),
                Strain.get(cmd.strainId)
        )
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.fail") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.succ") as String)
        }
        redirect(redirectParams)
    }

    def createSpecies(CreateSpeciesCommand cmd) {
        Errors errors = speciesService.createSpeciesAndSpeciesCommonName(cmd.speciesCommonName, cmd.scientificName)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.fail") as String, errors)
            flash.speciesCommonName = cmd.speciesCommonName
            flash.scientificName = cmd.scientificName
        } else {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.succ") as String)
        }
        redirect(redirectParams)
    }

    def createStrain(CreateStrainCommand cmd) {
        Errors errors = strainService.createStrain(cmd.newStrainName)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.fail") as String, errors)
            flash.strainName = cmd.newStrainName
        } else {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.succ") as String)
        }
        redirect(redirectParams)
    }
}

class CreateSpeciesWithStrainCommand {
    long speciesId
    long strainId
}

class CreateSpeciesCommand {
    String speciesCommonName
    String scientificName
}

class CreateStrainCommand {
    String newStrainName
}

class LegacyCommand implements Validateable {
    boolean legacy
}

class SpeciesLegacyCommand extends LegacyCommand {
    SpeciesWithStrain species
}

class CreateSpeciesImportAliasCommand {
    Long id
    String importAlias

    static constraints = {
        id(blank: false)
        importAlias(blank: false)
    }
}
