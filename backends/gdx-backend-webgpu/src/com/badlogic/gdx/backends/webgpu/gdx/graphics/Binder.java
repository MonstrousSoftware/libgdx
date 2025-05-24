package com.badlogic.gdx.backends.webgpu.gdx.graphics;

import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUBindGroup;

import java.util.HashMap;
import java.util.Map;

/** Manages bind groups
 *
 */
public class Binder {
    private final BindingDictionary bindMap;
    //private final Map<Integer, Integer> groupIds;

    public Binder() {
        bindMap = new BindingDictionary();
        //groupIds = new HashMap<>();
    }

//    public void defineGroup(int groupId){
//        groupIds.put(groupId, groupId);
//    }

    /** Associates a name with a groupId + bindingId */
    public void defineUniform(String name, int groupId, int bindingId){
        //WebGPUBindGroup bg = groups.get(groupId);

        bindMap.defineUniform(name, groupId, bindingId);
    }
}
