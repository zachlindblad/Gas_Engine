package com.glear.gas.lua;

import com.glear.gas.tree.TreeObject;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by biscuit on 8/17/2014.
 */
public class LuaUtil {

    //holds all our fast access lua table variables
    private final static Map<Class,Map<String,GetParameter>> GET_FUNCS = new HashMap<Class, Map<String,GetParameter> >();
    private final static Map<Class,Map<String,SetParameter> > ASSIGN_FUNCS = new HashMap<Class, Map<String,SetParameter> >();
    //simple name to class mapping
    public static final HashMap<String,Class> KEY_TO_CLASS = new HashMap<String, Class>();

    private static TwoArgFunction TREE_DIV = new TwoArgFunction() {
        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            Object obj = arg1.touserdata();
            return CoerceJavaToLua.coerce(((TreeObject)obj).getFirstChildByName(arg2.tojstring()));
        }
    };

    /**
     * this should ONLY be called once per class in its static initialization block
     * @param clazz
     * @param myGetters
     * @param mySetters
     */
    public static void initializeLuaAccess(final Class clazz, final Map<String, GetParameter> myGetters, final Map<String, SetParameter> mySetters)
    {
        if(GET_FUNCS.containsKey(clazz)) {
            return;
        }

        KEY_TO_CLASS.put(clazz.getSimpleName(),clazz);

        LuaValue luaobj = null;
        try {
            luaobj = CoerceJavaToLua.coerce(clazz.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        //This codes HINGES on the fact that java static initialization happens in order of class heiarchy
        Map<String, GetParameter> parentGetters = GET_FUNCS.get(clazz.getSuperclass());
        if(parentGetters != null) {
            myGetters.putAll(parentGetters);
        }

        Map<String, SetParameter> parentSetters = ASSIGN_FUNCS.get(clazz.getSuperclass());
        if(parentSetters != null) {
            mySetters.putAll(parentSetters);
        }
        GET_FUNCS.put(clazz,myGetters);
        ASSIGN_FUNCS.put(clazz,mySetters);

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
        classTable.set("__div",TREE_DIV);

    }


    /*
     * Global create function initialization
     */
    public static void initializeGlobalFunctions(LuaValue _G)
    {
        _G.set("create",new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                Class clazz = KEY_TO_CLASS.get(arg.tojstring());
                if(clazz != null)
                {
                    try {
                        return CoerceJavaToLua.coerce(clazz.newInstance());
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                        return null;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                return null;
            }
        });
    }
}
