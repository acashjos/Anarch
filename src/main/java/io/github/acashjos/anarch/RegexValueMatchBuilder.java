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

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegexValueMatchBuilder extends MatchBuilder {


    private String regex;
    private HashMap<String, Integer> firstmatch;
    private HashMap<String, String> submatch;
    private HashMap<String, String> prefill;
    private ArrayList<String> once;
    private boolean singleValue = false;

      protected JSONObject processResultText(Connection.Response response) throws JSONException {

          String result = response.body();
          JSONObject result_set = new JSONObject();
          result_set.put("headers", new JSONObject(response.headers()));

            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(result);
          //Log.e("debug", "patern: "+regex);
            int c = 0;
            while (m.find()) {
                //Log.e("debug", regex + "-" + (c++));
                //Log.e("debug", m.group());
                //Log.e("debug", m.group(0));
                HashMap<String, String> result_map = new HashMap<>();
                //copy prefills
                Iterator entries = prefill.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    result_map.put((String) entry.getKey(), (String) entry.getValue());
                }
                //copy back refferences
                entries = firstmatch.entrySet().iterator();
                while (entries.hasNext()) {

                    Map.Entry entry = (Map.Entry) entries.next();
                    String key = (String) entry.getKey();
                    Integer value = (Integer) entry.getValue();
                    if (m.group(value) != null || result_map.get(key)==null)
                        result_map.put(key, m.group(value));
                }
                //run sub regex and copy relevent data
                entries = submatch.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    String key = (String) entry.getKey();
                    String value = (String) entry.getValue();
                    for (int i = 1; i <= m.groupCount(); ++i) {
                        value = value.replace("$" + i, m.group(i));
                    }
                    Pattern pp = Pattern.compile(value);
                    Matcher mm = pp.matcher(result);
                    if (mm.find() && mm.group(1) != null)
                        result_map.put(key, mm.group(1));
                    if (once.contains(key)) {
                        entries.remove();
                        prefill.remove(key);
                    }
                }

                result_set.put("data", new JSONObject(result_map));
                if (singleValue) break;
            }
            return result_set;
        }



    public RegexValueMatchBuilder() {
        firstmatch = new HashMap<>();
        submatch = new HashMap<>();
        prefill = new HashMap<>();
        once = new ArrayList<>();
    }

    public RegexValueMatchBuilder base_exp(String regex) {
        this.regex = regex;
        return this;
    }

    public RegexValueMatchBuilder fill(String property, int matchGroup) {

        firstmatch.put(property, matchGroup);
        return this;
    }

    public RegexValueMatchBuilder fill(String property, String regex, String defaultValue) {
        prefill.put(property, defaultValue);
        submatch.put(property, regex);
        return this;
    }

    public RegexValueMatchBuilder pre_fill(String property, String value) {
        prefill.put(property, value);
        return this;
    }

    public RegexValueMatchBuilder fill_once(String property, String regex, String defaultval) {
        once.add(property);
        return fill(property, regex, defaultval);
    }

    public RegexValueMatchBuilder once() {
        singleValue = true;
        return this;
    }



}
