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
public class TreeObject {

    private int id;
    private String name;
    //keep this private, only base class messes with it directly
    private TreeObject parent;

    private Map<Integer,TreeObject> idToChild = new HashMap<Integer,TreeObject>();
    private Multimap<String,TreeObject> nameToChild = ArrayListMultimap.create();

    private static int ID_COUNTER = 0;

    /**
     * protected constructor, treeObject cannot be instantiated directly
     */
    public TreeObject()
    {
        super();
        id = ID_COUNTER++;
        name = "TreeObject";
    }

    public TreeObject getFirstChildByName(String name) {
        if(nameToChild.containsKey(name)) {
            return nameToChild.get(name).iterator().next();
        }
        return null;
    }
    /**
     *
     * @return unmodifyable collection of my children
     */
    public Collection<TreeObject> getChildren()
    {
        return idToChild.values();
    }
    private void addChild(TreeObject child)
    {
        idToChild.put(child.id,child);
        nameToChild.put(child.name,child);
    }
    private boolean removeChild(TreeObject child)
    {
        idToChild.remove(child.id);
        return nameToChild.remove(child.name, child);
    }
    private void destroy()
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

    public String getName()
    {
        return name;
    }
    private void changeChildName(TreeObject child, String nname)
    {
        if(nameToChild.remove(child.getName(), child))
        {
            child.name = nname;
            nameToChild.put(child.getName(), child);
        }
    }
    public void setName(String nname)
    {
        TreeObject parent = getParent();
        if(parent != null)
        {
            parent.changeChildName(this,nname);
        }
        else
        {
            name = nname;
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

        getmap.put("id",new GetParameter<TreeObject,Integer>() {
            @Override
            public Integer get(TreeObject object) {
                return object.getId();
            }
        });

        LuaUtil.initializeLuaAccess(TreeObject.class, getmap, putmap);
    }

}
