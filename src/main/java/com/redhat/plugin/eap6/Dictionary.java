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

import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.List;
import java.util.ArrayList;

import java.text.ParseException;

public class Dictionary {
    
    private List<List<DictItem>> dictionaries=new ArrayList<List<DictItem>>();

    public void addDictionary(List<DictItem> dict) {
        dictionaries.add(dict);
    }

    public void addDictionary(File f) throws IOException, ParseException {
        FileReader reader=new FileReader(f);
        addDictionary(DictItem.parse(reader));
    }

    public void addDictionary(InputStream stream) throws IOException, ParseException {
        addDictionary(DictItem.parse(new InputStreamReader(stream)));
    }

    public DictItem find(String groupId,
                         String artifactId,
                         String version) {
        
        for(int n=dictionaries.size()-1;n>=0;n--) {
            List<DictItem> dict=dictionaries.get(n);
            DictItem item=DictItem.find(dict,groupId,artifactId,version);
            if(item!=null)
                return item;
        }
        return null;
    }

}
