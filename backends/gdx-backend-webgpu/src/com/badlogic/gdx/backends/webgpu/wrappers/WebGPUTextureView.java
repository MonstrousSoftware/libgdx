package com.badlogic.gdx.backends.webgpu.wrappers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;

public class WebGPUTextureView implements Disposable {
    private final WebGPU_JNI webGPU;
    private final Pointer textureView;
    public WGPUTextureAspect aspect;
    public WGPUTextureViewDimension dimension;
    public WGPUTextureFormat format;
    public int baseMipLevel;
    public int mipLevelCount;
    public int baseArrayLayer;
    public int arrayLayerCount;


    public WebGPUTextureView(WebGPUTexture texture) {
        this(texture, 1, 1);
    }

    public WebGPUTextureView(WebGPUTexture texture, int mipLevelCount, int arrayLayerCount) {
        this(texture, WGPUTextureAspect.All, WGPUTextureViewDimension._2D, WGPUTextureFormat.RGBA8Unorm, 0, mipLevelCount, 0, arrayLayerCount);
    }

    public WebGPUTextureView(WebGPUTexture texture, WGPUTextureAspect aspect, WGPUTextureViewDimension dimension, WGPUTextureFormat format,
                             int baseMipLevel, int mipLevelCount, int baseArrayLayer, int arrayLayerCount) {
        WebGPUApplication app = (WebGPUApplication) Gdx.app;
        webGPU = app.getWebGPU();

        this.aspect = aspect;
        this.dimension = dimension;
        this.format = format;
        this.baseMipLevel = baseMipLevel;
        this.mipLevelCount = mipLevelCount;
        this.baseArrayLayer = baseArrayLayer;
        this.arrayLayerCount = arrayLayerCount;

        // Create the view of the  texture manipulated by the rasterizer
        WGPUTextureViewDescriptor textureViewDesc = WGPUTextureViewDescriptor.createDirect();   // todo reuse
        textureViewDesc.setAspect(WGPUTextureAspect.All);
        textureViewDesc.setBaseArrayLayer(baseArrayLayer);
        textureViewDesc.setArrayLayerCount(arrayLayerCount);
        textureViewDesc.setBaseMipLevel(baseMipLevel);
        textureViewDesc.setMipLevelCount(mipLevelCount);
        textureViewDesc.setDimension(dimension);
        textureViewDesc.setFormat(texture.getFormat());
        textureView = webGPU.wgpuTextureCreateView(texture.getHandle(), textureViewDesc);
    }

    public Pointer getHandle(){
        return textureView;
    }

    @Override
    public void dispose() {
        webGPU.wgpuTextureViewRelease(textureView);
    }


}
