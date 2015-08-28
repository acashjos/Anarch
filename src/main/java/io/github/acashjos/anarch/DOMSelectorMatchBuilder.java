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

    HashMap<String, PropertiesBlueprint> selectorMap;
    private String currentItem;

    public DOMSelectorMatchBuilder() {
        this.selectorMap = new HashMap<>();
    }

    @Override

    protected JSONObject processResultText(Connection.Response result) {

        JSONObject output=new JSONObject();
        Document dom = Jsoup.parse(result.body(), result.url().toString());
        for(Map.Entry<String,PropertiesBlueprint> item: selectorMap.entrySet())
        {
            Elements elems = dom.select(item.getKey());
            if(elems.size()==0)
                continue;
            else if(elems.size()==1)
            {
                try {
                    for(Map.Entry<String,String> property : item.getValue().props.entrySet())
                        output.put(property.getKey(), extract(property.getValue(),elems.get(0)));
                } catch (JSONException e) { continue;}
            }
            else
            {
                JSONArray jsonArray=new JSONArray();
                for(Element sub_elm:elems)
                {
                    try {
                        for(Map.Entry<String,String> property : item.getValue().props.entrySet())
                            output.put(property.getKey(), extract(property.getValue(),sub_elm));
                    } catch (JSONException e) { continue;}
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
     * @param selector the selector to select HTML element/elements from which values can be extracted
     * @return this, for chaining
     */
    public PropertiesBlueprint select(String selector)
    {
        if(selectorMap.containsKey(selector))
            return selectorMap.get(selector);

        PropertiesBlueprint propertiesBlueprint =new PropertiesBlueprint(selector);
        this.currentItem=selector;
        selectorMap.put(selector, propertiesBlueprint);
        return propertiesBlueprint;
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
            case "selector":
                return element.cssSelector();
            default:
                return element.attr(attr);
        }
    }

    private class PropertiesBlueprint {

        private final String selector;
        private HashMap<String,String> props;

        public PropertiesBlueprint(String selector) {

            this.selector=selector;
            props=new HashMap<>();

        }


        /**
         * Adds an attribute or property to the list of target attributes/properties for the last added key using {@code set(key,selector)}
         * @param key the key to which the attribute value is to be set in the output
         * @param attribute the attribute to be extracted
         * @return this, for chaining
         */
        public PropertiesBlueprint set(String key,String attribute)
        {
            props.put(key, attribute);
            return this;
        }

        public PropertiesBlueprint set(String key)
        {
            props.put(key,"html");
            return this;
        }

        public PropertiesBlueprint select(String selector)
        {
            return DOMSelectorMatchBuilder.this.select(selector);
        }

        public DOMSelectorMatchBuilder close()
        {
            return DOMSelectorMatchBuilder.this;
        }

    }


}
