/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.axis2.jaxws.description.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Future;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam.Mode;
import javax.jws.WebResult;
import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.Response;
import javax.xml.ws.ResponseWrapper;
import javax.xml.ws.WebFault;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisOperationFactory;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.description.EndpointDescriptionJava;
import org.apache.axis2.jaxws.description.EndpointInterfaceDescription;
import org.apache.axis2.jaxws.description.FaultDescription;
import org.apache.axis2.jaxws.description.OperationDescription;
import org.apache.axis2.jaxws.description.OperationDescriptionJava;
import org.apache.axis2.jaxws.description.OperationDescriptionWSDL;
import org.apache.axis2.jaxws.description.ParameterDescription;
import org.apache.axis2.jaxws.description.ParameterDescriptionJava;
import org.apache.axis2.jaxws.description.builder.DescriptionBuilderComposite;
import org.apache.axis2.jaxws.description.builder.MethodDescriptionComposite;
import org.apache.axis2.jaxws.description.builder.OneWayAnnot;
import org.apache.axis2.jaxws.description.builder.ParameterDescriptionComposite;
import org.apache.axis2.wsdl.WSDLConstants;

/**
 * @see ../OperationDescription
 *
 */
// TODO: Axis2 does not support overloaded operations, although EndpointInterfaceDescription.addOperation() does support overloading
//       of methods represented by OperationDescription classes.  However, the AxisOperation contained in an OperationDescription
//       does NOT support overloaded methods.
//
//       While overloading is not supported by WS-I, it IS supported by JAX-WS (p11).
//       Note that this requires support in Axis2; currently WSDL11ToAxisServiceBuilder.populateOperations does not
//       support overloaded methods in the WSDL; the operations are stored on AxisService as children in a HashMap with the wsdl
//       operation name as the key.

// TODO: Need tests for all the "default" code paths in the annotation getters.
// TODO: Need tests for each when annotation is not present where that is allowed by the spec. 
class OperationDescriptionImpl implements OperationDescription, OperationDescriptionJava, OperationDescriptionWSDL {
    private EndpointInterfaceDescription parentEndpointInterfaceDescription;
    private AxisOperation axisOperation;
    private QName operationName;
    private Method seiMethod;
    private MethodDescriptionComposite methodComposite;
    private ParameterDescription[] parameterDescriptions;
    private FaultDescription[] faultDescriptions;
    
    // ===========================================
    // ANNOTATION related information
    // ===========================================
    
    // ANNOTATION: @Oneway
    private Oneway              onewayAnnotation;
    private Boolean             onewayIsOneway;
    
    // ANNOTATION: @RequestWrapper
    private RequestWrapper      requestWrapperAnnotation;
    private String              requestWrapperTargetNamespace;
    private String              requestWrapperLocalName;
    private String              requestWrapperClassName;
    
    // ANNOTATION: @ResponseWrapper
    private ResponseWrapper     responseWrapperAnnotation;
    private String              responseWrapperLocalName;
    private String              responseWrapperTargetNamespace;
    private String              responseWrapperClassName;
    
    // ANNOTATION: @SOAPBinding
    // Note this is the Method-level annotation.  See EndpointInterfaceDescription for the Type-level annotation
    // Also note this annotation is only allowed on methods if SOAPBinding.Style is DOCUMENT and if the method-level
    // annotation is absent, the behavior defined on the Type is used.
    // per JSR-181 MR Sec 4.7 "Annotation: javax.jws.soap.SOAPBinding" pg 28
    private SOAPBinding         soapBindingAnnotation;
    // REVIEW: Should this be using the jaxws annotation values or should that be wrappered?
    private javax.jws.soap.SOAPBinding.Style            soapBindingStyle;
    public static final javax.jws.soap.SOAPBinding.Style SoapBinding_Style_VALID = javax.jws.soap.SOAPBinding.Style.DOCUMENT;
    private javax.jws.soap.SOAPBinding.Use              soapBindingUse;
    // Default value per JSR-181 MR Sec 4.7 "Annotation: javax.jws.soap.SOAPBinding" pg 28
    public static final javax.jws.soap.SOAPBinding.Use  SOAPBinding_Use_DEFAULT = javax.jws.soap.SOAPBinding.Use.LITERAL;
    private javax.jws.soap.SOAPBinding.ParameterStyle   soapBindingParameterStyle;
    // Default value per JSR-181 MR Sec 4.7 "Annotation: javax.jws.soap.SOAPBinding" pg 28
    public static final javax.jws.soap.SOAPBinding.ParameterStyle SOAPBinding_ParameterStyle_DEFAULT = javax.jws.soap.SOAPBinding.ParameterStyle.WRAPPED;

    // ANNOTATION: @WebMethod
    private WebMethod           webMethodAnnotation;
    private String              webMethodOperationName;
    // Default value per JSR-181 MR Sec 4.2, pg 17
    public static final String  WebMethod_Action_DEFAULT = "";
    private String              webMethodAction;
    // Default value per JSR-181 MR sec 4.2, pg 17
    public static final Boolean WebMethod_Exclude_DEFAULT = new Boolean(false);
    private Boolean             webMethodExclude;
    
    // ANNOTATION: @WebParam
    private String[]            webParamNames;
    private Mode[]              webParamMode;
    private String[]            webParamTargetNamespace;

    
    // ANNOTATION: @WebResult
    private WebResult           webResultAnnotation;
    private String              webResultName;
    private String              webResultPartName;
    // Default value per JSR-181 MR Sec 4.5.1, pg 23
    public static final String  WebResult_TargetNamespace_DEFAULT = "";
    private String              webResultTargetNamespace;
    // Default value per JSR-181 MR sec 4.5, pg 24
    public static final Boolean WebResult_Header_DEFAULT = new Boolean(false);
    private Boolean             webResultHeader;
    private Method serviceImplMethod;
    private boolean serviceImplMethodFound = false;

    OperationDescriptionImpl(Method method, EndpointInterfaceDescription parent) {
        // TODO: Look for WebMethod anno; get name and action off of it
        parentEndpointInterfaceDescription = parent;
        setSEIMethod(method);

        
        this.operationName = new QName(getOperationName());
    }
    
