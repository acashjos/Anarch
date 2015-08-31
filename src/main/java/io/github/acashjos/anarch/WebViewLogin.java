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

import android.annotation.SuppressLint;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;

import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebViewLogin extends Activity {

    private String cookies="";
    private int pageloadCount=0;
    private WebView web;
    String htmlText;
    static WebViewLogin instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_login);

        //Log.e("debug", "Login Activity created");
        instance=this;
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setCookie("https://m.facebook.com", "");
        setTitle("");
        web = (WebView) findViewById(R.id.loginfb);
        web.loadUrl(getIntent().getStringExtra("url"));
        WebSettings webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {

                setTitle(view.getTitle());
                CookieManager cookieManager = CookieManager.getInstance();
                WebViewLogin.this.cookies= cookieManager.getCookie(url);
                pageloadCount++;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    // In KitKat+ you should use the evaluateJavascript method
                    kitkat(view);


                else older(view);

            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                view.setVisibility(View.GONE);
                findViewById(R.id.loading).setVisibility(View.VISIBLE);
                findViewById(R.id.loadingtext).setVisibility(View.VISIBLE);
            }

        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        instance=null;
    }

    @SuppressLint("JavascriptInterface")
    private void older(WebView view) {
        view.addJavascriptInterface(new MyJavaScriptInterface(), "HtmlViewer");

                view.loadUrl("javascript:window.HtmlViewer.showHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");

    }


    @SuppressLint("NewApi")
    private void kitkat(WebView view) {
        view.evaluateJavascript(
                "(function() { return window.btoa('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String result) {
                        WebViewLogin.this.finalize(result);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

         void finalize(String result) {


             htmlText = new String(Base64.decode(result, Base64.DEFAULT));

             Session.LoginState decision=Session.getActiveSession().loginDecisionLogic.loginDecision(
                    pageloadCount,
                    web,
                    Session.getActiveSession().evaluateMatches(htmlText),
                    cookies);

             if(decision== Session.LoginState.SUCCESS)
            {
                getApplication().getSharedPreferences("cache", Context.MODE_PRIVATE).edit().putString("session",cookies).commit();

                Intent intent=new Intent();
                intent.putExtra("login",true);
                setResult(RESULT_OK,intent);
                finish();
            }
            else if(decision== Session.LoginState.FAIL)
            {
                Toast.makeText(this,"Login failed",Toast.LENGTH_SHORT).show();
                finish();
            }
             else if(decision==Session.LoginState.TRANSIT)

                web.setVisibility(View.VISIBLE);
                findViewById(R.id.loading).setVisibility(View.GONE);
             findViewById(R.id.loadingtext).setVisibility(View.GONE);
        }

    class MyJavaScriptInterface {

        public void showHTML(String result) {
            WebViewLogin.this.finalize(result);
        }

    }
}

