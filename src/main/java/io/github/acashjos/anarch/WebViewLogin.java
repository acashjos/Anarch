package io.github.acashjos.anarch;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;

public class WebViewLogin extends Activity {

    private String cookies="";
    private int pageloadCount=0;
    private WebView web;
    private ActionBar actionbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_login);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setCookie("https://m.facebook.com", "");
        actionbar=getActionBar();
        actionbar.setTitle("");
        web = (WebView) findViewById(R.id.loginfb);
        web.loadUrl("https://m.facebook.com");
        WebSettings webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {

                Log.d("debug",url);

                actionbar.setTitle(view.getTitle());
                CookieManager cookieManager = CookieManager.getInstance();
                WebViewLogin.this.cookies= cookieManager.getCookie(url);
                pageloadCount++;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    // In KitKat+ you should use the evaluateJavascript method
                    kitkat(view);


                else older(view);

            }

        });
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
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
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

            Session.LoginState decision=Session.getActiveSession().loginDecisionLogic.loginDecision(
                    pageloadCount,
                    web,
                    Session.getActiveSession().evaluateMatches(result),
                    cookies);

             Log.v("debug", "decision: " + decision.toString());
            if(decision== Session.LoginState.SUCCESS)
            {
                getApplication().getSharedPreferences("cache", Context.MODE_PRIVATE).edit().putString("session",cookies).commit();

                Intent intent=new Intent();
                intent.putExtra("login",true);
                setResult(Session.LOGIN_DONE,intent);
                finish();
            }
            else if(decision== Session.LoginState.FAIL)
            {
                Toast.makeText(this,"Login failed",Toast.LENGTH_SHORT).show();
                finish();
            }
             else
                return;
        }

    class MyJavaScriptInterface {

        public void showHTML(String result) {
            WebViewLogin.this.finalize(result);
        }

    }
}

