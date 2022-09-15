
databaseChangeLog = {

    changeSet(author: "", id: "pancancer-defaults-1", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-cvalue-1.2.73-1+1.2.73-201.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-2", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-cvalue-1.2.73-202.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-3", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-cvalue-1.2.73-204.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-5", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-cvalue-EXON-1.2.73-1+1.2.73-201+1.2.73-204.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-6", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-cvalue-WHOLE_GENOME-1.2.51-1.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-7", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-cvalue-WHOLE_GENOME-1.2.51-2.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-8", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-cvalue-WHOLE_GENOME-1.2.73-1+1.2.73-201+1.2.73-204.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-10", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-filenames-ChIPSeq-1.2.73-1+1.2.73-201+1.2.73-204.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-11", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-filenames-EXON-1.2.73-1+1.2.73-201+1.2.73-202+1.2.73-204.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-12", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-filenames-WHOLE_GENOME-1.2.51-1.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-13", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-filenames-WHOLE_GENOME-1.2.51-2+1.2.73-1+1.2.73-201+1.2.73-202+1.2.73-204.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-15", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-resources-1.2.73-1+1.2.73-201+1.2.73-204.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-16", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-resources-1.2.73-202.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-17", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-resources-ChIPSeq-1.2.73-1+1.2.73-201+1.2.73-204.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-19", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-resources-EXON-1.2.73-1+1.2.73-201+1.2.73-204.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-20", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-resources-EXON-1.2.73-202.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-22", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-resources-WHOLE_GENOME-1.2.51-1+1.2.51-2.sql')
    }


    changeSet(author: "", id: "pancancer-defaults-23", runOnChange: "true") {
        sqlFile(path: 'changelogs/defaultValues/ewc/ewc-roddy-pancancer-resources-WHOLE_GENOME-1.2.73-1+1.2.73-201+1.2.73-204.sql')
    }

}
