package com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUGraphicsBase;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.Binder;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.WebGPUShaderProgram;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Null;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to render model instances.
 */
public class WebGPUModelBatch implements Disposable {


    private final WebGPUGraphicsBase gfx;
    private boolean drawing;
    private Camera camera;
    private WebGPURenderPass renderPass;
    private final Color clearColor;


    /** Create a ModelBatch.
     *
     */
    public WebGPUModelBatch() {
        gfx = (WebGPUGraphicsBase) Gdx.graphics;

        drawing = false;
        clearColor = new Color(Color.GRAY);
    }

    public boolean isDrawing () {
        return drawing;
    }


    public void begin(final Camera camera) {
        this.camera = camera;

        if (drawing)
            throw new RuntimeException("Must end() before begin()");
        drawing = true;

        renderPass = RenderPassBuilder.create(null, gfx.getSamples());

    }

    public void render(Renderable renderable){

    }

    public void flush() {

    }

    public void end() {
        if (!drawing) // catch incorrect usage
            throw new RuntimeException("Cannot end() without begin()");
        drawing = false;
        flush();
        renderPass.end();
        renderPass = null;
    }




    @Override
    public void dispose(){

    }


    private String getDefaultShaderSource() {
        return "// basic model batch shader\n" +
                "\n" +
                "struct FrameUniforms {\n" +
                "    projectionMatrix: mat4x4f,\n" +
                "};\n" +
                "\n" +
                "@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;\n" +
                "\n" +
                "//@group(1) @binding(1) var albedoTexture:        texture_2d<f32>;\n" +
                "//@group(1) @binding(2) var textureSampler:       sampler;\n" +
                "\n" +
                "\n" +
                "struct VertexInput {\n" +
                "    @location(0) position: vec3f,\n" +
                "//    @location(1) uv: vec2f,\n" +
                "};\n" +
                "\n" +
                "struct VertexOutput {\n" +
                "    @builtin(position) position: vec4f,\n" +
                "//    @location(1) uv: vec2f,\n" +
                "};\n" +
                "\n" +
                "@vertex\n" +
                "fn vs_main(in: VertexInput) -> VertexOutput {\n" +
                "   var out: VertexOutput;\n" +
                "\n" +
                "   let worldPosition =  uFrame.projectionMatrix* vec4f(in.position, 1.0);\n" +
                "   out.position = worldPosition;\n" +
                "//   out.uv = in.uv;\n" +
                "\n" +
                "   return out;\n" +
                "}\n" +
                "\n" +
                "\n" +
                "@fragment\n" +
                "fn fs_main(in : VertexOutput) -> @location(0) vec4f {\n" +
                "    var color: vec3f = vec3f(1,0,0);\n" +
                "    return vec4f(color, 1);\n" +
                "}";
    }


}
