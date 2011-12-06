import QtQuick 1.0

Item {
    id: container
    width: 800
    height: 600
    property int textMargin: 2
    Component {
        id: jepDelegate
        Item {
            function fontColor(enabled) {
                if (enabled) {
                    return "#000000";
                } else {
                    return "#888888";
                }
            }
            width: 800
            height: 50
            Image {
                id: statusIcon
                source: "images/" + (jepEnabled ? statusName : "grey") + ".png"
                sourceSize {
                    width: 48
                    height: 48
                }
                width: 48
                height: 48
                anchors {
                    left: parent.left
                    leftMargin: textMargin
                    top: parent.top
                }
                SequentialAnimation on opacity {
                    running: jepRunning
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
            Image {
                function generateLink(percent) {
                    if (percent <= 19) {
                        return "00to19";
                    } else if (percent <= 39) {
                        return "20to39";
                    } else if (percent <= 59) {
                        return "40to59";
                    } else if (percent <= 79) {
                        return "60to79";
                    } else {
                        return "80plus";
                    }
                }
                id: healthIcon
                source: "images/health-" + generateLink(health) + ".png"
                sourceSize {
                    width: 48
                    height: 48
                }
                width: 48
                height: 48
                anchors {
                    left: statusIcon.right
                    top: parent.top
                    leftMargin: textMargin
                }
            }
            Text {
                id: nameText
                text: name
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                color: fontColor(jepEnabled)
                anchors {
                    left: healthIcon.right
                    right: versionText.left
                    top: parent.top
                    leftMargin: textMargin
                }
                MouseArea {
                    anchors.fill: parent
                    hoverEnabled: true
                    onEntered: parent.font.underline = true
                    onExited: parent.font.underline = false
                    onClicked: {
                        if (index == 1) {
                            container.state = "PROCESS";
                        }
                    }
                }
            }
            Text {
                id: versionText
                text: planVersion
                width: 40
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignRight
                color: fontColor(jepEnabled)
                anchors {
                    right: lastSuccessText.left
                    rightMargin: textMargin
                }
            }
            Text {
                id: lastSuccessText
                text: lastSuccess
                width: 100
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignRight
                color: fontColor(jepEnabled)
                anchors {
                    right: lastFailureText.left
                    rightMargin: textMargin
                }
            }
            Text {
                id: lastFailureText
                text: lastFailure
                width: 100
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignRight
                color: fontColor(jepEnabled)
                anchors {
                    right: durationText.left
                    rightMargin: textMargin
                }
            }
            Text {
                id: durationText
                text: duration
                width: 60
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignRight
                color: fontColor(jepEnabled)
                anchors {
                    right: parent.right
                    rightMargin: textMargin
                }
            }
        }
    }
    
    Component {
        id: jepHeader
        Item {
            width: parent.width
            height: 20
            Text {
                height: parent.height
                text: "Workflow"
                font.bold: true
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                anchors {
                    left: parent.left
                    leftMargin: 48*2
                    right: versionHeader.left
                    rightMargin: textMargin
                }
            }
            Text {
                id: versionHeader
                text: "#"
                width: 40
                height: parent.height
                font.bold: true
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                anchors {
                    right: lastSuccessHeader.left
                    rightMargin: textMargin
                }
            }
            Text {
                id: lastSuccessHeader
                text: "Last Success"
                width: 100
                height: parent.height
                font.bold: true
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                anchors {
                    right: lastFailureHeader.left
                    rightMargin: textMargin
                }
            }
            Text {
                id: lastFailureHeader
                text: "Last Failure"
                width: 100
                height: parent.height
                font.bold: true
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                anchors {
                    right: durationHeader.left
                    rightMargin: textMargin
                }
            }
            Text {
                id: durationHeader
                text: "Duration"
                font.bold: true
                width: 60
                height: parent.height
                verticalAlignment: Text.AlignVCenter
                horizontalAlignment: Text.AlignHCenter
                anchors {
                    right: parent.right
                    rightMargin: textMargin
                }
            }
        }
    }
    ListView {
        id: jepView
        anchors.fill: parent
        model: JepModel {}
        delegate: jepDelegate
        header: jepHeader
    }

    ProcessOverview {
        id: processView
        anchors.fill: parent
        onBack: container.state = "JEP"
    }

    state: "JEP"
    states: [
        State {
            name: "JEP"
            PropertyChanges {
                target: jepView
                visible: true
            }
            PropertyChanges {
                target: processView
                visible: false
            }
        },
        State {
            name: "PROCESS"
            PropertyChanges {
                target: jepView
                visible: false
            }
            PropertyChanges {
                target: processView
                visible: true
            }
        }
    ]
}