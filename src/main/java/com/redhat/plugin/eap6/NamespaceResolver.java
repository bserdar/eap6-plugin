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

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Namespace resolver for XML documents
 *
 * @author ykoer
 *
 */
public class NamespaceResolver implements NamespaceContext {

    public static final String NS_JBOSS_MODULE = "urn:jboss:module:1.0";
    public static final String NS_JBOSS_MODULE_PREFIX = "ns1";

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("No prefix provided!");
        } else if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return "";
        } else if (prefix.equals(NS_JBOSS_MODULE_PREFIX)) {
            return NS_JBOSS_MODULE;
        } else {
            return XMLConstants.NULL_NS_URI;
        }
    }

    public String getPrefix(String namespaceURI) {
        // Not needed in this context.
        return null;
    }

    public Iterator getPrefixes(String namespaceURI) {
        // Not needed in this context.
        return null;
    }

}
