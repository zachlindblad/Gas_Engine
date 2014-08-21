package com.glear.gas.tree;

import com.glear.gas.lua.GetParameter;
import com.glear.gas.lua.LuaUtil;
import com.glear.gas.lua.SetParameter;
import com.google.common.collect.ArrayListMultimap;

import com.google.common.collect.Multimap;

import java.util.*;

/**
 * Created by biscuit on 5/25/2014.
 */
public class TreeObject extends RootObject {


    //keep this private, only base class messes with it directly
    private TreeObject parent;

    /**
     * protected constructor, treeObject cannot be instantiated directly
     */
    public TreeObject()
    {
        super();
        setName("TreeObject");
    }
    @Override
    protected final void registerTrackers() {

        //we dont want any subclasses registering with rootmanager
    }

    @Override
    public void setName(String nname)
    {
        TreeObject parent = getParent();
        if(parent != null)
        {
            parent.changeChildName(this,nname);
        }
        else
        {
            super.setName(nname);
        }
    }

    public TreeObject getParent()
    {
        return parent;
    }

    public void setParent(TreeObject nparent)
    {
        if(parent != null)
        {
            parent.removeChild(this);
        }
        if(nparent == null)
        {
            //setting my parent to null is a destroy signal
            destroy();
        } else {
            nparent.addChild(this);
        }
        parent = nparent;
    }

    //------------------------------------------------------
    //Initialize our static lua access maps
    //------------------------------------------------------
    static
    {
        Map<String,GetParameter> getmap= new HashMap<String,GetParameter>();
        Map<String,SetParameter> putmap= new HashMap<String,SetParameter>();

        getmap.put("parent",new GetParameter<TreeObject,TreeObject>() {
            @Override
            public TreeObject get(TreeObject object) {
                return object.getParent();
            }
        });
        putmap.put("parent",new SetParameter<TreeObject,TreeObject>() {
            @Override
            public void set(TreeObject object, TreeObject value) {
                object.setParent(value);
            }
        });

        LuaUtil.initializeLuaAccess(TreeObject.class, getmap, putmap);
    }

}
