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

/**
 * Defines the columns of the metadata file
 */
enum MetaDataColumn {
    FASTQ_FILE,
    MD5,
    CENTER_NAME,
    RUN_ID,
    RUN_DATE,
    LANE_NO,
    SAMPLE_ID,
    SEQUENCING_TYPE,
    ANTIBODY_TARGET,
    ANTIBODY,
    INSTRUMENT_PLATFORM,
    INSTRUMENT_MODEL,
    PIPELINE_VERSION,
    ALIGN_TOOL,
    INSERT_SIZE,
    LIBRARY_LAYOUT,
    WITHDRAWN,
    WITHDRAWN_DATE,
    COMMENT,
    BARCODE,
    LIB_PREP_KIT,
    SEQUENCING_KIT,
    ILSE_NO,
    PROJECT,
    MATE,
    CUSTOMER_LIBRARY,
    SAMPLE_SUBMISSION_TYPE,
    TAGMENTATION_BASED_LIBRARY,
    BASE_MATERIAL,
}
