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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.util.Log;
import android.webkit.WebView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Session{

    public static final int LOGIN_DONE = 47;
    public static final int LOGIN_UI =11;

    private static Session session;
    private final SharedPreferences pref;
    private String url;
    private MatchBuilder matchBuilder;
    LoginDecisionLogic loginDecisionLogic;
    private String cookies;
    private StatusCallback callback;

     protected void setId(String id) {
         pref.edit().putString("id", id).commit();
    }

    protected void setName(String name) {
        pref.edit().putString("name", name).commit();
    }
    protected void setInfo(String key,String value) {
        pref.edit().putString(key,value).commit();
    }

    public String getName()
    {
        return pref.getString("name",null);
    }
    public String getId()
    {
        return pref.getString("id",null);
    }
    public String getInfo(String key)
    {
        return pref.getString(key,null);
    }

    public void logoutSession() {
        cookies="";
        pref.edit().clear().commit();
    }

    void makeCall(HttpURLConnection cn) {
        cn.setRequestProperty("Cookie", cookies);
        try {
            cn.connect();
        } catch (IOException e) {

        }

    }

    public enum LoginState{SUCCESS, TRANSIT, FAIL}

    private Session(Context applicationContext) {
        this.pref=applicationContext.getApplicationContext().getSharedPreferences("cache", Context.MODE_PRIVATE);
        cookies=pref.getString("session","");
    }

    public static boolean isIntialized() {
        return session!=null;
    }

    public static void initialize(Context applicationContext, String url, MatchBuilder matchBuilder, LoginDecisionLogic logic) {
        if(session!=null) throw new RuntimeException("Session has already been initialized");
        session=new Session(applicationContext);
        session.url=url;
        session.matchBuilder=matchBuilder;
        session.loginDecisionLogic =logic==null?new BackupLoginDecisionLogic():logic;
    }

    public static Session getActiveSession() {
        return session;
    }

    public ArrayList<HashMap<String, String>> evaluateMatches(String result) {
        if(matchBuilder==null) return null;
        return matchBuilder.processResultText(result);
    }

    public boolean isOpen() {
        return cookies!=null&& !cookies.equals("");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==LOGIN_UI && resultCode==LOGIN_DONE)
        {
            cookies=pref.getString("session","");
            callback.call(this);}
    }

    public void openNewSession(Activity activity, StatusCallback statusCallback) {
        this.callback=statusCallback;
        Intent i=new Intent(activity,WebViewLogin.class);
        activity.startActivityForResult(i,LOGIN_UI);
    }

    public interface StatusCallback {

        // callback when session changes state
        void call(Session session);
    }

    public static abstract class LoginDecisionLogic {
        public abstract LoginState loginDecision(int pageloadCount, WebView view, ArrayList<HashMap<String, String>> values, String cookies);
    }

    private static class BackupLoginDecisionLogic extends LoginDecisionLogic {
        public LoginState loginDecision(int pageloadCount, WebView view, ArrayList<HashMap<String, String>> values, String cookies) {
           // Log.v("debug", "pageloadCount: " + pageloadCount);
            //Log.v("debug", "values: " + (values==null?"null":values.size()));
            //Log.v("debug", "cookie: " + cookies);

      //if matchbuilder is not provided
            if (values == null ) {
                if (pageloadCount < 2) return LoginState.TRANSIT;
                else if (cookies == null || cookies.equals("")) return LoginState.FAIL;
                return LoginState.SUCCESS;
            }
            //else

            Boolean empty=false;
            if(values.size()==0) empty=true;
            for (HashMap hash : values) {
                Iterator entries = hash.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    //Log.d("debug", (String) entry.getValue());
                    if (entry.getValue() == null) empty=true;
                    if (entry.getKey().equals("id")) session.setId((String) entry.getValue());
                    else if (entry.getKey().equals( "name")) session.setName((String) entry.getValue());
                }
            }
            if(empty)
                if(pageloadCount<2) return LoginState.TRANSIT;
                else return LoginState.FAIL;
            return LoginState.SUCCESS;
        }
    }
}