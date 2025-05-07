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

import com.badlogic.gdx.backends.webgpu.gdx.WebGPUVertexAttributes.Usage;

public class ShaderPrefix {
    private static StringBuffer sb = new StringBuffer();

    public static String buildPrefix(WebGPUVertexAttributes vertexAttributes, WebGPUEnvironment environment ){
        sb.setLength(0);
        if(vertexAttributes != null) {
            if (vertexAttributes.hasUsage(Usage.TEXTURE_COORDINATE)) {
                sb.append("#define TEXTURE_COORDINATE\n");
            }
            if (vertexAttributes.hasUsage(Usage.COLOR) || vertexAttributes.hasUsage(Usage.COLOR_PACKED)) {
                sb.append("#define COLOR\n");
            }
            if (vertexAttributes.hasUsage(Usage.NORMAL)) {
                sb.append("#define NORMAL\n");
            }
            if (vertexAttributes.hasUsage(Usage.TANGENT)) {   // this is taken as indication that a normal map is used
                sb.append("#define NORMAL_MAP\n");
            }
            if (vertexAttributes.hasUsage(Usage.JOINTS)&&vertexAttributes.hasUsage(Usage.WEIGHTS)) {
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
