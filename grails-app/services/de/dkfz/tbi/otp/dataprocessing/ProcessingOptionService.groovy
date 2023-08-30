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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.utils.CollectionUtils.singleElement

@CompileDynamic
@Transactional
class ProcessingOptionService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProcessingOption createOrUpdate(OptionName name, String value, String type = null, Project project = null) {
        ProcessingOption option = findOption(name, type, project)
        if (option) {
            if (option.value == value) {
                return option
            }
            obsoleteOption(option)
        }
        option = new ProcessingOption(
            name: name,
            type: type,
            project: project,
            value: value,
        )
        assert(option.save(flush: true))
        return option
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void obsoleteOptionByName(OptionName name, String type = null, Project project = null) {
        ProcessingOption option = findOption(name, type, project)
        if (option) {
            if (option.name.necessity == Necessity.REQUIRED && !option.name.deprecated) {
                throw new ProcessingException("Required options can't be obsoleted")
            }
            obsoleteOption(option)
        }
    }

    static ProcessingOption findOption(OptionName name, String type = null, Project project = null) {
        return singleElement(ProcessingOption.findAllWhere(
                name: name,
                type: type,
                project: project,
                dateObsoleted: null
        ), true)
    }

    private void obsoleteOption(ProcessingOption option) {
        option.dateObsoleted = new Date()
        assert(option.save(flush: true))
    }

    String findOptionAsString(OptionName name, String type = null) {
        ProcessingOption option = findOption(name, type)
        return option?.value != null ? option?.value : name.defaultValue
    }

    int findOptionAsInteger(OptionName name, String type = null) {
        return Integer.parseInt(findOptionAsString(name, type))
    }

    long findOptionAsLong(OptionName name, String type = null) {
        return Long.parseLong(findOptionAsString(name, type))
    }

    double findOptionAsDouble(OptionName name, String type = null) {
        return Double.parseDouble(findOptionAsString(name, type))
    }

    boolean findOptionAsBoolean(OptionName name, String type = null) {
        return findOptionAsString(name, type) == "true"
    }

    List<String> findOptionAsList(OptionName name, String type = null) {
        return findOptionAsString(name, type)?.split(',')*.trim()
    }

    @Deprecated
    static String findOptionSafe(OptionName name, String type, Project project) {
        ProcessingOption option = findOption(name, type, project)
        return option?.value != null ? option?.value : name.defaultValue
    }

    /**
     * Return numerical value of the option or default value if option value
     * can not be cast to a number.
     */
    @Deprecated
    static long findOptionAsNumber(OptionName name, String type, Project project) {
        String value = findOptionSafe(name, type, project)
        return value.toLong()
    }

    /**
     * Retrieves all ProcessingOptions.
     * @return List of ProcessingOptions
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<ProcessingOption> listProcessingOptions() {
        return ProcessingOption.findAllByDateObsoletedIsNull()
    }
}
