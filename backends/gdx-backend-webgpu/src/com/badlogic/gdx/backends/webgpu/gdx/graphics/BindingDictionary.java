package com.badlogic.gdx.backends.webgpu.gdx.graphics;

import java.util.HashMap;
import java.util.Map;

/** Dictionary to map names to binding identifiers
 *
 */
public class BindingDictionary {

    public static class BindingMap {
        int groupId;
        int bindingId;

        public BindingMap(int groupId, int bindingId) {
            this.groupId = groupId;
            this.bindingId = bindingId;
        }
    }

    private final Map<String,BindingMap> map = new HashMap<>();

    public void defineUniform(String name, int groupId, int bindingId){
        BindingMap bm = new BindingMap(groupId, bindingId);
        map.put(name, bm);
    }

    public BindingMap findUniform(String name){
        return map.get(name);
    }

}
