/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
package org.apache.axis2.wsdl.codegen.schema;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.apache.axis2.namespace.Constants;

/**
 * this class represents an xml schema import
 */
public class XmlImport {

    private String targetNamespace;

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }

    public Element getXmlSchemaElement(Document document){

        Element importElement = document.createElementNS(Constants.URI_2001_SCHEMA_XSD,"xsd:import");
        importElement.setPrefix("xsd");
        importElement.setAttribute("namespace",this.targetNamespace);
        return importElement;
    }
}
