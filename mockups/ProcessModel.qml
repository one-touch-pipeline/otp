import QtQuick 1.0
ListModel {
    ListElement {
        number: 10
        processState: "blue"
        dateStarted: "01.01.2012 00:00"
        lastUpdate: "01.01.2012 00:01"
        lastJob: "Find New Tar Files"
        lastJobStatus: "CREATED"
        reachedMilestone: 0
        numberMilestones: 5
    }
    ListElement {
        number: 9
        processState: "yellow"
        dateStarted: "31.12.2011 05:00"
        lastUpdate: "31.12.2011 08:06"
        lastJob: "Verfiy last Job succeeded"
        lastJobStatus: "SUCCESS"
        reachedMilestone: 5
        numberMilestones: 5
    }
    ListElement {
        number: 8
        processState: "blue"
        dateStarted: "01.01.2012 00:00"
        lastUpdate: "01.01.2012 00:01"
        lastJob: "Find New Tar Files"
        lastJobStatus: "CREATED"
        reachedMilestone: 2
        numberMilestones: 5
    }
    ListElement {
        number: 7
        processState: "blue"
        dateStarted: "01.01.2012 00:00"
        lastUpdate: "01.01.2012 00:01"
        lastJob: "Find New Tar Files"
        lastJobStatus: "CREATED"
        reachedMilestone: 3
        numberMilestones: 5
    }
    ListElement {
        number: 6
        processState: "red"
        dateStarted: "20.12.2011 09:30"
        lastUpdate: "20.12.2011 09:35"
        lastJob: "unzip"
        lastJobStatus: "FAILED"
        reachedMilestone: 1
        numberMilestones: 5
    }
}
