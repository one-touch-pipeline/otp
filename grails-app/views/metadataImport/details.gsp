<%@ page import="de.dkfz.tbi.otp.ngsdata.RunSegment; de.dkfz.tbi.otp.tracking.OtrsTicket; org.joda.time.DateTime; de.dkfz.tbi.otp.ngsdata.ChipSeqSeqTrack"
        contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta name="layout" content="main"/>
  <title>${g.message(code: "metadataImport.details.title")}</title>
  <asset:javascript src="modules/editorSwitch"/>
</head>

<body>
<div class="body">
  <h2>${g.message(code: "metadataImport.details.otrsTicket")}</h2>
  <table style="width: auto;">
    <tr>
      <td>
        <g:message code="metadataImport.details.otrsTicketNumber"/>
      </td>
      <td>
        <otp:editorSwitch
                template="urlValue"
                roles="ROLE_OPERATOR"
                link="${g.createLink(
                        controller: 'metadataImport',
                        action: 'assignOtrsTicketToRunSegment',
                        id: runSegment.id)}"
                url="${runSegment.otrsTicket?.url ?: "#"}"
                value="${runSegment.otrsTicket?.ticketNumber ?: g.message(code: "metadataImport.details.ticketMissing")}"/>
      </td>
    </tr>
    <g:if test="${runSegment.otrsTicket}">
      <tr>
        <td>
          <g:message code="metadataImport.details.otrsTicket.seqcenter.comment"/>
        </td>
        <td>
          <otp:editorSwitch
                  roles="ROLE_OPERATOR"
                  template="textArea"
                  link="${g.createLink(controller: "metadataImport", action: "updateSeqCenterComment", id: runSegment.otrsTicket.id)}"
                  value="${runSegment.otrsTicket.seqCenterComment}"/>
        </td>
      </tr>
    </g:if>
  </table>
  <h2>${g.message(code: "metadataImport.details.metadataFiles")}</h2>
  <ul>
    <g:each in="${data.metaDataFiles}" var="metaDataFile">
      <li>
        ${metaDataFile.filePath}/${metaDataFile.fileName}<br>
        ${g.message(code: "metadataImport.details.dateCreated")} ${new DateTime(metaDataFile.dateCreated).toString("yyyy-MM-dd HH:mm:ss ZZ")}${metaDataFile.md5sum ? ", ${g.message(code: "metadataImport.details.md5")} ${metaDataFile.md5sum}" : ""}
      </li>
    </g:each>
  </ul>

  <h2>${g.message(code: "metadataImport.details.dataFiles")}</h2>
  <g:each in="${data.runs}" var="run">
    <h3>${g.message(code: "metadataImport.details.run")}
      <g:link controller="run" action="show" id="${run.run.id}">${run.run.name}</g:link>,
      ${[
        run.run.seqCenter.name,
        run.run.seqPlatform.fullName(),
        run.run.dateExecuted?.format("yyyy-MM-dd")
      ].findAll().join(', ')}
    </h3>
    <ul>
      <g:each in="${run.seqTracks}" var="seqTrack">
        <li>
          <g:link controller="seqTrack" action="show" id="${seqTrack.seqTrack.id}"><g:message code="metadataImport.details.lane"/> ${seqTrack.seqTrack.laneId}</g:link>,
          ${[
            seqTrack.seqTrack.ilseId ? "${g.message(code: "metadataImport.details.ilse")} ${seqTrack.seqTrack.ilseId}" : null,
            seqTrack.seqTrack.project.name,
          ].findAll().join(', ')},
          <g:link controller="individual" action="show" id="${seqTrack.seqTrack.individual.id}">${seqTrack.seqTrack.individual.displayName}</g:link>,
          ${[
            seqTrack.seqTrack.sampleType.name,
            "${seqTrack.seqTrack.seqType.name} ${seqTrack.seqTrack.seqType.libraryLayout}",
            seqTrack.seqTrack instanceof ChipSeqSeqTrack ? seqTrack.seqTrack.antibodyTarget.name : null,
            seqTrack.seqTrack instanceof ChipSeqSeqTrack ? seqTrack.seqTrack.antibody : null,
            seqTrack.seqTrack.libraryPreparationKit?.name,
            seqTrack.seqTrack.normalizedLibraryName ? "${g.message(code: "metadataImport.details.normalizedLibraryName")} ${seqTrack.seqTrack.normalizedLibraryName}" : null,
            seqTrack.seqTrack.pipelineVersion?.displayName,
          ].findAll().join(', ')}
          <ul>
            <g:each in="${seqTrack.dataFiles}" var="dataFile">
              <li>
                <g:message code="metadataImport.details.mateNumber" />
                ${dataFile.mateNumber}:
                <g:link controller="dataFile" action="showDetails" id="${dataFile.id}">
                  ${dataFile.fileName}
                </g:link>
              </li>
            </g:each>
            <g:each in="${seqTrack.seqTrack.logMessages}" var="msg">
              <li>${new DateTime(msg.dateCreated).toString("yyyy-MM-dd HH:mm:ss ZZ")}: ${msg.message}</li>
            </g:each>
          </ul>
        </li>
        <br>
      </g:each>
    </ul>
  </g:each>

  <h3>${g.message(code: "metadataImport.details.notAssigned")}</h3>
  <g:if test="${data.dataFilesNotAssignedToSeqTrack}">
    <ul>
      <g:each in="${data.dataFilesNotAssignedToSeqTrack}" var="dataFile">
        <li><g:link controller="dataFile" action="showDetails" id="${dataFile.id}">${dataFile.fileName}</g:link></li>
      </g:each>
    </ul>
  </g:if><g:else>
  ${g.message(code: "metadataImport.details.none")}
</g:else>
</div>
</body>

</html>
