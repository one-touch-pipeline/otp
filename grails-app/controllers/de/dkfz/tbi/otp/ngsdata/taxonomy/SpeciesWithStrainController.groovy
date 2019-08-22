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

import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage

class SpeciesWithStrainController {

    SpeciesWithStrainService speciesWithStrainService
    SpeciesService speciesService
    CommonNameService commonNameService
    StrainService strainService

    static allowedMethods = [
            index: "GET",
            createSpeciesWithStrain: "POST",
            createSpecies: "POST",
            createStrain: "POST",
    ]

    def index() {
        List<Species> species = Species.list()
        return [
            cachedCommonName: flash.commonName ?: '',
            cachedScientificName: flash.scientificName ?: '',
            cachedStrainName: flash.strainName ?: '',

            allSpecies: species.sort { it.toString() },
            speciesByCommonName: species.groupBy { it.commonName },
            strains: Strain.list().sort { it.toString() },
            commonNames: CommonName.list().sort { it.toString() },
            speciesWithStrainsBySpecies: SpeciesWithStrain.list().groupBy { it.species },
        ]
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
        redirect(action: 'index')
    }

    def createSpecies(CreateSpeciesCommand cmd) {
        Errors errors = speciesService.createSpeciesAndCommonName(cmd.commonNameName, cmd.scientificName)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.fail") as String, errors)
            flash.commonName = cmd.commonNameName
            flash.scientificName = cmd.scientificName
        } else {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.succ") as String)
        }
        redirect(action: 'index')
    }

    def createStrain(CreateStrainCommand cmd) {
        Errors errors = strainService.createStrain(cmd.newStrainName)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.fail") as String, errors)
            flash.strainName = cmd.newStrainName
        } else {
            flash.message = new FlashMessage(g.message(code: "speciesWithStrain.succ") as String)
        }
        redirect(action: 'index')
    }
}

class CreateSpeciesWithStrainCommand {
    long speciesId
    long strainId
}

class CreateSpeciesCommand {
    String commonNameName
    String scientificName
}

class CreateStrainCommand {
    String newStrainName
}
