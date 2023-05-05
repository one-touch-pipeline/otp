/*
 * Copyright 2011-2022 The OTP authors
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
}
