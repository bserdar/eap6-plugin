/*
   Copyright 2013 Red Hat, Inc. and/or its affiliates.

   This file is part of eap6 plugin.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.redhat.plugin.eap6;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This plugin generates jboss-deployment-structure.xml file based on the
 * module dependencies.
 *
 * Configuration items:
 * <ul>
 *
 * <li>generateDeploymentStructure: Default set to true. If set to
 * false, the file won't be generated, but dependency analysis will be
 * performed and warnings will be printed out.</li>
 *
 * <li>skeletonDir: Directory containing skeleton
 * jboss-deployment-structure.xml. By default,
 * <pre>src/main/etc</pre>. If there is a skeleton file in this
 * directory, it will be used as a baseline, and module dependencies
 * will be added as needed. Otherwise, a jboss-deployment-structure
 * with only module dependencies will be generated. If this file
 * already has dependencies to other modules, they will not be
 * modified.</li>
 *
 * <li>dictionaryFiles: This is a list of files containing maven
 * artifact to EAP6 module mapping. The plugin already contains a
 * dictionary file containing all EAP6 provided modules. These
 * dictionaries declared with this configuration statement are in
 * addition to what is already declared. Multiple dictionary files can
 * be defined. Later declared dictionary files override identical
 * definions in earlier files.</li>
 *
 * </ul>
 *
 * Usage:
 * <pre>
 *  <build>
 *       <plugins>
 *           <plugin>
 *              <groupId>org.apache.maven.plugins</groupId>
 *              <artifactId>eap6-maven-plugin</artifactId>
 *              <version>1.0.0</version>
 *               <executions>
 *                   <execution>
 *                       <goals>
 *                           <goal>build</goal>
 *                       </goals>
 *                   </execution>
 *               </executions>
 *              <configuration>
 *              </configuration>
 *           </plugin>
 * </pre>
 *
 *
 * Dictionary file format:
 *
 * A dictionary file contains a maven artifact to EAP6 module mapping at each line:
 *
 * <pre>
 *      javax.faces:jsf-impl=com.sun.jsf-impl
 * </pre>
 *
 * Here, it is declared that if the project depends on any version of
 * javax.faces:jsf-impl maven artifact, and if it appears with scope
 * <pre>provided</pre>, then a module dependency will be added to
 * "com.sun.jsf-impl". Version specific mappings can also be given:
 *
 * <pre>
 *     javax.faces:jsf-impl:1.0=com.sun.jsf-impl
 *     javax.faces:jsf-impl:2.0=com.sun.jsf-impl.2
 * </pre>
 * 
 * With this decleration, different versions of the same maven
 * artifact is mapped to different modules. The plugin searches for
 * the most specific match, that is, if there is a match with a
 * particular version is found, it is used, but if a version match is
 * not found, but a non-versioned match is found, then the
 * non-versioned mapping will be used.
 * 
 * @author Burak Serdar (bserdar@redhat.com)
 */

@Mojo( name = "build",  
       requiresDependencyResolution = ResolutionScope.COMPILE,
       defaultPhase=LifecyclePhase.PREPARE_PACKAGE,
       threadSafe = true )
public class EAP6DeploymentStructureMojo extends AbstractEAP6Mojo {    
    
    @Parameter( defaultValue = "${project.build.directory}/${project.build.finalName}", 
                required = true )
    private File workDirectory;

    @Parameter
    private List<SubDeployment> subDeployments;

    @Parameter(defaultValue="false",required=true)
    private boolean isSubDeployment;

    private static XPathFactory xpf;
    private static XPathExpression xp_module;
    private static XPathExpression xp_deployment;
    private static XPathExpression xp_dependencies;
    private static XPathExpression xp_exclusions;

    private static final String JBOSS_DEPLOYMENT_STRUCTURE="jboss-deployment-structure.xml";
    private static final String JBOSS_SUBDEPLOYMENT="jboss-subdeployment.xml";

