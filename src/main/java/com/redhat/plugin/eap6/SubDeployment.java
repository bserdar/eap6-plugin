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

import org.w3c.dom.Document;

public class SubDeployment {
    private String groupId;
    private String artifactId;
    private String name;
    private Document doc;

    /**
     * Gets the value of groupId
     *
     * @return the value of groupId
     */
    public String getGroupId() {
        return this.groupId;
    }

    /**
     * Sets the value of groupId
     *
     * @param argGroupId Value to assign to this.groupId
     */
    public void setGroupId(String argGroupId) {
        this.groupId = argGroupId;
    }

    /**
     * Gets the value of artifactId
     *
     * @return the value of artifactId
     */
    public String getArtifactId() {
        return this.artifactId;
    }

    /**
     * Sets the value of artifactId
     *
     * @param argArtifactId Value to assign to this.artifactId
     */
    public void setArtifactId(String argArtifactId) {
        this.artifactId = argArtifactId;
    }

    public String toString() {
        return groupId+":"+artifactId;
    }

    public Document getDocument() {
        return doc;
    }

    public void setDocument(Document doc) {
        this.doc=doc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name=name;
    }
}