    OperationDescriptionImpl(AxisOperation operation, EndpointInterfaceDescription parent) {
        parentEndpointInterfaceDescription = parent;
        axisOperation = operation;
        this.operationName = axisOperation.getName();
    }

    OperationDescriptionImpl(	MethodDescriptionComposite mdc, 
    							EndpointInterfaceDescription parent, 
    							AxisOperation axisOperation) {

        parentEndpointInterfaceDescription = parent;
        methodComposite = mdc;
        this.operationName = new QName(getOperationName());
        webMethodAnnotation = methodComposite.getWebMethodAnnot();

        this.axisOperation = axisOperation;
        
        //If an AxisOperation was already created for us by populateService then just use that onw
        //Otherwise, build it up here
        if (this.axisOperation == null) {
        	
        	try {
        		if (isOneWay()) {               
        			axisOperation = AxisOperationFactory.getOperationDescription(WSDLConstants.WSDL20_2004Constants.MEP_URI_IN_ONLY);
        		} else {
        			axisOperation = AxisOperationFactory.getOperationDescription(WSDLConstants.WSDL20_2004Constants.MEP_URI_IN_OUT);
        		}
        		//TODO: There are several other MEP's, such as: OUT_ONLY, IN_OPTIONAL_OUT, OUT_IN, OUT_OPTIONAL_IN, ROBUST_OUT_ONLY,
        		//                                              ROBUST_IN_ONLY
        		//      Determine how these MEP's should be handled, if at all
        		
        	} catch (Exception e) {
        		AxisFault ex = new AxisFault("OperationDescriptionImpl:cons - unable to build AxisOperation ");
        	}
            
        	if (axisOperation != null){
        		
        		axisOperation.setName(determineOperationQName(this.methodComposite));
        		axisOperation.setSoapAction(this.getAction());
        		
        		
        		//TODO: Determine other axisOperation values that may need to be set
        		//      Currently, the following values are being set on AxisOperation in 
        		//      ServiceBuilder.populateService which we are not setting:
        		//          AxisOperation.setPolicyInclude()
        		//          AxisOperation.setWsamappingList()
        		//          AxisOperation.setOutputAction()
        		//          AxisOperation.addFaultAction()
        		//          AxisOperation.setFaultMessages()
        		
        		// TODO: The WSMToAxisServiceBuilder sets the message receiver, not sure why this is done
        		//       since AxisService.addOperation does this as well by setting it to a default
        		//       MessageReceiver...it appears that this code is also setting it to a default
        		//       receiver..need to understand this
        		
        		/*
        		 String messageReceiverClass = "org.apache.axis2.rpc.receivers.RPCMessageReceiver";
        		 if(wsmOperation.isOneWay()){
        		 messageReceiverClass = "org.apache.axis2.rpc.receivers.RPCInOnlyMessageReceiver";
        		 }
        		 try{
        		 MessageReceiver msgReceiver = (MessageReceiver)Class.forName(messageReceiverClass).newInstance();
        		 axisOperation.setMessageReceiver(msgReceiver);
        		 
        		 }catch(Exception e){
        		 }
        		 */
        		
        		//TODO: Need to process the other annotations that can exist, on the server side
        		//      and at the method level.
        		//      They are, as follows:       
        		//          WebResultAnnot (181)
        		//          HandlerChain
        		//          SoapBinding (181)
        		//          WebServiceRefAnnot (List) (JAXWS)
        		//          WebServiceContextAnnot (JAXWS via injection)
        		//          RequestWrapper (JAXWS)
        		//          ResponseWrapper (JAXWS)	
        	}
        	
        	this.axisOperation = axisOperation;
        }
        
		parameterDescriptions = createParameterDescriptions();
		faultDescriptions = createFaultDescriptions();
    }
    
    void setSEIMethod(Method method) {
        if (seiMethod != null) {
            // TODO: This is probably an error, but error processing logic is incorrect
            throw new UnsupportedOperationException("Can not set an SEI method once it has been set.");
        }
        else  {
            seiMethod = method;
            webMethodAnnotation = seiMethod.getAnnotation(WebMethod.class);
            parameterDescriptions = createParameterDescriptions();
            faultDescriptions = createFaultDescriptions();
        }
    }

    public EndpointInterfaceDescription getEndpointInterfaceDescription() {
        return parentEndpointInterfaceDescription;
    }

    public EndpointInterfaceDescriptionImpl getEndpointInterfaceDescriptionImpl() {
        return (EndpointInterfaceDescriptionImpl) parentEndpointInterfaceDescription;
    }

    public AxisOperation getAxisOperation() {
        return axisOperation;
    }
    
    public QName getName() {
        return operationName;
    }
    
    // Java-related getters
    public String getJavaMethodName() {
        String returnString = null;
        
        if (!isDBC()) {
            if (seiMethod != null) {
                returnString = seiMethod.getName();
            }
        } else {
            if (methodComposite != null) {
                returnString = methodComposite.getMethodName();
            }
        }
        
        return returnString;
    }
    
    public String[] getJavaParameters() {
        
        ArrayList<String> returnParameters = new ArrayList<String>();
        
        if (!isDBC()) {
            if (seiMethod != null) {
                Class[] paramaters = seiMethod.getParameterTypes();
                for (Class param:paramaters) {
                    returnParameters.add(param.getName());
                }
            }
    
        } 
        else {
            if (methodComposite != null) {
                
                Iterator<ParameterDescriptionComposite> iter = 
                    methodComposite.getParameterDescriptionCompositeList().iterator();
                while (iter.hasNext()) {
                    returnParameters.add(iter.next().getParameterType());
                }
            }
        }
        
        // TODO: This is different than the rest, which return null instead of an empty array
        return returnParameters.toArray(new String[0]);
    }
    /**
     * Note this will return NULL unless the operation was built via introspection on the SEI.
     * In other words, it will return null if the operation was built with WSDL.
     * @return
     */
    public Method getSEIMethod() {
        return seiMethod;
    }
    
    MethodDescriptionComposite getMethodDescriptionComposite() {
        return methodComposite;
    }
    
    private boolean isWrappedParameters() {
        return getSoapBindingParameterStyle() == javax.jws.soap.SOAPBinding.ParameterStyle.WRAPPED;
    }
    
