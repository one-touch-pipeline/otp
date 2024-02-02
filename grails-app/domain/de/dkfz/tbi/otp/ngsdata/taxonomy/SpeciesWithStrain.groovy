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
package de.dkfz.tbi.otp.ngsdata.taxonomy

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.*

@ManagedEntity
class SpeciesWithStrain implements Entity, Legacy {
    Species species
    Strain strain

    Set<String> importAlias
    static hasMany = [importAlias: String]

    static constraints = {
        species(nullable: false)
        strain(nullable: false, unique: ['species'])
        importAlias validator: { val, obj ->
            if (val.any { it.contains("+") }) {
                return "plus"
            }
            if (CollectionUtils.atMostOneElement(SpeciesWithStrain.list().findAll { SpeciesWithStrain existingSpecies ->
                existingSpecies.id != obj.id &&
                val*.toLowerCase().any { String importAlias -> importAlias in existingSpecies.importAlias*.toLowerCase() }
            })) {
                return "duplicate"
            }
        }
    }

    String getDisplayName() {
        return this.toString()
    }

    @Override
    String toString() {
        return "${species} [${strain.name}]"
    }
}