    static {
        try {
            xpf=XPathFactory.newInstance();
            xp_module=xpf.newXPath().
                compile("/jboss-deployment-structure/deployment/dependencies/module");
            xp_deployment=xpf.newXPath().
                compile("/jboss-deployment-structure/deployment");
            xp_dependencies=xpf.newXPath().compile("dependencies");
            xp_exclusions=xpf.newXPath().compile("exclusions");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    

    public void execute()
        throws MojoExecutionException, MojoFailureException {
        initializeDictionaries();
        // Are we to generate the file?
        if(generate) {

            // First gather any subdeployments
            if(subDeployments!=null) {
                getLog().info("Sub deployments:"+subDeployments);
                for(SubDeployment sd:subDeployments) {
                    Artifact artifact=findArtifact(sd.getGroupId(),sd.getArtifactId());
                    if(artifact==null)
                        throw new MojoExecutionException("Cannot find file for artifact "+sd);
                    getLog().debug("Sub deployment artifact:"+artifact+" file:"+artifact.getFile());
                    sd.setName(artifact.getFile().getName());
                    try {
                        Document doc=getDeploymentStructure(artifact.getFile());
                        if(doc==null)
                            throw new MojoExecutionException("No deployment structure in "+artifact+
                                                             ", add eap6 plugin to that project to generate deployment structure");
                        sd.setDocument(doc);
                    } catch (Exception e) {
                        throw new MojoExecutionException(e.toString());
                    }
                }
            }

            // Is there a skeleton file?
            Document doc=initializeSkeletonFile(JBOSS_DEPLOYMENT_STRUCTURE);

            try {
                buildDeploymentStructure(doc,artifactsAsModules,subDeployments);
                
                // Check if there are any modules that are possibly unnecessary
                NodeList nl=(NodeList)xp_module.evaluate(doc,XPathConstants.NODESET);
                int n=nl.getLength();
                for(int i=0;i<n;i++) {
                    String mname=((Element)nl.item(i)).getAttribute("name");
                    // If this module is not in dependencies, warn
                    Artifact a=reverseMap.get(mname);
                    if(a==null)
                        getLog().warn("No dependencies to module "+mname);
                    else {
                        if(!a.getScope().equals(Artifact.SCOPE_PROVIDED))
                            getLog().warn("Module does not appear with provided scope in POM:"+
                                          mname);
                    }
                }
            } catch (Exception e) {
                throw new MojoFailureException("Cannot process XML",e);
            }

            File destinationDir;
            if(project.getPackaging().equalsIgnoreCase("war")) {
                File f=new File(project.getBuild().getDirectory(),
                                project.getBuild().getFinalName());
                f.mkdir();
                destinationDir=new File(f,"WEB-INF");
            }  else if(project.getPackaging().equalsIgnoreCase("ear"))
                destinationDir=new File(new File(project.getBuild().getDirectory(),
                                                 project.getBuild().getFinalName()),
                                        "META-INF");
            else
                destinationDir=new File(project.getBuild().getOutputDirectory(),"META-INF");
            if(!destinationDir.exists())
                destinationDir.mkdir();
            writeXmlFile(doc,destinationDir,isSubDeployment?JBOSS_SUBDEPLOYMENT:
                         JBOSS_DEPLOYMENT_STRUCTURE);
        }
    }
    
    
    protected void buildDeploymentStructure(Document doc,
                                            Map<Artifact,String> moduleMap,
                                            List<SubDeployment> subdeployments) 
        throws MojoFailureException,XPathExpressionException {
        Element root=doc.getDocumentElement();
        if(!root.getTagName().equals("jboss-deployment-structure"))
            throw new MojoFailureException("Root element is not jboss-deployment-structure");

        Element deployment=(Element)xp_deployment.evaluate(doc,XPathConstants.NODE);
        if(deployment==null) {
            deployment=doc.createElement("deployment");
            root.insertBefore(deployment,root.getFirstChild());
        }

        Element depDependencies=(Element)xp_dependencies.evaluate(deployment,XPathConstants.NODE);
        if(depDependencies==null) {
            depDependencies=doc.createElement("dependencies");
            deployment.appendChild(depDependencies);
        }

        fillModuleEntries(doc,depDependencies,moduleMap.values());

        if(subdeployments!=null&&!subdeployments.isEmpty()) {
            for(SubDeployment sd:subdeployments) {
                XPathExpression xp=xpf.newXPath().
                    compile("/jboss-deployment-structure/sub-deployment [@name='"+sd.getName()+"']");
                Element subEl=(Element)xp.evaluate(doc,XPathConstants.NODE);
                if(subEl==null) {
                    subEl=doc.createElement("sub-deployment");
                    root.appendChild(subEl);
                    subEl.setAttribute("name",sd.getName());
                }
                Element subDependencies=(Element)xp_dependencies.evaluate(subEl,XPathConstants.NODE);
                if(subDependencies==null) {
                    subDependencies=doc.createElement("dependencies");
                    subEl.appendChild(subDependencies);
                }
                Set<String> depModules = extractModules(
                        xpf.newXPath().compile("/jboss-deployment-structure/deployment/dependencies/module/@name"),
                        moduleMap, sd);
                getLog().debug("From "+sd.getName()+":"+depModules);
                fillModuleEntries(doc,subDependencies,depModules);
                
                Set<String> exModules = extractModules(
                        xpf.newXPath().compile("/jboss-deployment-structure/deployment/exclusions/module/@name"),
                        moduleMap, sd);
                if(!exModules.isEmpty()){
                    Element subExclusions=(Element)xp_exclusions.evaluate(subEl,XPathConstants.NODE);
                    if(subExclusions==null) {
                        subExclusions=doc.createElement("exclusions");
                        subEl.appendChild(subExclusions);
                    }
                    fillModuleEntries(doc,subExclusions,exModules);
                }
            }
        }
    }


    private Set<String> extractModules(XPathExpression xp, Map<Artifact, String> moduleMap, SubDeployment sd) throws XPathExpressionException {
        Set<String> modules=new HashSet<String>();
        NodeList nl=(NodeList)xp.evaluate(sd.getDocument(),XPathConstants.NODESET);
        int n=nl.getLength();
        for(int i=0;i<n;i++) {
            if (moduleMap.values().contains(nl.item(i).getTextContent()))
                continue;
            modules.add(nl.item(i).getTextContent());
        }
        return modules;
    }

    protected void fillModuleEntries(Document doc,Element dependencies,Collection<String> modules) 
        throws XPathExpressionException {
        for(String module:modules) {
            XPathExpression xp=xpf.newXPath().compile("module [@name=\""+module+"\"]");
            if(xp.evaluate(dependencies,XPathConstants.NODE)==null) {
                Element moduleEl=doc.createElement("module");
                moduleEl.setAttribute("name",module);
                dependencies.appendChild(moduleEl);
            }
        }
    }

    private int zread(InputStream in,byte[] arr) 
        throws Exception {
        int r;
        int off=0;
        getLog().debug("ZIP read attempt for "+arr.length+" bytes");
        while((r=in.read(arr,off,arr.length-off))>0)
            off+=r;
        getLog().debug("ZIP read "+off+" bytes");
        return off;
    }

    protected Document getDeploymentStructure(File file) throws Exception {
        ZipInputStream zis=new ZipInputStream(new FileInputStream(file));
        ZipEntry entry;
        Document doc=null;
        boolean done=false;
        while(!done&&(entry=zis.getNextEntry())!=null) {
            String entryName=entry.getName().toLowerCase();
            if(entryName.endsWith("meta-inf/"+JBOSS_SUBDEPLOYMENT)||
               entryName.endsWith("web-inf/"+JBOSS_SUBDEPLOYMENT)) {
                byte[] buf=new byte[(int)entry.getSize()];
                zread(zis,buf);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(buf));
                done=true;
            } else
                zis.skip(entry.getCompressedSize());
        }
        zis.close();
        return doc;
    }
}

