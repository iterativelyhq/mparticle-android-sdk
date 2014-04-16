package com.mparticle;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by sdozor on 3/13/14.
 */
abstract class EmbeddedProvider implements IEmbeddedKit {

    final static String KEY_ID = "id";
    private final static String KEY_PROPERTIES = "as";
    private final static String KEY_FILTERS = "hs";
    private final static String KEY_EVENT_TYPES = "et";
    private final static String KEY_EVENT_NAMES = "ec";
    private final static String KEY_EVENT_ATTRIBUTES = "ea";
    private final static int MAT = 32;

    //If set to true, our sdk honor user's optout wish. If false, we still collect data on opt-ed out users, but only for reporting
    private static final String HONOR_OPT_OUT = "honorOptOut";

    protected HashMap<String, String> properties = new HashMap<String, String>(0);
    protected HashMap<Integer, Boolean> types = new HashMap<Integer, Boolean>(0);
    protected HashMap<Integer, Boolean> names = new HashMap<Integer, Boolean>(0);
    protected HashMap<Integer, Boolean> attributes = new HashMap<Integer, Boolean>(0);
    protected Context context;

    public EmbeddedProvider(Context context) throws ClassNotFoundException{
        this.context = context;
    }

    protected EmbeddedProvider parseConfig(JSONObject json) throws JSONException {

        if (json.has(KEY_PROPERTIES)){
            JSONObject propJson = json.getJSONObject(KEY_PROPERTIES);
            for (Iterator<String> iterator = propJson.keys(); iterator.hasNext();) {
                String key = iterator.next();
                properties.put(key, propJson.getString(key));
            }
        }
        if (json.has(KEY_FILTERS)){
            if (json.has(KEY_EVENT_TYPES)){
                types = convertToHashMap(json.getJSONObject(KEY_EVENT_TYPES));
            }
            if (json.has(KEY_EVENT_NAMES)){
                names = convertToHashMap(json.getJSONObject(KEY_EVENT_NAMES));
            }
            if (json.has(KEY_EVENT_ATTRIBUTES)){
                attributes = convertToHashMap(json.getJSONObject(KEY_EVENT_ATTRIBUTES));
            }
        }
        return this;
    }

    private HashMap<Integer, Boolean> convertToHashMap(JSONObject json){
        HashMap<Integer, Boolean> map = new HashMap<Integer, Boolean>();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext();) {
            try {
                String key = iterator.next();
                map.put(Integer.parseInt(key), json.getBoolean(key));
            }catch (JSONException jse){
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Issue while parsing embedded kit configuration: " + jse.getMessage());
                }
            }
        }
        return map;
    }

    public boolean optedOut(){
        return Boolean.parseBoolean(properties.containsKey(HONOR_OPT_OUT) ? properties.get(HONOR_OPT_OUT) : "true")
                && !MParticle.getInstance().mConfigManager.getSendOoEvents();
    }

    static final EmbeddedProvider createInstance(JSONObject json, Context context) throws JSONException, ClassNotFoundException{
        int id = json.getInt(KEY_ID);
        switch (id){
            case MAT:
                return new EmbeddedMAT(context);
            default:
                return null;
        }

    }

    private static int hash(String input) {
        int hash = 0;

        if (input == null || input.length() == 0)
            return hash;

        char[] chars = input.toLowerCase().toCharArray();

        for (char c : chars) {
            hash = ((hash << 5) - hash) + c;
        }

        return hash;
    }

    protected boolean shouldSend(MParticle.EventType type, String name){
        int typeHash = hash(type.toString());
        if (types.containsKey(typeHash) && !types.get(typeHash)){
            return false;
        }
        int typeNameHash = hash(type.toString() + name);
        if (names.containsKey(typeNameHash) && !names.get(typeNameHash)){
            return false;
        }

        return true;
    }

    protected JSONObject filterAttributes(MParticle.EventType type, String name, JSONObject eventAttributes){
        Iterator attIterator = eventAttributes.keys();
        String nameType = type + name;
        while (attIterator.hasNext()){
            String attributeKey = (String)attIterator.next();
            int hash = hash(nameType + attributeKey);
            if (attributes.containsKey(hash) && !attributes.get(hash)){
                attIterator.remove();
            }
        }
        return eventAttributes;
    }

    protected abstract EmbeddedProvider update();
    public abstract String getName();


}
