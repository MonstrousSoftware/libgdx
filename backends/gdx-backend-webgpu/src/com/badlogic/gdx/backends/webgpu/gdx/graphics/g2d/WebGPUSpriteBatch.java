package com.badlogic.gdx.backends.webgpu.gdx.graphics.g2d;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUGraphicsBase;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.WebGPUShaderProgram;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to render textured rectangles in batches.
 */
public class WebGPUSpriteBatch implements Batch {

    //private final static String DEFAULT_SHADER = "shaders/sprite.wgsl";

    private final static int VERTS_PER_SPRITE = 4;
    private final static int INDICES_PER_SPRITE = 6;

    private final WebGPUGraphicsBase gfx;
    private final WebGPUShaderProgram specificShader;
    private final int maxSprites;
    private boolean drawing;
    private int vertexSize;
    private final ByteBuffer vertexBB;
    private final FloatBuffer vertexData;     // float buffer view on byte buffer
    public int numSprites;
    private final Color tint;
    private float tintPacked;
    private WebGPUVertexBuffer vertexBuffer;
    private WebGPUIndexBuffer indexBuffer;
    private WebGPUUniformBuffer uniformBuffer;
    private final WebGPUBindGroupLayout bindGroupLayout;
    private VertexAttributes vertexAttributes;
    private final VertexAttributes defaultVertexAttributes;
    private final WebGPUPipelineLayout pipelineLayout;
    private final PipelineSpecification pipelineSpec;
    private int uniformBufferSize;
    private WebGPUTexture lastTexture;
    private final Matrix4 projectionMatrix;
    private final Matrix4 transformMatrix;
    private final Matrix4 combinedMatrix;
    private WebGPURenderPass renderPass;
    private int vbOffset;
    private final PipelineCache pipelines;
    private WebGPUPipeline prevPipeline;
    public int maxSpritesInBatch;    // most nr of sprites in the batch over its lifetime
    public int renderCalls;
    public int pipelineCount;
    private float invTexWidth;
    private float invTexHeight;
    private final Map<Integer, WGPUBlendFactor> blendConstantMap = new HashMap<>(); // mapping GL vs WebGPU constants
    private final Map<WGPUBlendFactor, Integer> blendGLConstantMap = new HashMap<>(); // vice versa

    public WebGPUSpriteBatch() {
        this(10000); // default nr
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
        gfx = (WebGPUGraphicsBase) Gdx.graphics;

        this.maxSprites = maxSprites;
        this.specificShader = specificShader;

        drawing = false;

        vertexAttributes = new VertexAttributes(new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                VertexAttribute.ColorPacked(), VertexAttribute.TexCoords(0) );
        defaultVertexAttributes = vertexAttributes;

        // vertex: x, y, u, v, rgba
        vertexSize = vertexAttributes.vertexSize; // bytes

        initBlendMap(); // fill constants mapping table

        // allocate data buffers based on default vertex attributes which are assumed to be the worst case.
        // i.e. with setVertexAttributes() you can specify a subset
        createBuffers();
        fillIndexBuffer(maxSprites);


        // Create FloatBuffer to hold vertex data per batch, is reset every flush
        vertexBB = BufferUtils.newUnsafeByteBuffer(maxSprites * VERTS_PER_SPRITE * vertexSize);
        //vertexBB = ByteBuffer.allocateDirect(maxSprites * VERTS_PER_SPRITE * vertexSize);   // 4 vertices per sprite
        vertexBB.order(ByteOrder.nativeOrder());  // important
        vertexData = vertexBB.asFloatBuffer();

        projectionMatrix = new Matrix4();
        transformMatrix = new Matrix4();
        combinedMatrix = new Matrix4();

        tint = new Color(Color.WHITE);

        invTexWidth = 0f;
        invTexHeight = 0f;

        bindGroupLayout = createBindGroupLayout();
        pipelineLayout = new WebGPUPipelineLayout("SpriteBatch pipeline layout", bindGroupLayout);

        pipelines = new PipelineCache();
        pipelineSpec = new PipelineSpecification(vertexAttributes, this.specificShader);
        pipelineSpec.name = "SpriteBatch pipeline";

        // default blending values
        pipelineSpec.enableBlending();
        pipelineSpec.setBlendFactor(WGPUBlendFactor.SrcAlpha, WGPUBlendFactor.OneMinusSrcAlpha);
    }

