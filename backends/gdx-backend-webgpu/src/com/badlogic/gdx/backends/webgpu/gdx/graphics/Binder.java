package com.badlogic.gdx.backends.webgpu.gdx.graphics;

import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;

import java.util.HashMap;
import java.util.Map;

/** Manages bind groups and provides methods for binding by uniform name.
 *
 * todo add support for uniforms in UB
 * todo incorpotate into ShaderProg? Shader?
 */
public class Binder implements Disposable {
    private final BindingDictionary bindMap;
    private final Map<Integer, WebGPUBindGroupLayout> groupLayouts;
    private final Map<Integer, WebGPUBindGroup> groups;
    private final Map<Integer, BufferInfo> buffers;
    private WebGPUPipelineLayout pipelineLayout;

    public static class BufferInfo {
        WebGPUUniformBuffer buffer;
        int offset;
        long size;

        public BufferInfo(WebGPUUniformBuffer buffer, int offset, long size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
    }

    public Binder() {
        bindMap = new BindingDictionary();
        groupLayouts = new HashMap<>();
        groups = new HashMap<>();
        buffers = new HashMap<>();
    }

    public void defineGroup(int groupId, WebGPUBindGroupLayout layout){
        groupLayouts.put(groupId, layout);
    }

    /** Associates a name with a groupId + bindingId. */
    public void defineUniform(String name, int groupId, int bindingId){
        bindMap.defineUniform(name, groupId, bindingId);
    }

    /** Associates a name with a groupId + bindingId + offset.
     *  This is for a uniform in a uniform buffer.
     *  */
    public void defineUniform(String name, int groupId, int bindingId, int offset){
        bindMap.defineUniform(name, groupId, bindingId, offset);
    }

    public void setBuffer(String name, WebGPUBuffer buffer, int offset, long size ){
        BindingDictionary.BindingMap mapping = bindMap.findUniform(name);
        if(mapping == null) throw new RuntimeException("Uniform name "+name+" not defined.");
        WebGPUBindGroup bindGroup = getBindGroup(mapping.groupId);

        bindGroup.setBuffer( mapping.bindingId, buffer, offset, size);

        // keep hold of the buffer information, we may need it for uniforms.
    //    buffers.put(mapping.index, new BufferInfo(buffer, offset, size));
    }

    public void setUniformMatrix(String name, Matrix4 matrix){
        BindingDictionary.BindingMap mapping = bindMap.findUniform(name);
        if(mapping == null) throw new RuntimeException("Uniform name "+name+" not defined.");
        if(mapping.offset < 0) throw new RuntimeException("Uniform name "+name+" is not defined in a uniform buffer.");

//        BufferInfo bufferInfo = buffers.get();
//
//        WebGPUUniformBuffer buffer = bufferInfo.buffer;
//        buffer.set(bufferInfo.offset + mapping.offset, matrix);
    }

    public void setBuffer(int groupId, int bindingId, WebGPUBuffer buffer, int offset, long size ){
        WebGPUBindGroup bindGroup = getBindGroup(groupId);
        bindGroup.setBuffer(bindingId, buffer, offset, size);

        // keep hold of the buffer information, we may need it for uniforms.
        // which key to use?
        //buffers.put(mapping.index, new BufferInfo(buffer, offset, size));
    }

    public void setTexture(String name, WebGPUTextureView textureView ){
        BindingDictionary.BindingMap mapping = bindMap.findUniform(name);
        if(mapping == null) throw new RuntimeException("Uniform name "+name+" not defined.");
        WebGPUBindGroup bindGroup = getBindGroup(mapping.groupId);

        bindGroup.setTexture(mapping.bindingId, textureView);
    }



    public void setSampler(String name, Pointer sampler){
        BindingDictionary.BindingMap mapping = bindMap.findUniform(name);
        if(mapping == null) throw new RuntimeException("Uniform name "+name+" not defined.");
        WebGPUBindGroup bindGroup = getBindGroup(mapping.groupId);

        bindGroup.setSampler(mapping.bindingId, sampler);
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


    public WebGPUPipelineLayout getPipelineLayout(String label){
        // note: if label changes, this does not invalidate an existing pipeline layout
        // the method will return the cached layout with the original label.
        if(pipelineLayout == null){
            WebGPUBindGroupLayout[] layouts = new WebGPUBindGroupLayout[groupLayouts.size()];
            int i = 0;
            for(WebGPUBindGroupLayout layout : groupLayouts.values())
                layouts[i++] = layout;
            pipelineLayout = new WebGPUPipelineLayout(label, layouts);
        }
        return pipelineLayout;
    }

//    public void bindGroup(WebGPURenderPass renderPass, int groupId ){
//        WebGPUBindGroup bindGroup = groups.get(groupId);
//        renderPass.setBindGroup( 0, bindGroup.getHandle(), 0, JavaWebGPU.createNullPointer());
//    }

    @Override
    public void dispose() {
        for(WebGPUBindGroup bg : groups.values())
            bg.dispose();
        if(pipelineLayout != null)
            pipelineLayout.dispose();
    }
}
