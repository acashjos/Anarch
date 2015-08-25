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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class Request {

    private final MatchBuilder matchBuilder;
    private final ValuesHandler loadedListener;
    // private final String uid;
    private Session session;

    private ArrayList<String> regexList;


    public Request( MatchBuilder matchBuilder,ValuesHandler loadedListener)
    {
       // this.uid = url;
        this.matchBuilder = matchBuilder;
        this.loadedListener=loadedListener;
      }

    public void makeRequest(String url)
    {
        Async async = new Async();
        async.execute(url);//("https://m.facebook.com/messages/read/?fbid=" + uid, uid);

    }

    public void setSession(Session session) {
        this.session = session;
    }


    private class Async extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {
            //Log.v("debug", "url: "+params[0]);

            try{
                HttpURLConnection cn = (HttpURLConnection) new URL(params[0]).openConnection();
                //Log.v("debug", "connecting");
                cn.setRequestProperty("User-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0");
                cn.setRequestProperty("Host", "m.facebook.com");
                cn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                cn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                //cn.setRequestProperty("Accept-Encoding", "gzip, deflate");
                cn.setRequestProperty("Connection", "keep-alive");
                cn.setRequestProperty("Pragma", "no-cache");
                cn.setRequestProperty("Cache-Control", "no-cache");
                if(session!=null)
                    session.makeCall(cn);
                else cn.connect();

                InputStream in = cn.getInputStream();

                /*GZIPInputStream gzip= new GZIPInputStream(in);

                byte[] buffer = new byte[1024];
                int len;
                StringBuilder text = new StringBuilder();
                while((len = gzip.read(buffer)) != -1){
                    text.append(new String(buffer));
                }*/
                /**/
                InputStreamReader isw = new InputStreamReader(in);

                BufferedReader br = new BufferedReader(isw);
                String line;
                StringBuilder text = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();/**/
                return text.toString();

            } catch (IOException e) { return "";}
        }

        protected void onPostExecute(String result) {

            //Log.v("debug", "done");
            //Log.v("debug", ""+result.length());
            loadedListener.onValuesLoaded(matchBuilder.processResultText(result));
        }
    }


}
