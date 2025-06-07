package com.badlogic.gdx.webgpu.graphics.g3d.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.webgpu.graphics.Binder;
import com.badlogic.gdx.webgpu.graphics.WebGPUMesh;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.webgpu.webgpu.*;
import com.badlogic.gdx.webgpu.wrappers.*;


import static com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888;

public class WebGPUDefaultShader implements Shader {

    private final Config config;
    private static String defaultShader;
    private final WebGPUTexture defaultTexture;
    public final Binder binder;
    private final WebGPUUniformBuffer uniformBuffer;
    private final int uniformBufferSize;
    private final WebGPUUniformBuffer instanceBuffer;
    private final WebGPUPipeline pipeline;            // a shader has one pipeline
    public int numRenderables;
    private WebGPUTexture lastTexture;
    private WebGPURenderPass renderPass;
    private final VertexAttributes vertexAttributes;

    protected int numDirectionalLights;
    //protected int maxDirectionalLights;
    protected DirectionalLight[] directionalLights;


    public static class Config {
        public int maxInstances;
        public int maxDirectionalLights;

        public Config() {
            this.maxInstances = 1024;
            this.maxDirectionalLights = 3;  // todo hard coded in shader, don't change
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

        // Create uniform buffer for the projection matrix
        uniformBufferSize = (16 + 4 + 4 + 8*config.maxDirectionalLights)* Float.BYTES;
        uniformBuffer = new WebGPUUniformBuffer(uniformBufferSize, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform);


        binder = new Binder();
        // define groups
        binder.defineGroup(0, createFrameBindGroupLayout(uniformBufferSize));
        binder.defineGroup(1, createMaterialBindGroupLayout());
        binder.defineGroup(2, createInstancingBindGroupLayout());
        // define bindings in the groups
        binder.defineUniform("uniforms", 0, 0);
        binder.defineUniform("diffuseTexture", 1, 1);
        binder.defineUniform("diffuseSampler", 1, 2);
        binder.defineUniform("instanceUniforms", 2, 0);
        // define uniforms in uniform buffers with their offset
        // frame uniforms
        int offset = 0;
        binder.defineUniform("projectionViewTransform", 0, 0, offset); offset += 16*4;
        for(int i = 0; i < config.maxDirectionalLights; i++) {
            binder.defineUniform("dirLight["+i+"].color", 0, 0, offset);
            offset += 4 * 4;
            binder.defineUniform("dirLight["+i+"].direction", 0, 0, offset);
            offset += 4 * 4;
        }
        binder.defineUniform("ambientLight", 0, 0, offset); offset += 4*4;
        binder.defineUniform("numDirectionalLights", 0, 0, offset); offset += 4;

        // note: put shorter uniforms last for padding reasons

        System.out.println("offset:"+offset+" "+uniformBufferSize);
        if(offset > uniformBufferSize) throw new RuntimeException("Mismatch in frame uniform buffer size");
        //binder.defineUniform("modelMatrix", 2, 0, 0);


        // set binding 0 to uniform buffer
        binder.setBuffer("uniforms", uniformBuffer, 0, uniformBufferSize);

        int instanceSize = 16*Float.BYTES;      // data size per instance

        // for now we use a uniform buffer, but we organize data as an array of modelMatrix
        // and write with an dynamic offset (numRenderables * sizeof matrix4)
        instanceBuffer = new WebGPUUniformBuffer(instanceSize*config.maxInstances, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Storage);

        binder.setBuffer("instanceUniforms", instanceBuffer, 0, (long) instanceSize *config.maxInstances);


        // get pipeline layout which aggregates all the bind group layouts
        WebGPUPipelineLayout pipelineLayout = binder.getPipelineLayout("ModelBatch pipeline layout");

        //pipelines = new PipelineCache();    // use static cache?

        // vertexAttributes will be set from the renderable
        vertexAttributes = renderable.meshPart.mesh.getVertexAttributes();
        PipelineSpecification pipelineSpec = new PipelineSpecification(vertexAttributes, getDefaultShaderSource());
        pipelineSpec.name = "ModelBatch pipeline";

        // default blending values
        pipelineSpec.enableBlending();
        pipelineSpec.setBlendFactor(WGPUBlendFactor.SrcAlpha, WGPUBlendFactor.OneMinusSrcAlpha);
        pipeline = new WebGPUPipeline(pipelineLayout, pipelineSpec);

        directionalLights = new DirectionalLight[config.maxDirectionalLights];
        for(int i = 0; i <config.maxDirectionalLights; i++)
            directionalLights[i] = new DirectionalLight();
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

        renderPass.setPipeline(pipeline.getHandle());
    }



