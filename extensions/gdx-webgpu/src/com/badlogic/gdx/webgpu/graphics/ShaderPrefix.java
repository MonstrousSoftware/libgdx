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

package com.badlogic.gdx.webgpu.graphics;


import com.badlogic.gdx.graphics.VertexAttributes;

public class ShaderPrefix {
    private static StringBuffer sb = new StringBuffer();

    public static String buildPrefix(VertexAttributes vertexAttributes, WebGPUEnvironment environment ){
        sb.setLength(0);

        if(vertexAttributes != null) {
            long mask = vertexAttributes.getMask();
            if ((mask & VertexAttributes.Usage.TextureCoordinates) != 0) {
                sb.append("#define TEXTURE_COORDINATE\n");
            }
            if ((mask & VertexAttributes.Usage.ColorUnpacked) != 0 ) {
                sb.append("#define COLOR\n");
            }
            if ((mask & VertexAttributes.Usage.ColorPacked) != 0 ) {
                sb.append("#define COLOR\n");
            }
            if ((mask & VertexAttributes.Usage.Normal) != 0) {
                sb.append("#define NORMAL\n");
            }
            if ((mask & VertexAttributes.Usage.Tangent) != 0) {  // this is taken as indication that a normal map is used
                sb.append("#define NORMAL_MAP\n");
            }
            if ((mask & VertexAttributes.Usage.BoneWeight) != 0) {
                sb.append("#define SKIN\n");
            }
        }
//        if (environment != null && !environment.depthPass && environment.renderShadows) {
//            sb.append("#define SHADOWS\n");
//        }
//        if (environment != null && environment.cubeMap != null) {
//            sb.append("#define CUBEMAP\n");
//        }
//        if (environment != null && environment.useImageBasedLighting) {
//            sb.append("#define USE_IBL\n");
//        }
        return sb.toString();
    }
}
