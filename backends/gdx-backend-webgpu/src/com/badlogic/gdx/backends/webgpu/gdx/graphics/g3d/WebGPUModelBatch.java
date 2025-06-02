package com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUGraphicsBase;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.shaders.WebGPUDefaultShader;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.shaders.WebGPUDefaultShaderProvider;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;

import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.utils.*;


/**
 * Class for 3d rendering, e.g. to render model instances.
 */
public class WebGPUModelBatch implements Disposable {

    private boolean drawing;
    private WebGPURenderPass renderPass;
    private WebGPUDefaultShaderProvider shaderProvider;
    //private WebGPUDefaultShader defaultShader;
    //private WebGPUDefaultShader.Config config;
    private final Array<Renderable> renderables;
    protected final RenderablePool renderablesPool = new RenderablePool();
    private final int maxInstances;
    private Camera camera;

    protected static class RenderablePool extends FlushablePool<Renderable> {
        @Override
        protected Renderable newObject () {
            return new Renderable();
        }

        @Override
        public Renderable obtain () {
            Renderable renderable = super.obtain();
            renderable.environment = null;
            renderable.material = null;
            renderable.meshPart.set("", null, 0, 0, 0);
            renderable.shader = null;
            renderable.userData = null;
            return renderable;
        }
    }

    /** Create a ModelBatch.
     *
     */
    public WebGPUModelBatch() {
        this(1000);
    }

    public WebGPUModelBatch(int maxInstances) {
        drawing = false;
        WebGPUDefaultShader.Config config = new WebGPUDefaultShader.Config();
        this.maxInstances = maxInstances;
        config.maxInstances = maxInstances;

        shaderProvider = new WebGPUDefaultShaderProvider(config);
        //shader = new WebGPUDefaultShader(null, config);
        renderables = new Array<>();
    }

    public boolean isDrawing () {
        return drawing;
    }


    public void begin(final Camera camera) {
        if (drawing)
            throw new RuntimeException("Must end() before begin()");
        drawing = true;
        this.camera = camera;

        WebGPUGraphicsBase gfx = (WebGPUGraphicsBase) Gdx.graphics;
        renderPass = RenderPassBuilder.create(null, gfx.getSamples());

        //shader.begin(camera, renderPass);

        renderables.clear();
    }


    public void render(Renderable renderable){
        renderable.shader = shaderProvider.getShader(renderable);
        renderables.add(renderable);
    }

    public void render (final RenderableProvider renderableProvider) {

        int offset = renderables.size;
        renderableProvider.getRenderables(renderables, renderablesPool);
        for(int i = offset; i < renderables.size; i++){
            Renderable renderable = renderables.get(i);
            renderable.shader = shaderProvider.getShader(renderable);
        }
    }

    public void flush() {
        if(renderables.size > maxInstances)
            throw new ArrayIndexOutOfBoundsException("Too many renderables");

        // todo sort renderables

        WebGPUDefaultShader currentShader = null;
        for(Renderable renderable : renderables) {
            if (currentShader != renderable.shader) {
                if (currentShader != null) currentShader.end();
                currentShader = (WebGPUDefaultShader) renderable.shader;
                currentShader.begin(camera, renderPass);
            }
            currentShader.render(renderable);
        }
        if (currentShader != null) currentShader.end();
        renderablesPool.flush();
        renderables.clear();
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
        shaderProvider.dispose();
    }



}