    private ParameterDescription[] createParameterDescriptions() {
        
        ArrayList<ParameterDescription> buildParameterList = new ArrayList<ParameterDescription>();
    
        if (!isDBC()) {
            Class[] parameters = seiMethod.getParameterTypes();
            Type[] paramaterTypes = seiMethod.getGenericParameterTypes();
            Annotation[][] annotations = seiMethod.getParameterAnnotations();
            
            for(int i = 0; i < parameters.length; i++) {
                ParameterDescription paramDesc = new ParameterDescriptionImpl(i, parameters[i], paramaterTypes[i], annotations[i], this);
                buildParameterList.add(paramDesc);
            }
    
        } else {
            ParameterDescriptionComposite pdc = null;
            Iterator<ParameterDescriptionComposite> iter = 
                                methodComposite.getParameterDescriptionCompositeList().iterator();
            
            for (int i = 0; i < methodComposite.getParameterDescriptionCompositeList().size(); i++) {
                ParameterDescription paramDesc = 
                                new ParameterDescriptionImpl(  i, 
                                                            methodComposite.getParameterDescriptionComposite(i), 
                                                            this);
                buildParameterList.add(paramDesc);
            }
        }
        
        return buildParameterList.toArray(new ParameterDescription[buildParameterList.size()]);
    
    }

    private FaultDescription[] createFaultDescriptions() {
        
        ArrayList<FaultDescription> buildFaultList = new ArrayList<FaultDescription>();
        
        if (!isDBC()) {
            // get exceptions this method "throws"
            Class[] webFaultClasses = seiMethod.getExceptionTypes();

            for(Class wfClass:webFaultClasses) {
                // according to JAXWS 3.7, the @WebFault annotation is only used for customizations,
                // so we'll add all declared exceptions
                WebFault wfanno = null;
                for (Annotation anno:wfClass.getAnnotations()) {
                    if (anno.annotationType() == WebFault.class) {
                        wfanno = (WebFault)anno;
                    }
                }
                buildFaultList.add(new FaultDescriptionImpl(wfClass, wfanno, this));
            }
        } else {
            // TODO do I care about methodComposite like the paramDescription does?
        	//Call FaultDescriptionImpl for all non-generic exceptions...Need to check a
        	// a couple of things
        	// 1. If this is a generic exception, ignore it
        	// 2. If this is not a generic exception, then find it in the DBC Map
        	//       If not found in map, then throw not found exception
         	//3. Pass the validated WebFault dbc and possibly the classImpl dbc to FaultDescription
        	//4. Possibly set AxisOperation.setFaultMessages array...or something like that
        	
        	String[] webFaultClassNames = methodComposite.getExceptions();
        	
			HashMap<String, DescriptionBuilderComposite> dbcMap = 
				getEndpointInterfaceDescriptionImpl().getEndpointDescriptionImpl().getServiceDescriptionImpl().getDBCMap();
			
			if (webFaultClassNames != null) {
				for (String wfClassName:webFaultClassNames) {
					//	Try to find this exception class in the dbc list. If we can't find it
					//  then just assume that its a generic exception.
					
					DescriptionBuilderComposite faultDBC = dbcMap.get(wfClassName);
					
					if (faultDBC != null){
                        // JAXWS 3.7 does not require @WebFault annotation
                        // We found a valid exception composite thats annotated
                        buildFaultList.add(new FaultDescriptionImpl(faultDBC, this));
     			    }
                                       
				}
			}
        }
    
        return buildFaultList.toArray(new FaultDescription[0]);
    }
    
    // =====================================
    // ANNOTATION: WebMethod
    // =====================================
    public WebMethod getAnnoWebMethod() {
        return webMethodAnnotation;
    }
    
    static QName determineOperationQName(Method javaMethod) {
        return new QName(determineOperationName(javaMethod));
    }
    
    //TODO: For now, we are overriding the above method only because it is static, these should
    //be combined at some point
    public static QName determineOperationQName(MethodDescriptionComposite mdc) {
        return new QName(determineOperationName(mdc));
    }
    
    //TODO: Deprecate this after we use only DBC objects
    private static String determineOperationName(Method javaMethod) {
        
        String operationName = null;
        
        WebMethod wmAnnotation = javaMethod.getAnnotation(WebMethod.class);
        // Per JSR-181 MR Sec 4.2 "Annotation: javax.jws.WebMethod" pg 17,
        // if @WebMethod specifies and operation name, use that.  Otherwise
        // default is the Java method name
        if (wmAnnotation != null && !DescriptionUtils.isEmpty(wmAnnotation.operationName())) {
            operationName = wmAnnotation.operationName();
        }
        else {
            operationName = javaMethod.getName();
        }
        
        return operationName;    
    }

    //TODO: For now, we are overriding the above method only because it is static, these should
    //be combined at some point
    private static String determineOperationName(MethodDescriptionComposite mdc) {
        String operationName = null;
        
        WebMethod wmAnnotation = mdc.getWebMethodAnnot();
        if (wmAnnotation != null && !DescriptionUtils.isEmpty(wmAnnotation.operationName())) {
            operationName = wmAnnotation.operationName();
        }
        else {
            operationName = mdc.getMethodName();
        }

        return operationName;
    }
    
    public String getOperationName() {
        // REVIEW: WSDL/Anno merge
        return getAnnoWebMethodOperationName();
    }
    public String getAnnoWebMethodOperationName() {
        if (webMethodOperationName == null) {
            if (!isDBC())
                webMethodOperationName = determineOperationName(seiMethod);
            else
                webMethodOperationName = determineOperationName(methodComposite);       
        }
        return webMethodOperationName;
    }
    
    public String getAction() {
        // REVIEW: WSDL/Anno merge
        return getAnnoWebMethodAction();
    }
    
    public String getAnnoWebMethodAction() {
        if (webMethodAction == null) {
            if (getAnnoWebMethod() != null && !DescriptionUtils.isEmpty(getAnnoWebMethod().action())) {
                webMethodAction = getAnnoWebMethod().action();
            }
            else {
                webMethodAction = WebMethod_Action_DEFAULT;
            }
        }
        return webMethodAction;
    }
    
