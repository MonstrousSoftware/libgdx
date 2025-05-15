package com.badlogic.gdx.backends.webgpu.wrappers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUGraphicsBase;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;

public class WebGPUAdapter implements Disposable {
    private final WebGPU_JNI webGPU;
    private Pointer adapter;

    public WebGPUAdapter(Pointer instance, Pointer surface) {
        WebGPUGraphicsBase gfx = (WebGPUGraphicsBase)Gdx.graphics;
        webGPU = gfx.getWebGPU();

        WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
        options.setNextInChain();
        options.setCompatibleSurface(surface);
        options.setBackendType(gfx.getRequestedBackendType());
        options.setPowerPreference(WGPUPowerPreference.HighPerformance);

        if(gfx.getRequestedBackendType() == WGPUBackendType.Null)
            throw new IllegalStateException("Request Adapter: Back end 'Null' only valid if config.noWindow is true");

        // Get Adapter
        adapter = getAdapterSync(instance, options);
        if(adapter == null){
            System.out.println("Configured adapter back end ("+ gfx.getRequestedBackendType()+") not available, requesting fallback");
            options.setBackendType(WGPUBackendType.Undefined);
            options.setPowerPreference(WGPUPowerPreference.HighPerformance);
            adapter = getAdapterSync(instance, options);
        }


        gfx.setSupportedLimits(WGPUSupportedLimits.createDirect());
        WGPUSupportedLimits supportedLimits = gfx.getSupportedLimits();
        gfx.getWebGPU().wgpuAdapterGetLimits(adapter, supportedLimits);
//        System.out.println("adapter maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());
//        System.out.println("adapter maxBindGroups " + supportedLimits.getLimits().getMaxBindGroups());
//
//        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
//        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
//        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
//        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());


        WGPUAdapterProperties adapterProperties = WGPUAdapterProperties.createDirect();
        adapterProperties.setNextInChain();

        webGPU.wgpuAdapterGetProperties(adapter, adapterProperties);
        System.out.println("VendorID: " + adapterProperties.getVendorID());
        System.out.println("Vendor name: " + adapterProperties.getVendorName());
        System.out.println("Device ID: " + adapterProperties.getDeviceID());
        System.out.println("Back end: " + adapterProperties.getBackendType());
        System.out.println("Description: " + adapterProperties.getDriverDescription());
    }

    public Pointer getHandle(){
        return adapter;
    }

    @Override
    public void dispose() {
        webGPU.wgpuAdapterRelease(adapter);       // we can release our adapter as soon as we have a device
    }

    private Pointer getAdapterSync(Pointer instance, WGPURequestAdapterOptions options){

        Pointer userBuf = JavaWebGPU.createLongArrayPointer(new long[1]);
        userBuf.putPointer(0, null);

        WGPURequestAdapterCallback callback = (WGPURequestAdapterStatus status, Pointer adapter, String message, Pointer userdata) -> {
            if(status == WGPURequestAdapterStatus.Success)
                userdata.putPointer(0, adapter);
            else
                System.out.println("Could not get adapter: "+message);
        };
        webGPU.wgpuInstanceRequestAdapter(instance, options, callback, userBuf);
        // on native implementations, we don't have to wait for asynchronous operation. It returns result immediately.
        return  userBuf.getPointer(0);
    }
}
