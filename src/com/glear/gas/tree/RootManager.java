package com.glear.gas.tree;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * holds all created root objects
 */
public class RootManager {
    private static final RootManager rootManager = new RootManager();
    public static RootManager getInstance() {
        return rootManager;
    }

    private Multimap<String,RootObject> roots = ArrayListMultimap.create();
    //NAME OF ROOT CANNOT CHANGE AFTER ADDING!!
    public void addRoot(RootObject rootObject) {
        roots.put(rootObject.getName(),rootObject);
    }

    public RootObject getRoot(String name) {
        if(roots.containsKey(name)) {
            return roots.get(name).iterator().next();
        }
        return null;
    }
}
