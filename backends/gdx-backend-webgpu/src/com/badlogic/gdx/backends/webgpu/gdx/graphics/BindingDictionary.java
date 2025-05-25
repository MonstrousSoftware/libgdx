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
        int offset;     // used for uniforms in a uniform buffer
        int index;  // in order of definition

        public BindingMap(int groupId, int bindingId, int offset, int index) {
            this.groupId = groupId;
            this.bindingId = bindingId;
            this.offset  = offset;
            this.index = index;
        }
    }

    private final Map<String,BindingMap> map = new HashMap<>();
    private final Map<Integer, Integer> entriesPerGroup = new HashMap<>();

    public void defineUniform(String name, int groupId, int bindingId){
        defineUniform(name, groupId, bindingId, -1);
    }

    public void defineUniform(String name, int groupId, int bindingId, int offset){
        // find sequence number of bindingId within the specific group
        Integer index = entriesPerGroup.get(groupId);
        if(index == null)
            index = 0;
        entriesPerGroup.put(groupId, index+1);

        // store binding information under the uniform name
        BindingMap bm = new BindingMap(groupId, bindingId, offset, index);
        map.put(name, bm);
    }

    public BindingMap findUniform(String name){
        return map.get(name);
    }

}
