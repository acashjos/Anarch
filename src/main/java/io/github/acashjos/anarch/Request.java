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

import android.os.AsyncTask;
//import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Request {

    private final MatchBuilder matchBuilder;
    private final ValuesHandler loadedListener;
    private final Map<String, String> headers;
    private final Map<String, String> cookies;
    // private final String uid;
    private Session session;

    private ArrayList<String> regexList;


    public Request( MatchBuilder matchBuilder,ValuesHandler loadedListener)
    {
       // this.uid = url;
        this.matchBuilder = matchBuilder;
        this.loadedListener=loadedListener;
        this.headers=new HashMap<>();
        this.cookies=new HashMap<>();

        headers.put("User-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0");
        headers.put("Host", "m.facebook.com");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Connection", "keep-alive");
        headers.put("Pragma", "no-cache");
        headers.put("Cache-Control", "no-cache");
      }

    public void makeRequest(String url)
    {
        Async async = new Async();
        async.execute(url);//("https://m.facebook.com/messages/read/?fbid=" + uid, uid);

    }

    public void setSession(Session session) {
        this.session = session;

        String cookieset="";
        if(session!=null)
            cookieset=session.getCookieString();
        String[] rawCookieParams = cookieset.split(";");

        String[] rawCookieNameAndValue = rawCookieParams[0].split("=");

        for (int i = 1; i < rawCookieParams.length; i++) {
            String rawCookieParamNameAndValue[] = rawCookieParams[i].trim().split("=");

            if (rawCookieParamNameAndValue.length != 2) continue;

            cookies.put(rawCookieParamNameAndValue[0].trim(), rawCookieParamNameAndValue[1].trim());
        }

    }

    public void setHeader(String key,String val)
    {
        headers.put(key,val);
    }
    public void setCookie(String key,String val)
    {
        cookies.put(key,val);
    }

    private class Async extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {
            //Log.v("debug", "url: "+params[0]);

            try{
                Connection con= HttpConnection.connect(new URL(params[0]));
                HttpURLConnection cn = (HttpURLConnection) new URL(params[0]).openConnection();
                //Log.v("debug", "connecting");
                for(Map.Entry<String,String> item:headers.entrySet())
                {
                    con.header(item.getKey(), item.getValue());
                }

                con.cookies(cookies);

                Connection.Response response = con.execute();

                if(session!=null)
                    session.setCookies(response.cookies());
                return  response.body();

            } catch (IOException e) { return "";}
        }

        protected void onPostExecute(String result) {

            //Log.v("debug", "done");
            //Log.v("debug", ""+result.length());
            loadedListener.onValuesLoaded(matchBuilder.processResultText(result));
        }
    }


}
