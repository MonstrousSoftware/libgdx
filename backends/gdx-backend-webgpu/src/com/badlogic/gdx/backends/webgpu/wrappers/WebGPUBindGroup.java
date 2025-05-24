package com.badlogic.gdx.backends.webgpu.wrappers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUGraphicsBase;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUBindGroupDescriptor;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUBindGroupEntry;
import com.badlogic.gdx.backends.webgpu.webgpu.WebGPU_JNI;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;

/**
 * Encapsulated bind group.  Used to bind values to a shader.
 *
 * Example:
 *      WebGPUBindGroup bg = new WebGPUBindGroup(bindGroupLayout);
 *      bg.begin();
 *      bg.addBuffer(buffer);
 *      bg.addTexture(textureView);
 *      bg.addSampler(sampler);
 *      bg.end();
 *
 *      Note the sequence and types must correspond to what is defined in the
 *      BindGroupLayout.
 *
 *      Alternatively:
 *      bg.setBuffer(0, buffer);
 *      bg.setTexture(1, textureView);
 *      bg.setSampler(2, sampler);
 *      bg.create();
 *
 *      This allows also to only update specific bindings.
 *      create() is implied by getHandle().
 */
public class WebGPUBindGroup implements Disposable {
    private final WebGPU_JNI webGPU;
    private Pointer handle = null;
    private final WebGPUGraphicsBase gfx;

    private final WGPUBindGroupDescriptor bindGroupDescriptor;
    private final WGPUBindGroupEntry[] entryArray;
    private final int numEntries;
    private int entryIndex;
    private boolean dirty;  // has an entry changed?


    public WebGPUBindGroup(WebGPUBindGroupLayout layout) {
        gfx = (WebGPUGraphicsBase) Gdx.graphics;
        webGPU = gfx.getWebGPU();

        numEntries = layout.getEntryCount();

        // Create a bind group descriptor and an array of BindGroupEntry
        //
        bindGroupDescriptor = WGPUBindGroupDescriptor.createDirect();
        bindGroupDescriptor.setNextInChain()
                .setLayout(layout.getHandle())
                .setEntryCount(numEntries);

        entryArray = new WGPUBindGroupEntry[numEntries];
        for (int i = 0; i < numEntries; i++)
            entryArray[i] = WGPUBindGroupEntry.createDirect();
    }

    public void begin() {
        entryIndex = 0;
        handle = null;
    }

    /** bind a (subrange of a) buffer. */
    public void addBuffer(int bindingId, WebGPUBuffer buffer, int offset, long size) {
        setBuffer(entryIndex++, bindingId, buffer, offset, size);
    }

    /** bind a buffer */
    public void addBuffer(int bindingId, WebGPUBuffer buffer) {
        setBuffer(entryIndex++, bindingId, buffer);
    }

    /** bind a texture view */
    public void addTexture(int bindingId, WebGPUTextureView textureView) {
        setTexture(entryIndex++, bindingId, textureView);
    }

    /** bind a sampler */
    public void addSampler(int bindingId, Pointer sampler) {
        setSampler(entryIndex++, bindingId, sampler);
    }

    /** creates the bind group */
    public Pointer end() {
        return create();
    }

    /** bind a buffer. */
    public void setBuffer(int index, int bindingId, WebGPUBuffer buffer) {
        setBuffer(index, bindingId, buffer, 0, buffer.getSize());
    }

    /** bind a (subrange of a) buffer. */
    public void setBuffer(int index, int bindingId, WebGPUBuffer buffer, int offset, long size) {
        if(index >= numEntries) throw new ArrayIndexOutOfBoundsException("Too many entries. See BindGroupLayout");
        WGPUBindGroupEntry entry = entryArray[index];
        entry.setBinding(bindingId);
        entry.setBuffer(buffer.getHandle());
        entry.setOffset(offset);
        entry.setSize(size);
        dirty = true;
    }

    public void setTexture(int index, int bindingId, WebGPUTextureView textureView) {
        if(index >= numEntries) throw new ArrayIndexOutOfBoundsException("Too many entries. See BindGroupLayout");
        WGPUBindGroupEntry entry = entryArray[index];
        entry.setBinding(bindingId);
        entry.setTextureView(textureView.getHandle());
        dirty = true;
    }

    /** bind a sampler */
    public void setSampler(int index, int bindingId, Pointer sampler) {
        if(index >= numEntries) throw new ArrayIndexOutOfBoundsException("Too many entries. See BindGroupLayout");
        WGPUBindGroupEntry entry = entryArray[index];
        entry.setBinding(bindingId);
        entry.setSampler(sampler);
        dirty = true;
    }


    /** creates the bind group */
    public Pointer create() {
        if(dirty) {
            if(handle != null) {
                //System.out.println("Releasing bind group");
                webGPU.wgpuBindGroupRelease(handle);
            }
            //System.out.println("Creating bind group");
            bindGroupDescriptor.setEntries(entryArray);
            handle = webGPU.wgpuDeviceCreateBindGroup(gfx.getDevice().getHandle(), bindGroupDescriptor);
            dirty = false;
        }
        return handle;
    }

    public Pointer getHandle() {
        if(dirty)
            create();
        return handle;
    }

    @Override
    public void dispose() {
        if(handle != null) {
            //System.out.println("Releasing bind group");
            webGPU.wgpuBindGroupRelease(handle);
            handle = null;
        }
    }

}



