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
    changeSet(author: "", id: "seqtype", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/seqTypes.sql')
    }

    changeSet(author: "", id: "workflows", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/workflow.sql')
    }

    changeSet(author: "", id: "workflowVersions", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/workflowVersions.sql')
    }

    changeSet(author: "", id: "project-roles", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/project-roles.sql')
    }

    include file: 'changelogs/defaultValues/createSpeciesAndStrains.groovy'

    changeSet(author: "", id: "tool-names-of-reference-genome-indexes", runOnChange: "true") {
        include file: 'changelogs/defaultValues/tool-names-of-reference-genome-indexes.sql'
    }

    changeSet(author: "albrecjp", id: "otp-796", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/roles.sql')
    }

    changeSet(author: "", id: "ewc-roddy-base", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc-roddy-base.sql')
    }

    changeSet(author: "", id: "ewc-roddy-pancancer", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc-roddy-pancancer.sql')
    }

    changeSet(author: "", id: "file-types", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/file-types.sql')
    }

    changeSet(author: "", id: "pipeline", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/pipeline.sql')
    }
}
