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
import grails.util.Environment
import grails.util.Holders

import java.nio.file.Path
import java.nio.file.Paths

databaseChangeLog = {

    Path base = Environment.isWarDeployed() ?
            Paths.get(Holders.applicationContext.grailsResourceLocator.findResourceForURI('WEB-INF/classes').file.path) :
            Paths.get('migrations')
    Path dir = base.resolve("changelogs/defaultValues")

    // Default values
    List<Path> files = [
            dir.resolve("seq-types.sql"),
            dir.resolve("workflows.sql"),
            dir.resolve("workflow-versions.sql"),
            dir.resolve("project-roles.sql"),
            dir.resolve("species-and-strains.sql"),
            dir.resolve("tool-names-of-reference-genome-indexes.sql"),
            dir.resolve("roles.sql"),
            dir.resolve("file-types.sql"),
            dir.resolve("pipelines.sql"),
    ]

    // External workflow configs
    Path ewcDir = dir.resolve("ewc")

    // Roddy Pancancer default values
    files += [
            ewcDir.resolve("ewc-roddy-pancancer-cvalue-1.2.73-1+1.2.73-201.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-cvalue-1.2.73-202.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-cvalue-1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-cvalue-EXON-1.2.73-1+1.2.73-201+1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-cvalue-WHOLE_GENOME-1.2.51-1.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-cvalue-WHOLE_GENOME-1.2.51-2.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-cvalue-WHOLE_GENOME-1.2.73-1+1.2.73-201+1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-filenames-ChIPSeq-1.2.73-1+1.2.73-201+1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-filenames-EXON-1.2.73-1+1.2.73-201+1.2.73-202+1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-filenames-WHOLE_GENOME-1.2.51-1.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-filenames-WHOLE_GENOME-1.2.51-2+1.2.73-1+1.2.73-201+1.2.73-202+1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-resources-1.2.73-1+1.2.73-201+1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-resources-1.2.73-202.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-resources-ChIPSeq-1.2.73-1+1.2.73-201+1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-resources-EXON-1.2.73-1+1.2.73-201+1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-resources-EXON-1.2.73-202.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-resources-WHOLE_GENOME-1.2.51-1+1.2.51-2.sql"),
            ewcDir.resolve("ewc-roddy-pancancer-resources-WHOLE_GENOME-1.2.73-1+1.2.73-201+1.2.73-204.sql"),
    ]

    // Roddy WGBS defaults values
    files += [
            ewcDir.resolve("ewc-roddy-wgbs-cvalue-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-3.sql"),
            ewcDir.resolve("ewc-roddy-wgbs-cvalue-1.2.73-204.sql"),
            ewcDir.resolve("ewc-roddy-wgbs-cvalue-WHOLE_GENOME_BISULFITE-1.2.51-1+1.2.51-2.sql"),
            ewcDir.resolve("ewc-roddy-wgbs-filenames-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-204+1.2.73-3.sql"),
            ewcDir.resolve("ewc-roddy-wgbs-filenames-WHOLE_GENOME_BISULFITE-1.2.51-1+1.2.51-2.sql"),
            ewcDir.resolve("ewc-roddy-wgbs-resources-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-204+1.2.73-3.sql"),
            ewcDir.resolve("ewc-roddy-wgbs-resources-WHOLE_GENOME_BISULFITE-1.2.51-1+1.2.51-2.sql"),
            ewcDir.resolve("ewc-roddy-wgbs-resources-WHOLE_GENOME_BISULFITE-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-204+1.2.73-3.sql"),
            ewcDir.resolve("ewc-roddy-wgbs-resources-WHOLE_GENOME_BISULFITE_TAGMENTATION-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-204+1.2.73-3.sql"),
    ]

    files.each { file ->
        changeSet(author: "otp", id: file.getFileName().toString(), runOnChange: "true") {
            sql(file.text)
        }
    }
}