    // the index buffer is fixed and only has to be filled on start-up
    private void fillIndexBuffer(int maxSprites){
        ByteBuffer bb = BufferUtils.newUnsafeByteBuffer(maxSprites*INDICES_PER_SPRITE*Short.BYTES);
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
        indexBuffer.setIndices(bb);
        BufferUtils.disposeUnsafeByteBuffer(bb);
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

    @Override
    public void setPackedColor (float packedColor) {
        Color.abgr8888ToColor(tint, packedColor);
        this.tintPacked = packedColor;
    }

    @Override
    public float getPackedColor () {
        return tintPacked;
    }


    public void setBlendFactor(WGPUBlendFactor srcFunc, WGPUBlendFactor dstFunc) {
        pipelineSpec.setBlendFactorSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
    }

    public void setBlendFactorSeparate(WGPUBlendFactor srcFuncColor, WGPUBlendFactor dstFuncColor, WGPUBlendFactor srcFuncAlpha, WGPUBlendFactor dstFuncAlpha) {
        if (pipelineSpec.getBlendSrcFactor() == srcFuncColor && pipelineSpec.getBlendDstFactor() == dstFuncColor &&
            pipelineSpec.getBlendSrcFactorAlpha() == srcFuncAlpha && pipelineSpec.getBlendDstFactorAlpha() == dstFuncAlpha )
            return;

        flush();
        pipelineSpec.setBlendFactorSeparate(srcFuncColor, dstFuncColor, srcFuncAlpha, dstFuncAlpha);
        setPipeline();
    }


    public WGPUBlendFactor getBlendSrcFactor() {
        return pipelineSpec.getBlendSrcFactor();
    }

    public WGPUBlendFactor getBlendDstFactor() {
        return pipelineSpec.getBlendDstFactor();
    }

    public WGPUBlendFactor getBlendSrcFactorAlpha() {
        return pipelineSpec.getBlendSrcFactorAlpha();
    }

    public WGPUBlendFactor getBlendDstFactorAlpha() {
        return pipelineSpec.getBlendDstFactorAlpha();
    }

    // for compatibility with GL based methods
    public void setBlendFunction(int srcFunc, int dstFunc) {
        setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
    }

    public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
        WGPUBlendFactor srcFactorColor = blendConstantMap.get(srcFuncColor);
        WGPUBlendFactor dstFactorColor = blendConstantMap.get(dstFuncColor);
        WGPUBlendFactor srcFactorAlpha = blendConstantMap.get(srcFuncAlpha);
        WGPUBlendFactor dstFactorAlpha = blendConstantMap.get(dstFuncAlpha);
        if (pipelineSpec.getBlendSrcFactor() == srcFactorColor && pipelineSpec.getBlendDstFactor() == dstFactorColor &&
                pipelineSpec.getBlendSrcFactorAlpha() == srcFactorAlpha && pipelineSpec.getBlendDstFactorAlpha() == dstFactorAlpha )
            return;

        flush();
        pipelineSpec.setBlendFactorSeparate(srcFactorColor, dstFactorColor, srcFactorAlpha, dstFactorAlpha);
        setPipeline();
    }


    public int getBlendSrcFunc() {
        return blendGLConstantMap.get(pipelineSpec.getBlendSrcFactor());
    }


    public int getBlendDstFunc() {
        return blendGLConstantMap.get(pipelineSpec.getBlendDstFactor());
    }


    public int getBlendSrcFuncAlpha() {
        return blendGLConstantMap.get(pipelineSpec.getBlendSrcFactorAlpha());
    }

    public int getBlendDstFuncAlpha() {
        return blendGLConstantMap.get(pipelineSpec.getBlendDstFactorAlpha());
    }

