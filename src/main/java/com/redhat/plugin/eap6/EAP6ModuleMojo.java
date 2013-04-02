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

import java.util.Map;

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This plugin generates module.xml file for the given artifact based
 * on the module dependencies.
 *
 * Configuration items:
 * <ul>
 *
 * <li>generate: Default set to true. If set to false, the file won't be generated,
 * but dependency analysis will be performed and warnings will be printed out.</li>
 *
 * <li>skeletonDir: Directory containing skeleton module.xml. By default,
 * <pre>src/main/etc</pre>. If there is a skeleton file in this
 * directory, it will be used as a baseline, and module dependencies
 * will be added as needed. Otherwise, a module.xml with only module dependencies
 * will be generated. If this file already has dependencies to other modules,
 * they will not be modified.</li>
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
 * If you are generating a module.xml for a maven artifact, a module
 * mapping for that artifact must also be in the dictionary.
 *
 * Usage:
 * <pre>
 * <build>
 *   <plugins>
 *      <plugin>
 *        <groupId>org.apache.maven.plugins</groupId>
 *        <artifactId>eap6-maven-plugin</artifactId>
 *        <version>1.0.0</version>
 *        <executions>
 *           <execution>
 *              <goals>
 *                 <goal>build-module</goal>
 *              </goals>
 *           </execution>
 *        </executions>
 *        <configuration>
 *           <dictionaryFiles><dictionaryFile>../../services.dict</dictionaryFile></dictionaryFiles>
 *        </configuration>
 *     </plugin>
 * </pre>
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
 * non-versioned mapping will be used
 *
 * @author Yusuf Koer <ykoer@redhat.com>
 */

@Mojo(name = "build-module",
      requiresDependencyResolution = ResolutionScope.COMPILE,
      defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
      threadSafe = true
)
public class EAP6ModuleMojo extends AbstractEAP6Mojo {

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    protected File workDirectory;

    private static final String MODULE_DESCRIPTOR_NAME = "module.xml";

    private static XPathFactory xpf;
    private static XPathExpression xp_module;
    private static XPathExpression xp_dependencies;
    private static XPathExpression xp_resources;
    private static XPathExpression xp_resource_root;

    static {
        try {
            xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            xpath.setNamespaceContext(new NamespaceResolver());
            xp_module = xpath.compile("/ns1:module/ns1:dependencies/ns1:module");
            xp_dependencies = xpath.compile("/ns1:module/ns1:dependencies");
            xp_resources = xpath.compile("/ns1:module/ns1:resources");
            xp_resource_root = xpath.compile("/ns1:module/ns1:resources/ns1:resource-root");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        initializeDictionaries();

        // Are we to generate the file?
        if (generate) {

            Document doc = initializeSkeletonFile (MODULE_DESCRIPTOR_NAME);

            try {
                buildModule(doc, artifactsAsModules);

                // Check if there are any modules that are possibly unnecessary
                NodeList nl = (NodeList) xp_module.evaluate(doc, XPathConstants.NODESET);
                int n = nl.getLength();
                for (int i = 0; i < n; i++) {
                    String mname = ((Element) nl.item(i)).getAttribute("name");
                    // If this module is not in dependencies, warn
                    Artifact a = reverseMap.get(mname);
                    if (a == null)
                        getLog().warn("No dependencies to module " + mname);
                    else {
                        if (!a.getScope().equals(Artifact.SCOPE_PROVIDED))
                            getLog().warn("Module does not appear with provided scope in POM:" + mname);
                    }
                }
            } catch (Exception e) {
                throw new MojoFailureException("Cannot process XML", e);
            }

            writeXmlFile(doc, workDirectory, MODULE_DESCRIPTOR_NAME);
        }
    }

    protected void buildModule(Document doc, Map<Artifact, String> moduleMap) throws MojoFailureException, XPathExpressionException {

        // check if there is a mapping in the dictionary for the project artifact
        DictItem mapping = dictionary.find(project.getGroupId(), project.getArtifactId(), project.getVersion());
        if (mapping == null || mapping.moduleName == null) {
            throw new MojoFailureException("No mapping found for the project artifact: " + project.getArtifact());
        }

        Element root = doc.getDocumentElement();
        if (!root.getTagName().equals("module"))
            throw new MojoFailureException("Root element is not module");
        root.setAttribute("name", mapping.moduleName);

        Element dependencies = (Element) xp_dependencies.evaluate(doc, XPathConstants.NODE);
        if (dependencies == null) {
            dependencies = doc.createElement("dependencies");
            root.insertBefore(dependencies, root.getFirstChild());
        }

        Element resources = (Element) xp_resources.evaluate(doc, XPathConstants.NODE);
        if (resources == null) {
            resources = doc.createElement("resources");
            root.insertBefore(resources, root.getFirstChild());
        }

        Element resource_root = (Element) xp_resource_root.evaluate(doc, XPathConstants.NODE);
        if (resource_root == null) {
            resource_root = doc.createElement("resource-root");
            resources.appendChild(resource_root);
        }

        // set resource-root path attribute
        resource_root.setAttribute("path", buildFinalName+"."+project.getPackaging());

        for (Map.Entry<Artifact, String> entry : moduleMap.entrySet()) {
            String module = entry.getValue();
            XPathExpression xp = xpf.newXPath().compile("module [@name=\"" + module + "\"]");
            if (xp.evaluate(dependencies, XPathConstants.NODE) == null) {
                Element moduleEl = doc.createElement("module");
                moduleEl.setAttribute("name", module);
                dependencies.appendChild(moduleEl);
            }
        }
    }
}
