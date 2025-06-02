package com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUGraphicsBase;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.shaders.WebGPUDefaultShader;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;

import com.badlogic.gdx.utils.*;


/**
 * Class for 3d rendering, e.g. to render model instances.
 */
public class WebGPUModelBatch implements Disposable {

    private boolean drawing;
    private WebGPURenderPass renderPass;
    private final WebGPUDefaultShader shader;
    private final Array<Renderable> renderables;
    protected final RenderablePool renderablesPool = new RenderablePool();

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
        shader = new WebGPUDefaultShader(maxInstances);
        renderables = new Array<>();
    }

    public boolean isDrawing () {
        return drawing;
    }


    public void begin(final Camera camera) {
        if (drawing)
            throw new RuntimeException("Must end() before begin()");
        drawing = true;

        WebGPUGraphicsBase gfx = (WebGPUGraphicsBase) Gdx.graphics;
        renderPass = RenderPassBuilder.create(null, gfx.getSamples());

        shader.begin(camera, renderPass);

        renderables.clear();
    }

    public void render(Renderable renderable){
        renderables.add(renderable);
    }

    public void render (final RenderableProvider renderableProvider) {
        renderableProvider.getRenderables(renderables, renderablesPool);
    }

    public void flush() {
        // todo sort renderables

        for(Renderable renderable : renderables) {
            shader.render(renderable);
        }
    }

    public void end() {
        if (!drawing) // catch incorrect usage
            throw new RuntimeException("Cannot end() without begin()");
        drawing = false;
        flush();
        shader.end();
        renderPass.end();
        renderPass = null;
    }

    @Override
    public void dispose(){
        shader.dispose();
    }



}