    public void enableBlending(){
        if(pipelineSpec.isBlendingEnabled())
            return;
        flush();
        pipelineSpec.enableBlending();
    }
    public void disableBlending(){
        if(!pipelineSpec.isBlendingEnabled())
            return;
        flush();
        pipelineSpec.disableBlending();
    }

    public boolean isBlendingEnabled(){
        return pipelineSpec.isBlendingEnabled();
    }

    // note: this might invalidate the vertex buffer layout
    public void setVertexAttributes(VertexAttributes vattr){
        if (!drawing) // catch incorrect usage
            throw new RuntimeException("Call begin() before calling setVertexAttributes().");
        flush();
        vertexAttributes = vattr;
        vertexSize = vertexAttributes.vertexSize; // bytes
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
        renderPass = RenderPassBuilder.create(clearColor, gfx.getSamples());

        if (drawing)
            throw new RuntimeException("Must end() before begin()");
        drawing = true;
        numSprites = 0;
        vbOffset = 0;
        vertexData.clear();
        vertexAttributes = defaultVertexAttributes;
        vertexSize = vertexAttributes.vertexSize; // bytes
        maxSpritesInBatch = 0;
        renderCalls = 0;

        prevPipeline = null;

        // set default state
        tint.set(Color.WHITE);

        pipelineSpec.enableBlending();
        pipelineSpec.disableDepthTest();
        pipelineSpec.shader = specificShader;
        if(specificShader == null)
            pipelineSpec.shaderSource = getDefaultShaderSource();
        pipelineSpec.vertexAttributes = vertexAttributes;
        pipelineSpec.numSamples = gfx.getSamples();

        projectionMatrix.setToOrtho(0f, Gdx.graphics.getWidth(), 0f, Gdx.graphics.getHeight(), -1f, 1f);
        transformMatrix.idt();
        setupMatrices();
    }

    protected void switchTexture (Texture texture) {
        flush();
        if(!(texture instanceof WebGPUTexture))
            throw new IllegalArgumentException("texture must be WebGPUTexture");
        lastTexture = (WebGPUTexture)texture;
        invTexWidth = 1.0f / texture.getWidth();
        invTexHeight = 1.0f / texture.getHeight();
    }

