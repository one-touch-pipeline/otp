<%@ page contentType="text/html;charset=UTF-8" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="fastqc.show.fastqcReport"/></title>
    <r:require module="lightbox"/>
<!--It does not work with older versions of jquery... that is why this is here... actual jquery version 1.6.1.1 -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
  </head>
  <body>
    <div class="body">
      <div class="fastqc">
        <h1><g:message code="fastqc.show.fastqcReport"/></h1>
        <div class="modules">
          <div class="module" align="right">
            <g:link target="_blank" controller="fastqcResults" action="renderFile" params="[id: id, withinZipPath: 'fastqc_data.txt']">fastqc_data.txt</g:link>
          </div>
          <div class="module">
            <table>
              <tr>
                <th><g:message code="fastqc.show.measure"/></th>
                <th><g:message code="fastqc.show.value"/></th>
              </tr>
                <tr>
                  <td><g:message code="fastqc.show.filename"/></td>
                  <td>${fileName}</td>
                </tr>
                <tr>
                  <td><g:message code="fastqc.show.fileType"/></td>
                  <td>${basicStats.fileType}</td>
                </tr>
                <tr>
                  <td><g:message code="fastqc.show.encoding"/></td>
                  <td>${basicStats.encoding}</td>
                </tr>
                <tr>
                  <td><g:message code="fastqc.show.totalSequences"/></td>
                  <td>${basicStats.totalSequences}</td>
                </tr>
                <tr>
                  <td><g:message code="fastqc.show.filteredSequences"/></td>
                  <td>${basicStats.filteredSequences}</td>
                </tr>
                <tr>
                  <td><g:message code="fastqc.show.sequenceLength"/></td>
                  <td>${basicStats.sequenceLength}</td>
                </tr>
            </table>
          </div>
          <div class="module">
            <table>
                <g:each var="module" in="${moduleStatus}">
                    <tr>
                        <td class="${module.value}">
                            <a href="#${module.key}">${moduleText[module.key]}</a>
                        </td>
                    </tr>
                </g:each>
            </table>
          </div>

          <div id="PER_BASE_SEQUENCE_QUALITY" class="module">
            <div class="${moduleStatus.PER_BASE_SEQUENCE_QUALITY}"><h2><g:message code="fastqc.perBaseSequenceQuality"/></h2></div>
            <a href="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/per_base_quality.png'])}" rel="lightbox[fastqc]" title="Per base sequence quality">
              <img class="small" src="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/per_base_quality.png'])}"/>
            </a>
          </div>
          <div id="PER_SEQUENCE_QUALITY_SCORES" class="module">
            <div class="${moduleStatus.PER_SEQUENCE_QUALITY_SCORES}"><h2><g:message code="fastqc.perSequenceQualityScores"/></h2></div>
            <a href="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/per_sequence_quality.png'])}" rel="lightbox[fastqc]" title="Per sequence quality scores">
              <img class="small" src="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/per_sequence_quality.png'])}"/>
            </a>
          </div>
          <div id="PER_BASE_SEQUENCE_CONTENT" class="module">
            <div class="${moduleStatus.PER_BASE_SEQUENCE_CONTENT}"><h2><g:message code="fastqc.perBaseSequenceContent"/></h2></div>
            <a href="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/per_base_sequence_content.png'])}" rel="lightbox[fastqc]" title="Per base sequence content">
            <img class="small" onClick="resizeMe(this);" src="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id, withinZipPath : 'Images/per_base_sequence_content.png'])}"/>
            </a>
          </div>
          <div id="PER_BASE_GC_CONTENT" class="module">
            <div class="${moduleStatus.PER_BASE_GC_CONTENT}"><h2><g:message code="fastqc.perBaseGCContent"/></h2></div>
            <a href="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/per_base_gc_content.png'])}" rel="lightbox[fastqc]" title="Per base GC content">
            <img class="small" onClick="resizeMe(this);" src="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id, withinZipPath:'Images/per_base_gc_content.png'])}"/>
            </a>
          </div>
          <div id="PER_SEQUENCE_GC_CONTENT" class="module">
            <div class="${moduleStatus.PER_SEQUENCE_GC_CONTENT}"><h2><g:message code="fastqc.perSequenceGCContent"/></h2></div>
            <a href="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/per_sequence_gc_content.png'])}" rel="lightbox[fastqc]" title="Per sequence GC content">
              <img class="small" src="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id, withinZipPath:'Images/per_sequence_gc_content.png'])}"/>
            </a>
          </div>
          <div id="PER_BASE_N_CONTENT" class="module">
            <div class="${moduleStatus.PER_BASE_N_CONTENT}"><h2><g:message code="fastqc.perBaseNContent"/></h2></div>
            <a href="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/per_base_n_content.png'])}" rel="lightbox[fastqc]" title="Per base N content">
              <img class="small" onClick="resizeMe(this);" src="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id, withinZipPath:'Images/per_base_n_content.png'])}"/>
            <a>
          </div>
          <div id="SEQUENCE_LENGTH_DISTRIBUTION" class="module">
            <div class="${moduleStatus.SEQUENCE_LENGTH_DISTRIBUTION}"><h2><g:message code="fastqc.sequenceLengthDistribution"/></h2></div>
            <a href="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/sequence_length_distribution.png'])}" rel="lightbox[fastqc]" title="Sequence length distribution">
              <img class="small" src="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id, withinZipPath:'Images/sequence_length_distribution.png'])}"/>
            </a>
          </div>
          <div id="SEQUENCE_DUPLICATION_LEVELS" class="module">
            <div class="${moduleStatus.SEQUENCE_DUPLICATION_LEVELS}"><h2><g:message code="fastqc.sequenceDuplicationLevels"/></h2></div>
            <a href="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id ,withinZipPath : 'Images/duplication_levels.png'])}" rel="lightbox[fastqc]" title="Sequence Duplication Levels">
              <img  class="small" src="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id, withinZipPath:'Images/duplication_levels.png'])}"/>
            </a>
          </div>
          <div id="OVERREPRESENTED_SEQUENCES" class="module">
            <div class="${moduleStatus.OVERREPRESENTED_SEQUENCES}"><h2><g:message code="fastqc.overrepresentedSequences"/></h2></div>
            <g:if test="${overrepSeq.size == 0}">
              <g:message code="fastqc.show.noOverrepresentedSequences"/>
            </g:if>
            <g:else>
              <table>
                <tr>
                  <th><g:message code="fastqc.show.sequence"/></th>
                  <th><g:message code="fastqc.show.count"/></th>
                  <th><g:message code="fastqc.show.percentage"/></th>
                  <th><g:message code="fastqc.show.possibleSource"/></th>
                </tr>
                <g:each var="overSeq" status="i" in="${overrepSeq}">
                  <tr title="${overSeq.percentage}">
                    <td>${overSeq.sequence}</td>
                    <td>${overSeq.countOverRep}</td>
                    <td><div><g:formatNumber number="${overSeq.percentage}" type="number" maxFractionDigits="2" /><div></td>
                    <td>${overSeq.possibleSource}</td>
                  </tr>
                </g:each>
              </table>
            </g:else>
            <br/><br/><br/>
          </div>
          <div id="KMER_CONTENT" class="module">
            <div class="${moduleStatus.KMER_CONTENT}"><h2><g:message code="fastqc.kmerContent"/></h2></div>
            <g:if test="${kmerContent.size == 0}">
              <g:message code="fastqc.show.noKmerContent"/>
            </g:if>
            <g:else>
              <a href="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id, withinZipPath: 'Images/kmer_profiles.png'])}" rel="lightbox[fastqc]" title="Kmer content">
                <img  class="small" src="${createLink(controller: 'fastqcResults', action: 'renderFile', params : [id:id, withinZipPath:'Images/kmer_profiles.png'])}"/>
              </a>
              <br/><br/>
              <table>
                <tr>
                  <th><g:message code="fastqc.show.sequence"/></th>
                  <th><g:message code="fastqc.show.count"/></th>
                  <th><g:message code="fastqc.show.obs.exp.overall"/></th>
                  <th><g:message code="fastqc.show.obs.exp.max"/></th>
                  <th><g:message code="fastqc.show.max.obs.exp.position"/></th>
                </tr>
                <g:each var="kmer" status="i" in="${kmerContent}">
                  <tr>
                    <td>${kmer.sequence}</td>
                    <td>${kmer.countOfKmer}</td>
                    <td>${kmer.overall}</td>
                    <td>${kmer.max}</td>
                    <td>${kmer.position}</td>
                  </tr>
                </g:each>
              </table>
            </g:else>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>