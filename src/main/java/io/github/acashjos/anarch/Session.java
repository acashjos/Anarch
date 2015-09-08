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
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.Map;

//import android.util.Log;

/**
 * Session class
 */
public class Session{

    public static final int LOGIN_DONE = 47;
    public static final int LOGIN_UI =11;

    private static Session session;
    private final SharedPreferences pref;
    private String url;
    private MatchBuilder matchBuilder;
    LoginDecisionLogic loginDecisionLogic;
    private String cookies;
    private SuccessCallback callback;

    /**
     * Set user id for current session
     * @param id id
     */
    protected void setId(String id) {
         pref.edit().putString("id", id).commit();
    }

    /**
     * Set user name for current session
     * @param name name
     */
    protected void setName(String name) {
        pref.edit().putString("name", name).commit();
    }
    /**
     * Set metadata as key-value pair for current session
     * @param key key
     * @param value value
     */
    protected void setInfo(String key,String value) {
        pref.edit().putString(key,value).commit();
    }

    /**
     * returns Name associated with current session.
     * Will be available only if MatchBuilder used in initialization contains specification for a "name" value
     * or {@link LoginDecisionLogic#loginDecision(int, WebView, JSONObject, String)} provided in initialization calls {@link #setName(String)}
     * @return
     */
    public String getName()
    {
        return pref.getString("name",null);
    }
    /**
     * returns USER ID associated with current session.
     * Will be available only if MatchBuilder used in initialization contains specification for an "id" value
     * or {@link LoginDecisionLogic#loginDecision(int, WebView, JSONObject, String)} provided in initialization calls {@link #setId(String)}
     * @return
     */
    public String getId()
    {
        return pref.getString("id",null);
    }
    /**
     * returns metadata associated with current session.
     * Will be available only if {@link LoginDecisionLogic#loginDecision(int, WebView, JSONObject, String)} provided in initialization calls {@link #setInfo(String, String)}
     * @return
     */
    public String getInfo(String key)
    {
        return pref.getString(key,null);
    }

    /**
     * Logout current session. It clears all saved data about this session
     */
    public void logoutSession() {
        cookies="";
        session=null;
        pref.edit().clear().commit();
    }
//package local
    String getCookieString() {
        return cookies;

    }

//package local. updates cookies after jsoup makes requests
    void setCookies(Map<String, String> cookies) {
        String cookstrt="";
        for( Map.Entry<String,String> item:cookies.entrySet())
            cookstrt+=item.getKey()+"="+item.getValue()+";";
        pref.edit().putString("session",cookstrt).commit();
    }

    public enum LoginState{SUCCESS, TRANSIT, FAIL}

    /**
     * constructor
     * @param applicationContext applicationContext
     */
    private Session(Context applicationContext) {
        this.pref=applicationContext.getApplicationContext().getSharedPreferences("cache", Context.MODE_PRIVATE);
        cookies=pref.getString("session","");
    }

    /**
     * Checks if session is already initialized
     * @return boolean
     */
    public static boolean isIntialized() {
        return session!=null;
    }

    /**
     * Initializes Session with configurations provided
     * @param applicationContext
     * @param url Login url
     * @param matchBuilder to extract data from welcome page after login
     * @param logic Callback after each webview pageload. Used to decide if login was successful
     */
    public static void initialize(Context applicationContext, String url, MatchBuilder matchBuilder, LoginDecisionLogic logic) {
        if(session!=null) throw new RuntimeException("Session has already been initialized");
        session=new Session(applicationContext);
        session.url=url;
        session.matchBuilder=matchBuilder;
        session.loginDecisionLogic =logic==null?new BackupLoginDecisionLogic():logic;
    }

    /**
     * Initializes Session with configurations provided
     * @param applicationContext
     * @param url Login url
     * @param logic Callback after each webview pageload. Used to decide if login was successful
     */
    public static void initialize(Context applicationContext, String url, LoginDecisionLogic logic) {
        if(logic==null) throw new IllegalArgumentException("Logic cant be null with this initializer");
        initialize( applicationContext,  url,  null,  logic);
    }
    /**
     * Initializes Session with configurations provided
     * @param applicationContext
     * @param url Login url
      */public static void initialize(Context applicationContext, String url) {
        initialize( applicationContext,  url,  null, null);
    }

    /**
     * Returns currect session
     * @return Session
     */
    public static Session getActiveSession() {
        return session;
    }
//package local
     JSONObject evaluateMatches(String result) {

        if(matchBuilder==null) return null;
        return matchBuilder.processResponseText(result);
    }

    /**
     * To be called inside onActivityResult method of activity from which login is triggered
     * @param requestCode requestCode
     * @param resultCode resultCode
     * @param data data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==LOGIN_UI && resultCode==Activity.RESULT_OK)
        {
            cookies=pref.getString("session","");
            callback.call(this);}
    }

    /**
     * Initiates login flow.
     * @param activity parent activity from which this method is called
     * @param successCallback callback to be called when login completes successfully
     */
    public void openNewSession(Activity activity, SuccessCallback successCallback) {
        this.callback= successCallback;
        Intent i=new Intent(activity,WebViewLogin.class);
        i.putExtra("url",url);
        activity.startActivityForResult(i,LOGIN_UI);
    }

    public interface SuccessCallback {

        // callback when session changes state
        void call(Session session);
    }

    public static abstract class LoginDecisionLogic {
        public abstract LoginState loginDecision(int pageloadCount, WebView view, JSONObject values, String cookies);
    }

    private static class BackupLoginDecisionLogic extends LoginDecisionLogic {
        public LoginState loginDecision(int pageloadCount, WebView view, JSONObject values, String cookies) {

            if(WebViewLogin.instance!=null)
            {
                JSONObject passwordField = MatchBuilder.run(WebViewLogin.instance.htmlText,
                        (new DOMSelectorMatchBuilder())
                                .select("input[type=password]","null")
                                .set("pfield", "name")
                                .close());

                if(passwordField.has("pfield")) {

                    //Log.v("debug", "pageLoadcount: "+pageloadCount);
                    if (pageloadCount < 2)
                        return LoginState.TRANSIT;


                    return LoginState.FAIL;
                }
                else return LoginState.SUCCESS;

            }
            //Log.v("debug", "webloginview instance is null");
            return LoginState.FAIL;
        }
    }
}