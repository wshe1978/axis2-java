<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>
    <xsl:template match="/interface">
    /**
     * <xsl:value-of select="@name"/>.java
     *
     * This file was auto-generated from WSDL
     * by the Apache Axis2 version: #axisVersion# #today#
     */
    package <xsl:value-of select="@package"/>;
    /**
     *  <xsl:value-of select="@name"/> java skeleton for the axisService
     */
    public class <xsl:value-of select="@name"></xsl:value-of>
        <xsl:if test="@skeletonInterfaceName"> implements <xsl:value-of select="@skeletonInterfaceName"/></xsl:if>{
        <xsl:variable name="isbackcompatible" select="@isbackcompatible"/>
     <xsl:for-each select="method">
         <xsl:variable name="count"><xsl:value-of select="count(output/param)"/></xsl:variable>
         <xsl:variable name="outputtype" select="output/param/@type"/>
         <xsl:variable name="outputcomplextype"><xsl:value-of select="output/param/@complextype"/></xsl:variable>
         <xsl:variable name="outputparamcount"><xsl:value-of select="count(output/param[@location='body']/param)"/></xsl:variable>
         <!-- regardless of the sync or async status, the generated method signature would be just a usual
               java method -->
        /**
         * Auto generated method signature
         <!--  select only the body parameters  -->
         <xsl:choose>
            <xsl:when test="$isbackcompatible = 'true'">
                    <xsl:variable name="inputcount" select="count(input/param[@location='body' and @type!=''])"/>
                    <xsl:choose>
                        <xsl:when test="$inputcount=1">
                            <!-- Even when the parameters are 1 we have to see whether we have the
                                 wrapped parameters -->
                            <xsl:variable name="inputComplexType" select="input/param[@location='body' and @type!='']/@complextype"/>
                            <xsl:choose>
                                <xsl:when test="string-length(normalize-space($inputComplexType)) > 0">
                                   * @param<xsl:text> </xsl:text><xsl:value-of select="input/param[@location='body' and @type!='']/@name"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    * @param<xsl:text> </xsl:text><xsl:value-of select="input/param[@location='body' and @type!='']/@name"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                    </xsl:choose>
              </xsl:when>
            <xsl:otherwise>
                  <xsl:variable name="inputcount" select="count(input/param[@location='body' and @type!=''])"/>
                    <xsl:choose>
                        <xsl:when test="$inputcount=1">
                            <!-- Even when the parameters are 1 we have to see whether we have the
                                 wrapped parameters -->
                            <xsl:variable name="inputWrappedCount" select="count(input/param[@location='body' and @type!='']/param)"/>
                            <xsl:choose>
                                <xsl:when test="$inputWrappedCount &gt; 0">
                                   <xsl:for-each select="input/param[@location='body' and @type!='']/param">
                                     * @param<xsl:text> </xsl:text><xsl:value-of select="@name"/>
                                    </xsl:for-each>
                                </xsl:when>
                                <xsl:otherwise>
                                     * @param<xsl:text> </xsl:text><xsl:value-of select="input/param[@location='body' and @type!='']/@name"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                    </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
         */
        <xsl:choose>
            <xsl:when test="$isbackcompatible = 'true'">
                  public  <xsl:choose><xsl:when test="$count=0 or $outputtype=''">void</xsl:when>
                    <xsl:when test="string-length(normalize-space($outputcomplextype)) > 0"><xsl:value-of select="$outputcomplextype"/></xsl:when>
                    <xsl:when test="$outputtype!=''"><xsl:value-of select="$outputtype"/></xsl:when>
                    </xsl:choose><xsl:text> </xsl:text><xsl:value-of select="@name"/>
                  (
                  <xsl:variable name="inputcount" select="count(input/param[@location='body' and @type!=''])"/>
                        <xsl:choose>
                            <xsl:when test="$inputcount=1">
                                <!-- Even when the parameters are 1 we have to see whether we have the
                                     wrapped parameters -->
                                <xsl:variable name="inputComplexType" select="input/param[@location='body' and @type!='']/@complextype"/>
                                <xsl:choose>
                                    <xsl:when test="string-length(normalize-space($inputComplexType)) > 0">
                                       <xsl:value-of select="$inputComplexType"/><xsl:text> </xsl:text><xsl:value-of select="input/param[@location='body' and @type!='']/@name"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="input/param[@location='body' and @type!='']/@type"/><xsl:text> </xsl:text><xsl:value-of select="input/param[@location='body' and @type!='']/@name"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>
                            <xsl:otherwise><!-- Just leave it - nothing we can do here --></xsl:otherwise>
                        </xsl:choose>
                  )
            </xsl:when>
            <xsl:otherwise>

                 public <xsl:choose>
                    <xsl:when test="$count=0 or $outputtype=''">void</xsl:when>
                    <xsl:when test="$outputparamcount=1"><xsl:value-of select="output/param[@location='body']/param/@type"/></xsl:when>
                    <xsl:when test="string-length(normalize-space($outputcomplextype)) > 0"><xsl:value-of select="$outputcomplextype"/></xsl:when>
                    <xsl:otherwise><xsl:value-of select="$outputtype"/></xsl:otherwise></xsl:choose>
                <xsl:text> </xsl:text><xsl:value-of select="@name"/>
                  (
                  <xsl:variable name="inputcount" select="count(input/param[@location='body' and @type!=''])"/>
                        <xsl:choose>
                            <xsl:when test="$inputcount=1">
                                <!-- Even when the parameters are 1 we have to see whether we have the
                                     wrapped parameters -->
                                <xsl:variable name="inputWrappedCount" select="count(input/param[@location='body' and @type!='']/param)"/>
                                <xsl:choose>
                                    <xsl:when test="$inputWrappedCount &gt; 0">
                                       <xsl:for-each select="input/param[@location='body' and @type!='']/param">
                                            <xsl:if test="position()>1">,</xsl:if><xsl:value-of select="@type"/><xsl:text> </xsl:text><xsl:value-of select="@name"/>
                                        </xsl:for-each>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="input/param[@location='body' and @type!='']/@type"/><xsl:text> </xsl:text><xsl:value-of select="input/param[@location='body' and @type!='']/@name"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>
                            <xsl:otherwise><!-- Just leave it - nothing we can do here --></xsl:otherwise>
                        </xsl:choose>
                  )
            </xsl:otherwise>
        </xsl:choose>
         <!--add the faults-->
           <xsl:for-each select="fault/param[@type!='']">
               <xsl:if test="position()=1">throws </xsl:if>
               <xsl:if test="position()>1">,</xsl:if><xsl:value-of select="@name"/>
           </xsl:for-each>{
                //TODO : fill this with the necessary business logic
                <xsl:if test="string-length(normalize-space($outputtype)) &gt; 0">throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#<xsl:value-of select="@name"/>");</xsl:if>
        }
     </xsl:for-each>
    }
    </xsl:template>
 </xsl:stylesheet>