    public boolean isExcluded() {
        // REVIEW: WSDL/Annotation merge
        return getAnnoWebMethodExclude();
    }
    public boolean getAnnoWebMethodExclude() {
        if (webMethodExclude == null) {
            // TODO: Validation: if this attribute specified, no other elements allowed per JSR-181 MR Sec 4.2, pg 17
            // TODO: Validation: This element is not allowed on endpoint interfaces
            // Unlike the elements with a String value, if the annotation is present, exclude will always 
            // return a usable value since it will default to FALSE if the element is not present.
            if (getAnnoWebMethod() != null) {
                webMethodExclude = new Boolean(getAnnoWebMethod().exclude());
            }
            else {
                webMethodExclude = WebMethod_Exclude_DEFAULT;
            }
        }
        
        return webMethodExclude.booleanValue();
    }
    
    // ==========================================
    // ANNOTATION: RequestWrapper
    // ==========================================
    public RequestWrapper getAnnoRequestWrapper() {
        if (requestWrapperAnnotation == null) {
            if (!isDBC()) {
                requestWrapperAnnotation = seiMethod.getAnnotation(RequestWrapper.class); 
            } else {
                requestWrapperAnnotation = methodComposite.getRequestWrapperAnnot(); 
            }       
        }
        return requestWrapperAnnotation;
    }
    
    public String getRequestWrapperLocalName() {
        // REVIEW: WSDL/Anno merge
        return getAnnoRequestWrapperLocalName();
    }
    
    /**
     * For wrapped parameter style (based on the annotation and the WSDL), returns the 
     * wrapper value.  For non-wrapped (i.e. bare) parameter style, returns null.
     * @return
     */
    public String getAnnoRequestWrapperLocalName() {
        if (!isWrappedParameters()) {
            // A wrapper is only meaningful for wrapped parameters
            return null;
        }
        if (requestWrapperLocalName == null) {
            if (getAnnoRequestWrapper() != null
                    && !DescriptionUtils.isEmpty(getAnnoRequestWrapper().localName())) {
                requestWrapperLocalName = getAnnoRequestWrapper().localName();
            } else {
                // The default value of localName is the value of operationName as
                // defined in the WebMethod annotation. [JAX-WS Sec. 7.3, p. 80]
                requestWrapperLocalName = getAnnoWebMethodOperationName();
            }
        }
        return requestWrapperLocalName;
    }
    public String getRequestWrapperTargetNamespace() {
        // REVIEW: WSDL/Anno merge
        return getAnnoRequestWrapperTargetNamespace();
    }
    /**
     * For wrapped parameter style (based on the annotation and the WSDL), returns the 
     * wrapper value.  For non-wrapped (i.e. bare) parameter style, returns null.
     * @return
     */
    public String getAnnoRequestWrapperTargetNamespace() {
        if (!isWrappedParameters()) {
            // A wrapper is only meaningful for wrapped parameters
            return null;
        }
        if (requestWrapperTargetNamespace == null) {
            if (getAnnoRequestWrapper() != null && !DescriptionUtils.isEmpty(getAnnoRequestWrapper().targetNamespace())) {
                requestWrapperTargetNamespace = getAnnoRequestWrapper().targetNamespace();
            }
            else {
                // The default value for targetNamespace is the target namespace of the SEI. [JAX-WS Sec 7.3, p. 80]
                requestWrapperTargetNamespace = getEndpointInterfaceDescription().getTargetNamespace();
            }
        }
        return requestWrapperTargetNamespace;
    }
    
    public String getRequestWrapperClassName() {
        // REVIEW: WSDL/Anno merge
        return getAnnoRequestWrapperClassName();
    }
    /**
     * For wrapped parameter style (based on the annotation and the WSDL), returns the 
     * wrapper value.  For non-wrapped (i.e. bare) parameter style, returns null.
     * @return
     */
    public String getAnnoRequestWrapperClassName() {
        if (!isWrappedParameters()) {
            // A wrapper is only meaningful for wrapped parameters
            return null;
        }
        if (requestWrapperClassName == null) {
            if (getAnnoRequestWrapper() != null && !DescriptionUtils.isEmpty(getAnnoRequestWrapper().className())) {
                requestWrapperClassName = getAnnoRequestWrapper().className();
            }
            else {
                // Not sure what the default value should be (if any).  None is listed in Sec. 7.3 on p. 80 of
                // the JAX-WS spec, BUT Conformance(Using javax.xml.ws.RequestWrapper) in Sec 2.3.1.2 on p. 13
                // says the entire annotation "...MAY be omitted if all its properties would have default vaules."
                // implying there IS some sort of default.  We'll try this for now:
                if (isDBC()) {
                    requestWrapperClassName = this.methodComposite.getDeclaringClass(); 
                } else {
                    Class clazz = seiMethod.getDeclaringClass();
                    String packageName = clazz.getPackage().getName();
                    String className = DescriptionUtils.javaMethodtoClassName(seiMethod.getName());
                    requestWrapperClassName = packageName + "." + className;
                }
                requestWrapperClassName = DescriptionUtils.determineActualAritfactPackage(requestWrapperClassName);
            }
        }
        return requestWrapperClassName;
    }
    
    // ===========================================
    // ANNOTATION: ResponseWrapper
    // ===========================================
    public ResponseWrapper getAnnoResponseWrapper() {
        if (responseWrapperAnnotation == null) {
            if (!isDBC()) {
                responseWrapperAnnotation = seiMethod.getAnnotation(ResponseWrapper.class);
            } else {
                responseWrapperAnnotation = methodComposite.getResponseWrapperAnnot();              
            }
        }
        return responseWrapperAnnotation;
    }
    public String getResponseWrapperLocalName() {
        return getAnnoResponseWrapperLocalName();
    }
    /**
     * For wrapped parameter style (based on the annotation and the WSDL), returns the 
     * wrapper value.  For non-wrapped (i.e. bare) parameter style, returns null.
     * @return
     */
    public String getAnnoResponseWrapperLocalName() {
        if (!isWrappedParameters()) {
            // A wrapper is only meaningful for wrapped parameters
            return null;
        }
        if (responseWrapperLocalName == null) {
            if (getAnnoResponseWrapper() != null && !DescriptionUtils.isEmpty(getAnnoResponseWrapper().localName())) {
                responseWrapperLocalName = getAnnoResponseWrapper().localName();
            }
            else { 
                // The default value of localName is the value of operationName as 
                // defined in the WebMethod annotation appended with "Response". [JAX-WS Sec. 7.4, p. 81]
                responseWrapperLocalName = getAnnoWebMethodOperationName() + "Response";
            }
        }
        return responseWrapperLocalName;
    }
    
