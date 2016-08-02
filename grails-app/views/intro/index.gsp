<html>
<head>
    <title>OTP</title>
    <meta name="layout" content="mainV2"/>
</head>
<body>
<div class="container wrapper mainpage">
    <div class="row">
        <div class="col-xs-10 col-xs-offset-1 col-sm-12 col-sm-offset-0">
            <div class="alert alert-success js-alert-success hidden">
                <button type="button" class="close" data-dismiss="alert" aria-label="close">&times;</button>
                <div class="js-alert-message"></div>
            </div>
            <div class="alert alert-info js-alert-info hidden">
                <button type="button" class="close" data-dismiss="alert" aria-label="close">&times;</button>
                <div class="js-alert-message"></div>
            </div>
            <div class="alert alert-warning js-alert-warning hidden">
                <button type="button" class="close" data-dismiss="alert" aria-label="close">&times;</button>
                <div class="js-alert-message"></div>
            </div>
            <div class="alert alert-danger js-alert-danger hidden">
                <button type="button" class="close" data-dismiss="alert" aria-label="close">&times;</button>
                <div class="js-alert-message"></div>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-xs-12 col-sm-7 logo">
            <div class="logo-container">
                <div class="logo-img">
                    <g:link class="logo" uri="/">
                        <img class="img-responsive" src="${assetPath(src: 'v2/otp-logo.png')}" alt="OTP">
                    </g:link>
                </div>
                <div class="slogan">
                    <h1>
                        One<br>Touch<br>Pipeline
                        <g:if test="${otp.environmentName() != 'production'}">
                            <br><span class="environmentName"><otp:environmentName/></span>
                        </g:if>
                    </h1>
                </div>
            </div>
        </div>
        <div class="col-xs-12 col-sm-5">
            <div class="login">
                <form id="login-form" class="navbar-form navbar-right" role="form" method="POST" action="${createLink(controller: 'j_spring_security_check')}">
                    <div class="input-group">
                        <span class="input-group-addon"><i class="glyphicon glyphicon-user"></i></span>
                        <input id="account" type="text" class="form-control js-account" name="j_username" value="${username}" placeholder="${message(code:"intro.login.form.account")}">
                    </div>
                    <div class="input-group">
                        <span class="input-group-addon"><i class="glyphicon glyphicon-lock"></i></span>
                        <input id="password" type="password" class="form-control" name="j_password" value="" placeholder="${message(code:"intro.login.form.password")}">
                    </div>
                    <button type="submit" class="btn btn-primary login-btn"><g:message code="intro.login.form.login" /></button>
                    <a class="register-btn pull-left" data-toggle="modal" href="#register"><g:message code="intro.login.form.request" /></a>
                </form>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-xs-12">
            <h2><g:message code="intro.news.title" /> <a class="pull-right" data-toggle="modal" href="#moreNews"><g:message code="intro.news.more" /></a></h2>
            <hr>
        </div>
        <div class="col-xs-12 col-md-6 news-container">
            <div class="news-box">
                <div class="date">12 Jan 2015</div>
                <h3> Lorem Ipsum</h3>
                <p class="news-text">
                    Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.
                    At vero eos et accusam et justo duo dolores et ea rebum.
                </p>
                <a data-toggle="modal" href="#"><g:message code="intro.news.more2" /></a>
            </div>
        </div>
        <div class="col-xs-12 col-md-6 news-container">
            <div class="news-box">
                <div class="date">9 Dec 2014</div>
                <h3> Lorem Ipsum</h3>
                <p class="news-text">
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
        <div class="col-xs-12">
            <div class="statistics">
                <p class="projects"><span class="hide-on-xs"><g:message code="intro.numbers.numberOf" /> </span><g:message code="intro.numbers.projects" />
                <span class="highlight">${projects}</span></p>
                <p class="projects"><span class="hide-on-xs"><g:message code="intro.numbers.numberOf" /> </span><g:message code="intro.numbers.lanes" />
                <span class="highlight">${lanes}</span></p>
            </div>
        </div>
        <div class="col-xs-12 col-md-7 graph">
            <div class="chart">
                <div class="headline"><g:message code="intro.numbers.pids" /></div>
                <canvas class="img-responsive" id="js-patientsCountPerSequenceType" width="535" height="350">[No canvas support]</canvas>
            </div>
        </div>
        <div class="col-xs-12 col-md-5 pie-chart">
            <div class="chart">
                <div class="headline"><g:message code="intro.numbers.samples" /></div>
                <canvas class="img-responsive" id="js-sampleCountPerSequenceTypePie" width="380" height="350">[No canvas support]</canvas>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-xs-12 main">
            <h2><g:message code="intro.about.title" /></h2>
            <hr>
            <p>
                OTP is an automation platform for managing next generation sequencing (NGS) data.
                The application provides support in all steps of this process including data transfer from temporary to final storage,
                execution of data quality monitoring programs and alignment of reads to the reference genome using a BWA based pipeline.
            </p><p>
                The application provides three major benefits to stakeholders: first, the automation process reduces the man-power required for data management.
                Second, all operations are executed more reliably and faster reducing the time until the sequences can be analyzed by bio-informatics groups.
                Third, all information is located in one system with web access and search capabilities.
            </p><p>
                OTP development started in 2012. It currently processes NGS data for projects of different consortia such as ICGC, HIPO and DKTK.
            </p>
        </div>
    </div>
    <div class="row">
        <div class="col-xs-12">
            <h2><g:message code="intro.partners.title" /></h2>
            <hr>
        </div>
        <div id="partner">
            <div class="col-xs-6 col-md-3 otp-partner">
                <ul>
                    <li>
                        <a href="https://www.dkfz.de/en/dktk/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/partner/dktk-logo.png')}" alt="German Cancer Consortium" title="German Cancer Consortium">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-2 col-md-1 otp-partner">
                <ul>
                    <li>
                        <a href="https://www.dkfz.de/en/hipo/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/partner/hipo-logo.png')}" alt="Heidelberg Center for Personalized Oncology" title="Heidelberg Center for Personalized Oncology">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-4 col-md-2 otp-partner">
                <ul>
                    <li>
                        <a href="https://www.nct-heidelberg.de/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/partner/nct-logo.png')}" alt="National Center for Tumor Diseases" title="National Center for Tumor Diseases">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-2 col-md-1 otp-partner">
                <ul>
                    <li>
                        <a href="http://www.bioquant.uni-heidelberg.de/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/partner/bioquant-logo.png')}" alt="BioQuant" title="BioQuant">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-lg-2 col-md-2 col-sm-4 col-xs-4 otp-partner">
                <ul>
                    <li>
                        <a href="http://ihec-epigenomes.org/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/partner/ihec-logo.png')}" alt="International Human Epigenome Consortium" title="International Human Epigenome Consortium">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-4 col-md-2 otp-partner">
                <ul>
                    <li>
                        <a href="http://icgc.org/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/partner/icgc-logo.png')}" alt="International Cancer Genome Consortium" title="International Cancer Genome Consortium">
                        </a>
                    </li>
                </ul>
            </div>
            <div class="col-xs-2 col-md-1 otp-partner">
                <ul>
                    <li>
                        <a href="http://www.deutsches-epigenom-programm.de/" target="_blank">
                            <img class="img-responsive" src="${assetPath(src: 'v2/partner/deep-logo.jpg')}" alt="German Epigenome Programme" title="German Epigenome Programme">
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
    <div id="prefooter">
        <div class="row footer-nav">
                <div class="copyright-claim pull-right">
                    <ul>
                        <li><g:message code="intro.powered" /></li>
                        <li>
                            <a href="http://ibios.dkfz.de/tbi/" target="_blank"><img class="img-responsive" alt="eilslabs" src="${assetPath(src: 'v2/eilslabs-logo.png')}"></a>
                        </li>
                    </ul>
                </div>
        </div>
    </div>
</div>
<otp:modal modalId="register" title="Request Access">
    <p>To request access to OTP, please send an e-mail to <a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a>.</p>
</otp:modal>
<asset:script type="text/javascript">
    $(function() {
        OTP.showMessage("${message}", "${type}");
    });
</asset:script>
</body>
</html>
