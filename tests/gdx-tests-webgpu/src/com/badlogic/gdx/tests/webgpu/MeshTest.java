package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.WebGPUApplicationConfiguration;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUMesh;
import com.badlogic.gdx.backends.webgpu.gdx.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.gdx.g3d.WebGPUIndexData;
import com.badlogic.gdx.backends.webgpu.gdx.g3d.WebGPUVertexData;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.IndexData;
import com.badlogic.gdx.graphics.glutils.VertexData;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.utils.TimeUtils;

import java.nio.ShortBuffer;
import java.text.DecimalFormat;

public class MeshTest extends GdxTest {


    // launcher
    public static void main (String[] argv) {

        WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
        config.setWindowedMode(640, 480);
        config.setTitle("WebGPUTest");
        //config.backend = WGPUBackendType.D3D12;
        config.enableGPUtiming = false;

        new WebGPUApplication(new MeshTest(), config);
    }


    Mesh mesh;

    @Override
    public void create() {
        IndexData indexData = new WebGPUIndexData(3);

        short[] indices = { 3, 4, 5 };
        indexData.setIndices(indices, 0, 3);
        ShortBuffer sb = indexData.getBuffer(false);
        short i0 = sb.get(0);
        short i1 = sb.get(1);
        short i2 = sb.get(2);

        indexData.bind();

        VertexData vertexData = new WebGPUVertexData(3, VertexAttribute.Position(), VertexAttribute.TexCoords(0));

        float[] verts = { 0,0,0, 0, 0, 1, 1, 0, 1,1, 2, 0, 0, 0, 1 };
        vertexData.setVertices(verts, 0, verts.length);


        //mesh = new WebGPUMesh(true, 100, 100, VertexAttribute.Position(), VertexAttribute.TexCoords(0));

        mesh = new WebGPUMesh(true, 4, 6, VertexAttribute.Position(), VertexAttribute.ColorUnpacked(), VertexAttribute.TexCoords(0));
        mesh.setVertices(new float[] {-0.5f, -0.5f, 0, 1, 1, 1, 1, 0, 1, 0.5f, -0.5f, 0, 1, 1, 1, 1, 1, 1, 0.5f, 0.5f, 0, 1, 1, 1,
                1, 1, 0, -0.5f, 0.5f, 0, 1, 1, 1, 1, 0, 0});
        mesh.setIndices(new short[] {0, 1, 2, 2, 3, 0});

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void render() {

        mesh.render(null, GL20.GL_TRIANGLES);
    }



    @Override
    public void dispose() {
    }
}
