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

package com.badlogic.gdx.backends.webgpu.gdx.graphics;


import com.badlogic.gdx.backends.webgpu.webgpu.WGPUVertexAttribute;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUVertexBufferLayout;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUVertexFormat;
import com.badlogic.gdx.backends.webgpu.webgpu.WGPUVertexStepMode;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;



public class WebGPUVertexLayout  {

    /** create a vertex buffer layout object from the VertexAttributes */
    public static WGPUVertexBufferLayout buildVertexBufferLayout( VertexAttributes attributes ){

        WGPUVertexAttribute[] attribs = new WGPUVertexAttribute[attributes.size()];

        int index = 0;
        int offset = 0;
        for(VertexAttribute attrib : attributes ){
                WGPUVertexFormat format = convertFormat(attrib);

                attribs[index] = WGPUVertexAttribute.createDirect();
                attribs[index].setFormat(format);
                attribs[index].setOffset(offset);
                attribs[index].setShaderLocation(getLocation(attrib.usage));

                offset += getSize(format);
                index++;
        }

        WGPUVertexBufferLayout vertexBufferLayout = WGPUVertexBufferLayout.createDirect();
        vertexBufferLayout.setAttributeCount(attributes.size());
        vertexBufferLayout.setAttributes(attribs);
        vertexBufferLayout.setArrayStride(offset);
        vertexBufferLayout.setStepMode(WGPUVertexStepMode.Vertex);
        return vertexBufferLayout;
    }

    private static WGPUVertexFormat convertFormat(VertexAttribute attrib){
        WGPUVertexFormat format = WGPUVertexFormat.Undefined;

        // todo complete all combinations
        switch(attrib.type){
            case GL20.GL_FLOAT:
                switch(attrib.numComponents){
                    case 1:   format = WGPUVertexFormat.Float32; break;
                    case 2:   format = WGPUVertexFormat.Float32x2; break;
                    case 3:   format = WGPUVertexFormat.Float32x3; break;
                    case 4:   format = WGPUVertexFormat.Float32x4; break;
                }
                break;
            case GL20.GL_UNSIGNED_BYTE:
                switch(attrib.numComponents){
                    case 2:   format = WGPUVertexFormat.Unorm8x2; break;
                    case 4:   format = WGPUVertexFormat.Unorm8x4; break;
                }
                break;

        }
        if(format == WGPUVertexFormat.Undefined) {
            throw new RuntimeException("Unsupported vertex attribute format type: " + attrib.type + " numComponents: " + attrib.numComponents);
        }
        return format;
    }

    /** get size in bytes */
    public static int getSize(WGPUVertexFormat format) {
        switch (format) {

            case Uint8x2:
            case Sint8x2:
            case Unorm8x2:
            case Snorm8x2:
                return 2;

            case Uint8x4:
            case Unorm8x4:
            case Sint8x4:
            case Snorm8x4:
            case Uint16x2:
            case Sint16x2:
            case Unorm16x2:
            case Snorm16x2:
            case Uint32:
            case Sint32:
            case Float16x2:
            case Float32:
            case Unorm1010102:
                return 4;

            case Uint16x4:
            case Sint16x4:
            case Unorm16x4:
            case Snorm16x4:
            case Float16x4:
            case Float32x2:
            case Uint32x2:
            case Sint32x2:
                return 8;

            case Float32x3:
            case Uint32x3:
            case Sint32x3:
                return 12;

            case Float32x4:
            case Uint32x4:
            case Sint32x4:
                return 16;

            case Undefined:
            default:
                throw new RuntimeException("Unknown vertex format: " + format);

        }
    }


    /** use standard locations for vertex attributes. Shader code needs to follow this too.
     */
    public static int getLocation(int usage){
        int loc = -1;
        switch(usage){
            case VertexAttributes.Usage.Position:  loc = 0; break;
            case VertexAttributes.Usage.ColorUnpacked:  loc = 5; break;
            case VertexAttributes.Usage.ColorPacked:  loc = 5; break;
            case VertexAttributes.Usage.Normal:  loc = 2; break;
            case VertexAttributes.Usage.TextureCoordinates:  loc = 1; break;
            case VertexAttributes.Usage.Generic:  loc = 0; break;
            case VertexAttributes.Usage.BoneWeight:  loc = 7; break;
            case VertexAttributes.Usage.Tangent:  loc = 3; break;
            case VertexAttributes.Usage.BiNormal:  loc = 4; break;
            default:
                throw new RuntimeException("Unknown usage: " + usage);
        }
        return loc;
    }

}