    public void flush() {
        if(numSprites == 0)
            return;
        if(numSprites > maxSpritesInBatch)
            maxSpritesInBatch = numSprites;
        renderCalls++;

        setPipeline();

        // bind texture
        WebGPUBindGroup bg = makeBindGroup(bindGroupLayout, uniformBuffer, lastTexture);
        renderPass.setBindGroup( 0, bg.getHandle(), 0, JavaWebGPU.createNullPointer());

        // append new vertex data to GPU vertex buffer
        int numBytes = numSprites * VERTS_PER_SPRITE * vertexSize;
        vertexBuffer.setVertices(vertexBB, vbOffset, numBytes);

        // Set vertex buffer while encoding the render pass
        // use an offset to set the vertex buffer for this batch
        renderPass.setVertexBuffer( 0, vertexBuffer.getHandle(), vbOffset, numBytes);
        renderPass.setIndexBuffer( indexBuffer.getHandle(), WGPUIndexFormat.Uint16, 0, (long) numSprites *6*Short.BYTES);

        renderPass.drawIndexed( numSprites *6, 1, 0, 0, 0);

        bg.dispose();

        vbOffset += numBytes;
        vertexData.clear(); // reset fill position for next batch
        numSprites = 0;   // reset
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



    @Override
    public Matrix4 getProjectionMatrix() {
        return projectionMatrix;
    }

    @Override
    public Matrix4 getTransformMatrix() {
        return transformMatrix;
    }

    @Override
    public void setProjectionMatrix(Matrix4 projection) {
        if(drawing)
            flush();
        projectionMatrix.set(projection);
        setupMatrices();
    }

    @Override
    public void setTransformMatrix(Matrix4 transform) {
        if(drawing)
            flush();
        transformMatrix.set(transform);
        setupMatrices();
    }

    @Override
    public void setShader(ShaderProgram shader) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public ShaderProgram getShader() {
        return null;
    }

    public void draw(Texture texture, float x, float y) {
        draw(texture, x, y, texture.getWidth(), texture.getHeight());
    }

    public void draw(Texture texture, float x, float y, float w, float h){
        this.draw(texture, x, y, w, h, 0f, 1f, 1f, 0f);
    }

    public void draw(TextureRegion region, float x, float y){
        // note: v2 is top of glyph, v the bottom
        this.draw(region.getTexture(), x, y, region.getRegionWidth(), region.getRegionHeight(), region.getU(), region.getV2(), region.getU2(), region.getV());
    }

    public void draw(TextureRegion region, float x, float y, float w, float h){
        this.draw(region.getTexture(), x, y, w, h, region.getU(), region.getV(), region.getU2(), region.getV2());
    }


    public void draw (Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        if (!drawing)
            throw new RuntimeException("SpriteBatch: Must call begin() before draw().");

        if(numSprites == maxSprites)
            throw new RuntimeException("WebGPUSpriteBatch: Too many sprites. Enlarge maxSprites.");

        if(texture != lastTexture) { // changing texture, need to flush what we have so far
            switchTexture(texture);
        }
        addRect(x, y, width, height, u, v, u2, v2);
        numSprites++;
    }


    public void draw (Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX,
                      float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        if(numSprites == maxSprites)
            throw new RuntimeException("WebGPUSpriteBatch: Too many sprites. Enlarge maxSprites.");

        if(texture != lastTexture) { // changing texture, need to flush what we have so far
            switchTexture(texture);
        }

//        if (idx == vertices.length) //
//            flush();

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        if (scaleX != 1 || scaleY != 1) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        if (rotation != 0) {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;

            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;

            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;

            x2 = p2x;
            y2 = p2y;

            x3 = p3x;
            y3 = p3y;

            x4 = p4x;
            y4 = p4y;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        float u = srcX * invTexWidth;
        float v = (srcY + srcHeight) * invTexHeight;
        float u2 = (srcX + srcWidth) * invTexWidth;
        float v2 = srcY * invTexHeight;

        if (flipX) {
            float tmp = u;
            u = u2;
            u2 = tmp;
        }

        if (flipY) {
            float tmp = v;
            v = v2;
            v2 = tmp;
        }
        addVertex(x1, y1, u, v);
        addVertex(x2, y2, u, v2);
        addVertex(x3, y3, u2, v2);
        addVertex(x4, y4, u2, v);
    }



    public void draw (Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth,
                      int srcHeight, boolean flipX, boolean flipY) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        if(numSprites == maxSprites)
            throw new RuntimeException("WebGPUSpriteBatch: Too many sprites. Enlarge maxSprites.");

        if (texture != lastTexture)
            switchTexture(texture);

        float u = srcX * invTexWidth;
        float v = (srcY + srcHeight) * invTexHeight;
        float u2 = (srcX + srcWidth) * invTexWidth;
        float v2 = srcY * invTexHeight;
        final float fx2 = x + width;
        final float fy2 = y + height;

        if (flipX) {
            float tmp = u;
            u = u2;
            u2 = tmp;
        }

        if (flipY) {
            float tmp = v;
            v = v2;
            v2 = tmp;
        }

        addVertex(x, y, u, v);
        addVertex(x, fy2, u, v2);
        addVertex(fx2,fy2, u2, v2);
        addVertex(fx2, y, u2, v);
    }


    public void draw (Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        if(numSprites == maxSprites)
            throw new RuntimeException("WebGPUSpriteBatch: Too many sprites. Enlarge maxSprites.");
        if (texture != lastTexture)
            switchTexture(texture);

        final float u = srcX * invTexWidth;
        final float v = (srcY + srcHeight) * invTexHeight;
        final float u2 = (srcX + srcWidth) * invTexWidth;
        final float v2 = srcY * invTexHeight;
        final float fx2 = x + srcWidth;
        final float fy2 = y + srcHeight;

        addVertex(x, y, u, v);
        addVertex(x, fy2, u, v2);
        addVertex(fx2,fy2, u2, v2);
        addVertex(fx2, y, u2, v);
    }


//    public void draw (Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
//        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
//
//        if (texture != lastTexture)
//            switchTexture(texture);
//        final float fx2 = x + width;
//        final float fy2 = y + height;
//
//        addVertex(x, y, u, v);
//        addVertex(x, fy2, u, v2);
//        addVertex(fx2,fy2, u2, v2);
//        addVertex(fx2, y, u2, v);
//    }

//    @Override
//    public void draw (Texture texture, float x, float y) {
//        draw(texture, x, y, texture.getWidth(), texture.getHeight());
//    }

    // used by Sprite class and BitmapFont
    public void draw(Texture texture, float[] vertices, int offset, int numFloats){
        if (!drawing)
            throw new RuntimeException("SpriteBatch: Must call begin() before draw().");
        if(numSprites == maxSprites)
            throw new RuntimeException("WebGPUSpriteBatch: Too many sprites. Enlarge maxSprites.");

        if(texture != lastTexture) { // changing texture, need to flush what we have so far
            switchTexture(texture);
        }
        int remaining = 20*(maxSprites - numSprites);
        if(numFloats > remaining)   // avoid buffer overflow by truncating as needed
            numFloats = remaining;
        vertexData.put(vertices, offset, numFloats);
        numSprites += numFloats/20;
    }

//    public void draw (Texture texture, float[] spriteVertices, int offset, int count) {
//        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
//
//        int verticesLength = vertices.length;
//        int remainingVertices = verticesLength;
//        if (texture != lastTexture)
//            switchTexture(texture);
//        else {
//            remainingVertices -= idx;
//            if (remainingVertices == 0) {
//                flush();
//                remainingVertices = verticesLength;
//            }
//        }
//        int copyCount = Math.min(remainingVertices, count);
//
//        System.arraycopy(spriteVertices, offset, vertices, idx, copyCount);
//        idx += copyCount;
//        count -= copyCount;
//        while (count > 0) {
//            offset += copyCount;
//            flush();
//            copyCount = Math.min(verticesLength, count);
//            System.arraycopy(spriteVertices, offset, vertices, 0, copyCount);
//            idx += copyCount;
//            count -= copyCount;
//        }
//    }



    public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                      float scaleX, float scaleY, float rotation) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        if(numSprites == maxSprites)
            throw new RuntimeException("WebGPUSpriteBatch: Too many sprites. Enlarge maxSprites.");
        Texture texture = region.getTexture();
        if (texture != lastTexture)
            switchTexture(texture);

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        if (scaleX != 1 || scaleY != 1) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        if (rotation != 0) {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;

            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;

            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;

            x2 = p2x;
            y2 = p2y;

            x3 = p3x;
            y3 = p3y;

            x4 = p4x;
            y4 = p4y;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        addVertex(x1, y1, u, v);
        addVertex(x2, y2, u, v2);
        addVertex(x3, y3, u2, v2);
        addVertex(x4, y4, u2, v);

    }


