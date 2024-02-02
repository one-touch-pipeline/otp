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

import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(COMMAND_LOAD_MODULE_LOADER, "")

processingOptionService.createOrUpdate(COMMAND_ENABLE_MODULE, "module load")

// when updating fastqc, make sure that FastqcDataFilesService.fastqcFileNameWithoutZipSuffix is still correct
processingOptionService.createOrUpdate(COMMAND_FASTQC, "fastqc")
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_FASTQC, "module load fastqc/0.11.5")

processingOptionService.createOrUpdate(COMMAND_GROOVY, 'groovy')
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_GROOVY, 'module load groovy/2.4.15')

processingOptionService.createOrUpdate(COMMAND_ACTIVATION_JAVA, 'module load java/1.8.0_131')

processingOptionService.createOrUpdate(COMMAND_SAMTOOLS, 'samtools')
processingOptionService.createOrUpdate(COMMAND_ACTIVATION_SAMTOOLS, 'module load samtools/1.2')

processingOptionService.createOrUpdate(COMMAND_ACTIVATION_R, "module load R/3.4.0")
