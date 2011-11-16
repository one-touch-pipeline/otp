import QtQuick 1.0
ListModel {
    id: jepModel
    ListElement {
        name: "Meta Data Import"
        statusName: "blue"
        health: 40
        planVersion: 2
        lastSuccess: "yesterday"
        lastFailure: "today"
        duration: "5 min"
        jepEnabled: true
        jepRunning: true
    }
    ListElement {
        name: "Solid Processing"
        statusName: "yellow"
        health: 99
        planVersion: 5
        lastSuccess: "today"
        lastFailure: "one month ago"
        duration: "50 min"
        jepEnabled: true
        jepRunning: false
    }
    ListElement {
        name: "Ilumina Processing"
        statusName: "red"
        health: 0
        planVersion: 0
        lastSuccess: "never"
        lastFailure: "two month ago"
        duration: "0 min"
        jepEnabled: false
        jepRunning: false
    }
    ListElement {
        name: "Statistics"
        statusName: "blue"
        health: 70
        planVersion: 5
        lastSuccess: "today"
        lastFailure: "two month ago"
        duration: "2 h"
        jepEnabled: true
        jepRunning: false
    }
}
