/*
 * Copyright (C) 2015 Akash Kurian Jose
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.acashjos.anarch;

//import android.util.Log;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegexValueMatchBuilder extends MatchBuilder {

    ArrayList<PatternBlueprint> patternSet;

    /**
     * RegexValueMatchBuilder constructor
     */
    public RegexValueMatchBuilder() {

        patternSet =new ArrayList<>();
    }

    /**
     * Internal function
     * Processes response body according to the matchbuilder specifications and returns JSONObject
     * @param responseText String on which the operations are to be conducted
     * @return JSONObject Object with all the extracted properties
     */
    @Override
    protected JSONObject processResponseText(String responseText) {
        JSONObject output = new JSONObject();

        for(PatternBlueprint test: patternSet)
        {
            Pattern p=Pattern.compile(test.pattern);
            Matcher m=p.matcher(responseText);

            JSONArray arr=new JSONArray();
            while (m.find())
            {
                Log.v("debug", "find(): " + m.group());
                JSONObject single=test.once?output:new JSONObject();

                //insers keys from mainTest
                for (Map.Entry<String,Integer> outkey:test.outputKeySet.entrySet()) {

                    Log.v("debug", "outputkeyset: " + outkey.getKey());
                    try {
                        single.put(outkey.getKey(), m.group(outkey.getValue()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                for(PatternBlueprint subtest:test.subPatternSet)
                {
                    String patern=test.pattern;
                    for (int i = 1; i <= m.groupCount(); ++i) {
                        patern = patern.replace("%" + i, m.group(i));
                    }
                    Pattern p2=Pattern.compile(patern);
                    Matcher m2=p2.matcher(m.group(subtest.source_group));
                    if(m2.find())
                        //insert keys from subtest
                        for (Map.Entry<String,Integer> outkey:subtest.outputKeySet.entrySet())
                            try {
                                single.put(outkey.getKey(),m2.group(outkey.getValue()));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                continue;
                            }
                }
                if(!test.once)
                {
                    arr.put(single);
                }

            }
            if(!test.once) try {
                output.put(test.id,arr);
            } catch (JSONException e) {
                continue;
            }

        }return output;
    }

    /**
     * Adds a new regex pattern to the lookup list.
     * This creates and returns a new {@link PatternBlueprint} Object, insert it into pattern set and returns the object
     * @param id Unique identifier for this pattern. If the pattern matches multiple elements, the extracted value branch will be a JSONArray with this id as key
     * @param pattern the pattern to match on the target string
     * @return {@link PatternBlueprint} Object, for chaining
     */
    public PatternBlueprint addTest(String id,String pattern)
    {
        PatternBlueprint blueprint=new PatternBlueprint(id,pattern);
        patternSet.add(blueprint);
        return blueprint;
    }

    /**
     * Blueprint for values to be extracted corresponding to each regex pattern enlisted using builder
     */

    public class PatternBlueprint {

        private final String pattern;
        private final PatternBlueprint parent;
        private Boolean once=false;
        private String id;
        private int source_group;
        private HashMap<String,Integer> outputKeySet;
        private ArrayList<PatternBlueprint> subPatternSet;

        /**
         * constructor #1
         * for main patterns
         */
        public PatternBlueprint(String id,String pattern) {
            this.pattern=pattern;
            outputKeySet=new HashMap<>();
            this.id=id;
            this.parent=this;
            subPatternSet=new ArrayList<>();
        }

        /**
         * constructor #2
         * for subpatterns
         */
        public PatternBlueprint(String pattern,int group,PatternBlueprint parent) {
            //if(parent.parent!=parent) throw new IllegalArgumentException("Subpatterns can't be created on a subpattern");
            this.pattern=pattern;
            this.once=true;
            this.source_group=group;
            outputKeySet=new HashMap<>();
           this.parent = parent;
        }

        /**
         * match the pattern only once. If the pattern is recurring, only the first occurrence is matched.
         */

        public PatternBlueprint doOnce()
        {
            once=true;
            return this;
        }

        /**
         * Calls {@link RegexValueMatchBuilder#addTest(String, String)}
         * Adds a new regex pattern to the lookup list.
         * This creates and returns a new {@link PatternBlueprint} Object, insert it into pattern set and returns the object
         * @param id Unique identifier for this pattern. If the pattern matches multiple elements, the extracted value branch will be a JSONArray with this id as key
         * @param pattern the pattern to match on the target string
         * @return {@link PatternBlueprint} Object, for chaining
         */
        public PatternBlueprint addTest(String id,String pattern) {
            return RegexValueMatchBuilder.this.addTest(id, pattern);
        }

        /**
         * Adds a new regex pattern to the lookup list within the parent {@link PatternBlueprint} scope
         * This creates and returns a new {@link PatternBlueprint} Object, insert it into pattern set and returns the object
         * Subpatterns are matched only once, equivalent to calling {@link #doOnce()}
         * Subpatterns defined on a subpattern will be ignored
         * @param pattern the pattern to match on the target string
         * @param source_group backreference from the parent patten match to be used as the target string for this sub pattern to test
         * @return {@link PatternBlueprint} Object, for chaining
         */
        //        * Subpatterns defined on a subpattern will throw IllegalArgumentException

        public PatternBlueprint addSubTest(String pattern,int source_group) {
            PatternBlueprint blueprint=new PatternBlueprint(pattern,source_group,parent);
            subPatternSet.add(blueprint);
            return blueprint;
        }

        /**
         * Sets {@code key} in Output JSON to value corresponding to backreference group number from pattern match
         * @param key the key to which the value is to be set in the output
         * @param group backreference group number from pattern match
         * @return this, for chaining
         */
        public PatternBlueprint set(String key, int group)
        {
            outputKeySet.put(key,group);
            return this;
        }

        /**
         * Returns the MatchBuilder Object
         * @return RegexValueMatchBuilder object containing list of all the {@link PatternBlueprint} objects
         */
        public RegexValueMatchBuilder close()
        {
            return RegexValueMatchBuilder.this;
        }

    }
}