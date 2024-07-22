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

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.SqlUtil

@Transactional
class SoftwareToolService {

    @CompileDynamic
    static SoftwareToolIdentifier getBaseCallingTool(String name) {
        return (SoftwareToolIdentifier) SoftwareToolIdentifier.createCriteria().get {
            ilike('name', SqlUtil.replaceWildcardCharactersInLikeExpression(name))
            softwareTool {
                eq('type', SoftwareTool.Type.BASECALLING)
            }
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    Map<SoftwareTool, List<SoftwareToolIdentifier>> identifiersPerSoftwareTool() {
        return SoftwareToolIdentifier.list(sort: "name", order: "asc").groupBy { it.softwareTool }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    Map<String, List<SoftwareTool>> softwareToolsPerProgramName() {
        return SoftwareTool.findAllByType(SoftwareTool.Type.BASECALLING, [sort: "programVersion", order: "asc"]).groupBy { it.programName }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    SoftwareTool getSoftwareTool(long id) {
        return SoftwareTool.get(id)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    SoftwareTool updateSoftwareTool(Long id, String version, boolean legacy = false) {
        SoftwareTool softwareTool = getSoftwareTool(id)
        softwareTool.programVersion = version
        softwareTool.legacy = legacy
        return softwareTool.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    Errors createSoftwareTool(String programName, String programVersion, SoftwareTool.Type type) {
        SoftwareTool softwareTool = new SoftwareTool(programName: programName, programVersion: programVersion, type: type)
        try {
            softwareTool.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    void changeLegacyState(SoftwareTool softwareTool, boolean legacy = false) {
        SoftwareTool softwareToolUpdate = SoftwareTool.get(softwareTool.id)
        softwareToolUpdate.legacy = legacy
        assert softwareToolUpdate.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    SoftwareToolIdentifier getSoftwareToolIdentifier(long id) {
        return SoftwareToolIdentifier.get(id)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    SoftwareToolIdentifier updateSoftwareToolIdentifier(Long id, String alias) {
        SoftwareToolIdentifier softwareToolIdentifier = getSoftwareToolIdentifier(id)
        softwareToolIdentifier.name = alias
        return softwareToolIdentifier.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    SoftwareToolIdentifier createSoftwareToolIdentifier(SoftwareTool softwareTool, String alias) {
        SoftwareToolIdentifier softwareToolIdentifier = new SoftwareToolIdentifier(
                name: alias,
                softwareTool: softwareTool
        )
        return softwareToolIdentifier.save(flush: true)
    }
}
