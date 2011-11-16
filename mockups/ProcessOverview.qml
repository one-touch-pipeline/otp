import QtQuick 1.0
Item {
    width: 800
    height: 600
    property int textMargin: 2
    signal back()

    Component {
        id: processOverviewDelegate
        Item {
            width: parent.width
            height: 70
            Item {
                id: textRow
                height: 50
                anchors {
                    left: parent.left
                    right: parent.right
                }
                Text {
                    id: numberItem
                    text: number
                    width: 20
                    height: parent.height
                    verticalAlignment: Text.AlignVCenter
                    horizontalAlignment: Text.AlignHCenter
                    font {
                        underline: true
                    }
                    color: "#0000FF"
                    anchors {
                        left: parent.left
                        leftMargin: textMargin
                        top: parent.top
                    }
                }
                Image {
                    id: statusIcon
                    source: "images/" + processState + ".png"
                    sourceSize {
                        width: 48
                        height: 48
                    }
                    width: 48
                    height: 48
                    anchors {
                        left: numberItem.right
                        leftMargin: textMargin
                        top: parent.top
                    }
                    SequentialAnimation on opacity {
                        running: lastJobStatus != "SUCCESS" && lastJobStatus != "FAILED"
                        loops: Animation.Infinite
                        NumberAnimation {
                            from: 1.0
                            to: 0.0
                            duration: 500
                        }
                        NumberAnimation {
                            from: 0.0
                            to: 1.0
                            duration: 500
                        }
                    }
                }
                Text {
                    id: dateStartedElement
                    text: dateStarted
                    width: 100
                    height: parent.height
                    verticalAlignment: Text.AlignVCenter
                    anchors {
                        left: statusIcon.right
                        leftMargin: textMargin
                    }
                }
                Text {
                    id: lastUpdateElement
                    text: lastUpdate
                    width: 100
                    height: parent.height
                    verticalAlignment: Text.AlignVCenter
                    anchors {
                        left: dateStartedElement.right
                        leftMargin: textMargin
                    }
                }
                Text {
                    text: lastJob
                    height: parent.height
                    verticalAlignment: Text.AlignVCenter
                    horizontalAlignment: Text.AlignHCenter
                    anchors {
                        left: lastUpdateElement.right
                        right: statusElement.left
                        leftMargin: textMargin
                        rightMargin: textMargin
                    }
                }
                Text {
                    id: statusElement
                    text: lastJobStatus
                    width: 150
                    height: parent.height
                    verticalAlignment: Text.AlignVCenter
                    anchors {
                        right: parent.right
                        rightMargin: textMargin
                    }
                }
            }
            Item {
                height: 20
                anchors {
                    top: textRow.bottom
                    left: parent.left
                    right: parent.right
                }
                Rectangle {
                    anchors {
                        left: parent.left
                        bottom: parent.bottom
                        top: parent.top
                    }
                    width: parent.width * reachedMilestone/numberMilestones
                    color: "green"
                }
                Text {
                    text: reachedMilestone + " / " + numberMilestones + " milestones"
                    anchors.fill: parent
                    horizontalAlignment: Text.AlignHCenter
                    verticalAlignment: Text.AlignVCenter
                }
            }
        }
    }

    Component {
        id: overviewHeaderComponent
        Item {
            width: parent.width
            height: 40
            Text {
                id: creationDateHeader
                text: "Creation date"
                width: 100
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                font.bold: true
                anchors {
                    left: parent.left
                    leftMargin: 68
                }
            }
            Text {
                id: lastUpdateHeader
                text: "Last update"
                width: 100
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                font.bold: true
                anchors {
                    left: creationDateHeader.right
                    leftMargin: textMargin
                }
            }
            Text {
                id: currentProcessingStepHeader
                text: "Current Processing Step"
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                font.bold: true
                anchors {
                    left: lastUpdateHeader.right
                    right: statusHeader.left
                    leftMargin: textMargin
                    rightMargin: textMargin
                }
            }
            Text {
                id: statusHeader
                text: "Status"
                width: 150
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                font.bold: true
                anchors {
                    right: parent.right
                    rightMargin: textMargin
                }
            }
        }
    }

    Text {
        id: headerText
        text: "Solid Processing"
        horizontalAlignment: Text.AlignHCenter
        font {
            bold: true
            pixelSize: 40
        }
        anchors {
            top: parent.top
            left: parent.left
            right: parent.right
        }
    }

    ListView {
        id: processOverview
        model: ProcessModel {}
        delegate: processOverviewDelegate
        anchors {
            left: parent.left
            right: parent.right
            top: headerText.bottom
            topMargin: textMargin
            bottom: backButton.top
            bottomMargin: 10
        }
        header: overviewHeaderComponent
    }
    
    Text {
        id: backButton
        text: "Back to Overview"
        height: 50
        font {
            pixelSize: 40
            bold: true
        }
        anchors {
            bottom: parent.bottom
            bottomMargin: 10
            horizontalCenter: parent.horizontalCenter
        }
        MouseArea {
            anchors.fill: parent
            onClicked: back()
        }
    }
}