
package org.apache.axis2.jaxws.sample.faultsservice;

import org.test.polymorphicfaults.*;
import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0_01-b15-fcs
 * Generated source version: 2.0
 * 
 */
@WebFault(name = "BaseFault", targetNamespace = "http://org/test/polymorphicfaults")
public class BaseFault_Exception
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private BaseFault faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public BaseFault_Exception(String message, BaseFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param cause
     * @param message
     * @param faultInfo
     */
    public BaseFault_Exception(String message, BaseFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.test.polymorphicfaults.BaseFault
     */
    public BaseFault getFaultInfo() {
        return faultInfo;
    }

}