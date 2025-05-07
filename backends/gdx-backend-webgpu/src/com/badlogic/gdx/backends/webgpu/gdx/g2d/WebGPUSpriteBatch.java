package com.badlogic.gdx.backends.webgpu.gdx.g2d;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUShaderProgram;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUVertexAttributes;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUVertexAttributes.Usage;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import jnr.ffi.Pointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Class to render textured rectangles in batches.
 */
public class WebGPUSpriteBatch {

    //private final static String DEFAULT_SHADER = "shaders/sprite.wgsl";
    private final WebGPUApplication app;
    private final WebGPU_JNI webGPU;
    private final WebGPUShaderProgram specificShader;
    private final int maxSprites;
    private boolean drawing;
    private int vertexSize;
    private final FloatBuffer vertexData;     // float buffer view on byte buffer
    private final Pointer vertexDataPtr;      // Pointer wrapped around the byte buffer
    private int numRects;
    private final Color tint;
    private float tintPacked;
    private WebGPUVertexBuffer vertexBuffer;
    private WebGPUIndexBuffer indexBuffer;
    private WebGPUUniformBuffer uniformBuffer;
    private final WebGPUBindGroupLayout bindGroupLayout;
    private WebGPUVertexAttributes vertexAttributes;
    private final WebGPUVertexAttributes defaultVertexAttributes;
    private final WebGPUPipelineLayout pipelineLayout;
    private final PipelineSpecification pipelineSpec;
    private int uniformBufferSize;
    private WebGPUTexture texture;
    private Matrix4 projectionMatrix;
    private Matrix4 transformMatrix;
    private Matrix4 combinedMatrix;
    private WebGPURenderPass renderPass;
    private int vbOffset;
    private final PipelineCache pipelines;
    private WebGPUPipeline prevPipeline;
    private boolean blendingEnabled;
    public int maxSpritesInBatch;
    public int renderCalls;
    public int pipelineCount;


    public WebGPUSpriteBatch() {
        this(1000); // default nr
    }

    public WebGPUSpriteBatch(int maxSprites) {
        this(maxSprites, null);
    }

    /** Create a SpriteBatch.
     *
     * @param maxSprites        maximum number of sprite to be supported (default is 1000)
     * @param specificShader    specific ShaderProgram to use, must be compatible with "sprite.wgsl". Leave null to use the default shader.
     */
    public WebGPUSpriteBatch(int maxSprites, WebGPUShaderProgram specificShader) {
        app = (WebGPUApplication) Gdx.app;
        webGPU = app.getWebGPU();

        this.maxSprites = maxSprites;
        this.specificShader = specificShader;

        drawing = false;

        vertexAttributes = new WebGPUVertexAttributes(Usage.POSITION_2D|Usage.TEXTURE_COORDINATE|Usage.COLOR_PACKED);
        defaultVertexAttributes = vertexAttributes;

        // vertex: x, y, u, v, rgba
        vertexSize = vertexAttributes.getVertexSizeInBytes(); // bytes

        // allocate data buffers based on default vertex attributes which are assumed to be the worst case.
        // i.e. with setVertexAttributes() you can specify a subset
        createBuffers();
        fillIndexBuffer();

        ByteBuffer vertexBB = ByteBuffer.allocateDirect(maxSprites * 4 * vertexSize);
        vertexBB.order(ByteOrder.nativeOrder());  // important
        vertexData = vertexBB.asFloatBuffer();
        vertexDataPtr = Pointer.wrap(JavaWebGPU.getRuntime(), vertexBB);

        projectionMatrix = new Matrix4();
        transformMatrix = new Matrix4();
        combinedMatrix = new Matrix4();

        tint = new Color(Color.WHITE);

        bindGroupLayout = createBindGroupLayout();
        pipelineLayout = new WebGPUPipelineLayout("SpriteBatch pipeline layout", bindGroupLayout);

        pipelines = new PipelineCache();
        pipelineSpec = new PipelineSpecification(vertexAttributes, this.specificShader);
        pipelineSpec.name = "SpriteBatch pipeline";
    }

