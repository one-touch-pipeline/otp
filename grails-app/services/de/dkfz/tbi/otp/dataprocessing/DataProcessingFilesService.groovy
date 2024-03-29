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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.Individual

import java.nio.file.Path

@Transactional
@Deprecated // legacy data
class DataProcessingFilesService {

    ConfigService configService
    FileSystemService fileSystemService

    @Deprecated // legacy data
    enum OutputDirectories {
        @Deprecated // legacy data
        BASE,

        /** @deprecated legacy data */
        ALIGNMENT,

        /** @deprecated legacy data */
        MERGING,

        /** @deprecated legacy data */
        COVERAGE,

        /** @deprecated legacy data */
        FASTX_QC,

        /** @deprecated legacy data */
        FLAGSTATS,

        /** @deprecated legacy data */
        INSERTSIZE_DISTRIBUTION,

        /** @deprecated legacy data */
        SNPCOMP,

        /** @deprecated legacy data */
        STRUCTURAL_VARIATION
    }

    @Deprecated // legacy data
    Path getOutputDirectory(Individual individual, OutputDirectories dir) {
        Path p = fileSystemService.remoteFileSystem.getPath(
                configService.processingRootPath.absolutePath,
                individual.project.dirName,
                'results_per_pid',
                individual.pid,
        )
        if (!dir || dir == OutputDirectories.BASE) {
            return p
        }
        return p.resolve(dir.toString().toLowerCase())
    }
}
