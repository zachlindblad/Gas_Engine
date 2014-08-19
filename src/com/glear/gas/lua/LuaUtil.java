package com.glear.gas.lua;

import com.glear.gas.tree.TreeObject;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.HashMap;

/**
 * Created by biscuit on 8/17/2014.
 */
public class LuaUtil {
    public static final HashMap<String,Class> KEY_TO_CLASS = new HashMap<String, Class>();
    static
    {
        KEY_TO_CLASS.put("TreeObject", TreeObject.class);
    }
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