    public String getResponseWrapperTargetNamespace() {
        // REVIEW: WSDL/Anno merge
        return getAnnoResponseWrapperTargetNamespace();
    }
    /**
     * For wrapped parameter style (based on the annotation and the WSDL), returns the 
     * wrapper value.  For non-wrapped (i.e. bare) parameter style, returns null.
     * @return
     */
    public String getAnnoResponseWrapperTargetNamespace() {
        if (!isWrappedParameters()) {
            // A wrapper is only meaningful for wrapped parameters
            return null;
        }
        if (responseWrapperTargetNamespace == null) {
            if (getAnnoResponseWrapper() != null && !DescriptionUtils.isEmpty(getAnnoResponseWrapper().targetNamespace())) {
                responseWrapperTargetNamespace = getAnnoResponseWrapper().targetNamespace();
            }
            else {
                // The default value for targetNamespace is the target namespace of the SEI. [JAX-WS Sec 7.3, p. 80]
                // TODO: Implement getting the TNS from the SEI 
                responseWrapperTargetNamespace = getEndpointInterfaceDescription().getTargetNamespace();
            }
        }
        return responseWrapperTargetNamespace;
    }
    
    public String getResponseWrapperClassName() {
        // REVIEW: WSDL/Anno merge
        return getAnnoResponseWrapperClassName();
    }
    /**
     * For wrapped parameter style (based on the annotation and the WSDL), returns the 
     * wrapper value.  For non-wrapped (i.e. bare) parameter style, returns null.
     * @return
     */
    public String getAnnoResponseWrapperClassName() {
        if (!isWrappedParameters()) {
            // A wrapper is only meaningful for wrapped parameters
            return null;
        }
        if (responseWrapperClassName == null) {
            if (getAnnoResponseWrapper() != null && !DescriptionUtils.isEmpty(getAnnoResponseWrapper().className())) {
                responseWrapperClassName = getAnnoResponseWrapper().className();
            }
            else {
                // Not sure what the default value should be (if any).  None is listed in Sec. 7.4 on p. 81 of
                // the JAX-WS spec, BUT Conformance(Using javax.xml.ws.ResponseWrapper) in Sec 2.3.1.2 on p. 13
                // says the entire annotation "...MAY be omitted if all its properties would have default vaules."
                // implying there IS some sort of default.  We'll try this for now:
                if (!isDBC()) {
                    Class clazz = seiMethod.getDeclaringClass();
                    String packageName = clazz.getPackage().getName();
                    String className = DescriptionUtils.javaMethodtoClassName(seiMethod.getName());
                    responseWrapperClassName = packageName + "." + className + "Response";
                } else {
                	//JAXWS Spec is not clear on what default should be added. We think its the endpoint impls package + OperationName + Response.
                	//In situation where wsGen uses sei's package to store jaxb bean.
                	String declaringClazz = methodComposite.getDeclaringClass();
                	String packageName = declaringClazz.substring(0, declaringClazz.lastIndexOf("."));
                    responseWrapperClassName = packageName + "." + DescriptionUtils.javaMethodtoClassName(methodComposite.getMethodName()) + "Response";
                }
                responseWrapperClassName = DescriptionUtils.determineActualAritfactPackage(responseWrapperClassName);
            }
        }
        return responseWrapperClassName;
    }

    
    // ===========================================
    // ANNOTATION: WebFault
    // ===========================================

    /*
     * TODO some of the WebFault stuff should be moved to FaultDescription
     */
    
    /*
     *  TODO:  this will need revisited.  The problem is that a WebFault is not mapped 1:1 to an
     *  OperationDescription.  We should do a better job caching the information.  For now, I'm
     *  following the getWebParam() pattern.
     *  
     *  This is gonna get complicated.  One other thing to consider is that a method (opdesc) may declare
     *  several types of exceptions it throws
     *  
     */
    
    public FaultDescription[] getFaultDescriptions() {
        return faultDescriptions;
    }
    
    public FaultDescription resolveFaultByFaultBeanName(String faultBeanName) {
        if (faultDescriptions != null) {
            for(FaultDescription fd: faultDescriptions) {
                if (faultBeanName.equals(fd.getFaultBean()))
                    return fd;
            }
        }
        return null;
    }
    
    public FaultDescription resolveFaultByExceptionName(String exceptionClassName) {
        if (faultDescriptions != null) {
            for(FaultDescription fd: faultDescriptions) {
                if (exceptionClassName.equals(fd.getExceptionClassName()))
                    return fd;
            }
        }
        return null;
    }
    
    // ===========================================
    // ANNOTATION: WebParam
    // ===========================================
    // Note that this annotation is handled by the ParameterDescripton.
    // Methods are provided on OperationDescription as convenience methods.
    public ParameterDescription[] getParameterDescriptions() {
        return parameterDescriptions;
    }
    
    public ParameterDescription getParameterDescription(String parameterName) {
        // TODO: Validation: For BARE paramaterUse, only a single IN our INOUT paramater and a single output (either return or OUT or INOUT) is allowed 
        //       Per JSR-224, Sec 3.6.2.2, pg 37
        ParameterDescription matchingParamDesc = null;
        if (parameterName != null && !parameterName.equals("")) {
            for (ParameterDescription paramDesc:parameterDescriptions) {
                if (parameterName.equals(paramDesc.getParameterName())) {
                    matchingParamDesc = paramDesc;
                    break;
                }
            }
        }
        return matchingParamDesc;
    }
    
    public ParameterDescription getParameterDescription(int parameterNumber) {
        return parameterDescriptions[parameterNumber];
    }
    
    public String[] getParamNames() {
        // REVIEW: WSDL/Anno merge
        return getAnnoWebParamNames();
    }

