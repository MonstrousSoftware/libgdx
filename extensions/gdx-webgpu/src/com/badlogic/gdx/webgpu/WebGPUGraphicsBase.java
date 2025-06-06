package com.badlogic.gdx.webgpu;

import com.badlogic.gdx.webgpu.webgpu.WGPUBackendType;
import com.badlogic.gdx.webgpu.webgpu.WGPUSupportedLimits;
import com.badlogic.gdx.webgpu.webgpu.WGPUTextureFormat;
import com.badlogic.gdx.webgpu.webgpu.WebGPU_JNI;
import com.badlogic.gdx.webgpu.wrappers.*;
import com.badlogic.gdx.webgpu.wrappers.WebGPUCommandEncoder;
import com.badlogic.gdx.webgpu.wrappers.WebGPUDevice;
import com.badlogic.gdx.webgpu.wrappers.WebGPUQueue;
import com.badlogic.gdx.webgpu.wrappers.WebGPUTexture;
import jnr.ffi.Pointer;

public interface WebGPUGraphicsBase {
    WebGPU_JNI getWebGPU ();

    WebGPUDevice getDevice ();
    WebGPUQueue getQueue ();
    Pointer getSurface ();
    WGPUTextureFormat getSurfaceFormat ();
    Pointer getTargetView ();
    WebGPUCommandEncoder getCommandEncoder ();
    WebGPUTexture getDepthTexture ();
//    WebGPUTextureView getDepthTextureView ();
//    WGPUTextureFormat getDepthTextureFormat ();
    boolean getGPUtimingEnabled();
    WGPUBackendType getRequestedBackendType();
    int getSamples();
    WGPUSupportedLimits getSupportedLimits();
    void setSupportedLimits(WGPUSupportedLimits supportedLimits);
    WebGPUTexture getMultiSamplingTexture();

}
