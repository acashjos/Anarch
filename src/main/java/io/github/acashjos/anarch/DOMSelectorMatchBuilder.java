package io.github.acashjos.anarch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by acashjos on 26/8/15.
 */
public class DOMSelectorMatchBuilder extends MatchBuilder {

    HashMap<String, Properties> keyList;
    private String currentItem;

    public DOMSelectorMatchBuilder() {
        this.keyList = new HashMap<>();
    }

    @Override

    protected JSONObject processResultText(Connection.Response result) {

        JSONObject output=new JSONObject();
        Document dom = Jsoup.parse(result.body(), result.url().toString());
        for(Map.Entry<String,Properties> item:keyList.entrySet())
        {
            Elements elems = dom.select(item.getValue().selector);
            if(elems.size()==0)
                continue;
            else if(elems.size()==1)
            {
                try {
                    output.put(item.getKey(), extract(item.getValue().props,elems.get(0)));
                } catch (JSONException e) { continue;}
            }
            else
            {
                JSONArray jsonArray=new JSONArray();
                for(Element sub_elm:elems)
                {
                    try {
                        if(item.getValue().props.size()==0)
                            //if no properties specified, return innerHTML
                            jsonArray.put(extract("html",elems.get(0)));

                        else if(item.getValue().props.size()==1)
                            jsonArray.put(extract(item.getValue().props.get(0),elems.get(0)));

                        else
                            jsonArray.put(extract(item.getValue().props,elems.get(0)));
                    } catch (JSONException e) {
                        continue;
                    }
                }
            }
        }
        return output;
    }

    /**
     * Adds a new key to the addTargetProperty list along with a CSS selector string.
     * The values will be extracted from HTML element/elements matched by the selector string.
     * Add attributes/properties to be extracted using {@code addTargetProperty(String attribute)}. Default property to be extracted is innerHTML
     * This will result in a JSONObject with {@code key} as key and extracted result as value
     * @param key the key to be extracted
     * @param selector the selector to select HTML element/elements from which values can be extracted
     * @return this, for chaining
     * @see DOMSelectorMatchBuilder#addTargetProperty(String attribute)
     * @see DOMSelectorMatchBuilder#addTargetProperty(String key,String attribute)
     */
    public DOMSelectorMatchBuilder set(String key,String selector)
    {
        Properties properties=new Properties(selector);
        this.currentItem=key;
        keyList.put(key,properties);
        return this;
    }


    /**
     * Adds an attribute or property to the list of target attributes/properties for the last added key using {@code set(key,selector)}
     * @param attribute the attribute to be extracted
     * @return this, for chaining
     * @see DOMSelectorMatchBuilder#addTargetProperty(String key,String attribute)
     */
    public DOMSelectorMatchBuilder addTargetProperty(String attribute)
    {
        if(currentItem!=null)
            keyList.get(currentItem).add(attribute);
        return this;
    }


    /**
     * Adds an attribute or property to the list of target attributes for the last added key using {@code set(key,selector)}
     * @param key the key for which the attribute is to be added
     * @param attribute the attribute to be extracted
     * @return this, for chaining
     * @see DOMSelectorMatchBuilder#addTargetProperty(String key,String attribute)
     */
    public DOMSelectorMatchBuilder addTargetProperty(String key, String attribute)
    {
        keyList.get(key).add(attribute);
        return this;
    }

    private String extract(String attr, Element element) throws JSONException {
        switch (attr.toLowerCase())
        {
            case "innertext":
            case "text":
                return element.text();

            case "tagname":
            case "tag":
                return element.tagName();

            case "innerhtml":
            case "html":
                return element.html();
            case "cssselector":
                return element.cssSelector();
            default:
                return element.attr(attr);
        }
    }
    private JSONObject extract(ArrayList<String> props,Element element) throws JSONException {
        JSONObject vals=new JSONObject();
        for(String attr:props)
        {
            vals.put(attr, extract(attr,element));
        }
        return vals;
    }

    public class Properties{
        private final String selector;
        private ArrayList<String> props;

        public Properties(String selector) {

            this.selector=selector;
            props=new ArrayList<>();

        }


        public void add(String attribute) {
            props.add(attribute);
        }
    }


}
