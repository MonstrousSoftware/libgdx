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
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUPixmapInfo;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Disposable;
import jnr.ffi.Pointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;


public class WebGPUTexture implements Disposable {
    private WebGPUApplication app = (WebGPUApplication) Gdx.app;
    private final WebGPU_JNI webGPU =  app.getWebGPU();
    protected int width;
    protected int height;
    protected int mipLevelCount;
    private Pointer image;
    private Pointer texture;
    private WebGPUTextureView textureView;
    private Pointer sampler;
    protected WGPUTextureFormat format;
    protected String label;
    private int numSamples;

    public WebGPUTexture(){
    }

    public WebGPUTexture(int width, int height){
        this(width, height, true, false, WGPUTextureFormat.RGBA8Unorm, 1);
    }

    public WebGPUTexture(int width, int height, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format, int numSamples ) {

        this.width = width;
        this.height = height;
        mipLevelCount = mipMapping ? Math.max(1, bitWidth(Math.max(width, height))) : 1;
        this.numSamples = numSamples;
        int textureUsage = WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst;
        if (renderAttachment)
            textureUsage |= (WGPUTextureUsage.RenderAttachment | WGPUTextureUsage.CopySrc);    // todo COPY_SRC is temp
        create( "texture", mipLevelCount, textureUsage, format, 1, numSamples, null);
    }

    public WebGPUTexture(int width, int height, int mipLevelCount, int textureUsage, WGPUTextureFormat format, int numSamples ) {
        this.width = width;
        this.height = height;
        this.numSamples = numSamples;
        this.mipLevelCount = mipLevelCount;
        this.format = format;

        create( "texture", mipLevelCount, textureUsage, format, 1, numSamples, null);
    }

    public WebGPUTexture(int width, int height, int mipLevelCount, int textureUsage, WGPUTextureFormat format, int numSamples, WGPUTextureFormat viewFormat ) {
        this.width = width;
        this.height = height;
        this.numSamples = numSamples;
        this.mipLevelCount = mipLevelCount;
        this.format = format;
        create( "texture", mipLevelCount, textureUsage, format, 1, numSamples, viewFormat);
    }

    /*
     * File loading.
     */

    public WebGPUTexture(String fileName) {
        this(fileName, true);
    }

    public WebGPUTexture(String fileName, boolean mipMapping) {
        this(Gdx.files.internal(fileName), mipMapping);
    }

    public WebGPUTexture(FileHandle file, boolean mipMapping ){
        byte[] byteArray = file.readBytes();
        loadFileData(byteArray, file.name(), mipMapping);
    }

    /** byte array contains full file content, i.e. including file header */
    public WebGPUTexture(byte[] byteArray, String name, boolean mipMapping) {
        loadFileData(byteArray, name, mipMapping);
    }

    public void loadFileData(byte[] byteArray, String name, boolean mipMapping) {

        Pointer data = JavaWebGPU.createByteArrayPointer(byteArray);
        image = JavaWebGPU.getUtils().gdx2d_load(data, byteArray.length);        // use native function to parse image file

        WebGPUPixmapInfo info = WebGPUPixmapInfo.createAt(image);
        this.width = info.width.intValue();
        this.height = info.height.intValue();
        int channelsInFile = info.format.intValue();    // gdx2d_load will convert to RGBA, this value gives the original #channels in the file, e.g. 3 for RGB
        Pointer pixelPtr = info.pixels.get();
        format = WGPUTextureFormat.RGBA8Unorm;

        mipLevelCount = mipMapping ? Math.max(1, bitWidth(Math.max(width, height))) : 1;
        numSamples = 1;
        int textureUsage = WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst;
        create( name, mipLevelCount, textureUsage, format, 1, numSamples, null);
        load(pixelPtr, 0);
    }








    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMipLevelCount() {
        return mipLevelCount;
    }

    public WebGPUTextureView getTextureView(){
        return textureView;
    }

    public Pointer getSampler() { return sampler; }

    public WGPUTextureFormat getFormat(){
        return format;
    }

    public Pointer getHandle(){
        return texture;
    }

    public int getNumSamples(){
        return numSamples;
    }


