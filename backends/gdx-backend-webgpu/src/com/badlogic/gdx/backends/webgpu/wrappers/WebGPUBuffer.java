/*******************************************************************************
 * Copyright 2025 Monstrous Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.backends.webgpu.wrappers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.lwjgl3.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUBufferDescriptor;
import com.badlogic.gdx.backends.webgpu.webgpu.WebGPU_JNI;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;


/**
 * Encapsulation of WebGPU Buffer
 *
 * label: for debug/error messages, no functional value
 * bufferSize: in bytes, to be aligned if necessary
 * usage: one or more flags in combination, e.g. WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform
 */
public class WebGPUBuffer implements Disposable {
    protected final WebGPUApplication app;
    protected final WebGPU_JNI webGPU;
    private Pointer handle;
    private final long bufferSize;

    public WebGPUBuffer(String label, long usage, long bufferSize){
        app = (WebGPUApplication) Gdx.app;
        webGPU = app.getWebGPU();

        this.bufferSize = bufferSize;

        // Create uniform buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel( label );
        bufferDesc.setUsage( usage );
        bufferDesc.setSize( bufferSize );
        bufferDesc.setMappedAtCreation(0L);
        this.handle = webGPU.wgpuDeviceCreateBuffer(app.getDevice().getHandle(), bufferDesc);
    }

    public Pointer getHandle(){
        return handle;
    }

    public long getSize(){
        return bufferSize;
    }

    public void write(int destOffset, Pointer data, int dataSize){
        if(destOffset + dataSize > bufferSize) throw new RuntimeException("Overflow in Buffer.write().");
        app.getQueue().writeBuffer(this, destOffset, data, dataSize);
    }

    @Override
    public void dispose() {
        webGPU.wgpuBufferDestroy(handle);
        webGPU.wgpuBufferRelease(handle);
        handle = null;
    }
}
