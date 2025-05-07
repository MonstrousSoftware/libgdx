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
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUEnvironment;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUShaderProgram;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUVertexAttributes;
import com.badlogic.gdx.backends.webgpu.webgpu.*;

import java.util.Objects;

// todo Environment

public class PipelineSpecification {
    public String name;
    public WebGPUVertexAttributes vertexAttributes;
    public WGPUIndexFormat indexFormat;
    public WGPUPrimitiveTopology topology;
    public WebGPUEnvironment environment;
    public String shaderSource;
    public WebGPUShaderProgram shader;
    public boolean useDepthTest;
    public boolean noDepthAttachment; // use only when not rendering to the screen, removes the depth attachment
    public boolean isSkyBox;
    public boolean isDepthPass;
    public boolean afterDepthPrepass;
    public int numSamples;

    public WGPUBlendFactor blendSrcColor;
    public WGPUBlendFactor blendDstColor;
    public WGPUBlendOperation blendOpColor;
    public WGPUBlendFactor blendSrcAlpha;
    public WGPUBlendFactor blendDstAlpha;
    public WGPUBlendOperation blendOpAlpha;
    public WGPUCullMode cullMode;

    public WGPUTextureFormat colorFormat;
    public WGPUTextureFormat depthFormat;
    private int hash;


    public PipelineSpecification() {
        this.name = "pipeline";
        enableDepthTest();
        noDepthAttachment = false;
        disableBlending();
        setCullMode(WGPUCullMode.None);
        indexFormat = WGPUIndexFormat.Uint16;
        topology =  WGPUPrimitiveTopology.TriangleList;
        isDepthPass = false;
        colorFormat = ((WebGPUApplication) Gdx.app).getSurfaceFormat();
        depthFormat = WGPUTextureFormat.Depth24Plus;       // todo get from adapter?
        numSamples = 1;
        isSkyBox = false;
        afterDepthPrepass = false;
        recalcHash();
    }

    public PipelineSpecification(WebGPUVertexAttributes vertexAttributes, String shaderSource) {
        this();
        this.vertexAttributes = vertexAttributes;
        this.shaderSource = shaderSource;
        recalcHash();
    }

    public PipelineSpecification(WebGPUVertexAttributes vertexAttributes, WebGPUShaderProgram shader) {
        this();
        this.vertexAttributes = vertexAttributes;
        this.shader = shader;
        recalcHash();
    }

    public PipelineSpecification(PipelineSpecification spec) {
        this.name  = spec.name;
        this.vertexAttributes = spec.vertexAttributes;       // should be deep copy
        this.environment = spec.environment;
        this.shaderSource = spec.shaderSource;
        this.shader = spec.shader;
        this.useDepthTest = spec.useDepthTest;
        this.noDepthAttachment = spec.noDepthAttachment;
        this.isDepthPass= spec.isDepthPass;
        this.blendSrcColor = spec.blendSrcColor;
        this.blendDstColor = spec.blendDstColor;
        this.blendOpColor = spec.blendOpColor;
        this.blendSrcAlpha = spec.blendSrcAlpha;
        this.blendDstAlpha = spec.blendDstAlpha;
        this.blendOpAlpha = spec.blendOpAlpha;
        this.cullMode = spec.cullMode;
        this.topology = spec.topology;
        this.indexFormat = spec.indexFormat;
        this.isSkyBox = spec.isSkyBox;
        this.afterDepthPrepass = spec.afterDepthPrepass;

        this.colorFormat = spec.colorFormat;
        this.depthFormat = spec.depthFormat;
        this.numSamples = spec.numSamples;
        recalcHash();
    }

    public void enableDepthTest(){
        useDepthTest = true;
        recalcHash();
    }

    public void disableDepthTest(){
        useDepthTest = false;
        recalcHash();
    }

    public void setCullMode(WGPUCullMode cullMode){
        this.cullMode = cullMode;
        recalcHash();
    }

    public void enableBlending(){
        blendSrcColor = WGPUBlendFactor.SrcAlpha;
        blendDstColor = WGPUBlendFactor.OneMinusSrcAlpha;
        blendOpColor = WGPUBlendOperation.Add;
        blendSrcAlpha = WGPUBlendFactor.Zero;
        blendDstAlpha = WGPUBlendFactor.One;
        blendOpAlpha = WGPUBlendOperation.Add;
        recalcHash();
    }

    public void disableBlending(){
        blendSrcColor = WGPUBlendFactor.One;
        blendDstColor = WGPUBlendFactor.Zero;
        blendOpColor = WGPUBlendOperation.Add;
        blendSrcAlpha = WGPUBlendFactor.One;
        blendDstAlpha = WGPUBlendFactor.Zero;
        blendOpAlpha = WGPUBlendOperation.Add;
        recalcHash();
    }

    // used?
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipelineSpecification that = (PipelineSpecification) o;
        return useDepthTest == that.useDepthTest && Objects.equals(vertexAttributes, that.vertexAttributes) && blendSrcColor == that.blendSrcColor && blendDstColor == that.blendDstColor
                && blendOpColor == that.blendOpColor && blendSrcAlpha == that.blendSrcAlpha && blendDstAlpha == that.blendDstAlpha && blendOpAlpha == that.blendOpAlpha &&
                numSamples == that.numSamples;
        // todo compare shader
    }

    @Override
    public int hashCode() {
        return hash;
    }

    /** to be called whenever relevant content changes (to avoid doing this in hashCode which is called a lot) */
    public void recalcHash() {
        hash = Objects.hash(vertexAttributes != null ? vertexAttributes.getUsageFlags() : 0,
                shaderSource,
                isDepthPass, afterDepthPrepass,
                useDepthTest, noDepthAttachment,
                topology, indexFormat,
//                environment == null ? 0 :!environment.depthPass && environment.renderShadows,
//                environment == null ? 0 : environment.cubeMap != null,
//                environment == null ? 0 : environment.useImageBasedLighting,
                blendSrcColor, blendDstColor, blendOpColor, blendSrcAlpha, blendDstAlpha, blendOpAlpha, numSamples, cullMode, isSkyBox, depthFormat, numSamples);
    }

    // note: don't include compiled shader in the hash because this would force new compiles every frame since a spec of an uncompiled shader <> compiled shader

}
