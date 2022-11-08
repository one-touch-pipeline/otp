
databaseChangeLog = {

    changeSet(author: "", id: "wgbs-defaults-0", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-wgbs-cvalue-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-3.sql')
    }


    changeSet(author: "", id: "wgbs-defaults-1", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-wgbs-cvalue-1.2.73-204.sql')
    }


    changeSet(author: "", id: "wgbs-defaults-2", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-wgbs-cvalue-WHOLE_GENOME_BISULFITE-1.2.51-1+1.2.51-2.sql')
    }


    changeSet(author: "", id: "wgbs-defaults-3", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-wgbs-filenames-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-204+1.2.73-3.sql')
    }


    changeSet(author: "", id: "wgbs-defaults-4", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-wgbs-filenames-WHOLE_GENOME_BISULFITE-1.2.51-1+1.2.51-2.sql')
    }


    changeSet(author: "", id: "wgbs-defaults-5", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-wgbs-resources-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-204+1.2.73-3.sql')
    }


    changeSet(author: "", id: "wgbs-defaults-6", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-wgbs-resources-WHOLE_GENOME_BISULFITE-1.2.51-1+1.2.51-2.sql')
    }


    changeSet(author: "", id: "wgbs-defaults-7", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-wgbs-resources-WHOLE_GENOME_BISULFITE-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-204+1.2.73-3.sql')
    }


    changeSet(author: "", id: "wgbs-defaults-8", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-wgbs-resources-WHOLE_GENOME_BISULFITE_TAGMENTATION-1.2.73-1+1.2.73-2+1.2.73-201+1.2.73-202+1.2.73-204+1.2.73-3.sql')
    }

}