    @Override
    public int compareTo(Shader other) {
        if (other == null) return -1;
        if (other == this) return 0;
        return 0; // FIXME compare shaders on their impact on performance
    }

    @Override
    public boolean canRender(Renderable instance) {
        return instance.meshPart.mesh.getVertexAttributes().getMask() == vertexAttributes.getMask();
    }

    private Attributes combinedAttributes = new Attributes();


    public void render (Renderable renderable) {
        if (renderable.worldTransform.det3x3() == 0) return;
        combinedAttributes.clear();
        if (renderable.environment != null) combinedAttributes.set(renderable.environment);
        if (renderable.material != null) combinedAttributes.set(renderable.material);
        render(renderable, combinedAttributes);
    }

    public void render (Renderable renderable, Attributes attributes) {
        if(numRenderables > config.maxInstances) {
            Gdx.app.error("WebGPUModelBatch", "Too many instances, max is " + config.maxInstances);
            return;
        }

        bindLights(renderable, attributes);


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



        final MeshPart meshPart = renderable.meshPart;
        if (!(meshPart.mesh instanceof WebGPUMesh))
            throw new RuntimeException("WebGPUMeshPart supports only WebGPUMesh");
        final WebGPUMesh mesh = (WebGPUMesh) meshPart.mesh;

        // use an instance offset to find the right modelMatrix in the instanceBuffer
        mesh.render(renderPass, meshPart.primitiveType, meshPart.offset, meshPart.size, 1, numRenderables);

        // we can't use the following statement, because meshPart was unmodified and doesn't know about WebGPUMesh
        // and we're not using WebGPUMeshPart because then we need to modify Renderable.
        //renderable.meshPart.render(renderPass);

        numRenderables++;
    }

    public void end(){
        instanceBuffer.flush();
    }

    private WebGPUBindGroupLayout createFrameBindGroupLayout(int uniformBufferSize) {
        WebGPUBindGroupLayout layout = new WebGPUBindGroupLayout("ModelBatch bind group layout (frame)");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex|WGPUShaderStage.Fragment, WGPUBufferBindingType.Uniform, uniformBufferSize, false);
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
        if(defaultShader == null){
            defaultShader = Gdx.files.classpath("shaders/modelbatch.wgsl").readString();
        }
        return defaultShader;

    }

    @Override
    public void dispose() {
        binder.dispose();
        defaultTexture.dispose();
        instanceBuffer.dispose();
        uniformBuffer.dispose();
    }


    private void bindLights(final Renderable renderable, Attributes attributes){
        final Environment lights = renderable.environment;
        final DirectionalLightsAttribute dla = attributes.get(DirectionalLightsAttribute.class, DirectionalLightsAttribute.Type);
        final Array<DirectionalLight> dirs = dla == null ? null : dla.lights;


        if(dirs != null){
            if( dirs.size > config.maxDirectionalLights)
                throw new RuntimeException("Too many directional lights");

            for(int i = 0; i < dirs.size; i++) {
                directionalLights[i].color.set(dirs.get(i).color);
                directionalLights[i].direction.set(dirs.get(i).direction);
            }
        }

        numDirectionalLights = dirs == null ? 0 : dirs.size;
        for(int i = 0; i < numDirectionalLights; i++) {
            // todo probably not so great concatenating strings like this
            binder.setUniform("dirLight["+i+"].color", directionalLights[i].color);
            binder.setUniform("dirLight["+i+"].direction", directionalLights[i].direction);
        }
//        binder.setUniform("dirLight[0].color", directionalLights[0].color);
//        binder.setUniform("dirLight[0].direction", directionalLights[0].direction);
//        binder.setUniform("dirLight[1].color", directionalLights[1].color);
//        binder.setUniform("dirLight[1].direction", directionalLights[1].direction);
        binder.setUniform("numDirectionalLights", numDirectionalLights);

        final ColorAttribute ambient = attributes.get(ColorAttribute.class,ColorAttribute.AmbientLight);
        if(ambient != null)
            binder.setUniform("ambientLight", ambient.color);

    }
}
