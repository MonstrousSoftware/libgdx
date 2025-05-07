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

package com.badlogic.gdx.backends.webgpu.gdx;


import com.badlogic.gdx.backends.webgpu.webgpu.WGPUVertexAttribute;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUVertexBufferLayout;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUVertexFormat;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUVertexStepMode;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import static com.badlogic.gdx.backends.webgpu.gdx.WebGPUVertexAttributes.Usage.*;


public class WebGPUVertexAttributes implements Disposable {

    public Array<WebGPUVertexAttribute> attributes;
    private WGPUVertexBufferLayout vertexBufferLayout;
    private int vertexSize; // in floats
    private long usageFlags;        // bit mask of Usage values

    public static class Usage {
        static public final int POSITION = 1;
        static public final int POSITION_2D = 2;
        static public final int COLOR = 4;
        static public final int COLOR_PACKED = 8;
        static public final int TEXTURE_COORDINATE = 16;
        static public final int NORMAL= 32;
        static public final int TANGENT = 64;
        static public final int BITANGENT = 128;
        static public final int JOINTS = 256;
        static public final int WEIGHTS = 512;

        static public final int _LAST = 512;

        static public final int GENERIC = 1024;
    }

    public WebGPUVertexAttributes() {
        attributes = new Array<>();
        vertexBufferLayout = null;
        vertexSize = -1;
        usageFlags = 0L;
    }

    public WebGPUVertexAttributes(long usageFlags) {
        attributes = new Array<>();
        vertexBufferLayout = null;
        vertexSize = -1;
        this.usageFlags = 0L;
        for(int flag = 1; flag <= WebGPUVertexAttributes.Usage._LAST; flag++){
            if((usageFlags & flag) == flag){
                switch(flag){
                    case POSITION:      add(flag, "position", WGPUVertexFormat.Float32x3, 0); break;
                    case POSITION_2D:   add(flag, "position", WGPUVertexFormat.Float32x2, 0); break;
                    case COLOR:         add(flag, "color", WGPUVertexFormat.Float32x4, 5); break;
                    case COLOR_PACKED:  add(flag, "color", WGPUVertexFormat.Unorm8x4, 5); break;
                    case TEXTURE_COORDINATE: add(flag, "uv", WGPUVertexFormat.Float32x2, 1); break;
                    case NORMAL:        add(flag, "normal", WGPUVertexFormat.Float32x3, 2); break;
                    case TANGENT:       add(flag, "tangent", WGPUVertexFormat.Float32x3, 3); break;
                    case BITANGENT:     add(flag, "bitangent", WGPUVertexFormat.Float32x3, 4); break;
                    case JOINTS:        add(flag, "joints", WGPUVertexFormat.Float32x4, 6); break;  // todo compress
                    case WEIGHTS:       add(flag, "weights", WGPUVertexFormat.Float32x4, 7); break;
                }
            }
        }
        end();
    }

    /** use with caution */
    public void add(long usage, String label, WGPUVertexFormat format, int shaderLocation){
        WebGPUVertexAttribute va = new WebGPUVertexAttribute(usage, label, format, shaderLocation);
        usageFlags |= usage;
        attributes.add(va);
    }

    public void end(){
        vertexSize = 0;
        for(WebGPUVertexAttribute va : attributes) {
            vertexSize += va.getSize();
        }
    }

    public boolean hasUsage(long usage){
        return (usageFlags & usage) == usage;
    }

    public long getUsageFlags(){
        return usageFlags;
    }


    public int getVertexSizeInBytes() {
        if(vertexSize < 0)
            throw new RuntimeException("getVertexSize: call VertexAttributes.end() first");
        return vertexSize;
    }

    /** create a vertex buffer layout object from the VertexAttributes */
    public WGPUVertexBufferLayout getVertexBufferLayout(){
        if(vertexBufferLayout != null)
            return vertexBufferLayout;

        WGPUVertexAttribute[] attribs = new WGPUVertexAttribute[attributes.size];

        int offset = 0;
        int index = 0;
        for(WebGPUVertexAttribute va : attributes) {

            attribs[index] =  WGPUVertexAttribute.createDirect();
            attribs[index].setFormat(va.format);
            attribs[index].setOffset(offset);
            attribs[index].setShaderLocation(va.shaderLocation);

            offset += va.getSize();
            index++;
        }

        vertexBufferLayout = WGPUVertexBufferLayout.createDirect();
        vertexBufferLayout.setAttributeCount(attributes.size);
        vertexBufferLayout.setAttributes(attribs);
        vertexBufferLayout.setArrayStride(offset);
        vertexBufferLayout.setStepMode(WGPUVertexStepMode.Vertex);
        return vertexBufferLayout;
    }

    /** find (first) vertex attribute corresponding to requested usage */
    public WebGPUVertexAttribute getAttributeByUsage( long usage ){
        for(WebGPUVertexAttribute va : attributes) {
            if(va.usage == usage)
                return va;
        }
        return null;
    }

    /** find offset in bytes for a particulate usage, e.g. POSITION. Returns -1 if not found */
    public int getOffset( long usage ){
        int offset = 0;
        for(WebGPUVertexAttribute va : attributes) {
            if (va.usage == usage)
                return offset;
            offset += va.getSize();
        }
        return -1;
    }


    @Override
    public void dispose() {
        // todo free layout?
    }
}
