%{--
  - Copyright 2011-2019 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils" %>
<html>
<head>
    <title>OTP</title>
    <meta name="layout" content="mainV2"/>
</head>
<body>
<div class="body container">
    <div class="row">
        <div class="col-xs-12">
            <h2><g:message code="intro.news.title" /> <a class="pull-right" data-toggle="modal" href="#moreNews"><g:message code="intro.news.more" /></a></h2>
            <hr>
        </div>
        <div class="col-xs-12 col-md-6 info-box-container">
            <div class="info-box">
                <div class="date">12 Jan 2015</div>
                <h3> Lorem Ipsum</h3>
                <p>
                    Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.
                    At vero eos et accusam et justo duo dolores et ea rebum.
                </p>
                <a data-toggle="modal" href="#"><g:message code="intro.news.more2" /></a>
            </div>
        </div>
        <div class="col-xs-12 col-md-6 info-box-container">
            <div class="info-box">
                <div class="date">9 Dec 2014</div>
                <h3> Lorem Ipsum</h3>
                <p>
                    Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.
                    At vero eos et accusam et justo duo dolores et ea rebum.
                </p>
                <a data-toggle="modal" href="#"><g:message code="intro.news.more2" /></a>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-xs-12">
            <h2><g:message code="intro.numbers.title" /></h2>
            <hr>
        </div>
        <div class="col-xs-12 statistics-container">
            <div class="statistics-box">
                <p class="projects"><span class="hide-on-xs"><g:message code="intro.numbers.numberOf" /> </span><g:message code="intro.numbers.projects" />
                <span class="highlight">${projects}</span></p>
                <p class="projects"><span class="hide-on-xs"><g:message code="intro.numbers.numberOf" /> </span><g:message code="intro.numbers.lanes" />
                <span class="highlight">${lanes}</span></p>
            </div>
        </div>
        <div class="col-xs-12 col-md-7 statistics-container">
            <div class="statistics-box statistics-box-chart">
                <div class="headline"><g:message code="intro.numbers.pids" /></div>
                <canvas class="img-responsive" id="js-patientsCountPerSequenceType" width="535" height="350">[No canvas support]</canvas>
            </div>
        </div>
        <div class="col-xs-12 col-md-5 statistics-container statistics-container-right">
            <div class="statistics-box statistics-box-chart">
                <div class="headline"><g:message code="intro.numbers.samples" /></div>
                <canvas class="img-responsive" id="js-sampleCountPerSequenceTypePie" width="380" height="350">[No canvas support]</canvas>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-xs-12 main">
            <h2><g:message code="intro.about.title" /></h2>
            <hr>
            <p class="keep-whitespace">${raw(aboutOtp)}</p>
        </div>
    </div>
<g:if test="${showPartners}">
    <div class="row">
        <div class="col-xs-12">
            <h2><g:message code="intro.partners.title" /></h2>
            <hr>
        </div>
        <div id="partner">
            <div class="col-xs-3 col-md-2 otp-partner">
                <ul>
                    <li>
                        <a href="https://www.dkfz.de/en/dktk/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/intro/dktk-logo.png')}" alt="German Cancer Consortium" title="German Cancer Consortium">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-2 col-md-1 otp-partner">
                <ul>
                    <li>
                        <a href="https://www.dkfz.de/en/hipo/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/intro/hipo-logo.png')}" alt="Heidelberg Center for Personalized Oncology" title="Heidelberg Center for Personalized Oncology">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-4 col-md-2 otp-partner">
                <ul>
                    <li>
                        <a href="https://www.nct-heidelberg.de/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/intro/nct-logo.png')}" alt="National Center for Tumor Diseases" title="National Center for Tumor Diseases">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-2 col-md-1 otp-partner">
                <ul>
                    <li>
                        <a href="http://www.bioquant.uni-heidelberg.de/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/intro/bioquant-logo.png')}" alt="BioQuant" title="BioQuant">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-lg-2 col-md-2 col-sm-4 col-xs-4 otp-partner">
                <ul>
                    <li>
                        <a href="http://ihec-epigenomes.org/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/intro/ihec-logo.png')}" alt="International Human Epigenome Consortium" title="International Human Epigenome Consortium">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-4 col-md-2 otp-partner">
                <ul>
                    <li>
                        <a href="http://icgc.org/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/intro/icgc-logo.png')}" alt="International Cancer Genome Consortium" title="International Cancer Genome Consortium">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-2 col-md-1 otp-partner">
                <ul>
                    <li>
                        <a href="http://www.deutsches-epigenom-programm.de/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/intro/deep-logo.jpg')}" alt="German Epigenome Programme" title="German Epigenome Programme">
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</g:if>
</div>
<otp:modal modalId="register" title="${g.message(code: "login.requestAccess.title")}">
    <p>${g.message(code: "login.requestAccess.description")} <a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a>.</p>
</otp:modal>
<asset:script type="text/javascript">
    $(function() {
        OTP.showMessage("${message}", "${type}");
    });
</asset:script>
</body>
</html>