    public String[] getAnnoWebParamNames() {
        if (webParamNames == null) {
            ArrayList<String> buildNames = new ArrayList<String>();
            ParameterDescription[] paramDescs = getParameterDescriptions();
            for (ParameterDescription currentParamDesc:paramDescs) {
                buildNames.add(currentParamDesc.getParameterName());
            }
            webParamNames = buildNames.toArray(new String[0]);
        }
        return webParamNames;
    }
    
    public String[] getAnnoWebParamTargetNamespaces(){
        if (webParamTargetNamespace == null) {
            ArrayList<String> buildTargetNS = new ArrayList<String>();
            ParameterDescription[] paramDescs = getParameterDescriptions();
            for (ParameterDescription currentParamDesc:paramDescs) {
                buildTargetNS.add(currentParamDesc.getTargetNamespace());
            }
            webParamTargetNamespace = buildTargetNS.toArray(new String[0]);
        }
        return webParamTargetNamespace;
    }

    public String getAnnoWebParamTargetNamespace(String name){
        String returnTargetNS = null;
        ParameterDescription paramDesc = getParameterDescription(name);
        if (paramDesc != null) {
            returnTargetNS = paramDesc.getTargetNamespace();
        }
        return returnTargetNS;
    }
    
             
    public Mode[] getAnnoWebParamModes(){
        if(webParamMode == null){
            ArrayList<Mode> buildModes = new ArrayList<Mode>();
            ParameterDescription[] paramDescs = getParameterDescriptions();
            for (ParameterDescription currentParamDesc:paramDescs) {
                // TODO: Consider new ParamDesc.Mode vs WebParam.Mode
                buildModes.add(((ParameterDescriptionJava) currentParamDesc).getAnnoWebParamMode());
            }
             webParamMode = buildModes.toArray(new Mode[0]);
        }
        return webParamMode;
    }
    public boolean isAnnoWebParamHeader(String name){
        ParameterDescription paramDesc = getParameterDescription(name);
        if (paramDesc != null) {
            return paramDesc.isHeader();
        }
        return false;    
    }
    
    // ===========================================
    // ANNOTATION: WebResult
    // ===========================================
    public WebResult getAnnoWebResult() {
        if (webResultAnnotation == null) {
            if (!isDBC()) {
                webResultAnnotation = seiMethod.getAnnotation(WebResult.class);
            } else {
                webResultAnnotation = methodComposite.getWebResultAnnot();
            }
            
        }
        return webResultAnnotation;
    }
    
    public boolean isWebResultAnnotationSpecified() {
        return getAnnoWebResult() != null;
    }

    public boolean isOperationReturningResult() {
        boolean isResult = false;
        if (!isAnnoOneWay()) {
            if (!isDBC()) {
                if (seiMethod.getReturnType() != Void.TYPE) {
                    isResult = true;
                }
            } else {
                if (!DescriptionUtils.isEmpty(methodComposite.getReturnType()) &&
                        !methodComposite.getReturnType().equals("void"))
                    isResult = true;
            }
        } 
        return isResult;
    }

    public String getResultName() {
        // REVIEW: WSDL/Anno merge
        return getAnnoWebResultName();
    }
    
    public String getAnnoWebResultName() {
        if (!isOperationReturningResult()) {
            return null;
        }
        if (webResultName == null) {
            if (getAnnoWebResult() != null && !DescriptionUtils.isEmpty(getAnnoWebResult().name())) {
                webResultName = getAnnoWebResult().name();
            }
            else if (getAnnoSoapBindingStyle() == SOAPBinding.Style.DOCUMENT
                    && getAnnoSoapBindingParameterStyle() == SOAPBinding.ParameterStyle.BARE) {
                // Default for operation style DOCUMENT and paramater style BARE per JSR 181 MR Sec 4.5.1, pg 23
                webResultName = getAnnoWebMethodOperationName() + "Response";
                
            }
            else {
                // Defeault value is "return" per JSR-181 MR Sec. 4.5.1, p. 22
                webResultName = "return";
            }
        }
        return webResultName;
    }
    
    public String getResultPartName() {
        // REVIEW: WSDL/Anno merge
        return getAnnoWebResultPartName();
    }
    public String getAnnoWebResultPartName() {
        if (!isOperationReturningResult()) {
            return null;
        }
        if (webResultPartName == null) {
            if (getAnnoWebResult() != null && !DescriptionUtils.isEmpty(getAnnoWebResult().partName())) {
                webResultPartName = getAnnoWebResult().partName();
            }
            else {
                // Default is the WebResult.name per JSR-181 MR Sec 4.5.1, pg 23
                webResultPartName = getAnnoWebResultName();
            }
        }
        return webResultPartName;
    }
    
    public String getResultTargetNamespace() {
        // REVIEW: WSDL/Anno merge
        return getAnnoWebResultTargetNamespace();
    }
    public String getAnnoWebResultTargetNamespace() {
        if (!isOperationReturningResult()) {
            return null;
        }
        if (webResultTargetNamespace == null) {
            if (getAnnoWebResult() != null && !DescriptionUtils.isEmpty(getAnnoWebResult().targetNamespace())) {
                webResultTargetNamespace = getAnnoWebResult().targetNamespace();
            }
            else if (getAnnoSoapBindingStyle() == SOAPBinding.Style.DOCUMENT
                    && getAnnoSoapBindingParameterStyle() == SOAPBinding.ParameterStyle.WRAPPED
                    && !getAnnoWebResultHeader()) {
                // Default for operation style DOCUMENT and paramater style WRAPPED and the return value
                // does not map to a header per JSR-181 MR Sec 4.5.1, pg 23-24
                webResultTargetNamespace = WebResult_TargetNamespace_DEFAULT;
            }
            else {
                // Default is the namespace from the WebService per JSR-181 MR Sec 4.5.1, pg 23-24
                webResultTargetNamespace = ((EndpointDescriptionJava) getEndpointInterfaceDescription().getEndpointDescription()).getAnnoWebServiceTargetNamespace();
            }
            
        }
        return webResultTargetNamespace;
    }
    