    protected int bitWidth(int value) {
        if (value == 0)
            return 0;
        else {
            int w = 0;
            while ((value >>= 1) > 0)
                ++w;
            return w;
        }
    }

    // renderAttachment - will this texture be used for render output
    // numLayers - normally 1, e.g. 6 for a cube map
    // numSamples - for anti-aliasing
    //
    protected void create( String label, int mipLevelCount, int textureUsage, WGPUTextureFormat format, int numLayers, int numSamples, WGPUTextureFormat viewFormat) {
        if (app.getDevice() == null || app.getQueue() == null)
            throw new RuntimeException("Texture creation requires device and queue to be available\n");

        this.label = label;

        // Create the texture
        WGPUTextureDescriptor textureDesc = WGPUTextureDescriptor.createDirect();
        textureDesc.setNextInChain();
        textureDesc.setLabel(label);
        textureDesc.setDimension( WGPUTextureDimension._2D);
        this.format = format; //
        textureDesc.setFormat(format);
        textureDesc.setMipLevelCount(mipLevelCount);
        textureDesc.setSampleCount(numSamples);
        textureDesc.getSize().setWidth(width);
        textureDesc.getSize().setHeight(height);
        textureDesc.getSize().setDepthOrArrayLayers(numLayers);
        textureDesc.setUsage(textureUsage);
        if (viewFormat == null) {
            textureDesc.setViewFormatCount(0);
            textureDesc.setViewFormats(JavaWebGPU.createNullPointer());
        } else {
            long[] formats = new long[1];       // TMP
            formats[0] = viewFormat.ordinal();
            Pointer formatPtr = JavaWebGPU.createLongArrayPointer(formats);
            textureDesc.setViewFormatCount(1);
            textureDesc.setViewFormats( formatPtr );
        }

        texture = webGPU.wgpuDeviceCreateTexture(app.getDevice().getHandle(), textureDesc);

        //System.out.println("dimensions: "+textureDesc.getSize().getDepthOrArrayLayers());


        // Create the view of the  texture manipulated by the rasterizer
        WGPUTextureViewDimension dimension = (numLayers == 1 ? WGPUTextureViewDimension._2D : (numLayers == 6 ? WGPUTextureViewDimension.Cube: WGPUTextureViewDimension._2DArray));
        textureView =  new WebGPUTextureView(this, WGPUTextureAspect.All, dimension, format, 0,
                mipLevelCount, 0, numLayers );

        // Create a sampler
        //
        WGPUSamplerDescriptor samplerDesc = WGPUSamplerDescriptor.createDirect();
        samplerDesc.setLabel("Standard texture sampler");
        samplerDesc.setAddressModeU(WGPUAddressMode.Repeat);
        samplerDesc.setAddressModeV(WGPUAddressMode.Repeat);
        samplerDesc.setAddressModeW(WGPUAddressMode.Repeat);
        samplerDesc.setMagFilter(WGPUFilterMode.Linear);
        samplerDesc.setMinFilter(WGPUFilterMode.Linear);
        samplerDesc.setMipmapFilter(WGPUMipmapFilterMode.Linear);

        samplerDesc.setLodMinClamp(0);
        samplerDesc.setLodMaxClamp(mipLevelCount);
        samplerDesc.setCompare(WGPUCompareFunction.Undefined);
        samplerDesc.setMaxAnisotropy(1);
        sampler = webGPU.wgpuDeviceCreateSampler(app.getDevice().getHandle(), samplerDesc);

    }

