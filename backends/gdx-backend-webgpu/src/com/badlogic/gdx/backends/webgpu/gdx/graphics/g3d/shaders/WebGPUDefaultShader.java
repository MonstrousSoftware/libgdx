package com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.Binder;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.WebGPUMesh;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.utils.Disposable;

import static com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888;

public class WebGPUDefaultShader implements Shader {

    private Config config;
    private final WebGPUTexture defaultTexture;
    public Binder binder;
    private final WebGPUUniformBuffer uniformBuffer;
    private final WebGPUUniformBuffer instanceBuffer;
    private final WebGPUPipelineLayout pipelineLayout;
    private final PipelineCache pipelines;
    private final PipelineSpecification pipelineSpec;
    public int numRenderables;
    private WebGPUTexture lastTexture;
    private WebGPURenderPass renderPass;
    private VertexAttributes vertexAttributes;


    public static class Config {
        public int maxInstances;

        public Config() {
            this.maxInstances = 1024;
        }
    }

    public WebGPUDefaultShader(final Renderable renderable) {
        this(renderable, new Config());
    }

    public WebGPUDefaultShader(final Renderable renderable, Config config) {
        this.config = config;

        // fallback texture
        Pixmap pixmap = new Pixmap(1,1,RGBA8888);
        pixmap.setColor(Color.PINK);
        pixmap.fill();
        defaultTexture = new WebGPUTexture(pixmap);

        binder = new Binder();
        // define groups
        binder.defineGroup(0, createFrameBindGroupLayout());
        binder.defineGroup(1, createMaterialBindGroupLayout());
        binder.defineGroup(2, createInstancingBindGroupLayout());
        // define bindings in the groups
        binder.defineUniform("uniforms", 0, 0);
        binder.defineUniform("diffuseTexture", 1, 1);
        binder.defineUniform("diffuseSampler", 1, 2);
        binder.defineUniform("instanceUniforms", 2, 0);
        // define uniforms in uniform buffers with their offset
        binder.defineUniform("projectionViewTransform", 0, 0, 0);
        //binder.defineUniform("modelMatrix", 2, 0, 0);

        // Create uniform buffer for the projection matrix
        int uniformBufferSize = 16 * Float.BYTES;
        uniformBuffer = new WebGPUUniformBuffer(uniformBufferSize, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform);

        // set binding 0 to uniform buffer
        binder.setBuffer("uniforms", uniformBuffer, 0, uniformBufferSize);

        int instanceSize = 16*Float.BYTES;      // data size per instance

        // for now we use a uniform buffer, but we organize data as an array of modelMatrix
        // and write with an dynamic offset (numRenderables * sizeof matrix4)
        instanceBuffer = new WebGPUUniformBuffer(instanceSize*config.maxInstances, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Storage);

        binder.setBuffer("instanceUniforms", instanceBuffer, 0, (long) instanceSize *config.maxInstances);


        // get pipeline layout which aggregates all the bind group layouts
        pipelineLayout = binder.getPipelineLayout("ModelBatch pipeline layout");

        pipelines = new PipelineCache();
        // vertexAttributes will be set from the renderable
        vertexAttributes = renderable.meshPart.mesh.getVertexAttributes();
        pipelineSpec = new PipelineSpecification(vertexAttributes, getDefaultShaderSource());
        pipelineSpec.name = "ModelBatch pipeline";

        // default blending values
        pipelineSpec.enableBlending();
        pipelineSpec.setBlendFactor(WGPUBlendFactor.SrcAlpha, WGPUBlendFactor.OneMinusSrcAlpha);

    }

    @Override
    public void init() {
        // todo some constructor stuff to init()?

    }


    @Override
    public void begin(Camera camera, RenderContext context) {
        throw new IllegalArgumentException("Use begin(Camera, WebGPURenderPass)");
    }

    public void begin(Camera camera, WebGPURenderPass renderPass){
        this.renderPass = renderPass;

        // set global uniforms, that do not depend on renderables
        // e.g. camera, lighting, environment uniforms
        //
        binder.setUniform("projectionViewTransform", camera.combined);

        // bind group 0 (frame) once per frame
        binder.bindGroup(renderPass, 0);

        // idem for group 2 (instances), we will fill in the buffer as we go
        binder.bindGroup(renderPass, 2);

        numRenderables = 0;
        lastTexture = null;     // force a texture bind on the first texture we encounter
    }



    @Override
    public int compareTo(Shader other) {
        if (other == null) return -1;
        if (other == this) return 0;
        return 0; // FIXME compare shaders on their impact on performance
    }

