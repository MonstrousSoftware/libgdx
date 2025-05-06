package com.badlogic.gdx.backends.webgpu.wrappers;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUBufferUsage;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUIndexFormat;
import jnr.ffi.Pointer;

import java.util.ArrayList;

public class WebGPUIndexBuffer extends WebGPUBuffer {

    private int indexSizeInBytes;   // 2 or 4
    private int indexCount;

    public WebGPUIndexBuffer(long usage, int bufferSize, int indexSizeInBytes) {
        super("index buffer", usage, align(bufferSize));
        this.indexSizeInBytes = indexSizeInBytes;
    }

    public WebGPUIndexBuffer(ArrayList<Integer> indexValues, int indexSizeInBytes) {
        this(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index, align(indexValues.size()*indexSizeInBytes),indexSizeInBytes);
        setIndices(indexValues);
    }

    public WebGPUIndexBuffer(short[] indexValues, int indexCount) {
        this(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index, align(indexCount*2), 2);
        setIndices(indexValues, indexCount);
    }

    private static int align(int indexBufferSize ){
        return (indexBufferSize + 3) & ~3; // round up to the next multiple of 4
    }

    public int getIndexCount(){
        return indexCount;
    }

    public WGPUIndexFormat getFormat(){
        return determineFormat(indexSizeInBytes);
    }

    public static WGPUIndexFormat determineFormat(int indexSizeInBytes ){
        if(indexSizeInBytes == 2)
            return WGPUIndexFormat.Uint16;
        else if(indexSizeInBytes == 4)
            return WGPUIndexFormat.Uint32;
        else
            throw new RuntimeException("setIndices: support only 16 bit or 32 bit indices.");
    }

    public void setIndices(short[] indices, int indexCount){
        this.indexSizeInBytes = 2;
        this.indexCount = indexCount;
        int indexBufferSize = align(indexCount * indexSizeInBytes);

        Pointer iData = JavaWebGPU.createDirectPointer(indexBufferSize);
        iData.put(0, indices, 0, indexCount);
        setIndices(iData, indexBufferSize);
    }

    public void setIndices(int[] indices, int indexCount){
        this.indexSizeInBytes = 4;
        this.indexCount = indexCount;
        int indexBufferSize = align(indexCount * indexSizeInBytes);

        Pointer iData = JavaWebGPU.createDirectPointer(indexBufferSize);
        iData.put(0, indices, 0, indexCount);
        setIndices(iData, indexBufferSize);
    }

    public void setIndices(ArrayList<Integer> indexValues) {
        if(indexValues == null) {
            indexCount = 0;
            return;
        }
        indexCount = indexValues.size();
        int indexBufferSize = align(indexCount * indexSizeInBytes);

        Pointer idata = JavaWebGPU.createDirectPointer(indexBufferSize);
        if (indexSizeInBytes == 2) {
            for (int i = 0; i < indexCount; i++) {
                idata.putShort((long) i * indexSizeInBytes, (short) (int) indexValues.get(i));
            }
        } else if (indexSizeInBytes == 4) {
            for (int i = 0; i < indexCount; i++) {
                idata.putInt((long) i * indexSizeInBytes, indexValues.get(i));
            }
        }
        setIndices(idata, indexBufferSize);
    }

    /** fill index buffer with raw data. */
    private void setIndices(Pointer idata, int indexBufferSize) {
        if(indexBufferSize > getSize()) throw new IllegalArgumentException("IndexBuffer.setIndices: data too large.");

        // Upload data to the buffer
        app.getQueue().writeBuffer(this, 0, idata, indexBufferSize);
        //LibGPU.webGPU.wgpuQueueWriteBuffer(LibGPU.queue, getHandle(), 0, idata, indexBufferSize);
    }
}
