package com.badlogic.gdx.backends.webgpu.gdx;

import com.badlogic.gdx.backends.webgpu.webgpu.WGPUBackendType;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUSupportedLimits;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUTextureFormat;
import com.badlogic.gdx.backends.webgpu.webgpu.WebGPU_JNI;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
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
    WebGPUTextureView getDepthTextureView ();
    WGPUTextureFormat getDepthTextureFormat ();
    boolean getGPUtimingEnabled();
    WGPUBackendType getRequestedBackendType();
    int getSamples();
    WGPUSupportedLimits getSupportedLimits();
    void setSupportedLimits(WGPUSupportedLimits supportedLimits);
    WebGPUTexture getMultiSamplingTexture();

}
