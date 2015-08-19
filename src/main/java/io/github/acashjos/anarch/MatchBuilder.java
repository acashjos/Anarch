package io.github.acashjos.anarch;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by acashjos on 16/8/15.
 */
public abstract class MatchBuilder {
    protected abstract ArrayList<HashMap<String, String>> processResultText(String result);

}
