<tr>
    <td class="myKey"><g:message code="seqTrack.show.details.laneId"/></td>
    <td class="myValue">${seqTrack.laneId}</td>
</tr>
<tr>
    <td class="myKey"><g:message code="seqTrack.show.run"/></td>
    <td><g:link controller="run" action="show" id="${seqTrack.run.id}">${seqTrack.run.name}</g:link></td>
</tr>
      <tr>
    <td class="myKey"><g:message code="seqTrack.show.sampleType"/></td>
    <td class="myValue">${seqTrack.sample.individual.mockFullName} ${seqTrack.sample.sampleType.name}</td>
</tr>
<tr>
    <td class="myKey"><g:message code="seqTrack.show.seqType"/></td>
    <td class="myValue">${seqTrack.seqType.name}</td>
</tr>