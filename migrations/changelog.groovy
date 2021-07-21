/*
 * Copyright 2011-2020 The OTP authors
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

    changeSet(author: "klinga", id: "initial_database_schema") {
        sqlFile(path: 'changelogs/2018/initial_database_schema.sql')
    }

    changeSet(author: "kosnac", id: "MWP-ST-NULL-CONSTRAINT") {
        sqlFile(path: 'changelogs/2018/null-constraint-on-mwp-st.sql')
    }

    changeSet(author: "wieset", id: "OTP-2882-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2882.sql')
    }

    changeSet(author: "strubelp", id: "OTP-2879-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2879.sql')
    }

    changeSet(author: "strubelp", id: "OTP-2880-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2880.sql')
    }

    changeSet(author: "klinga", id: "OTP-2862-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2862.sql')
    }

    changeSet(author: "wieset", id: "OTP-2964-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2964.sql')
    }

    changeSet(author: "klinga", id: "OTP-2863-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2863.sql')
    }

    changeSet(author: "wieset", id: "OTP-2965-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2965.sql')
    }

    changeSet(author: "wieset", id: "OTP-2966-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2966.sql')
    }

    changeSet(author: "kosnac", id: "OTP-2941-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2941.sql')
    }

    changeSet(author: "kosnac", id: "OTP-2991-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2991.sql')
    }

    changeSet(author: "wieset", id: "OTP-2967-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2967.sql')
    }

    changeSet(author: "strubelp", id: "OTP-2890-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2890.sql')
    }

    changeSet(author: "gruenj", id: "OTP-2878-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2878.sql')
    }

    changeSet(author: "strubelp", id: "OTP-2648-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2648.sql')
    }

    changeSet(author: "strubelp", id: "OTP-2822-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2822.sql')
    }

    changeSet(author: "gruenj", id: "remove-identifierInRunName-from-database-SQL") {
        sqlFile(path: 'changelogs/2018/remove-identifierInRunName-from-datadase.sql')
    }

    changeSet(author: "strubelp", id: "remove-unique-constraint-SQL") {
        sqlFile(path: 'changelogs/2018/remove-unique-constraint.sql')
    }

    changeSet(author: "borufka", id: "rename-COMMAND_ACTIVATION_RUN_YAPSA_PREFIX") {
        sqlFile(path: 'changelogs/2018/rename-COMMAND_ACTIVATION_RUN_YAPSA_PREFIX-to-COMMAND_ENABLE_MODULE.sql')
    }

    changeSet(author: "kosnac", id: "OTP-2899-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2899.sql')
    }

    changeSet(author: "wieset", id: "OTP-2968-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2968.sql')
    }

    changeSet(author: "klinga", id: "rename-columns-in-merging_criteria-SQL") {
        sqlFile(path: 'changelogs/2018/rename-columns-in-merging_criteria.sql')
    }

    changeSet(author: "strubelp", id: "index-on-datafile-md5sum") {
        sqlFile(path: 'changelogs/2018/index-on-datafile-md5sum.sql')
    }

    changeSet(author: "wieset", id: "rename-submission-to-egaSubmissin") {
        sqlFile(path: 'changelogs/2018/rename-submission-to-egaSubmissin.sql')
    }

    changeSet(author: "kosnac", id: "OTP-2984-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2984.sql')
    }

    changeSet(author: "strubelp", id: "OTP-3036-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-3036.sql')
    }

    changeSet(author: "gruenj", id: "OTP-2784-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-2784.sql')
    }

    changeSet(author: "kosnac", id: "OTP-3060-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-3060.sql')
    }

    changeSet(author: "borufka", id: "OTP-3052-rename-project-copy-file-flag") {
        sqlFile(path: 'changelogs/2019/OTP-3052-rename-project-copy-file-flag.sql')
    }

    include file: 'changelogs/2019/OTP-3075.groovy'

    changeSet(author: "borufka", id: "OTP-3075-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-3075.sql')
    }

    changeSet(author: "kosnac", id: "add-missing-null-constraints") {
        sqlFile(path: 'changelogs/2019/add-missing-null-constraints.sql')
    }

    changeSet(author: "kosnac", id: "match-unique-constraints-between-gorm-and-database") {
        sqlFile(path: 'changelogs/2019/match-unique-constraints-between-gorm-and-database.sql')
    }

    changeSet(author: "kosnac", id: "remove-default-values-from-database") {
        sqlFile(path: 'changelogs/2019/remove-default-values-from-database.sql')
    }

    changeSet(author: "kosnac", id: "set-blank-phabricator-alias-to-null") {
        sqlFile(path: 'changelogs/2019/set-blank-phabricator-alias-to-null.sql')
    }

    changeSet(author: "kosnac", id: "replace-unique-index-with-unique-constraint") {
        sqlFile(path: 'changelogs/2019/replace-unique-index-with-unique-constraint.sql')
    }

    changeSet(author: "wieset", id: "OTP-3139") {
        sqlFile(path: 'changelogs/2019/OTP-3139.sql')
    }

    changeSet(author: "wieset", id: "OTP-2538-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-2538.sql')
    }

    include file: 'changelogs/2019/fix-ega-tables-OTP-2538.groovy'

    include file: 'changelogs/2019/add-unique-constraints-required-by-migration-plugin.groovy'

    include file: 'changelogs/2019/make-anitbody-configureable-in-seqtype.groovy'

    include file: 'changelogs/2019/create-missing-indexes-in-seqtype.groovy'

    include file: 'changelogs/2019/apply-foreign-keys-for-migration-plugin.groovy'

    changeSet(author: "borufka", id: "add-missing-indexes") {
        sqlFile(path: 'changelogs/2019/add-missing-indexes.sql')
    }

    include file: 'changelogs/2019/remove-seqplatform-unique-constraint.groovy'

    changeSet(author: "", id: "OTP-3119-update") {
        sqlFile(path: 'changelogs/2019/OTP-3119-update.sql')
    }
    changeSet(author: "", id: "OTP-3119") {
        sqlFile(path: 'changelogs/2019/OTP-3119.sql')
    }
    changeSet(author: "", id: "OTP-3119-abstract-quality-assessment") {
        sqlFile(path: 'changelogs/2019/OTP-3119-abstract-quality-assessment.sql')
    }
    changeSet(author: "", id: "OTP-3119-cluster-job") {
        sqlFile(path: 'changelogs/2019/OTP-3119-cluster-job.sql')
    }
    changeSet(author: "", id: "OTP-3119-meta-data-entry") {
        sqlFile(path: 'changelogs/2019/OTP-3119-meta-data-entry.sql')
    }
    changeSet(author: "", id: "OTP-3119-parameter") {
        sqlFile(path: 'changelogs/2019/OTP-3119-parameter.sql')
    }
    changeSet(author: "", id: "OTP-3119-processing-step") {
        sqlFile(path: 'changelogs/2019/OTP-3119-processing-step.sql')
    }
    changeSet(author: "", id: "OTP-3119-processing-step-update") {
        sqlFile(path: 'changelogs/2019/OTP-3119-processing-step-update.sql')
    }

    include file: 'changelogs/2019/add-additional-indexes.groovy'

    changeSet(author: "kosnac", id: "OTP-3128-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-3128.sql')
    }

    include file: 'changelogs/2019/addNotificationFlagsToProject.groovy'

    include file: 'changelogs/2019/OTP-3149-index-files.groovy'

    changeSet(author: "", id: "OTP-2980-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-2980.sql')
    }

    changeSet(author: "wieset", id: "OTP-3176-PROJECT-PREFIX") {
        sqlFile(path: 'changelogs/2019/OTP-3176-project-prefix.sql')
    }
    changeSet(author: "wieset", id: "OTP-3176-UPDATE-PROJECT") {
        sqlFile(path: 'changelogs/2019/OTP-3176-update-project.sql')
    }

    include file: 'changelogs/2019/OTP-3220-improve-dta.groovy'

    changeSet(author: "borufka", id: "changeNameProjectType") {
        sqlFile(path: 'changelogs/2019/changeNameProjectType.sql')
    }

    changeSet(author: "strubelp", id: "otp-147") {
        sqlFile(path: 'changelogs/2019/otp-147.sql')
    }

    changeSet(author: "wieset", id: "otp-223") {
        sqlFile(path: 'changelogs/2019/otp-223.sql')
    }

    changeSet(author: "wieset", id: "otp-245") {
        sqlFile(path: 'changelogs/2019/otp-245.sql')
    }

    include file: 'changelogs/2019/otp-160.groovy'

    include file: 'changelogs/2019/delete-changelog.groovy'

    include file: 'changelogs/2019/delete-mde-status-source.groovy'

    include file: 'changelogs/2019/otp-117-create-indexes-for-ega.groovy'

    changeSet(author: "strubelp", id: "otp-149") {
        sqlFile(path: 'changelogs/2019/otp-149.sql')
    }

    changeSet(author: "strubelp", id: "otp-150") {
        sqlFile(path: 'changelogs/2019/otp-150.sql')
    }

    changeSet(author: "kosnac", id: "drop-processing-type") {
        sqlFile(path: "changelogs/2019/drop-processing-type.sql")
    }

    changeSet(author: "kosnac", id: "otp-210") {
        sqlFile(path: "changelogs/2019/otp-210.sql")
    }

    changeSet(author: "", id: "otp-164") {
        sqlFile(path: 'changelogs/2019/otp-164.sql')
    }

    include file: 'changelogs/2019/otp-137-merge-create-samplepair-services.groovy'

    include file: 'changelogs/2019/remove-group.groovy'

    changeSet(author: "m139l", id: "otp-255") {
        sqlFile(path: 'changelogs/2019/otp-255-rename-run-segment.sql')
    }

    include file: 'changelogs/2019/drop-seq-track-lane-id-unique-constraint.groovy'

    include file: 'changelogs/2019/processing-step-update-previous-index.groovy'

    include file: 'changelogs/2019/otp-272.groovy'

    changeSet(author: "", id: "project-changes") {
        sqlFile(path: 'changelogs/2019/project-changes.sql')
    }

    changeSet(author: "", id: "project-grant-funding") {
        sqlFile(path: 'changelogs/2019/project-grant-funding.sql')
    }

    include file: 'changelogs/2019/project-storage-until.groovy'

    changeSet(author: "", id: "otp-194") {
        sqlFile(path: 'changelogs/2019/otp-194.sql')
    }

    include file: 'changelogs/2019/otp-194.groovy'

    changeSet(author: "strubelp", id: "otp-283") {
        sqlFile(path: 'changelogs/2020/otp-283.sql')
    }

    changeSet(author: "strubelp", id: "otp-306") {
        sqlFile(path: 'changelogs/2020/otp-306.sql')
    }

    changeSet(author: "kosnac", id: "otp-330") {
        sqlFile(path: 'changelogs/2020/otp-330.sql')
    }

    changeSet(author: "wieset", id: "otp-266") {
        sqlFile(path: 'changelogs/2020/otp-266.sql')
    }

    include file: 'changelogs/2020/otp-215-multiple-transfers-per-DTA.groovy'

    changeSet(author: "", id: "project-request") {
        sqlFile(path: 'changelogs/2020/project-request.sql')
    }

    changeSet(author: "", id: "project-predecessor") {
        sqlFile(path: 'changelogs/2020/project-predecessor.sql')
    }

    include file: 'changelogs/2019/otp-148.groovy'

    changeSet(author: "kosnac", id: "drop-snv-from-project") {
        sqlFile(path: 'changelogs/2020/drop-snv-from-project.sql')
    }

    changeSet(author: "borufka", id: "otp-367-well") {
        sqlFile(path: 'changelogs/2020/otp-367-well.sql')
    }

    include file: "changelogs/2020/otp-335-single-cell-well.groovy"

    changeSet(author: "strubelp", id: "otp-349") {
        sqlFile(path: 'changelogs/2020/otp-349.sql')
    }

    include file: 'changelogs/2020/otp-334-add-file-change-request-added.groovy'

    changeSet(author: "kosnac", id: "otp-411") {
        sqlFile(path: 'changelogs/2020/otp-411.sql')
    }

    changeSet(author: "", id: "otp-379") {
        sqlFile(path: 'changelogs/2020/otp-379.sql')
    }

    include file: 'changelogs/2020/otp-336.groovy'

    changeSet(author: "kosnac", id: "otp-336-3") {
        sqlFile(path: 'changelogs/2020/otp-336.sql')
    }

    changeSet(author: "", id: "otp-400") {
        sqlFile(path: 'changelogs/2020/otp-400.sql')
    }

    changeSet(author: "", id: "otp-371-1") {
        sqlFile(path: 'changelogs/2020/otp-371-1.sql')
    }

    changeSet(author: "albrecjp", id: "otp-358") {
        sqlFile(path: 'changelogs/2020/otp-358.sql')
    }

    changeSet(author: "albrecjp", id: "otp-480") {
        sqlFile(path: 'changelogs/2020/otp-480.sql')
    }

    changeSet(author: "m39l", id: "otp-437") {
        sqlFile(path: 'changelogs/2020/otp-437.sql')
    }

    include file: 'changelogs/2020/otp-437.groovy'

    changeSet(author: "kosnac", id: "otp-436-1") {
        sqlFile(path: 'changelogs/2020/otp-436-1-extend-existing-domains.sql')
    }

    changeSet(author: "kosnac", id: "otp-436-2") {
        sqlFile(path: 'changelogs/2020/otp-436-2-create-log-domains.sql')
    }

    changeSet(author: "kosnac", id: "otp-436-3") {
        sqlFile(path: 'changelogs/2020/otp-436-3-base-workflow-step-structure.sql')
    }

    changeSet(author: "", id: "otp-168") {
        sqlFile(path: 'changelogs/2020/otp-168.sql')
    }

    include file: 'changelogs/2020/otp-442-link-option-for-bam-import.groovy'

    changeSet(author: "", id: "otp-438") {
        sqlFile(path: 'changelogs/2020/otp-438.sql')
    }
    include file: 'changelogs/2020/otp-438.groovy'

    changeSet(author: "m39l", id: "otp-466") {
        sqlFile(path: 'changelogs/2020/otp-466.sql')
    }

    include file: 'changelogs/2020/otp-375-priority-as-domain.groovy'

    include file: 'changelogs/2020/otp-507.groovy'

    include file: 'changelogs/2020/otp-512.groovy'

    include file: 'changelogs/2020/otp-508-workflow-run-scheduler.groovy'

    include file: 'changelogs/2020/workflowrun-outputartefacts.groovy'

    changeSet(author: "kosnac", id: "default-project-roles", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/project-roles.sql')
    }

    changeSet(author: "strubelp", id: "otp-557") {
        sqlFile(path: 'changelogs/2020/otp-557.sql')
    }

    changeSet(author: "strubelp", id: "otp-563") {
        sqlFile(path: 'changelogs/2020/otp-563.sql')
    }

    include file: 'changelogs/2020/otp-547.groovy'

    changeSet(author: "kosnac", id: "otp-569") {
        sqlFile(path: 'changelogs/2020/otp-569.sql')
    }

    include file: 'changelogs/2020/otp-582-migrate-data-installation-start-job.groovy'

    include file: 'changelogs/2020/otp-333-project-fields.groovy'

    changeSet(author: "kosnac", id: "otp-555") {
        sqlFile(path: 'changelogs/2020/otp-555.sql')
    }

    changeSet(author: "albrecjp", id: "otp-463") {
        sqlFile(path: 'changelogs/2020/otp-463.sql')
    }

    include file: 'changelogs/2020/otp-463.groovy'

    include file: 'changelogs/2020/delete-sample-swap.groovy'
    changeSet(author: "wieset", id: "otp-369") {
        sqlFile(path: 'changelogs/2020/otp-369.sql')
    }

    include file: 'changelogs/2020/otp-588-change-log-connection-direction.groovy'

    include file: 'changelogs/2020/otp-613.groovy'

    include file: 'changelogs/2020/otp-585.groovy'

    changeSet(author: "albrecjp", id: "otp-623") {
        sqlFile(path: 'changelogs/2020/otp-623.sql')
    }

    include file: 'changelogs/2020/otp-695.sql'

    include file: 'changelogs/2020/otp-664.groovy'

    include file: 'changelogs/2020/otp-641.groovy'

    include file: 'changelogs/2020/otp-338.groovy'

    changeSet(author: "kosnac", id: "otp-635") {
        sqlFile(path: 'changelogs/2020/otp-635.sql')
    }

    include file: 'changelogs/2020/deprecatable.groovy'

    changeSet(author: "albrecjp", id: "otp-675") {
        sqlFile(path: 'changelogs/2020/otp-675.sql')
    }

    changeSet(author: "", id: "otp-758") {
        sqlFile(path: 'changelogs/2020/otp-758.sql')
    }

    changeSet(author: "", id: "otp-600") {
        sqlFile(path: 'changelogs/2020/otp-600.sql')
    }

    changeSet(author: "", id: "otp-370") {
        sqlFile(path: 'changelogs/2020/otp-370.sql')
    }

    include file: 'changelogs/2020/otp-610-flag-job-restartable.groovy'

    changeSet(author: "albrecjp", id: "otp-698") {
        sqlFile(path: 'changelogs/2020/otp-698.sql')
    }

    include file: 'changelogs/2020/otp-681.groovy'

    changeSet(author: "sunakshi", id: "otp-685") {
        sqlFile(path: 'changelogs/2021/otp-685.sql')
    }

    changeSet(author: "gabkol", id: "otp-915") {
        sqlFile(path: 'changelogs/2021/otp-915.sql')
    }

    changeSet(author: "borufka", id: "otp-596") {
        sqlFile(path: 'changelogs/2021/otp-596-use-text-for-workdirectory.sql')
    }

    include file: "changelogs/2021/otp-985-delete-deletionDate.groovy"

    changeSet(author: "gabkol", id: "otp-229") {
        sqlFile(path: 'changelogs/2021/otp-229-drop-wes-server.sql')
    }

    include file: "changelogs/2021/otp-959.groovy"

    include file: "changelogs/2021/otp-958.groovy"

    include file: 'changelogs/2020/otp-661.groovy'

    changeSet(author: "", id: "otp-661") {
        sqlFile(path: 'changelogs/2021/otp-661.sql')
    }

    include file: 'changelogs/2021/otp-919-1-new-dta-data-structure.groovy'

    changeSet(author: "gabkol", id: "otp-919-2-dta-data-migration") {
        sqlFile(path: 'changelogs/2021/otp-919-2-dta-data-migration.sql')
    }

    include file: 'changelogs/2021/otp-919-3-dta-delete-old-structure.groovy'

    include file: 'changelogs/2021/otp-1050-drop-unique-mail-constraint.groovy'

    changeSet(author: "", id: "otp-1063") {
        sqlFile(path: 'changelogs/2021/otp-1063.sql')
    }

    changeSet(author: "sunakshi", id: "otp-1108-update-strain.sql") {
        sqlFile(path: 'changelogs/2021/otp-1108-update-strain.sql')
    }

    include file: 'changelogs/2021/otp-1116-RoddyBamFile-extends-artefact.groovy'

    changeSet(author: "nlangh", id: "otp-874") {
        sqlFile(path: 'changelogs/2021/otp-874-deactivate-old-data-installation-workflows.sql')
    }

    include file: 'changelogs/2021/otp-1181-unique-constraint-for-processing-thresholds.groovy'

    changeSet(author: "gabkol", id: "otp-1108-add-shortDisplayName-to-workflowRun") {
        sqlFile(path: 'changelogs/2021/otp-1106-workflowRun-shortDisplayName.sql')
    }

    include file: 'changelogs/2021/otp-1107.groovy'

    changeSet(author: "", id: "otp-970") {
        sqlFile(path: 'changelogs/2021/otp-970-deactivate-old-fastqc-workflow.sql')
    }
}
