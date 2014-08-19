package com.glear.gas.tree;

import com.glear.gas.lua.GetParameter;
import com.glear.gas.lua.SetParameter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;
import org.luaj.vm2.lib.jse.LuajavaLib;
import sun.reflect.generics.tree.Tree;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by biscuit on 5/25/2014.
 */
public class TreeObject {


    //holds all our fast access lua table variables
    private final static Map<Class,Map<String,GetParameter> > GET_FUNCS = new HashMap<Class, Map<String,GetParameter> >();
    private final static Map<Class,Map<String,SetParameter> > ASSIGN_FUNCS = new HashMap<Class, Map<String,SetParameter> >();



    private final LuaTable internalTable = new LuaTable();
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


    /**
     * this should ONLY be called once per class in its static initialization block
     * @param clazz
     */
    protected static void initializeLuaAccess(final Class clazz)
    {

        LuaValue luaobj = null;
        try {
            luaobj = CoerceJavaToLua.coerce(clazz.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        final Map<String, GetParameter> myGetters = GET_FUNCS.get(clazz);
        final Map<String, SetParameter> mySetters = ASSIGN_FUNCS.get(clazz);
        LuaTable classTable = (LuaTable)luaobj.getmetatable();
        final TwoArgFunction oldIndex = (TwoArgFunction) classTable.get("__index");
        classTable.set("__index",new TwoArgFunction() {
            private Map methods;
            @Override
            public LuaValue call(LuaValue arg1, LuaValue key) {
                TreeObject obj = (TreeObject)arg1.touserdata();
                GetParameter func = myGetters.get(key.tojstring());
                if(func!= null)
                {
                    return CoerceJavaToLua.coerce(func.get(obj));
                }
                //WARNING: this old access will be slow, put as much in the fast accessing static map as possible
                return oldIndex.call(arg1,key);
            }
        });
        final ThreeArgFunction oldNewIndex = (ThreeArgFunction) classTable.get("__newindex");
        classTable.set("__newindex",new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                TreeObject obj = (TreeObject)arg1.touserdata();
                SetParameter func = mySetters.get(arg2.tojstring());
                if(func!= null)
                {
                    switch (arg3.type()) {
                        case TBOOLEAN:
                            func.set(obj, arg3.toboolean());
                            break;
                        case TNUMBER:
                            func.set(obj, arg3.tofloat());
                            break;
                        case TSTRING:
                            func.set(obj, arg3.tojstring());
                            break;
                        case TTABLE:
                            func.set(obj,(LuaTable)arg3);
                            break;
                        case TFUNCTION:
                            func.set(obj,(LuaFunction)arg3);
                            break;
                        default:
                            func.set(obj,arg3.touserdata());
                            break;
                    }
                }
                else
                {
                    return oldNewIndex.call(arg1,arg2,arg3);
                }
                return NONE;
            }
        });
        classTable.set("__div",new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                Object obj = arg1.touserdata();
                return CoerceJavaToLua.coerce(((TreeObject)obj).getFirstChildByName(arg2.tojstring()));
            }
        });
        classTable.set("__add",new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                Object obj1 = arg1.touserdata();
                Object obj2 = arg2.touserdata();
                if(obj2 != null && obj2 instanceof TreeObject) {
                    ((TreeObject)obj1).addChild((TreeObject)obj2);
                    return arg1;
                }
                return null;
            }
        });

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

        GET_FUNCS.put(TreeObject.class,getmap);
        ASSIGN_FUNCS.put(TreeObject.class,putmap);

        initializeLuaAccess(TreeObject.class);
    }

}
