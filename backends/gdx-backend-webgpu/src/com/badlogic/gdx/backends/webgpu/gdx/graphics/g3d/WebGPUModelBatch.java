package com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUGraphicsBase;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.Binder;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.WebGPUMesh;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.WebGPUShaderProgram;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.model.WebGPUMeshPart;
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
import com.badlogic.gdx.utils.Array;
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
    private final WebGPUBindGroupLayout bindGroupLayout;
    private final Binder binder;
    private final WebGPUUniformBuffer uniformBuffer;
    private final Array<Renderable> renderables;
    private final WebGPUPipelineLayout pipelineLayout;
    private final PipelineCache pipelines;
    private final PipelineSpecification pipelineSpec;
    private final VertexAttributes vertexAttributes;
    private final WebGPUTexture texture;


    /** Create a ModelBatch.
     *
     */
    public WebGPUModelBatch() {
        gfx = (WebGPUGraphicsBase) Gdx.graphics;

        drawing = false;
        clearColor = new Color(Color.GRAY);

        texture = new WebGPUTexture(Gdx.files.internal("data/badlogic.jpg"));       // TMP


        bindGroupLayout = createBindGroupLayout();

        binder = new Binder();
        // define group
        binder.defineGroup(0, bindGroupLayout);
        // define bindings in the group
        binder.defineUniform("uniforms", 0, 0);
        binder.defineUniform("diffuseTexture", 0, 1);
        binder.defineUniform("diffuseSampler", 0, 2);
        // define uniforms in uniform buffer (binding 0) with their offset
        binder.defineUniform("projectionMatrix", 0, 0, 0);

        // Create uniform buffer for the projection matrix
        int uniformBufferSize = 16 * Float.BYTES;
        uniformBuffer = new WebGPUUniformBuffer(uniformBufferSize,WGPUBufferUsage.CopyDst |WGPUBufferUsage.Uniform  );

        // set binding 0 to uniform buffer
        binder.setBuffer("uniforms", uniformBuffer, 0, uniformBufferSize);

        binder.setTexture("diffuseTexture", texture.getTextureView());
        binder.setSampler("diffuseSampler", texture.getSampler());

        // get pipeline layout which aggregates all the bind group layouts
        pipelineLayout = binder.getPipelineLayout("ModelBatch pipeline layout");

        vertexAttributes = new VertexAttributes(VertexAttribute.Position(), VertexAttribute.TexCoords(0), VertexAttribute.ColorUnpacked());


        pipelines = new PipelineCache();
        pipelineSpec = new PipelineSpecification(vertexAttributes, getDefaultShaderSource());
        pipelineSpec.name = "ModelBatch pipeline";

        // default blending values
        pipelineSpec.enableBlending();
        pipelineSpec.setBlendFactor(WGPUBlendFactor.SrcAlpha, WGPUBlendFactor.OneMinusSrcAlpha);


        renderables = new Array<>();

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

        binder.setUniform("projectionMatrix", camera.combined);

        // bind group 0 once per frame
        binder.bindGroup(renderPass, 0);


        renderables.clear();

    }

    public void render(Renderable renderable){
        renderables.add(renderable);
    }

    public void flush() {
        if(renderables.size == 0)
            return;

        Renderable renderable = renderables.get(0); // to do loop

        WebGPUPipeline pipeline = pipelines.findPipeline( pipelineLayout.getHandle(), pipelineSpec);
        renderPass.setPipeline(pipeline.getHandle());

        Mesh mesh = renderable.meshPart.mesh;

        if (!(mesh instanceof WebGPUMesh))
            throw new RuntimeException("WebGPUMeshPart supports only WebGPUMesh");
        ((WebGPUMesh)mesh).render(renderPass, renderable.meshPart.primitiveType, renderable.meshPart.offset, renderable.meshPart.size);

        //meshPart.render(renderPass);

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

    private WebGPUBindGroupLayout createBindGroupLayout() {
        WebGPUBindGroupLayout layout = new WebGPUBindGroupLayout("ModelBatch bind group layout");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex, WGPUBufferBindingType.Uniform, 16*Float.BYTES, false);
        layout.addTexture(1, WGPUShaderStage.Fragment, WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
        layout.addSampler(2, WGPUShaderStage.Fragment, WGPUSamplerBindingType.Filtering );

        layout.end();
        return layout;
    }


    private String getDefaultShaderSource() {
        return "// basic model batch shader\n" +
                "\n" +
                "struct FrameUniforms {\n" +
                "    projectionMatrix: mat4x4f,\n" +
                "};\n" +
                "\n" +
                "@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;\n" +
                "@group(0) @binding(1) var diffuseTexture:        texture_2d<f32>;\n" +
                "@group(0) @binding(2) var diffuseSampler:       sampler;\n" +
                "\n" +
                "\n" +
                "struct VertexInput {\n" +
                "    @location(0) position: vec3f,\n" +
                "    @location(1) uv: vec2f,\n" +
                "    @location(5) color: vec4f\n" +
                "};\n" +
                "\n" +
                "struct VertexOutput {\n" +
                "    @builtin(position) position: vec4f,\n" +
                "    @location(1) uv: vec2f,\n" +
                "    @location(2) color: vec4f\n" +
                "};\n" +
                "\n" +
                "@vertex\n" +
                "fn vs_main(in: VertexInput) -> VertexOutput {\n" +
                "   var out: VertexOutput;\n" +
                "\n" +
                "   let worldPosition =  uFrame.projectionMatrix* vec4f(in.position, 1.0);\n" +
                "   out.position = worldPosition;\n" +
                "   out.uv = in.uv;\n" +
                "   out.color = in.color;\n" +
                "\n" +
                "   return out;\n" +
                "}\n" +
                "\n" +
                "\n" +
                "@fragment\n" +
                "fn fs_main(in : VertexOutput) -> @location(0) vec4f {\n" +
                "    let color = in.color * textureSample(diffuseTexture, diffuseSampler, in.uv);\n" +
                "    return color;\n" +
                "}";
    }


}