    public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                      float scaleX, float scaleY, float rotation, boolean clockwise) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        if(numSprites == maxSprites)
            throw new RuntimeException("WebGPUSpriteBatch: Too many sprites. Enlarge maxSprites.");
        Texture texture = region.getTexture();
        if (texture != lastTexture)
            switchTexture(texture);

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        if (scaleX != 1 || scaleY != 1) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        if (rotation != 0) {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;

            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;

            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;

            x2 = p2x;
            y2 = p2y;

            x3 = p3x;
            y3 = p3y;

            x4 = p4x;
            y4 = p4y;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        float u1, v1, u2, v2, u3, v3, u4, v4;
        if (clockwise) {
            u1 = region.getU2();
            v1 = region.getV2();
            u2 = region.getU();
            v2 = region.getV2();
            u3 = region.getU();
            v3 = region.getV();
            u4 = region.getU2();
            v4 = region.getV();
        } else {
            u1 = region.getU();
            v1 = region.getV();
            u2 = region.getU2();
            v2 = region.getV();
            u3 = region.getU2();
            v3 = region.getV2();
            u4 = region.getU();
            v4 = region.getV2();
        }

        addVertex(x1, y1, u1, v1);
        addVertex(x2, y2, u2, v2);
        addVertex(x3, y3, u3, v3);
        addVertex(x4, y4, u4, v4);
    }


    public void draw (TextureRegion region, float width, float height, Affine2 transform) {
        if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        if(numSprites == maxSprites)
            throw new RuntimeException("WebGPUSpriteBatch: Too many sprites. Enlarge maxSprites.");
        Texture texture = region.getTexture();
        if (texture != lastTexture)
            switchTexture(texture);

        // construct corner points
        float x1 = transform.m02;
        float y1 = transform.m12;
        float x2 = transform.m01 * height + transform.m02;
        float y2 = transform.m11 * height + transform.m12;
        float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
        float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
        float x4 = transform.m00 * width + transform.m02;
        float y4 = transform.m10 * width + transform.m12;

        float u = region.getU();
        float v = region.getV2();
        float u2 = region.getU2();
        float v2 = region.getV();

        addVertex(x1, y1, u, v);
        addVertex(x2, y2, u, v2);
        addVertex(x3, y3, u2, v2);
        addVertex(x4, y4, u2, v);
    }




    private void addRect(float x, float y, float w, float h, float u, float v, float u2, float v2) {
        addVertex(x, y, u, v);
        addVertex(x, y+h, u, v2);
        addVertex(x+w, y+h, u2, v2);
        addVertex(x+w, y, u2, v);
    }

    private void addVertex(float x, float y, float u, float v) {
        boolean hasColor = (vertexAttributes.getMask() & VertexAttributes.Usage.ColorPacked) != 0;
        boolean hasUV = (vertexAttributes.getMask() & VertexAttributes.Usage.TextureCoordinates) != 0;
        float col = tint.toFloatBits();

//        vertices[idx++] = x;
//        vertices[idx++] = y;
//        if (hasColor) {
//            vertices[idx++] = col;
//        }
//        if (hasUV) {
//            vertices[idx++] = u;
//            vertices[idx++] = v;
//        }


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
        int indexSize = maxSprites * INDICES_PER_SPRITE * Short.BYTES;

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


    @Override
    public void dispose(){
        pipelines.dispose();
        vertexBuffer.dispose();
        indexBuffer.dispose();
        uniformBuffer.dispose();
        bindGroupLayout.dispose();
        pipelineLayout.dispose();
        BufferUtils.disposeUnsafeByteBuffer(vertexBB);
    }


    private String getDefaultShaderSource() {
        return "\n" +
                "struct Uniforms {\n" +
                "    projectionMatrix: mat4x4f,\n" +
                "};\n" +
                "\n" +
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



    private void initBlendMap(){
        blendConstantMap.put(GL20.GL_ZERO, WGPUBlendFactor.Zero);
        blendConstantMap.put(GL20.GL_ONE, WGPUBlendFactor.One);
        blendConstantMap.put(GL20.GL_SRC_ALPHA, WGPUBlendFactor.SrcAlpha);
        blendConstantMap.put(GL20.GL_ONE_MINUS_SRC_ALPHA, WGPUBlendFactor.OneMinusSrcAlpha);
        blendConstantMap.put(GL20.GL_DST_ALPHA, WGPUBlendFactor.DstAlpha);
        blendConstantMap.put(GL20.GL_ONE_MINUS_DST_ALPHA, WGPUBlendFactor.OneMinusDstAlpha);
        blendConstantMap.put(GL20.GL_SRC_COLOR, WGPUBlendFactor.Src);
        blendConstantMap.put(GL20.GL_ONE_MINUS_SRC_COLOR, WGPUBlendFactor.OneMinusSrc);
        blendConstantMap.put(GL20.GL_DST_COLOR, WGPUBlendFactor.Dst);
        blendConstantMap.put(GL20.GL_ONE_MINUS_DST_COLOR, WGPUBlendFactor.OneMinusDst);
        blendConstantMap.put(GL20.GL_SRC_ALPHA_SATURATE, WGPUBlendFactor.SrcAlphaSaturated);

        // and build the inverse mapping
        for(int key : blendConstantMap.keySet()){
            WGPUBlendFactor factor = blendConstantMap.get(key);
            blendGLConstantMap.put(factor, key);
        }
    }
}
