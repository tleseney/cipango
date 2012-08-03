<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
								xmlns="http://www.w3.org/2000/svg"
								version="1.0">

	<xsl:param name="animation">true</xsl:param>

	<xsl:output
		method="xml"
		encoding="utf-8"
		doctype-public="-//W3C//DTD SVG 1.1//EN"
		doctype-system="http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"/>

	<xsl:template match="/">
		<xsl:processing-instruction name="xml-stylesheet">href="css/svg.css" type="text/css"</xsl:processing-instruction>
		<svg	width="100%" height="100%" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:html="http://www.w3.org/1999/xhtml">
				<xsl:apply-templates/>
		</svg>
	</xsl:template>

	
	<xsl:template match="hosts">
		<xsl:for-each select="host">
			<xsl:variable name="x" select='position() * 150 - 80' />
			<g>
				<text class='host-text'>
					<xsl:attribute name="x"><xsl:value-of select="$x"/></xsl:attribute>
					<xsl:choose>
						<xsl:when test="position() mod 2 &gt; 0">
							<xsl:attribute name="y">20</xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="y">40</xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:value-of select="text()"/>
				</text>
				<line y1="45" class='host-line'>
					<xsl:attribute name="x1"><xsl:value-of select="$x"/></xsl:attribute>
					<xsl:attribute name="x2"><xsl:value-of select="$x"/></xsl:attribute>
					<xsl:attribute name="y2"><xsl:value-of select="60 + count(/data/messages/message) * 25"/></xsl:attribute>
				</line>
			</g>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template match="messages">
		<xsl:for-each select="message">
			<xsl:variable name="yText" select='40 + position() * 25' />
			<xsl:variable name="yLine" select='45 + position() * 25' />
			<xsl:variable name="xStart" select='@from * 150 - 80' />
			<xsl:variable name="xEnd" select='@to * 150 - 80' />
			<xsl:variable name="d">
				<xsl:choose>
					<xsl:when test="@from &lt; @to">-1</xsl:when>
					<xsl:otherwise>1</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			
			<g>
				<xsl:attribute name="id">msg<xsl:value-of select="position()"/></xsl:attribute>
				<xsl:attribute name="onclick">window.parent.location.hash='msg-<xsl:value-of select="position()"/>';</xsl:attribute>
				
				<text x="30" class='message-header'>
					<xsl:attribute name="y"><xsl:value-of select="$yLine"/></xsl:attribute>
					(<xsl:value-of select="position()"/>)
				</text>
				
				<text class='message-text' >
					<xsl:attribute name="y"><xsl:value-of select="$yText"/></xsl:attribute>
					<xsl:choose>
						<xsl:when test="@from &gt; @to">
							<xsl:attribute name="x"><xsl:value-of select="15 + $xEnd"/></xsl:attribute>
						</xsl:when>
						<xsl:when test="@from &lt; @to">
							<xsl:attribute name="x"><xsl:value-of select="15 + $xStart"/></xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="x"><xsl:value-of select="25 + $xStart"/></xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:value-of select="name/text()"/>
				</text>
				<xsl:choose>
					<xsl:when test="@from = @to">
						<xsl:call-template name="loopback">
							<xsl:with-param name="x" select="$xStart"/>
							<xsl:with-param name="y" select="$yLine"/>
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<line class='message-line'>
							<xsl:attribute name="x1"><xsl:value-of select="$xStart"/></xsl:attribute>
							<xsl:attribute name="x2"><xsl:value-of select="$xEnd"/></xsl:attribute>
							<xsl:attribute name="y1"><xsl:value-of select="$yLine"/></xsl:attribute>
							<xsl:attribute name="y2"><xsl:value-of select="$yLine"/></xsl:attribute>
						</line>
					</xsl:otherwise>
				</xsl:choose>
				
				<xsl:call-template name="arrow">
					<xsl:with-param name="x" select="$xEnd"/>
					<xsl:with-param name="y" select="$yLine"/>
					<xsl:with-param name="d" select="$d"/>
				</xsl:call-template>

			</g>
		</xsl:for-each>
		
		<xsl:if test="message/content"> <!-- Display animation only if the message content is available -->
			<xsl:for-each select="message">
			
					<xsl:variable name="y" select='50 + position() * 25' />
					<xsl:variable name="x" select='80' />
					<xsl:variable name="height" select='(content/@nbLines) * 15 + 4' />
					<xsl:variable name="y2">
						<xsl:choose>
							<xsl:when test="($y + $height) &gt; (60 + count(/data/messages/message) * 25) and ($y - $height - 25) &gt; 0">
								<xsl:value-of select="$y - $height - 25"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$y"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					
					<g style="display:none">
						<xsl:attribute name="transform">translate(<xsl:value-of select="$x"/>,<xsl:value-of select="$y2"/>)</xsl:attribute>
					
						<rect x="0" y="0" width="600" class='msg-background'>
							<xsl:attribute name="height"><xsl:value-of select="$height"/></xsl:attribute>
						</rect>
						<foreignObject  width="600">
							<xsl:attribute name="height"><xsl:value-of select="$height"/></xsl:attribute>
							<body xmlns="http://www.w3.org/1999/xhtml">
								<!-- xsl:value-of select="date/text()"/ -->
								<pre>
									<xsl:value-of select="content/text()"/>
								</pre>
							</body>
						</foreignObject>
	
						<animate fill="freeze" dur="0.1s"
									from="none" to="block" attributeName="display">
							<xsl:attribute name="begin">msg<xsl:value-of select="position()"/>.mouseover</xsl:attribute>
						</animate>
						
						<animate fill="freeze" dur="0.1s"
									from="block" to="none" attributeName="display">
								<xsl:attribute name="begin">msg<xsl:value-of select="position()"/>.mouseout</xsl:attribute>
						</animate>
						
					</g>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="arrow">
		<xsl:param name="x" select="0"/>
		<xsl:param name="y" select="0"/>
		<xsl:param name="d" select="0"/>
		<polygon class='arrowhead'>
			<xsl:attribute name="points">
				<xsl:value-of select="$x"/>,<xsl:value-of select="$y"/><xsl:text> </xsl:text>
				<xsl:value-of select="$x + 5 * $d"/>,<xsl:value-of select="$y - 3"/><xsl:text> </xsl:text>
				<xsl:value-of select="$x + 3 * $d"/>,<xsl:value-of select="$y"/><xsl:text> </xsl:text>
				<xsl:value-of select="$x + 5 * $d"/>,<xsl:value-of select="$y + 3"/>
			</xsl:attribute>
		</polygon>
	</xsl:template>
	
	<xsl:template name="loopback">
		<xsl:param name="x" select="0"/>
		<xsl:param name="y" select="0"/>
		<polyline class='message-line'>
			<xsl:attribute name="points">
				<xsl:value-of select="$x"/>,<xsl:value-of select="$y - 15"/><xsl:text> </xsl:text>
				<xsl:value-of select="$x + 20"/>,<xsl:value-of select="$y - 15"/><xsl:text> </xsl:text>
				<xsl:value-of select="$x + 20"/>,<xsl:value-of select="$y"/><xsl:text> </xsl:text>
				<xsl:value-of select="$x"/>,<xsl:value-of select="$y"/>
			</xsl:attribute>
		</polyline>
	</xsl:template>
	
</xsl:stylesheet>