    @Override
    public boolean canRender(Renderable instance) {
        Gdx.app.log("canRender","can render? "+ (instance.meshPart.mesh.getVertexAttributes() == vertexAttributes));
        return instance.meshPart.mesh.getVertexAttributes() == vertexAttributes;
    }



    public void render (Renderable renderable) {
        if(numRenderables > config.maxInstances) {
            Gdx.app.error("WebGPUModelBatch", "Too many instances, max is " + config.maxInstances);
            return;
        }
        // renderable-specific data

        // add instance data to instance buffer (instance transform)
        int offset = numRenderables * 16 * Float.BYTES;
        instanceBuffer.set(offset,  renderable.worldTransform);

        // apply renderable's material
        WebGPUTexture texture;
        if(renderable.material.has(TextureAttribute.Diffuse)) {
            TextureAttribute ta = (TextureAttribute) renderable.material.get(TextureAttribute.Diffuse);
            assert ta != null;
            Texture tex = ta.textureDescription.texture;
            texture = (WebGPUTexture)tex;
        } else {
            texture = defaultTexture;
        }
        if(texture != lastTexture) { // avoid unnecessary binds
            binder.setTexture("diffuseTexture", texture.getTextureView());
            binder.setSampler("diffuseSampler", texture.getSampler());
            binder.bindGroup(renderPass, 1);    // material bind group
            lastTexture = texture;
        }

        pipelineSpec.vertexAttributes = renderable.meshPart.mesh.getVertexAttributes();
        WebGPUPipeline pipeline = pipelines.findPipeline(pipelineLayout.getHandle(), pipelineSpec);
        renderPass.setPipeline(pipeline.getHandle());

        final MeshPart meshPart = renderable.meshPart;
        if (!(meshPart.mesh instanceof WebGPUMesh))
            throw new RuntimeException("WebGPUMeshPart supports only WebGPUMesh");
        final WebGPUMesh mesh = (WebGPUMesh) meshPart.mesh;

        // use an instance offset to find the right modelMatrix in the instanceBuffer
        mesh.render(renderPass, meshPart.primitiveType, meshPart.offset, meshPart.size, 1, numRenderables);

        // we can't use this, because meshPart was unmodified and doesn't know about WebGPUMesh
        // and we're not using WebGPUMeshPart because then we need to modify Renderable.
        //renderable.meshPart.render(renderPass);

        numRenderables++;
    }

    public void end(){
        instanceBuffer.flush();
    }

    private WebGPUBindGroupLayout createFrameBindGroupLayout() {
        WebGPUBindGroupLayout layout = new WebGPUBindGroupLayout("ModelBatch bind group layout (frame)");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex, WGPUBufferBindingType.Uniform, 16*Float.BYTES, false);
        layout.end();
        return layout;
    }

    private WebGPUBindGroupLayout createMaterialBindGroupLayout() {
        WebGPUBindGroupLayout layout = new WebGPUBindGroupLayout("ModelBatch bind group layout (material)");
        layout.begin();
        layout.addTexture(1, WGPUShaderStage.Fragment, WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
        layout.addSampler(2, WGPUShaderStage.Fragment, WGPUSamplerBindingType.Filtering );
        layout.end();
        return layout;
    }

    private WebGPUBindGroupLayout createInstancingBindGroupLayout(){
        WebGPUBindGroupLayout layout = new WebGPUBindGroupLayout("ModelBatch Binding Group Layout (instance)");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex , WGPUBufferBindingType.ReadOnlyStorage, 16L *Float.BYTES*config.maxInstances, false);
        layout.end();
        return layout;
    }


    // todo vertex attributes are hardcoded, should use conditional compilation.

    private String getDefaultShaderSource() {
        return "// basic model batch shader\n" +
                "\n" +
                "struct FrameUniforms {\n" +
                "    projectionViewTransform: mat4x4f,\n" +
                "};\n" +
                "struct ModelUniforms {\n" +
                "    modelMatrix: mat4x4f,\n" +
                "};\n" +
                "\n" +
                "@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;\n" +
                "@group(1) @binding(1) var diffuseTexture:        texture_2d<f32>;\n" +
                "@group(1) @binding(2) var diffuseSampler:       sampler;\n" +
                "@group(2) @binding(0) var<storage, read> instances: array<ModelUniforms>;\n"+
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
                "fn vs_main(in: VertexInput, @builtin(instance_index) instance: u32) -> VertexOutput {\n" +
                "   var out: VertexOutput;\n" +
                "\n" +
                "   out.position =  uFrame.projectionViewTransform * instances[instance].modelMatrix * vec4f(in.position, 1.0);\n" +
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

    @Override
    public void dispose() {
        binder.dispose();
        defaultTexture.dispose();
        instanceBuffer.dispose();
        uniformBuffer.dispose();
    }
}
