package com.glear.gas.tree;

import com.glear.gas.lua.GetParameter;
import com.glear.gas.lua.LuaUtil;
import com.glear.gas.lua.SetParameter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

/**
 * Created by biscuit on 8/20/2014.
 */
public class RootObject {

    private Map<Integer,TreeObject> idToChild = new HashMap<Integer,TreeObject>();
    private Multimap<String,TreeObject> nameToChild = ArrayListMultimap.create();

    private int id;
    private String name;

    private static int ID_COUNTER = 0;

    public RootObject()
    {
        super();
        id = ID_COUNTER++;
        name = "RootObject";
        registerTrackers();
    }

    protected void registerTrackers() {
        RootManager.getInstance().addRoot(this);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String nname)
    {
        name = nname;
    }

    /**
     *
     * @return unmodifyable collection of my children
     */
    public Collection<TreeObject> getChildren()
    {
        return idToChild.values();
    }
    protected final void addChild(TreeObject child)
    {
        idToChild.put(child.getId(),child);
        nameToChild.put(child.getName(),child);
    }
    protected final boolean removeChild(TreeObject child)
    {
        idToChild.remove(child.getId());
        return nameToChild.remove(child.getName(), child);
    }

    public TreeObject getFirstChildByName(String name) {
        if(nameToChild.containsKey(name)) {
            return nameToChild.get(name).iterator().next();
        }
        return null;
    }

    protected final void changeChildName(TreeObject nchild, String nname)
    {
        RootObject child = (RootObject)nchild;
        if(nameToChild.remove(nchild.getName(), nchild))
        {
            child.name = nname;
            nameToChild.put(nchild.getName(), nchild);
        }
    }

    public int getId()
    {
        return id;
    }

    public void printTree() {
        System.out.println("<"+name+":"+this.getClass().getSimpleName()+">");
        for(TreeObject obj : getChildren())
        {
            obj.printTree();
        }
        System.out.println("</end "+name+">");
    }

    protected final void destroy()
    {
        Map<Integer,TreeObject> savedIdToChild = idToChild;
        idToChild = Collections.emptyMap();
        nameToChild.clear();
        Iterator<TreeObject> it= savedIdToChild.values().iterator();
        while(it.hasNext())
        {
            TreeObject obj = it.next();
            it.remove();
            obj.setParent(null);
        }
        idToChild=savedIdToChild;
    }

    //------------------------------------------------------
    //Initialize our static lua access maps
    //------------------------------------------------------
    static
    {
        Map<String,GetParameter> getmap= new HashMap<String,GetParameter>();
        Map<String,SetParameter> putmap= new HashMap<String,SetParameter>();
        getmap.put("name",new GetParameter<TreeObject,String>() {
            @Override
            public String get(TreeObject object) {
                return object.getName();
            }
        });
        putmap.put("name",new SetParameter<TreeObject,String>() {
            @Override
            public void set(TreeObject object, String value) {
                object.setName(value);
            }
        });

        getmap.put("id",new GetParameter<TreeObject,Integer>() {
            @Override
            public Integer get(TreeObject object) {
                return object.getId();
            }
        });

        LuaUtil.initializeLuaAccess(RootObject.class, getmap, putmap);
    }

}
