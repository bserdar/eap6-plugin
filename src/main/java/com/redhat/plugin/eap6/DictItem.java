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

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

import java.text.ParseException;

public class DictItem {

    public final String groupId;
    public final String artifactId;
    public final String version;
    public final String moduleName;

    private DictItem(String groupId,
                     String artifactId,
                     String version,
                     String moduleName) {
        this.groupId=groupId;
        this.artifactId=artifactId;
        this.version=version;
        this.moduleName=moduleName;
    }
    
    public static DictItem parse(String s) 
        throws ParseException {
        int index=s.indexOf('#');
        if(index!=-1)
            s=s.substring(0,index);
        s=s.trim();
        if(s.length()<=0)
            return null;

        index=s.indexOf('=');
        if(index==-1)
            throw new ParseException("Expected = in "+s,0);
        String m=s.substring(index+1).trim();
        s=s.substring(0,index);
        index=s.indexOf(':');
        String g=s.substring(0,index);
        s=s.substring(index+1);
        index=s.indexOf(':');
        String a;
        String v="*";
        if(index==-1)
            a=s.trim();
        else {
            a=s.substring(0,index);
            s=s.substring(index+1);
            v=s.trim();
        }
        return new DictItem(g,a,v,m.length()==0?null:m);
    }

    public static List<DictItem> parse(Reader rd) 
        throws IOException,ParseException {
        BufferedReader br=rd instanceof BufferedReader?(BufferedReader)rd:
            new BufferedReader(rd);
        String line;
        List<DictItem> list=new ArrayList<DictItem>();
        while((line=br.readLine())!=null) {
            line=line.trim();
            if(line.length()>0) {
                DictItem item=parse(line);
                if(item!=null)
                    list.add(item);
            }
        }
        return list;
    }

    /**
     * Finds the best matching artifact mapping
     */
    public static DictItem find(List<DictItem> dictionary,
                                String groupId,
                                String artifactId,
                                String version) {
        DictItem match=null;
        for(DictItem item:dictionary) {
            if(item.groupId.equals(groupId)&&
               item.artifactId.equals(artifactId)) {
                // If there is a version matching this version, pick that
                if(version.equals(item.version))
                    match=item;
                else if(item.version.equals("*")) {
                    if(match!=null&&match.version.equals("*"))
                        throw new RuntimeException("Duplicate:"+match);
                    if(match==null)
                        match=item;
                }
            }
        }
        return match;
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append(groupId).append(':').append(artifactId);
        if(version!=null)
            buf.append(':').append(version);
        buf.append('=');
        if(moduleName!=null)
            buf.append(moduleName);
        return buf.toString();
    }
}
