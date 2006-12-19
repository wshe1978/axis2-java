package org.apache.axis2.jaxws.wsdl.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;

import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.util.JavaUtils;
import org.apache.axis2.jaxws.util.WSDLWrapper;
import org.apache.axis2.jaxws.wsdl.SchemaReader;
import org.apache.axis2.jaxws.wsdl.SchemaReaderException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SchemaReaderImpl implements SchemaReader {

	private static String JAXB_SCHEMA_BINDING = "schemaBindings";
	private static String JAXB_SCHEMA_BINDING_PACKAGE = "package";
	private static String JAXB_SCHEMA_Binding_PACKAGENAME = "name";
	private static String SCHEMA_TARGETNAMESPACE = "targetNamespace";
	private Definition wsdlDefinition = null;
	private static Log log = LogFactory.getLog(SchemaReaderImpl.class);
	
	public Set<String> readPackagesFromSchema(Definition wsdlDefinition) throws SchemaReaderException{
		if(wsdlDefinition == null){
			if(log.isDebugEnabled()){
				log.debug("Invalid wsdl definition provided, NULL");
			}
			throw new SchemaReaderException(Messages.getMessage("SchemaReaderErr1"));
		}
		this.wsdlDefinition = wsdlDefinition;
		HashSet<String> set = new HashSet<String>();
		//Add WSDL TargetNamespace
		String namespace = wsdlDefinition.getTargetNamespace();
		String packageString = JavaUtils.getPackageFromNamespace(namespace);
		set.add(packageString);
		
		//Read Schema Definition in wsdl;
		Types types = wsdlDefinition.getTypes();
		
		List extensibilityElements = types.getExtensibilityElements();
		
		//Read the schema defined in the wsdl
		for(Object obj:extensibilityElements){
			if(isSchema((ExtensibilityElement)obj)){
				Schema schema = (Schema)obj;
				//First Read all inline Schema packages
				List<String> inlineSchemaPkgList = readPackagesFromInlineSchema(schema);
				for(String pkgAsString:inlineSchemaPkgList){
					if(pkgAsString!=null){
						set.add(pkgAsString);
					}
				}
				
				//Then read all imported schema definitions
				ArrayList<String> schemaImportPkgList = readPackagesFromImports(schema);
				for(String pkgAsString : schemaImportPkgList){
					if(pkgAsString!=null){
						set.add(pkgAsString);
					}
				}
			}
		}
		//Set always stores unique objects, so I dont have to worry about removing duplicates from this set.
		return set;
	}
	/*
	 * Read ShemaBinding from inline schema, if not SchemaBinding defined then read the targetnamespace.
	 * Inline Schema - Schema defined in WSDL Types.
	 */
	private ArrayList<String> readPackagesFromInlineSchema(Schema schema){
		ArrayList<String> pkgList = new ArrayList<String>();
		
		//check if there is any jaxb customization/binding defined namely schemaBinding.
		String packageString = readSchemaBindingPackageName(schema);
		if(packageString == null){
			//no Schema Binding package name found, this means no jaxb customizations in schema, lets read wsdl 
			//targetnamespace. Thats what will be used by RI tooling to store java Beans
			String namespace = readSchemaTargetnamespace(schema);
			if(namespace !=null){
				packageString = JavaUtils.getPackageFromNamespace(namespace);
			}
		}
		pkgList.add(packageString);
		
		return pkgList;
	}
	/*
	 * Read ShemaBinding from import schema, if not SchemaBinding defined then read the targetnamespace.
	 * import Schema - import defined in Schema section of WSDL Types.
	 */
	private ArrayList<String> readPackagesFromImports(Schema schema) throws SchemaReaderException{
		ArrayList<String> schemaImportPkgList = new ArrayList<String>();
		Map map  = schema.getImports();
		Collection collection = map.values();
		for(Iterator i =collection.iterator(); i.hasNext(); ){
			Vector value = (Vector) i.next();
			for(Object vectorObj:value){
				SchemaImport si = (SchemaImport)vectorObj;
				if(log.isDebugEnabled()){
					if(si!=null)
						log.debug("Reading import for SchemaLocation ="+si.getSchemaLocationURI());
				}
				
				Schema refSchema = si.getReferencedSchema();
				
				//Implementing recursion for reading import within import

				//First read inline schema for imported schema
				ArrayList<String> inlineSchemaPkgList  = readPackagesFromInlineSchema(refSchema);
				for(String packageString:inlineSchemaPkgList){
					if(packageString!=null){
						schemaImportPkgList.add(packageString);
					}
				}
				//Before we fetch import within imports lets check for circular dependency
				//Circular dependency is two imports calling each other in different xsd files.
				if(isCircularDependency(this.readSchemaTargetnamespace(schema), refSchema)){
					if(log.isDebugEnabled()){
						log.debug("Circular Dependency Found in WSDL Schema Imports, Two Schemas are importing each other.");
					}
					throw new SchemaReaderException(Messages.getMessage("SchemaReaderErr2"));
				}
				
				//Lets read if import has any imports by recurisvely calling readPackageFromImport again....
				ArrayList<String> rec_ImportPkgList = readPackagesFromImports(refSchema);
				for(String packageString:rec_ImportPkgList){
					if(packageString!=null){
						schemaImportPkgList.add(packageString);
					}
				}
			}
			
		}
		
		return schemaImportPkgList;
	}
	
	private String readSchemaTargetnamespace(Schema schema){
		Node root = schema.getElement();
		if(root!=null){
			NamedNodeMap nodeMap = root.getAttributes();
			Node attributeNode = nodeMap.getNamedItem(SCHEMA_TARGETNAMESPACE);
			if(attributeNode!=null){
				return attributeNode.getNodeValue();
			}
		}
		return null;
	}
	private String readSchemaBindingPackageName(Schema schema){
		
		/* JAXB Specification section 7.6 have following important points
		 * 1) <schemaBindings> binding declaration have schema scope
		 * 2) For inline annotation  a <schemaBindings> is valid only in the annotation element of the <schema> element.
		 * 3) There must only be a single instance of <schemaBindings> declaration in the annotation element of the <schema> element.
		 */
		
		//Get root node for schema.
		Node root = schema.getElement();
		if(root.hasChildNodes()){
			
			//get all child nodes for schema
			NodeList list = root.getChildNodes();
			
			//search for JAXB schemaBinding customization in schema element definitions. 
			for(int i=0; i<list.getLength(); i++){
				Node childNode = list.item(i);
				if(isElementName(JAXB_SCHEMA_BINDING, childNode)){
					
					//SchemaBinding has been defined, so lets look for package element. 
					NodeList schemaBindingNodeList = childNode.getChildNodes();
					for(int j =0; j<schemaBindingNodeList.getLength(); j++){
						Node schemaBindingNode = schemaBindingNodeList.item(j);
						if(isElementName(JAXB_SCHEMA_BINDING_PACKAGE, schemaBindingNode)){
							
							//Package Element found, so lets read the package name attribute and return that.
							NamedNodeMap nodeMap = schemaBindingNode.getAttributes();
							Node attributeNode = nodeMap.getNamedItem(JAXB_SCHEMA_Binding_PACKAGENAME);
							return attributeNode.getNodeValue();
						}
					}
				}
			}
		}
		return null;
	}
	private boolean  isElementName(String name, Node domNode){
		if(domNode == null){
			return false;
		}
		if(domNode.getNodeType() == Node.ELEMENT_NODE){
			String localName = domNode.getLocalName();
			return localName!=null && localName.equals(name);
		}
		return false;
	}
	
	private boolean isSchema(ExtensibilityElement exElement){
		return WSDLWrapper.SCHEMA.equals(exElement.getElementType());
	}
	/*
	 * Check if Schema1 imports Schema2 and Schema2 imports Schema1
	 */
	private boolean isCircularDependency(String targetNamespaceSchema1, Schema Schema2){
		//Get All imports of the schema2
		Map map  = Schema2.getImports();
		Collection collection =map.values();
		for(Iterator i =collection.iterator(); i.hasNext(); ){
			Vector value = (Vector) i.next();
			for(Object vectorObj:value){
				SchemaImport si = (SchemaImport)vectorObj;
				
				//Comparte Schema2 import targetNamespace URI with targetNamespace of Schema1 if they match its a circular
				//dependency.
				//TODO should we also check for Schema location here.
				if(si.getNamespaceURI().equals(targetNamespaceSchema1)){
					return true;
				}
			}
			
		}
		return false;
	}
	
}