    public boolean isResultHeader() {
        // REVIEW: WSDL/Anno merge
        return getAnnoWebResultHeader();
    }
    public boolean getAnnoWebResultHeader() {
        if (!isOperationReturningResult()) {
            return false;
        }
        if (webResultHeader == null) {
            if (getAnnoWebResult() != null) {
                // Unlike the elements with a String value, if the annotation is present, exclude will always 
                // return a usable value since it will default to FALSE if the element is not present.
                webResultHeader = new Boolean(getAnnoWebResult().header());
            }
            else {
                webResultHeader = WebResult_Header_DEFAULT;
            }
        }
        return webResultHeader.booleanValue();
    }

    // ===========================================
    // ANNOTATION: SOAPBinding
    // ===========================================
    public SOAPBinding getAnnoSoapBinding() {
        // TODO: VALIDATION: Only style of DOCUMENT allowed on Method annotation; remember to check the Type's style setting also
        //       JSR-181 Sec 4.7 p. 28
        if (soapBindingAnnotation == null) {
            if (!isDBC()) {
                soapBindingAnnotation = seiMethod.getAnnotation(SOAPBinding.class);
            } else {
                soapBindingAnnotation = methodComposite.getSoapBindingAnnot();
            }
        }
        return soapBindingAnnotation;
    }
    public javax.jws.soap.SOAPBinding.Style getSoapBindingStyle() {
        // REVIEW: WSDL/Anno merge
        return getAnnoSoapBindingStyle();
    }
    public javax.jws.soap.SOAPBinding.Style getAnnoSoapBindingStyle() {
        if (soapBindingStyle == null) {
            if (getAnnoSoapBinding() != null && getAnnoSoapBinding().style() != null) {
                soapBindingStyle = getAnnoSoapBinding().style();
            }
            else {
                // Per JSR-181 MR Sec 4.7, pg 28: if not specified, use the Type value.
                soapBindingStyle = getEndpointInterfaceDescription().getSoapBindingStyle(); 
            }
        }
        return soapBindingStyle;
    }
    
    public javax.jws.soap.SOAPBinding.Use getSoapBindingUse() {
        // REVIEW: WSDL/Anno merge
        return getAnnoSoapBindingUse();
    }
    public javax.jws.soap.SOAPBinding.Use getAnnoSoapBindingUse() {
        if (soapBindingUse == null) {
            if (getAnnoSoapBinding() != null && getAnnoSoapBinding().use() != null) {
                soapBindingUse = getAnnoSoapBinding().use();
            }
            else {
                // Per JSR-181 MR Sec 4.7, pg 28: if not specified, use the Type value.
                soapBindingUse = getEndpointInterfaceDescription().getSoapBindingUse(); 
            }
        }
        return soapBindingUse;
    }

    public javax.jws.soap.SOAPBinding.ParameterStyle getSoapBindingParameterStyle() {
        // REVIEW: WSDL/Anno merge
        return getAnnoSoapBindingParameterStyle();
    }
    public javax.jws.soap.SOAPBinding.ParameterStyle getAnnoSoapBindingParameterStyle() {
        if (soapBindingParameterStyle == null) {
            if (getAnnoSoapBinding() != null && getAnnoSoapBinding().use() != null) {
                soapBindingParameterStyle = getAnnoSoapBinding().parameterStyle();
            }
            else {
                // Per JSR-181 MR Sec 4.7, pg 28: if not specified, use the Type value.
                soapBindingParameterStyle = getEndpointInterfaceDescription().getSoapBindingParameterStyle(); 
            }
        }
        return soapBindingParameterStyle;
    }
    
    // ===========================================
    // ANNOTATION: OneWay
    // ===========================================
    public Oneway getAnnoOneway() {
        //TODO: Shouldn't really do it this way...if there is not Oneway annotation, 
        //      we will always be calling the methods to try to retrieve it, since
        //      it will always be null, should consider relying on 'isOneWay'
        
        if (onewayAnnotation == null) {         
            if (isDBC()) {
                if (methodComposite.isOneWay()) {
                    onewayAnnotation = OneWayAnnot.createOneWayAnnotImpl();
                }
            } else
                onewayAnnotation = seiMethod.getAnnotation(Oneway.class);
        }
        return onewayAnnotation;
    }

    public boolean isOneWay() {
        // REVIEW: WSDL/Anno merge
        return isAnnoOneWay();
    }
    
    public boolean isAnnoOneWay() {
        if (onewayIsOneway == null) {
            if (getAnnoOneway() != null) {
                // The presence of the annotation indicates the method is oneway
                onewayIsOneway = new Boolean(true);
            }
            else {
                // If the annotation is not present, the default is this is NOT a One Way method
                onewayIsOneway = new Boolean(false);
            }
        }
        return onewayIsOneway.booleanValue();   
    }
    
