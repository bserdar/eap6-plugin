This plugin generates `jboss-deployment-structure.xml` file or
`module.xml` based on the module dependencies.

Configuration items:

 - `generate`: Default set to `true`. If set to
   `false`, the file won't be generated, but dependency analysis will be
   performed and warnings will be printed out.

 - `skeletonDir`: Directory containing skeleton
   `jboss-deployment-structure.xml`, or `module.xml`. By default,
   `src/main/etc`. If there is a skeleton file in this
   directory, it will be used as a baseline, and module dependencies
   will be added as needed. Otherwise, a `jboss-deployment-structure` or
   `module.xml` with only module dependencies will be generated. If this
   file already has dependencies to other modules, they will not be
   modified.

 - `dictionaryFiles`: This is a list of files containing maven
   artifact to EAP6 module mapping. The plugin already contains a
   dictionary file containing all EAP6 provided modules. These
   dictionaries declared with this configuration statement are in
   addition to what is already declared. Multiple dictionary files can
   be defined. Later declared dictionary files override identical
   definions in earlier files.
   
 - `isSubdeployment`: If true, this project is a sub-deployment of an
   EAR file. A `jboss-subdeployment.xml` will be generated instead of
   `jboss-deployment-structure.xml`. This project should be listed as a
   subdeployment of the EAR project. The plugin will read the
   `jboss-subdeployment.xml` from the artifact, and build the correct
   `jboss-deployment-structure.xml`.

 - `subDeployments`: A list of `subDeployment` elements, each containing a
   `groupId` and `artifactId`. The sub-deployments of an EAR file. 


Usage 

`jboss-deployment-structure` for an EAR file with an ejb-jar:

    ...
    <groupId>myApp</groupId>
    <artifactId>myEJB</artifactId>
    <build>
    <plugins>
         <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>eap6-maven-plugin</artifactId>
            <version>1.0.0</version>
             <executions>
                 <execution>
                     <goals>
                         <goal>build</goal>
                     </goals>
                 </execution>
             </executions>
             <configuration>
                <dictionaryFiles>
                   <dictionaryFile>../mydict.dict</dictionaryFile>
                </dictionaryFiles>
                <isSubDeployment>true</isSubDeployment>
             </configuration>
            </configuration>
         </plugin>
     </plugins>
    </build>


    ...
    <groupId>myApp</groupId>
    <artifactId>myEAR</artifactId>
    <build>
     <plugins>
         <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>eap6-maven-plugin</artifactId>
            <version>1.0.0</version>
            <executions>
                <execution>
                     <goals>
                         <goal>build</goal>
                     </goals>
                </execution>
            </executions>
            <configuration>
               <subDeployments>
                  <subDeployment>
                    <groupId>myApp</groupId>
                    <artifactId>myEJB</artifactId>
                  </subDeployment>
               </subDeployments>
               <dictionaryFiles>
                   <dictionaryFile>../mydict.dict</dictionaryFile>
               </dictionaryFiles>
            </configuration>
        </plugin>

Generating a `module.xml` file for an EAP6 module:

    <plugin>
     <groupId>org.apache.maven.plugins</groupId>
     <artifactId>eap6-maven-plugin</artifactId>
     <executions>
        <execution>
           <goals>
              <goal>build-module</goal>
           </goals>
        </execution>
     </executions>
     <configuration>
        <dictionaryFiles>
           <dictionaryFile>../mydict.dict</dictionaryFile>
        </dictionaryFiles>
     </configuration>
    </plugin>


Dictionary file format:

A dictionary file contains a maven artifact to EAP6 module mapping at
each line:

    javax.faces:jsf-impl=com.sun.jsf-impl

Here, it is declared that if the project depends on any version of
`javax.faces:jsf-impl` maven artifact, and if it appears with scope
provided, then a module dependency will be added to
`com.sun.jsf-impl`. Version specific mappings can also be given:

    javax.faces:jsf-impl:1.0=com.sun.jsf-impl
    javax.faces:jsf-impl:2.0=com.sun.jsf-impl.2
 
With this decleration, different versions of the same maven
artifact is mapped to different modules. The plugin searches for
the most specific match, that is, if there is a match with a
particular version is found, it is used, but if a version match is
not found, but a non-versioned match is found, then the
non-versioned mapping will be used.