    public void fill(Color color) {
        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(0);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(0);
        destination.setAspect(WGPUTextureAspect.All);   // not relevant

        // Arguments telling how the C++ side pixel memory is laid out
        WGPUTextureDataLayout source = WGPUTextureDataLayout.createDirect();
        source.setOffset(0);
        source.setBytesPerRow(4 * width);
        source.setRowsPerImage(height);

        byte[] pixels = new byte[4 * width * height];
        byte r = (byte) (color.r * 255);
        byte g = (byte) (color.g * 255);
        byte b = (byte) (color.b * 255);
        byte a = (byte) (color.a * 255);

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[offset++] = r;
                pixels[offset++] = g;
                pixels[offset++] = b;
                pixels[offset++] = a;
            }
        }

        Pointer pixelPtr = JavaWebGPU.createByteArrayPointer(pixels);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        destination.setMipLevel(0);

        // N.B. using textureDesc.getSize() for param won't work!
        webGPU.wgpuQueueWriteTexture(app.getQueue().getHandle(), destination, pixelPtr, width * height * 4, source, ext);
   }

    /** fill textures using bytes arranged as r, g, b, a, r, g, b, a, etc.
     * Size of buffer must be 4*width*height
     * */
    public void fill(byte[] pixels) {
        if(pixels.length != 4*width*height) throw new IllegalArgumentException("Texture.fill(): byte array is wrong size.");
        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(0);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(0);
        destination.setAspect(WGPUTextureAspect.All);   // not relevant

        // Arguments telling how the C++ side pixel memory is laid out
        WGPUTextureDataLayout source = WGPUTextureDataLayout.createDirect();
        source.setOffset(0);
        source.setBytesPerRow(4 * width);
        source.setRowsPerImage(height);

        Pointer pixelPtr = JavaWebGPU.createByteArrayPointer(pixels);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        destination.setMipLevel(0);

        // N.B. using textureDesc.getSize() for param won't work!
        webGPU.wgpuQueueWriteTexture(app.getQueue().getHandle(), destination, pixelPtr, width * height * 4, source, ext);
    }


    /** fill textures using half-floats masquerading as shorts arranged as r, g, b, a, r, g, b, a, etc.
     * format RBGAFloat16
     * Size of buffer must be 4*width*height
     * */
    public void fill(short[] pixels) {
        if(pixels.length != 4*width*height) throw new IllegalArgumentException("Texture.fill(): array is wrong size.");
        if(format != WGPUTextureFormat.RGBA16Float) throw new IllegalArgumentException("Texture.fill(): expected RGBA16Float texture.");
        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(0);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(0);
        destination.setAspect(WGPUTextureAspect.All);   // not relevant

        // Arguments telling how the C++ side pixel memory is laid out
        WGPUTextureDataLayout source = WGPUTextureDataLayout.createDirect();
        source.setOffset(0);
        source.setBytesPerRow(4 * width*2);
        source.setRowsPerImage(height);

        ByteBuffer bb = ByteBuffer.allocateDirect(8*width*height);
        bb.order( ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sb = bb.asShortBuffer();
        sb.put(pixels);
        Pointer pixelPtr = JavaWebGPU.createByteBufferPointer(bb);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        destination.setMipLevel(0);

        // N.B. using textureDesc.getSize() for param won't work!
        webGPU.wgpuQueueWriteTexture(app.getQueue().getHandle(), destination, pixelPtr, width * height * 8, source, ext);
    }

    /** fill textures using floats arranged as r, g, b, a, r, g, b, a, etc.
     * Size of buffer must be 4*width*height
     * Allows for HDR textures.
     * */
    public void fill(float[] pixels) {
        if(pixels.length != 4*width*height) throw new IllegalArgumentException("Texture.fill(): float array is wrong size.");
        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(0);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(0);
        destination.setAspect(WGPUTextureAspect.All);   // not relevant

        // Arguments telling how the C++ side pixel memory is laid out
        WGPUTextureDataLayout source = WGPUTextureDataLayout.createDirect();
        source.setOffset(0);
        source.setBytesPerRow(4L * width*Float.BYTES);
        source.setRowsPerImage(height);

        Pointer pixelPtr = JavaWebGPU.createFloatArrayPointer(pixels);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        destination.setMipLevel(0);

        // N.B. using textureDesc.getSize() for param won't work!
        webGPU.wgpuQueueWriteTexture(app.getQueue().getHandle(), destination, pixelPtr, width * height * 4L*Float.BYTES, source, ext);
    }

    public void fillHDR(Color color) {
        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(0);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(0);
        destination.setAspect(WGPUTextureAspect.All);   // not relevant

        // Arguments telling how the C++ side pixel memory is laid out
        WGPUTextureDataLayout source = WGPUTextureDataLayout.createDirect();
        source.setOffset(0);
        source.setBytesPerRow(16 * width);
        source.setRowsPerImage(height);

        float[] pixels = new float[4 * width * height];
//        byte r = (byte) (color.r * 255);
//        byte g = (byte) (color.g * 255);
//        byte b = (byte) (color.b * 255);
//        byte a = (byte) (color.a * 255);

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[offset++] = 0; //color.r;
                pixels[offset++] = 0; //color.g;
                pixels[offset++] = 0; //color.b;
                pixels[offset++] = 0; //color.a;
            }
        }

        Pointer pixelPtr = JavaWebGPU.createFloatArrayPointer(pixels);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        destination.setMipLevel(0);

        // N.B. using textureDesc.getSize() for param won't work!
        webGPU.wgpuQueueWriteTexture(app.getQueue().getHandle(), destination, pixelPtr, (long) width * height * 4*4, source, ext);
    }


    /** Load pixel data into texture.
     *
     * @param pixelPtr
     * @param layer which layer to load in case of a 3d texture, otherwise 0
     */
    public void load(Pointer pixelPtr, int layer) {

        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(0);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(0);
        destination.setAspect(WGPUTextureAspect.All);   // not relevant

        // Arguments telling how the C++ side pixel memory is laid out
        WGPUTextureDataLayout source = WGPUTextureDataLayout.createDirect();
        source.setOffset(0);
        source.setBytesPerRow(4*width);
        source.setRowsPerImage(height);

        // Generate mipmap levels
        // candidate for compute shader

        int mipLevelWidth = width;
        int mipLevelHeight = height;
        int numComponents = numComponents(format);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();

        byte[] prevPixels = null;
        for(int mipLevel = 0; mipLevel < mipLevelCount; mipLevel++) {

            byte[] pixels = new byte[4 * mipLevelWidth * mipLevelHeight];

            if(mipLevel == 0){
                // fast copy for most common case: mip level 0
                pixelPtr.get(0, pixels, 0, numComponents * mipLevelWidth * mipLevelHeight);
            }
            else {
                // todo with compute shader
                int offset = 0;
                for (int y = 0; y < mipLevelHeight; y++) {
                    for (int x = 0; x < mipLevelWidth; x++) {

                        // Get the corresponding 4 pixels from the previous level
                        int offset00 = 4 * ((2 * y + 0) * (2 * mipLevelWidth) + (2 * x + 0));
                        int offset01 = 4 * ((2 * y + 0) * (2 * mipLevelWidth) + (2 * x + 1));
                        int offset10 = 4 * ((2 * y + 1) * (2 * mipLevelWidth) + (2 * x + 0));
                        int offset11 = 4 * ((2 * y + 1) * (2 * mipLevelWidth) + (2 * x + 1));

                        // Average r, g and b components
                        // beware that java bytes are signed. So we convert to integer first
                        int r = toUnsignedInt(prevPixels[offset00]) + toUnsignedInt(prevPixels[offset01]) + toUnsignedInt(prevPixels[offset10]) + toUnsignedInt(prevPixels[offset11]);
                        int g = toUnsignedInt(prevPixels[offset00 + 1]) + toUnsignedInt(prevPixels[offset01 + 1]) + toUnsignedInt(prevPixels[offset10 + 1]) + toUnsignedInt(prevPixels[offset11 + 1]);
                        int b = toUnsignedInt(prevPixels[offset00 + 2]) + toUnsignedInt(prevPixels[offset01 + 2]) + toUnsignedInt(prevPixels[offset10 + 2]) + toUnsignedInt(prevPixels[offset11 + 2]);
                        int a = toUnsignedInt(prevPixels[offset00 + 3]) + toUnsignedInt(prevPixels[offset01 + 3]) + toUnsignedInt(prevPixels[offset10 + 3]) + toUnsignedInt(prevPixels[offset11 + 3]);
                        pixels[offset++] = (byte) (r >> 2);    // divide by 4
                        pixels[offset++] = (byte) (g >> 2);
                        pixels[offset++] = (byte) (b >> 2);
                        pixels[offset++] = (byte) (a >> 2);  // alpha
                    }
                }
            }


            destination.setMipLevel(mipLevel);
            destination.getOrigin().setZ(layer);

            source.setBytesPerRow(4*mipLevelWidth);
            source.setRowsPerImage(mipLevelHeight);

            ext.setWidth(mipLevelWidth);
            ext.setHeight(mipLevelHeight);
            ext.setDepthOrArrayLayers(1);

            if(mipLevel == 0){
                webGPU.wgpuQueueWriteTexture(app.getQueue().getHandle(), destination, pixelPtr, mipLevelWidth * mipLevelHeight * 4, source, ext);
            } else {

                // wrap byte array in native pointer
                Pointer pixelData = JavaWebGPU.createByteArrayPointer(pixels);
                // N.B. using textureDesc.getSize() for param won't work!
                webGPU.wgpuQueueWriteTexture(app.getQueue().getHandle(), destination, pixelData, mipLevelWidth * mipLevelHeight * 4, source, ext);
            }

            mipLevelWidth /= 2;
            mipLevelHeight /= 2;
            prevPixels = pixels;
        }
    }

    /** Load image data into a specific layer and mip level
     *
     * @param info
     * @param layer
     * @param mipLevel
     */
