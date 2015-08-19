package io.github.acashjos.anarch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
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

import in.oxylab.buddybuzzer.R;

public class WebViewLogin extends ActionBarActivity {

    private String cookies="";
    private int pageloadCount=0;
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_login);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setCookie("https://m.facebook.com","");

        web = (WebView) findViewById(R.id.loginfb);
        web.loadUrl("https://m.facebook.com");
        WebSettings webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {

                Log.d("debug",url);
                CookieManager cookieManager = CookieManager.getInstance();
                String cookies = cookieManager.getCookie(url);
                WebViewLogin.this.cookies= cookies;
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_web_view_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

         void finalize(String result) {

             File file = new File(getApplicationContext().getFilesDir(), "page.cache");
/*
             try {
                 file.createNewFile();
                 FileOutputStream writer = openFileOutput(file.getName(), Context.MODE_PRIVATE);
                 writer.write(result.getBytes());
                 writer.flush();
                 writer.close();
             } catch (IOException e) {	e.printStackTrace();			}*/

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