    // the index buffer is fixed and only has to be filled on start-up
    private void fillIndexBuffer(){
        ByteBuffer bb = ByteBuffer.allocateDirect(maxSprites*6*Short.BYTES);
        bb.order(ByteOrder.nativeOrder());  // important
        ShortBuffer indexData = bb.asShortBuffer();
        for(int i = 0; i < maxSprites; i++){
            short vertexOffset = (short)(i * 4);
            // two triangles per sprite
            indexData.put(vertexOffset);
            indexData.put((short)(vertexOffset + 1));
            indexData.put((short)(vertexOffset + 2));

            indexData.put(vertexOffset);
            indexData.put((short)(vertexOffset + 2));
            indexData.put((short)(vertexOffset + 3));
        }
        indexData.flip();
        Pointer indexDataPtr = Pointer.wrap(JavaWebGPU.getRuntime(), bb);
        webGPU.wgpuQueueWriteBuffer(app.getQueue().getHandle(), indexBuffer.getHandle(), 0, indexDataPtr, (long) maxSprites *6*Short.BYTES);
    }


    public void setColor(float r, float g, float b, float a){
        tint.set(r,g,b,a);
    }

    public void setColor(Color color){
        tint.set(color);
    }

    public Color getColor() {
        return tint;
    }

    //@Override
    public void setPackedColor (float packedColor) {
        Color.abgr8888ToColor(tint, packedColor);
        this.tintPacked = packedColor;
    }

    //@Override
    public float getPackedColor () {
        return tintPacked;
    }


    public void enableBlending(){
        if(blendingEnabled)
           return;
        flush();
        blendingEnabled = true;
        pipelineSpec.enableBlending();
        setPipeline();
    }

    public void disableBlending(){
        if(!blendingEnabled)
            return;
        flush();
        blendingEnabled = false;
        pipelineSpec.disableBlending();
        setPipeline();
    }

    public boolean isBlendingEnabled(){
        return blendingEnabled;
    }

    public void setVertexAttributes(WebGPUVertexAttributes vattr){
        if (!drawing) // catch incorrect usage
            throw new RuntimeException("Call begin() before calling setVertexAttributes().");
        flush();
        vertexAttributes = vattr;
        vertexSize = vertexAttributes.getVertexSizeInBytes(); // bytes
        pipelineSpec.vertexAttributes = vattr;
        pipelineSpec.shader = null;     // force recompile of shader
        setPipeline();
    }
    public boolean isDrawing () {
        return drawing;
    }

    public void begin(){
        begin(null);
    }

    public void begin(Color clearColor) {

        renderPass = RenderPassBuilder.create(clearColor, app.getConfiguration().samples);

        if (drawing)
            throw new RuntimeException("Must end() before begin()");
        drawing = true;
        numRects = 0;
        vbOffset = 0;
        vertexData.clear();
        vertexAttributes = defaultVertexAttributes;
        vertexSize = vertexAttributes.getVertexSizeInBytes(); // bytes
        maxSpritesInBatch = 0;
        renderCalls = 0;

        prevPipeline = null;

        // set default state
        tint.set(Color.WHITE);
        blendingEnabled = true;
        pipelineSpec.enableBlending();
        pipelineSpec.disableDepthTest();
        pipelineSpec.shader = specificShader;
        if(specificShader == null)
            pipelineSpec.shaderSource = getDefaultShaderSource();
        pipelineSpec.vertexAttributes = vertexAttributes;
        pipelineSpec.numSamples = app.getConfiguration().samples;

        projectionMatrix.setToOrtho(0f, Gdx.graphics.getWidth(), 0f, Gdx.graphics.getHeight(), -1f, 1f);
        transformMatrix.idt();
        setupMatrices();
    }

    public void flush() {
        if(numRects == 0)
            return;
        if(numRects > maxSpritesInBatch)
            maxSpritesInBatch = numRects;
        renderCalls++;

        setPipeline();

        // Add number of vertices to the GPU's vertex buffer
        //
        int numBytes = numRects * 4 * vertexSize;

        // append new vertex data to GPU vertex buffer
        app.getQueue().writeBuffer(vertexBuffer, vbOffset, vertexDataPtr, numBytes);

        // bind texture
        WebGPUBindGroup bg = makeBindGroup(bindGroupLayout, uniformBuffer, texture);

        // Set vertex buffer while encoding the render pass
        // use an offset to set the vertex buffer for this batch
        renderPass.setVertexBuffer( 0, vertexBuffer.getHandle(), vbOffset, numBytes);
        renderPass.setIndexBuffer( indexBuffer.getHandle(), WGPUIndexFormat.Uint16, 0, (long)numRects*6*Short.BYTES);

        renderPass.setBindGroup( 0, bg.getHandle(), 0, JavaWebGPU.createNullPointer());


        renderPass.drawIndexed( numRects*6, 1, 0, 0, 0);

        bg.dispose();

        vbOffset += numBytes;

        vertexData.clear(); // reset fill position for next batch
        numRects = 0;   // reset
    }

