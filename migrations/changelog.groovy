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

databaseChangeLog = {
    include file: 'changelogs/2022/initialDatabaseSchema.groovy'

    include file: 'changelogs/2022/otp-1860-adapt-databasechangelog.groovy'

    include file: 'changelogs/2022/otp-1611-wes-domains.groovy'

    include file: 'changelogs/2022/otp-1612-state-for-workflow-creation.groovy'

    include file: 'changelogs/2022/otp-1135.groovy'

    include file: 'changelogs/2023/otp-1732.groovy'

    include file: 'changelogs/2023/otp-1926-bugfix-wgbs-defaults.groovy'

    include file: 'changelogs/2023/otp-1909.groovy'

    include file: 'changelogs/2023/otp-1919-fix-pancan-defaults.groovy'

    include file: 'changelogs/2023/otp-1871.groovy'

    include file: 'changelogs/2023/otp-1911-remove-fastq-linking.groovy'

    include file: 'changelogs/2023/otp-1549-low_cov.groovy'

    include file: 'changelogs/2023/otp-1940.groovy'

    include file: 'changelogs/2023/otp-1986.groovy'

    include file: 'changelogs/2023/otp-1651.groovy'

    include file: 'changelogs/2023/otp-1651-bugfix-wgbs-defaults.groovy'

    include file: 'changelogs/2023/otp-474-legacy.groovy'

    include file: 'changelogs/2023/otp-1997.groovy'

    include file: 'changelogs/2023/otp-2013-wes-run.groovy'

    include file: 'changelogs/2023/otp-1920.groovy'

    include file: 'changelogs/2022/otp-1811.groovy'

    include file: 'changelogs/2023/otp-2064.groovy'

    include file: 'changelogs/2023/otp-2067-fix-defaults-adapter-trimming.groovy'

    include file: 'changelogs/2023/otp-1882.groovy'

    include file: 'changelogs/2023/otp-2006-uuid.groovy'

    include file: 'changelogs/2023/otp-2044.groovy'

    include file: 'changelogs/2023/otp-2047-piUser-department.groovy'

    include file: 'changelogs/2023/otp-1883.groovy'

    include file: 'changelogs/2023/otp-233-fix-wes-tables.groovy'

    include file: 'changelogs/2023/otp-2091-handle-non-unique-libprepkits-for-fragments.groovy'

    include file: 'changelogs/2023/otp-2073-save-final-metadatafile-location.groovy'

    include file: 'changelogs/2023/otp-2123.groovy'

    include file: 'changelogs/2023/otp-1982.groovy'

    include file: 'changelogs/2023/otp-2162-add-not-null-for-workfolder-uuid.groovy'

    include file: 'changelogs/2023/otp-1930.groovy'

    include file: 'changelogs/2023/otp-319.groovy'

    include file: 'changelogs/2023/otp-2158-individual-uuid.groovy'

    include file: 'changelogs/2023/otp-2228-set-rna-bean.groovy'

    include file: 'changelogs/2023/otp-2181-rename-pancancer-to-roddy.groovy'

    include file: 'changelogs/2023/otp-1608-wes-fastq-workflow-test.groovy'

    include file: 'changelogs/2023/otp-2206-create-gui-to-delegate-department-rights.groovy'

    include file: 'changelogs/2023/otp-2172-roddy_alignment_prepare_job.groovy'

    include file: 'changelogs/2023/otp-564.groovy'

    include file: 'changelogs/2023/otp-2210.groovy'

    include file: 'changelogs/2023/otp-2211.groovy'

    include file: 'changelogs/2023/otp-2205.groovy'

    include file: 'changelogs/2023/otp-2048.groovy'

    include file: 'changelogs/2023/otp-2206-add-unique-constraint.groovy'

    include file: 'changelogs/2023/otp-2246.groovy'

    include file: 'changelogs/2023/otp-2173.groovy'

    include file: 'changelogs/2023/otp-2219.groovy'

    include file: 'changelogs/2023/otp-2301-notification.groovy'

    include file: 'changelogs/2023/otp-1890.groovy'

    include file: 'changelogs/2023/otp-2268.groovy'

    include file: 'changelogs/2023/otp-2220.groovy'

    include file: 'changelogs/2023/otp-1465.groovy'

    include file: 'changelogs/2023/otp-2333-set-bean-name-bam-import.groovy'

    include file: 'changelogs/2023/otp-2226.groovy'

    include file: 'changelogs/2023/otp-2260.groovy'

    include file: 'changelogs/2023/otp-2304-delete-old-rna-workflow-defaults.groovy'

    include file: 'changelogs/2023/otp-2286-bam-import.groovy'

    include file: 'changelogs/2023/otp-2185.groovy'

    include file: 'changelogs/2023/otp-2230.groovy'

    include file: 'changelogs/2023/otp-2258.groovy'

    include file: 'changelogs/2024/otp-2413.groovy'

    include file: 'changelogs/2024/otp-2362.groovy'

    include file: 'changelogs/2024/otp-2452-rename-ImportProcess-to-BamImportInstance-in-database.groovy'

    include file: 'changelogs/2024/otp-2340.groovy'

    include file: 'changelogs/2024/otp-2385-set-bean-name-analysis.groovy'

    include file: 'changelogs/2024/otp-2385-add-artefact-to-bamFilePairAnalysis.groovy'

    include file: 'changelogs/2024/otp-2382-change-packages.groovy'

    include file: 'changelogs/2024/otp-2448-cache-email.groovy'

    include file: 'changelogs/2024/otp-2372.groovy'

    include file: 'changelogs/2024/otp-2470-add-ticket-to-bamImportInstance.groovy'

    include(file: 'changelogs/2024/otp-2495-change-packages-for-analyses-in-process-parameter.groovy')

    include(file: 'changelogs/2024/otp-2419.groovy')

    include file: 'changelogs/2024/otp-2198.groovy'
}
