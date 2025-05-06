package com.badlogic.gdx.backends.webgpu.wrappers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUBindGroupDescriptor;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUBindGroupEntry;
import com.badlogic.gdx.backends.webgpu.webgpu.WebGPU_JNI;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;

import java.util.ArrayList;

/**
 * Encapsulated bind group.  Use begin(), addXXX(), end() to define a layout.
 */
public class WebGPUBindGroup implements Disposable {
    private final WebGPUApplication app;
    private final WebGPU_JNI webGPU;
    private Pointer handle = null;

    private final WebGPUBindGroupLayout layout;
    private final ArrayList<WGPUBindGroupEntry> entries;

    public WebGPUBindGroup(WebGPUBindGroupLayout layout) {
        app = (WebGPUApplication) Gdx.app;
        webGPU = app.getWebGPU();

        this.layout = layout;
        entries = new ArrayList<>();
    }

    public void begin() {
        entries.clear();
        handle = null;
    }

    /**
     * Add binding for a buffer.
     *
     * @param bindingId         integer as in the shader, 0, 1, 2, ...
     */
    public void addBuffer(int bindingId, WebGPUBuffer buffer, int offset, long size) {
        WGPUBindGroupEntry entry = WGPUBindGroupEntry.createDirect();
        entry.setBinding(bindingId);
        entry.setBuffer(buffer.getHandle());
        entry.setOffset(offset);
        entry.setSize(size);
        entries.add(entry);
    }

    // shorthand to add whole buffer with no offset
    public void addBuffer(int bindingId, WebGPUBuffer buffer) {
        addBuffer(bindingId, buffer, 0, buffer.getSize());
    }

    public void addTexture(int bindingId, WebGPUTextureView textureView) {
        WGPUBindGroupEntry entry = WGPUBindGroupEntry.createDirect();
        entry.setBinding(bindingId);
        entry.setTextureView(textureView.getHandle());
        entries.add(entry);
    }

    public void addSampler(int bindingId, Pointer sampler) {
        WGPUBindGroupEntry entry = WGPUBindGroupEntry.createDirect();
        entry.setBinding(bindingId);
        entry.setSampler(sampler);
        entries.add(entry);
    }


    // fallback option
    public void addBindGroupEntry(int bindingId, WGPUBindGroupEntry entry) {
        entry.setBinding(bindingId);
        entries.add(entry);
    }

    // todo other types

    public void end() {
        // Create a bind group
        WGPUBindGroupDescriptor bindGroupDescriptor = WGPUBindGroupDescriptor.createDirect();
        bindGroupDescriptor.setNextInChain()
                .setLayout(layout.getHandle())
                .setEntryCount(entries.size());

        WGPUBindGroupEntry[] entryArray = new WGPUBindGroupEntry[entries.size()];
        for (int i = 0; i < entries.size(); i++)
            entryArray[i] = entries.get(i);
        bindGroupDescriptor.setEntries(entryArray);

        handle = webGPU.wgpuDeviceCreateBindGroup(app.getDevice().getHandle(), bindGroupDescriptor);
    }

    public Pointer getHandle() {
        if (handle == null)
            throw new RuntimeException("BindGroup not defined, did you forget to call end()?");
        return handle;
    }

    @Override
    public void dispose() {
        webGPU.wgpuBindGroupRelease(handle);
    }

}