    public void end() {
        if (!drawing) // catch incorrect usage
            throw new RuntimeException("Cannot end() without begin()");
        drawing = false;
        flush();
        renderPass.end();
        renderPass = null;
        pipelineCount = pipelines.size();   // statistics
    }

    // create or reuse pipeline on demand to match the pipeline spec
    private void setPipeline() {
        WebGPUPipeline pipeline = pipelines.findPipeline( pipelineLayout.getHandle(), pipelineSpec);
        if (pipeline != prevPipeline) { // avoid unneeded switches
            renderPass.setPipeline(pipeline.getHandle());
            prevPipeline = pipeline;
        }
    }

    public void setShader(WebGPUShaderProgram shaderProgram) {
        flush();
        if (shaderProgram == null) {
            pipelineSpec.shader = specificShader;
            if (specificShader == null)
                pipelineSpec.shaderSource = getDefaultShaderSource();
            pipelineSpec.recalcHash();
        }
        else {
            pipelineSpec.shader = shaderProgram;
            pipelineSpec.shaderSource = "precompiled"; //shaderProgram.getName();   // todo
            pipelineSpec.recalcHash();
        }

        setPipeline();  // probably not needed because flush() will do it again
    }


    //@Override
    public Matrix4 getProjectionMatrix() {
        return projectionMatrix;
    }

   // @Override
    public Matrix4 getTransformMatrix() {
        return transformMatrix;
    }

   // @Override
    public void setProjectionMatrix(Matrix4 projection) {
        if(drawing)
            flush();
        projectionMatrix.set(projection);
        setupMatrices();
    }

  //  @Override
    public void setTransformMatrix(Matrix4 transform) {
        if(drawing)
            flush();
        transformMatrix.set(transform);
        setupMatrices();
    }

    public void draw(Texture texture, float x, float y) {
        draw(texture, x, y, texture.getWidth(), texture.getHeight());
    }

    public void draw(Texture texture, float x, float y, float w, float h){
        this.draw(texture, x, y, w, h, 0f, 1f, 1f, 0f);
    }

//    public void draw(TextureRegion region, float x, float y){
//        // note: v2 is top of glyph, v the bottom
//        this.draw(region.texture, x, y, region.regionWidth, region.regionHeight, region.u, region.v2, region.u2, region.v  );
//    }
//
//    public void draw(TextureRegion region, float x, float y, float w, float h){
//        this.draw(region.texture, x, y, w, h, region.u, region.v, region.u2, region.v2  );
//    }


    public void draw (Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        if(!(texture instanceof WebGPUTexture))
            throw new IllegalArgumentException("texture must be WebGPUTexture");
        if (!drawing)
            throw new RuntimeException("SpriteBatch: Must call begin() before draw().");

        if(numRects == maxSprites)
            throw new RuntimeException("SpriteBatch: Too many sprites.");

        if(texture != this.texture) { // changing texture, need to flush what we have so far
            flush();
            this.texture = (WebGPUTexture)texture;
        }
        addRect(x, y, width, height, u, v, u2, v2);
        numRects++;
    }



    // used by Sprite class
    public void draw(Texture texture, float[] vertices){
        if(!(texture instanceof WebGPUTexture))
            throw new IllegalArgumentException("texture must be WebGPUTexture");
        if(vertices.length != 20)
            throw new IllegalArgumentException("SpriteBatch.draw: vertices must have length 20");
        if (!drawing)
            throw new RuntimeException("SpriteBatch: Must call begin() before draw().");

        if(numRects == maxSprites)
            throw new RuntimeException("SpriteBatch: Too many sprites.");

        if(texture != this.texture) { // changing texture, need to flush what we have so far
            flush();
            this.texture = (WebGPUTexture)texture;
        }
        for(int i = 0; i < vertices.length; i++){
            vertexData.put(vertices[i]);
        }
        numRects++;
    }


    private void addRect(float x, float y, float w, float h, float u, float v, float u2, float v2) {
        addVertex(x, y, u, v);
        addVertex(x, y+h, u, v2);
        addVertex(x+w, y+h, u2, v2);
        addVertex(x+w, y, u2, v);
    }

    private void addVertex(float x, float y, float u, float v) {
        boolean hasColor = vertexAttributes.hasUsage(Usage.COLOR_PACKED);
        boolean hasUV = vertexAttributes.hasUsage(Usage.TEXTURE_COORDINATE);
        float col = tint.toFloatBits();

        vertexData.put(x);
        vertexData.put(y);
        if (hasColor) {
            vertexData.put(col);
        }
        if (hasUV) {
            vertexData.put(u);
            vertexData.put(v);
        }
    }


