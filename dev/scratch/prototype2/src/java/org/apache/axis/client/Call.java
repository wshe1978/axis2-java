/*
 * Copyright 2003,2004 The Apache Software Foundation.
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
package org.apache.axis.client;

import org.apache.axis.addressing.AddressingConstants;
import org.apache.axis.addressing.EndpointReferenceType;
import org.apache.axis.context.MessageContext;
import org.apache.axis.description.AxisGlobal;
import org.apache.axis.engine.AxisEngine;
import org.apache.axis.engine.AxisFault;
import org.apache.axis.engine.EngineRegistry;
import org.apache.axis.engine.TransportSenderLocator;
import org.apache.axis.impl.engine.EngineRegistryImpl;
import org.apache.axis.impl.llom.builder.StAXBuilder;
import org.apache.axis.impl.llom.builder.StAXSOAPModelBuilder;
import org.apache.axis.om.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;


/**
 * This is conveneice API for the User who do not need to see the complexity of the
 * Engine.
 * //TODO this a a MOCK call things are subjected to be decided
 */
public class Call {
    private EngineRegistry registry;
    protected Log log = LogFactory.getLog(getClass());


    public Call() {
        //TODO look for the Client XML and creatre a Engine registy
        this.registry = new EngineRegistryImpl(new AxisGlobal());
    }


    public OMElement syncCall(OMElement in, URL url) throws AxisFault {
        try {
            URLConnection urlConnect = url.openConnection();
            urlConnect.setDoOutput(true);

            SOAPEnvelope env = OMFactory.newInstance().getDefaultEnvelope();

            env.getBody().addChild(in);
            AxisEngine engine = new AxisEngine(registry);
            MessageContext msgctx = new MessageContext(registry);
            msgctx.setEnvelope(env);

            OutputStream out = urlConnect.getOutputStream();
            msgctx.setProperty(MessageContext.TRANSPORT_DATA, out);
            msgctx.setProperty(MessageContext.TRANSPORT_TYPE, TransportSenderLocator.TRANSPORT_HTTP);
            msgctx.setTo(new EndpointReferenceType(AddressingConstants.WSA_TO,url.toString()));
            engine.send(msgctx);

            MessageContext response = createIncomingMessageContext(urlConnect.getInputStream(), engine);
            response.setServerSide(false);
            engine.receive(response);
            SOAPEnvelope envelope = response.getEnvelope();

            SOAPBody body = envelope.getBody();

            Iterator children = body.getChildren();
            while (children != null && children.hasNext()) {
                OMNode child = (OMNode) children.next();
                if (child.getType() == OMNode.ELEMENT_NODE) {
                    OMElement element = (OMElement) child;
                    OMNamespace ns = element.getNamespace();
                    if (OMConstants.SOAPFAULT_LOCAL_NAME.equals(element.getLocalName())
                            && OMConstants.SOAPFAULT_NAMESPACE_URI.equals(ns.getName())) {
                        Iterator it = element.getChildren();
                        String error = null;
                        while (it.hasNext()) {
                            OMNode node = (OMNode) it.next();
                            if (OMNode.TEXT_NODE == node.getType()) {
                                error = ((OMText) node).getValue();
                                if (error != null) {
                                    break;
                                }
                            }
                        }
                        throw new AxisFault(error);
                    }
                    return (OMElement) child;
                }
            }
            return null;
        } catch (OMException e) {
            throw AxisFault.makeFault(e);
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        }
    }

    public void asyncCall(OMElement in, URL url, final CallBack callback) throws AxisFault {
        try {
            final URLConnection urlConnect = url.openConnection();
            final AxisEngine engine = new AxisEngine(registry);
            urlConnect.setDoOutput(true);

            SOAPEnvelope env = OMFactory.newInstance().getDefaultEnvelope();

            env.getBody().addChild(in);

            MessageContext msgctx = new MessageContext(registry);
            msgctx.setEnvelope(env);

            OutputStream out = urlConnect.getOutputStream();
            msgctx.setProperty(MessageContext.TRANSPORT_DATA, out);
            msgctx.setProperty(MessageContext.TRANSPORT_TYPE, TransportSenderLocator.TRANSPORT_HTTP);
            msgctx.setTo(new EndpointReferenceType(AddressingConstants.WSA_TO,url.toString()));

            engine.send(msgctx);

            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        log.info("Starting new Thread ");
                        MessageContext response = createIncomingMessageContext(urlConnect.getInputStream(), engine);
                        response.setServerSide(false);
                        engine.receive(response);
                        SOAPEnvelope envelope = response.getEnvelope();

                        SOAPBody body = envelope.getBody();

                        Iterator children = body.getChildren();
                        while (children != null && children.hasNext()) {
                            OMNode child = (OMNode) children.next();
                            if (child.getType() == OMNode.ELEMENT_NODE) {
                                callback.doWork((OMElement) child);
                            }
                        }
                    } catch (Exception e) {
                        callback.reportError(e);
                    }
                }
            };
            new Thread(runnable).start();

        } catch (OMException e) {
            throw AxisFault.makeFault(e);
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        }
    }

    public void completeAsyncCall(OMElement in, URL url, final CallBack callback) throws AxisFault {
        try {
            final URLConnection urlConnect = url.openConnection();
            final AxisEngine engine = new AxisEngine(registry);
            urlConnect.setDoOutput(true);

            SOAPEnvelope env = OMFactory.newInstance().getDefaultEnvelope();

            env.getBody().addChild(in);

            MessageContext msgctx = new MessageContext(registry);
            msgctx.setEnvelope(env);

            OutputStream out = urlConnect.getOutputStream();
            msgctx.setProperty(MessageContext.TRANSPORT_DATA, out);
            msgctx.setProperty(MessageContext.TRANSPORT_TYPE, TransportSenderLocator.TRANSPORT_HTTP);
            msgctx.setTo(new EndpointReferenceType(AddressingConstants.WSA_TO,url.toString()));

            engine.send(msgctx);
            //TODO
            //Corelater.corealte(msgID,callback);
            //e.g. TransportReciverFactory.startReciverIfNotYetStrated();
            throw new UnsupportedOperationException();
        } catch (OMException e) {
            throw AxisFault.makeFault(e);
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        }
    }

    private MessageContext createIncomingMessageContext(InputStream in, AxisEngine engine) throws AxisFault {
        MessageContext msgContext;
        try {
            msgContext = new MessageContext(engine.getRegistry());
            //TODO Thanks to the URL we need nothng here .. may be need parsing code 
//      int level = 0;
//      while(level != 2){
//          byte b = (byte)inp.read();
//          System.out.print((char)b);
//          if(b == '\n'){
//              if(level == 0){
//                level = 1;    
//              }else{
//                level = 2;  
//              }
//          }else{
//              level = 0;
//          }
//
//      }
            InputStreamReader isr = new InputStreamReader(in);
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(isr);
            StAXBuilder builder = new StAXSOAPModelBuilder(OMFactory.newInstance(), reader);
            msgContext.setEnvelope((SOAPEnvelope) builder.getDocumentElement());
        } catch (XMLStreamException e) {
            throw AxisFault.makeFault(e);
        }
        return msgContext;

    }

}
