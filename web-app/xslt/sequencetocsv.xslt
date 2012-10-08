<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" media-type="text/plain"/>

    <xsl:template match="/sequences">
        <xsl:call-template name="header"/>
        <xsl:for-each select="sequence">
            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/@id"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/laneId"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/numberBasePairs"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/numberReads"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/insertSize"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/qualityEncoding"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/alignmentState"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/fastqcState"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/@finalBam"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/@originalBam"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqTrack/@usingOriginalBam"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="run/@id"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="run/name"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="run/dateExecuted"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="run/dateCreated"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="run/storageRealm"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="run/dataQuality"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="run/@blacklisted"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="run/@multipleSource"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="run/@qualityEvaluated"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqPlatform/@id"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqPlatform/name"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqPlatform/model"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqType/@id"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqType/name"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqType/libraryLayout"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqType/dirName"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="individual/@id"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="individual/pid"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="individual/mockPid"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="individual/mockFullName"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="individual/type"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="individual/sample/@id"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="individual/sample/sampleType/@id"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="individual/sample/sampleType/text()"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="project/@id"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="project/name"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="project/dirName"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="project/realm"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqCenter/@id"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqCenter/name"/>
            </xsl:call-template>

            <xsl:call-template name="field">
                <xsl:with-param name="value" select="seqCenter/dirName"/>
            </xsl:call-template>

            <xsl:for-each select="dataFiles/path">
                <xsl:if test="position() != last()">
                    <xsl:call-template name="field">
                        <xsl:with-param name="value" select="."/>
                    </xsl:call-template>
                </xsl:if>
                <xsl:if test="position() = last()">
                    <xsl:text>"</xsl:text>
                    <xsl:value-of select="."/>
                    <xsl:text>"</xsl:text>
                </xsl:if>
            </xsl:for-each>

            <xsl:text>&#10;</xsl:text>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="field">
        <xsl:param name="value"/>
        <xsl:text>"</xsl:text>
        <xsl:value-of select="$value"/>
        <xsl:text>", </xsl:text>
    </xsl:template>

    <xsl:template name="header">
        <xsl:text>#</xsl:text>
        <xsl:text>SeqTrack Id, </xsl:text>
        <xsl:text>Lane Id, </xsl:text>
        <xsl:text>Number Base Pairs, </xsl:text>
        <xsl:text>Number Reads, </xsl:text>
        <xsl:text>Insert Size, </xsl:text>
        <xsl:text>Quality Encoding, </xsl:text>
        <xsl:text>Alignment State, </xsl:text>
        <xsl:text>FastQC State, </xsl:text>
        <xsl:text>Final BAM, </xsl:text>
        <xsl:text>Original BAM, </xsl:text>
        <xsl:text>Using Original BAM, </xsl:text>
        <xsl:text>Run ID, </xsl:text>
        <xsl:text>Run Name, </xsl:text>
        <xsl:text>Run Date Executed, </xsl:text>
        <xsl:text>Run Date Created, </xsl:text>
        <xsl:text>Storage Realm, </xsl:text>
        <xsl:text>Data Quality, </xsl:text>
        <xsl:text>Blacklisted, </xsl:text>
        <xsl:text>Multiple Sources, </xsl:text>
        <xsl:text>Quality Evaluated, </xsl:text>
        <xsl:text>Seq Platform ID, </xsl:text>
        <xsl:text>Seq Platform Name, </xsl:text>
        <xsl:text>Seq Platform Model, </xsl:text>
        <xsl:text>Seq Type ID, </xsl:text>
        <xsl:text>Seq Type Name, </xsl:text>
        <xsl:text>Library Layout, </xsl:text>
        <xsl:text>Seq Type Dir Name, </xsl:text>
        <xsl:text>Individual ID, </xsl:text>
        <xsl:text>PID, </xsl:text>
        <xsl:text>Mock PID, </xsl:text>
        <xsl:text>Mock Full Name, </xsl:text>
        <xsl:text>Individual Type, </xsl:text>
        <xsl:text>Sample ID, </xsl:text>
        <xsl:text>Sample Type ID, </xsl:text>
        <xsl:text>Sample Type Name, </xsl:text>
        <xsl:text>Project ID, </xsl:text>
        <xsl:text>Project Name, </xsl:text>
        <xsl:text>Project Dir Name, </xsl:text>
        <xsl:text>Project Realm, </xsl:text>
        <xsl:text>Seq Center ID, </xsl:text>
        <xsl:text>Seq Center Name, </xsl:text>
        <xsl:text>Seq Center Dir Name, </xsl:text>
        <xsl:text>Path 1, </xsl:text>
        <xsl:text>Path 2</xsl:text>
        <xsl:text>&#10;</xsl:text>
    </xsl:template>
</xsl:stylesheet>