    private void createBuffers() {

        int indexSize = maxSprites * 6 * Short.BYTES;

        // Create vertex buffer and index buffer
        vertexBuffer = new WebGPUVertexBuffer(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex, (long) maxSprites * 4 * vertexSize);
        indexBuffer = new WebGPUIndexBuffer(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index, indexSize, Short.BYTES);

        // Create uniform buffer for the projection matrix
        uniformBufferSize = 16 * Float.BYTES;
        uniformBuffer = new WebGPUUniformBuffer(uniformBufferSize,WGPUBufferUsage.CopyDst |WGPUBufferUsage.Uniform  );
    }

    private void setupMatrices(){
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        setUniforms();
    }

    private void setUniforms(){
        uniformBuffer.beginFill();
        uniformBuffer.append(combinedMatrix);
        uniformBuffer.endFill();
    }

    private WebGPUBindGroupLayout createBindGroupLayout() {
        WebGPUBindGroupLayout layout = new WebGPUBindGroupLayout("SpriteBatch bind group layout");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex, WGPUBufferBindingType.Uniform, uniformBufferSize, false);
        layout.addTexture(1, WGPUShaderStage.Fragment, WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
        layout.addSampler(2, WGPUShaderStage.Fragment, WGPUSamplerBindingType.Filtering );

        layout.end();
        return layout;
    }


    private WebGPUBindGroup makeBindGroup(WebGPUBindGroupLayout bindGroupLayout, WebGPUBuffer uniformBuffer, WebGPUTexture texture) {
        WebGPUBindGroup bg = new WebGPUBindGroup(bindGroupLayout);
        bg.begin();
        bg.addBuffer(0, uniformBuffer);
        bg.addTexture(1, texture.getTextureView());
        bg.addSampler(2, texture.getSampler());
        bg.end();
        return bg;
    }


    //@Override
    public void dispose(){
        pipelines.dispose();
        vertexBuffer.dispose();
        indexBuffer.dispose();
        uniformBuffer.dispose();
        bindGroupLayout.dispose();
        pipelineLayout.dispose();
    }


    private String getDefaultShaderSource() {
        return "\n" +
                "struct Uniforms {\n" +
                "    projectionMatrix: mat4x4f,\n" +
                "};\n" +
                "\n" +
                "// The memory location of the uniform is given by a pair of a *bind group* and a *binding*\n" +
                "@group(0) @binding(0) var<uniform> uniforms: Uniforms;\n" +
                "@group(0) @binding(1) var texture: texture_2d<f32>;\n" +
                "@group(0) @binding(2) var textureSampler: sampler;\n" +
                "\n" +
                "\n" +
                "struct VertexInput {\n" +
                "    @location(0) position: vec2f,\n" +
                "#ifdef TEXTURE_COORDINATE\n" +
                "    @location(1) uv: vec2f,\n" +
                "#endif\n" +
                "#ifdef COLOR\n" +
                "    @location(5) color: vec4f,\n" +
                "#endif\n" +
                "};\n" +
                "\n" +
                "struct VertexOutput {\n" +
                "    @builtin(position) position: vec4f,\n" +
                "#ifdef TEXTURE_COORDINATE\n" +
                "    @location(0) uv : vec2f,\n" +
                "#endif\n" +
                "    @location(1) color: vec4f,\n" +
                "};\n" +
                "\n" +
                "\n" +
                "@vertex\n" +
                "fn vs_main(in: VertexInput) -> VertexOutput {\n" +
                "   var out: VertexOutput;\n" +
                "\n" +
                "   var pos =  uniforms.projectionMatrix * vec4f(in.position, 0.0, 1.0);\n" +
                "   out.position = pos;\n" +
                "#ifdef TEXTURE_COORDINATE\n" +
                "   out.uv = in.uv;\n" +
                "#endif\n" +
                "\n" +
                "#ifdef COLOR\n" +
                "   let color:vec4f = in.color;\n" +
                "#else\n" +
                "   let color:vec4f = vec4f(1,1,1,1);   // white\n" +
                "#endif\n" +
                "   out.color = color;\n" +
                "\n" +
                "   return out;\n" +
                "}\n" +
                "\n" +
                "@fragment\n" +
                "fn fs_main(in : VertexOutput) -> @location(0) vec4f {\n" +
                "\n" +
                "#ifdef TEXTURE_COORDINATE\n" +
                "    let color = in.color * textureSample(texture, textureSampler, in.uv);\n" +
                "#else\n" +
                "    let color = in.color;\n" +
                "#endif\n" +
                "    return vec4f(color);\n" +
                "}";
    }
}