//    protected void loadMipLevel(PixmapInfo info, int layer, int mipLevel) {
//        loadMipLevel(info.pixels.get(), info.width.intValue(), info.height.intValue(), layer, mipLevel);
//    }

    protected void loadMipLevel(Pointer data, int width, int height, int layer, int mipLevel) {

        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(mipLevel);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(layer);
        destination.setAspect(WGPUTextureAspect.All);   // not relevant

        // Arguments telling how the C++ side pixel memory is laid out
        WGPUTextureDataLayout source = WGPUTextureDataLayout.createDirect();
        source.setOffset(0);
        source.setBytesPerRow(4*width);
        source.setRowsPerImage(height);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        webGPU.wgpuQueueWriteTexture(app.getQueue().getHandle(), destination, data, 4L * width * height, source, ext);
    }



    // load HDR image (RBGA16Float), no mip mapping, no layers
    protected void loadHDR(Pointer pixelPtr) {

        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(0);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(0);
        destination.setAspect(WGPUTextureAspect.All);   // not relevant

        // Arguments telling how the C++ side pixel memory is laid out
        WGPUTextureDataLayout source = WGPUTextureDataLayout.createDirect();
        source.setOffset(0);
        source.setBytesPerRow(2*4*width);   // 2 bytes per component
        source.setRowsPerImage(height);


        WGPUExtent3D ext = WGPUExtent3D.createDirect();

        float[] pixels = new float[4 * width * height];

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[offset] = pixelPtr.getFloat(offset);  offset++;
                pixels[offset] = pixelPtr.getFloat(offset);  offset++;
                pixels[offset] = pixelPtr.getFloat(offset);  offset++;
                pixels[offset] = pixelPtr.getFloat(offset);  offset++;
            }
        }

        destination.setMipLevel(0);
        destination.getOrigin().setZ(0);

        source.setBytesPerRow(2*4*width);
        source.setRowsPerImage(height);

        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        // wrap byte array in native pointer
        Pointer pixelData = JavaWebGPU.createFloatArrayPointer(pixels);
        // N.B. using textureDesc.getSize() for param won't work!
        webGPU.wgpuQueueWriteTexture(app.getQueue().getHandle(), destination, pixelData, width * height * 8, source, ext);

    }


    private static int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    public static int numComponents(WGPUTextureFormat format ){
        int n = 4;
        switch(format) {
            case R8Unorm: n = 1; break;
            case RG8Uint: n = 2; break;
            case RGBA8Uint: n = 4; break;
            case RGBA8Unorm: n = 4; break;
            case BGRA8Unorm: n = 4; break;
            default: throw new IllegalArgumentException("Unsupported format: "+format);

        }
        return n;
    }

    @Override
    public void dispose(){
        if(image != null) {
            //System.out.println("free: "+image);
            JavaWebGPU.getUtils().gdx2d_free(image);
            image = null;
        }
        if(texture != null) {   // guard against double dispose
            //System.out.println("Destroy texture " + label);
            // todo released when?
            //LibGPU.webGPU.wgpuSamplerRelease(sampler);
            textureView.dispose();
            //LibGPU.webGPU.wgpuTextureViewRelease(textureView);
            webGPU.wgpuTextureDestroy(texture);
            webGPU.wgpuTextureRelease(texture);
            texture = null;
        }
    }
}
