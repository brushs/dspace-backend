<?xml version="1.0" encoding="UTF-8" ?>
<!-- http://www.openarchives.org/OAI/2.0/oai_dc.xsd -->
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />
	
	<xsl:template match="/">
		<qdc:qualifieddc xmlns:qdc="http://dspace.org/qualifieddc/"
				xmlns:dc="http://purl.org/dc/elements/1.1/"
				xmlns:dcterms="http://purl.org/dc/terms/"
				xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd http://dspace.org/qualifieddc/ http://www.ukoln.ac.uk/metadata/dcmi/xmlschema/qualifieddc.xsd">
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
				<dc:title><xsl:value-of select="." /></dc:title>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='creator']/doc:element/doc:field[@name='value']">
				<dc:creator>
					<xsl:value-of select="." />
				</dc:creator>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
				<dc:creator>
					<xsl:value-of select="." />
				</dc:creator>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author']/doc:element/doc:field[@name='value']">
				<dc:contributor>
					<xsl:value-of select="." />
				</dc:contributor>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
				<dc:subject>
					<xsl:value-of select="." />
				</dc:subject>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
				<dcterms:abstract>
					<xsl:value-of select="." />
				</dcterms:abstract>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field[@name='value']">
				<dcterms:available>
					<xsl:value-of select="." />
				</dcterms:available>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
				<dcterms:issued>
					<xsl:value-of select="." />
				</dcterms:issued>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
				<dc:type>
					<xsl:value-of select="." />
				</dc:type>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element/doc:element/doc:field[@name='value']">
				<dc:identifier>
					<xsl:value-of select="." />
				</dc:identifier>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:element/doc:field[@name='value']">
				<dc:language>
					<xsl:value-of select="." />
				</dc:language>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:element/doc:field[@name='value']">
				<dc:relation>
					<xsl:value-of select="." />
				</dc:relation>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value']">
				<dc:relation>
					<xsl:value-of select="." />
				</dc:relation>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:element/doc:field[@name='value']">
				<dc:rights>
					<xsl:value-of select="." />
				</dc:rights>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value']">
				<dc:rights>
					<xsl:value-of select="." />
				</dc:rights>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='bitstreams']/doc:element[@name='bitstream']/doc:field[@name='format']">
				<dc:format>
					<xsl:value-of select="." />
				</dc:format>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element/doc:field[@name='value']">
				<dc:coverage>
					<xsl:value-of select="." />
				</dc:coverage>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element/doc:element/doc:field[@name='value']">
				<dc:coverage>
					<xsl:value-of select="." />
				</dc:coverage>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
				<dc:publisher>
					<xsl:value-of select="." />
				</dc:publisher>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:element/doc:field[@name='value']">
				<dc:publisher>
					<xsl:value-of select="." />
				</dc:publisher>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element/doc:field[@name='value']">
				<dc:source>
					<xsl:value-of select="." />
				</dc:source>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element/doc:element/doc:field[@name='value']">
				<dc:source>
					<xsl:value-of select="." />
				</dc:source>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='citation']/doc:element/doc:field[@name='value']">
				<dcterms:bibliographicCitation>
					<xsl:value-of select="." />
				</dcterms:bibliographicCitation>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='license']/doc:element/doc:field[@name='value']">
				<dcterms:license>
					<xsl:value-of select="." />
				</dcterms:license>
			</xsl:for-each>
			<!-- nrcan.filetype -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='filetype']/doc:element/doc:field[@name='value']">
				<dc:format><xsl:value-of select="." /></dc:format>
			</xsl:for-each>
			<!-- nrcan.openaccess -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='openaccess']/doc:element/doc:field[@name='value']">
				<dcterms:license><xsl:value-of select="." /></dcterms:license>
			</xsl:for-each>
			<!-- geospatial.polygon -->
			<xsl:for-each select="doc:metadata/doc:element[@name='geospatial']/doc:element[@name='polygon']/doc:element/doc:field[@name='value']">
				<dcterms:spatial><xsl:value-of select="." /></dcterms:spatial>
			</xsl:for-each>
			<!-- geospatial.bbox -->
			<xsl:for-each select="doc:metadata/doc:element[@name='geospatial']/doc:element[@name='bbox']/doc:element/doc:field[@name='value']">
				<dcterms:spatial><xsl:value-of select="." /></dcterms:spatial>
			</xsl:for-each>
			<!-- nrcan.nts -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='nts']/doc:element/doc:field[@name='value']">
				<dcterms:spatial><xsl:value-of select="." /></dcterms:spatial>
			</xsl:for-each>
			<!-- nrcan.area -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='area']/doc:element/doc:field[@name='value']">
				<dcterms:coverage><xsl:value-of select="." /></dcterms:coverage>
			</xsl:for-each>
			<!-- nrcan.province.name -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='province']/doc:element[@name='name']/doc:element/doc:field[@name='value']">
				<dcterms:coverage><xsl:value-of select="." /></dcterms:coverage>
			</xsl:for-each>
			<!-- nrcan.country.name -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='country']/doc:element[@name='name']/doc:element/doc:field[@name='value']">
				<dcterms:coverage><xsl:value-of select="." /></dcterms:coverage>
			</xsl:for-each>
			<!-- nrcan.sponsor.program -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='sponsor']/doc:element[@name='program']/doc:element/doc:field[@name='value']">
				<dcterms:contributor><xsl:value-of select="." /></dcterms:contributor>
			</xsl:for-each>
			<!-- nrcan.sponsor.project -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='sponsor']/doc:element[@name='project']/doc:element/doc:field[@name='value']">
				<dcterms:contributor><xsl:value-of select="." /></dcterms:contributor>
			</xsl:for-each>
			<!-- nrcan.sponsor.code -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='sponsor']/doc:element[@name='code']/doc:element/doc:field[@name='value']">
				<dcterms:contributor><xsl:value-of select="." /></dcterms:contributor>
			</xsl:for-each>
			<!-- nrcan.media -->
			<xsl:for-each select="doc:metadata/doc:element[@name='nrcan']/doc:element[@name='media']/doc:element/doc:field[@name='value']">
				<dcterms:medium><xsl:value-of select="." /></dcterms:medium>
			</xsl:for-each>
		</qdc:qualifieddc>
	</xsl:template>
</xsl:stylesheet>
