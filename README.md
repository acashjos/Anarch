# Anarch
>This library is still in beta. Various components may change in future.

An android library to login to websites, manage session and extract data using those cookies
>Uses [jsoup](http://jsoup.org/)

##Include in Android project

* Clone this repository
* Import module into androidstudio project [ *file > new > import module* ]
* in your app/build.gradle add 
    `compile project(':anarch')` in dependancies section

##Usage
###Session and login
>**Session is a singleton**

Explained using login to Wikipedia . 
>This example uses basic built in login decision logic, that is, to check if password field is present in the loaded page. 
>You can use your own decision logic using other versions of `Session.initialize`. see documentation

Implement the following within an activity. This code will open up a login page. SuccessCallback will be called when login is successfull
```
String loginURL="https://en.m.wikipedia.org/wiki/Special:UserLogin";
Session.initialize(getApplicationContext(), loginURL);
 Session.getActiveSession().openNewSession(MainActivity.this, new Session.SuccessCallback() {
            @Override
            public void call(Session session) {
            //callback when login is successfull
            }
        });
```
You must include this in the `onActivityResult` block of your activity
```
Session.getActiveSession().onActivityResult(requestCode, resultCode, data);
```
###Request and data extraction
The usage is shown with a simple example on wikipedia homepage. This example will extract the innerHTML of \<title\> tag.

A request is made as follows
```
Request request=new Request(matchBuilder,valuesHandler);
request.makeRequest("https://en.m.wikipedia.org");
```
where *matchBuilder* is an object of **MatchBuilder**. This object packs the blueprints for all operations to be performed on response body.
Anarch includes 2 implementations of MatchBuilder abstract class
* **DOMSelectorMatchBuilder** for data extraction using CSS selectors on an HTML page
* **RegexValueMatchBuilder**, a generic solution for usage on non-HTML/plain-text responses

In this example, *DOMSelectorMatchBuilder* is used
```
DOMSelectorMatchBuilder matchBuilder = new DOMSelectorMatchBuilder();
matchBuilder.select("title","some-identifier")
            .set("key","innerHTML")
            .close();
  ```
> **Note**
>`.set(key,attribute)` can be called repeatedly to extract more attributes from an HTML element.
>`.select(selector,identifier)` can be called repeatedly to extract data from differnt HTML elements.
>`.select(selector,identifier)` must be followed by `.set(key,attribute)` to be performed on currently selected element.
>  This would look like
>  `matchBuilder.select(selector1,id1).set(key,attrib).set(key2,attrib).select(selector2,id2).set(key3,attrib).close()`
  
  *valuesHandler* is the callback to be executed when extracted data is available
  ```
  ValuesHandler valuesHandler = new ValuesHandler() {
            @Override
            public void onValuesLoaded(JSONObject result) {
            //result JSON
                Log.i("JSON String",result.toString());
            }
        };
````

The resultant JSON will look like this
```
{
	"headers": {
		"Accept-Ranges": "****",
		"Age": "****",
		"Cache-Control": "****",
		"Connection": "****",
		... etc
	},
	"data": {
		"key": "Wikipedia, the free encyclopedia"
	}
}
```
