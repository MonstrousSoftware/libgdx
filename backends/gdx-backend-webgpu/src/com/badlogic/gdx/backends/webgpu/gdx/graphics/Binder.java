package com.badlogic.gdx.backends.webgpu.gdx.graphics;

import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;

import java.util.HashMap;
import java.util.Map;

/** Manages bind groups and provides methods for binding by uniform name.
 *
 */
public class Binder implements Disposable {
    private final BindingDictionary bindMap;
    private final Map<Integer, WebGPUBindGroupLayout> groupLayouts;
    private final Map<Integer, WebGPUBindGroup> groups;

    public Binder() {
        bindMap = new BindingDictionary();
        groupLayouts = new HashMap<>();
        groups = new HashMap<>();
    }

    public void defineGroup(int groupId, WebGPUBindGroupLayout layout){
        groupLayouts.put(groupId, layout);
    }

    /** Associates a name with a groupId + bindingId. */
    public void defineUniform(String name, int groupId, int bindingId){
        bindMap.defineUniform(name, groupId, bindingId);
    }

    public void setBuffer(String name, WebGPUBuffer buffer, int offset, long size ){
        BindingDictionary.BindingMap mapping = bindMap.findUniform(name);
        if(mapping == null) throw new RuntimeException("Uniform name "+name+" not defined.");
        WebGPUBindGroup bindGroup = getBindGroup(mapping.groupId);

        bindGroup.setBuffer(mapping.index, mapping.bindingId, buffer, offset, size);
    }

    public void setTexture(String name, WebGPUTextureView textureView ){
        BindingDictionary.BindingMap mapping = bindMap.findUniform(name);
        if(mapping == null) throw new RuntimeException("Uniform name "+name+" not defined.");
        WebGPUBindGroup bindGroup = getBindGroup(mapping.groupId);

        bindGroup.setTexture(mapping.index, mapping.bindingId, textureView);
    }

    public void setSampler(String name, Pointer sampler){
        BindingDictionary.BindingMap mapping = bindMap.findUniform(name);
        if(mapping == null) throw new RuntimeException("Uniform name "+name+" not defined.");
        WebGPUBindGroup bindGroup = getBindGroup(mapping.groupId);

        bindGroup.setSampler(mapping.index, mapping.bindingId, sampler);
    }

    /** find or create bind group */
    public WebGPUBindGroup getBindGroup(int groupId){
        WebGPUBindGroup bindGroup = groups.get(groupId);
        if(bindGroup == null){
            WebGPUBindGroupLayout layout = groupLayouts.get(groupId);
            if(layout == null) throw new RuntimeException("Group "+groupId+" not defined. Use defineGroup()");
            bindGroup = new WebGPUBindGroup(layout);
            groups.put(groupId, bindGroup);
        }
        return bindGroup;
    }

//    public void bindGroup(WebGPURenderPass renderPass, int groupId ){
//        WebGPUBindGroup bindGroup = groups.get(groupId);
//        renderPass.setBindGroup( 0, bindGroup.getHandle(), 0, JavaWebGPU.createNullPointer());
//    }

    @Override
    public void dispose() {
        for(WebGPUBindGroup bg : groups.values())
            bg.dispose();
    }
}
