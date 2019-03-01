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

class SampleTypePerProjectService {

    /**
     * @return SampleTypePerProject for a given Project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<SampleTypePerProject> findByProject(Project project) {
        return SampleTypePerProject.findAllByProject(project)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SampleTypePerProject createOrUpdate(Project project, SampleType sampleType, SampleType.Category category) {
        SampleTypePerProject sampleTypePerProject = SampleTypePerProject.findByProjectAndSampleType(project, sampleType)
        if (sampleTypePerProject) {
            sampleTypePerProject.category = category
        } else {
            sampleTypePerProject = new SampleTypePerProject(
                            project: project,
                            sampleType: sampleType,
                            category: category
                            )
        }
        sampleTypePerProject.save()
        return sampleTypePerProject
    }
}
