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

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class MatchBuilder {

    protected Connection.Response response;

    protected JSONObject processResponse(Connection.Response response) {

        this.response=response;
        Log.e("debug", "url" + response.url());
        String result = response.body();

        JSONObject data = processResponseText(result);
        JSONObject output = new JSONObject();
        try {
            return  (new JSONObject())
                    .put("headers", new JSONObject(response.headers()))
                    .put("data",data);
        } catch (JSONException e) {
            return null;
        }
    }
    protected abstract JSONObject processResponseText(String responseText);

    public static JSONObject run(String target,MatchBuilder matchBuilder){
        return matchBuilder.processResponseText(target);
    }

}