    private boolean isDBC() {
        if (methodComposite != null)
            return true;
        else
            return false;
    }

    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.OperationDescription#getResultType()
     */
    public Class getResultType() {

        if (!isDBC()) {
            Method seiMethod = this.getSEIMethod();
            return seiMethod.getReturnType();           
        } 
        else {
            return methodComposite.getReturnTypeClass();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.OperationDescription#getResultActualType()
     */
    public Class getResultActualType() {
        // TODO: Fix this!  it isn't doing the right thing for DBC as noted below with FIXME comments
        //       This is used to marshall the rsp on the service (dbc) and demarshall on the client (reflection)
        //       But we shouldn't get an async OpDesc on the service since getDispatchableOperation(QN) removes them.
        
       Class returnType = getResultType();
       if(isJAXWSAsyncClientMethod()){
           //pooling implementation
           if(Response.class == returnType){
               if (!isDBC()) {
                   Type type = seiMethod.getGenericReturnType();
                   ParameterizedType pType = (ParameterizedType) type;
                   Type aType = pType.getActualTypeArguments()[0];
                   if (aType != null && ParameterizedType.class.isInstance(aType)) {
                       return (Class) ((ParameterizedType) aType).getRawType();
                   }
                   return (Class) aType;
               } else {
                 // FIXME: This doesn't work for DBC.  That's OK for now because DBC isn't used on the client side
                 //        yet; the client is all Java Reflection.  On the Service side, the Async methods are not used.
                 //        This needs to return T for Response<T>, or List for Response<List<T>>>
                 return returnType;  
               }   
           }
           //Callback Implementation
           else{
               // FIXME: This doesn't work for DBC.  That's OK for now because DBC isn't used on the client side
               //        yet; the client is all Java Reflection.  On the Service side, the Async methods are not used.
               //        This needs to find and return T for AsyncHandler<T>, or List for AsyncHandler<List<T>>>
               Type[] type = getGenericParameterTypes();
               Class parameters[]= getParameterTypes();
               int i=0;
               for(Class param:parameters){
                   if(AsyncHandler.class.isAssignableFrom(param)){
                       ParameterizedType pType = (ParameterizedType)type[i];
                       Type aType = pType.getActualTypeArguments()[0];
                       if (aType != null && ParameterizedType.class.isInstance(aType)) {
                           return (Class) ((ParameterizedType) aType).getRawType();
                       }
                       return (Class)aType;
                   }
                   i++;
               }
           }
       }
       
       return returnType;  
    }

    
    private Type[] getGenericParameterTypes() {
       if (isDBC()) {
           // FIXME: This doesn't work for DBC.  That's OK for now because DBC isn't used on the client side
           //        yet; the client is all Java Reflection.  On the Service side, the Async methods are not used.
           //        And this method is only used to parse out the JAX-WS Async parameter types to find 
           //        AsyncHandler<T>.  The problem with the code that was removed is that a Type can not be
           //        instantiated, so we can't new up a Type inside the PDC.
           throw new UnsupportedOperationException("OperationDescriptionImpl.getParameterActualGenericType not supported for DBC");
//           Type [] type = new Type[parameterDescriptions.length];
//           for (int i=0; i < parameterDescriptions.length; i++){
//               type[i] = ((ParameterDescriptionImpl) parameterDescriptions[i]).getParameterActualGenericType();
//           }
//           return type;
       } else {
           Type [] type = seiMethod.getGenericParameterTypes();
           return type;
       }
    }
    
    private Class[] getParameterTypes() {
       if (isDBC()) {
           Class [] parameters = new Class[parameterDescriptions.length];
           for (int i=0; i < parameterDescriptions.length; i++){
               parameters[i] = parameterDescriptions[i].getParameterType();
           }
           return parameters;
       } else {
           Class [] parameters = seiMethod.getParameterTypes();
           return parameters;
       }
    }
    /*
     * (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.OperationDescription#isJAXWSAsyncClientMethod()
     */
    public boolean isJAXWSAsyncClientMethod() {
        boolean answer = false;
        String methodName = null;
        String returnTypeName = null;
        if (isDBC()) {
            methodName = getMethodDescriptionComposite().getMethodName();
            returnTypeName = getMethodDescriptionComposite().getReturnType();
        }
        else {
            Method method = this.getSEIMethod();
            if(method != null) {
                methodName = method.getName();
                returnTypeName = method.getReturnType().getName();
            }
        }
        if (methodName != null && returnTypeName != null) {
            // REVIEW: Not sure the method MUST end with "Async"; I think it can be customized.
            answer = methodName.endsWith("Async") 
                     && (returnTypeName.equals(Response.class.getName()) || returnTypeName.equals(Future.class.getName()));
        }
        return answer;
    }

    public Method getMethodFromServiceImpl(Class serviceImpl) {
        
        // TODO: This doesn't support overloaded methods in the service impl  This is
        //       DIFFERENT than overloaded WSDL operations (which aren't supported).  We
        //       MUST support overloaded service impl methods as long as they have different
        //       wsdl operation names.  For example:
        //  ServiceImple Class          SEI Class
        //                              @WebMethod.name = Foo1
        //  void foo()                  void foo()
        //                              @WebMethod.name = Foo2
        //  void foo(int)               void foo(int)
        //                              @WebMethod.name = Foo3
        //  void foo(String)            void foo(String)
        //
        //  There will be two OpDescs, Foo1 and Foo2; the incoming wsdl operation will correctly identify
        //  which OpDesc.  However, to return the correct service impl method, we need to compare the
        //  signatures, not just the method names.
        if(!serviceImplMethodFound) {
        	Method[] methods = serviceImpl.getMethods();
            String opDescMethodName = getJavaMethodName();
            ParameterDescription[] paramDesc = getParameterDescriptions();
            // TODO: As noted above, a full signature is necessary, not just number of params
            int numberOfParams = 0;
            if (paramDesc != null) {
                numberOfParams = paramDesc.length;
            }
            
            // Loop through all the methods on the service impl and find the method that maps
            // to this OperationDescripton
            for (Method checkMethod : methods) {
                if (checkMethod.getName().equals(opDescMethodName)) {
                    Class[] methodParams = checkMethod.getParameterTypes();
                    // TODO: As noted above, a full signature is necessary, not just number of params
                    if (methodParams.length == numberOfParams) {
                       if (paramTypesMatch(paramDesc, methodParams)) {
                        	serviceImplMethod = checkMethod;
                        	break;
                       }
                    }
                }
            }
            serviceImplMethodFound = true;
        }
        return serviceImplMethod;
    } 
    
    /**
     * This method will compare the types of the parameters in a 
     * <code>ParameterDescription</code> vs. the type of the arguments in 
     * the parameters of a <code>Method</code>.
     * @param paramDescs - <code>ParameterDescription</code>[]
     * @param methodParams - <code>Class</code>[]
     * @return - <code>boolean</code>
     */
    private boolean paramTypesMatch(ParameterDescription[] paramDescs, Class[] 
        methodParams) {
    	for(int i=0; i < paramDescs.length; i++) {
    		String mParamType = methodParams[i].getName();
    		String pdType = getPDType(paramDescs[i]);
    		if(mParamType == null || !mParamType.equals(pdType)) {
    			return false;
    		}
    	}
    	return true;
    }
    
    /**
     * This will get a <code>String</code> representing the parameter class of a 
     * <code>ParameterDescription</code>.
     * @param pd - <code>ParameterDescrition</code>
     * @return - <code>String</code>
     */
    private String getPDType(ParameterDescription pd) {
    	String type = null;
    	if(pd.getParameterType() != null) {
    		type = pd.getParameterType().getName();
    	}
    	else if(pd.getParameterActualType() != null) {
    		type = pd.getParameterActualType().getName();
    	}
    	return type;
    }
}
