package com.badlogic.gdx.backends.webgpu.wrappers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUCommandBufferDescriptor;
import com.badlogic.gdx.backends.webgpu.webgpu.WebGPU_JNI;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;

public class WebGPUCommandBuffer implements Disposable {
    private final WebGPU_JNI webGPU;
    private final Pointer commandBuffer;

    public WebGPUCommandBuffer(WebGPUCommandEncoder encoder ) {
        WebGPUApplication app = (WebGPUApplication) Gdx.app;
        webGPU = app.getWebGPU();

        // finish the encoder to give use command buffer
        WGPUCommandBufferDescriptor bufferDescriptor = WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        commandBuffer = webGPU.wgpuCommandEncoderFinish(encoder.getHandle(), bufferDescriptor);
    }

    public Pointer getHandle(){
        return commandBuffer;
    }

    @Override
    public void dispose() {
        webGPU.wgpuCommandBufferRelease(commandBuffer);
    }
}
