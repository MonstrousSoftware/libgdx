/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
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

package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3_webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.lwjgl3_webgpu.WebGPUApplicationConfiguration;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.WebGPUMesh;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g2d.WebGPUBitmapFont;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.WebGPUModelBatch;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.g3d.model.WebGPUMeshPart;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.utils.WebGPUScreenUtils;
import com.badlogic.gdx.backends.webgpu.gdx.graphics.viewport.WebGPUScreenViewport;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.tests.utils.PerspectiveCamController;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class ModelBatchTest extends GdxTest {

	WebGPUModelBatch modelBatch;
	PerspectiveCamera cam;
	PerspectiveCamController controller;
	WebGPUSpriteBatch batch;
	WebGPUBitmapFont font;
	WebGPUMesh mesh;
	Renderable renderable;
	Renderable renderable2;


	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");

		new WebGPUApplication(new ModelBatchTest(), config);
	}

	public void create () {
		modelBatch = new WebGPUModelBatch();
		cam = new PerspectiveCamera(47, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 0, 2);
		cam.near = 0.1f;


		controller = new PerspectiveCamController(cam);
		Gdx.input.setInputProcessor(controller);
		batch = new WebGPUSpriteBatch();
		font = new WebGPUBitmapFont(Gdx.files.internal("data/lsans-15.fnt"), false);


		Material mat1 = new Material(TextureAttribute.createDiffuse(new WebGPUTexture(Gdx.files.internal("data/planet_earth.png"))));
		Material mat2 = new Material(TextureAttribute.createDiffuse(new WebGPUTexture(Gdx.files.internal("data/badlogic.jpg"))));


		final WebGPUMeshPart meshPart = createMeshPart();
		renderable = new Renderable();
		renderable.meshPart.set(meshPart);
		renderable.worldTransform.idt();
		renderable.material = mat1;

		renderable2 = new Renderable();
		renderable2.meshPart.set(meshPart);
		renderable2.worldTransform.idt().trn(0,1,-3);
		renderable2.material = mat2;

	}

	public void render () {
		float delta = Gdx.graphics.getDeltaTime();
		renderable.worldTransform.rotate(Vector3.Y, delta*45f);
		renderable2.worldTransform.rotate(Vector3.Y, -delta*45f);

		WebGPUScreenUtils.clear(Color.TEAL);

		cam.update();
		modelBatch.begin(cam);

		modelBatch.render(renderable2);

		modelBatch.render(renderable);

		modelBatch.end();


		batch.begin();
		font.draw(batch, "fps: " + Gdx.graphics.getFramesPerSecond(), 0, 20);
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth = width;
		cam.viewportHeight = height;
		cam.update();

	}

	@Override
	public void dispose () {
		batch.dispose();
		font.dispose();
		modelBatch.dispose();
		mesh.dispose();

	}

	public WebGPUMeshPart createMeshPart() {
		VertexAttributes vattr = new VertexAttributes(VertexAttribute.Position(),  VertexAttribute.TexCoords(0), VertexAttribute.ColorUnpacked());

		mesh = new WebGPUMesh(true, 8, 12, vattr);
		mesh.setVertices(new float[]{
				-0.5f, -0.5f, 0.5f, 	0, 1, 	1,0,1,1,
				0.5f, -0.5f, 0.5f, 	1,1,	0,1,1,1,
				0.5f, 0.5f, 0.5f, 		1,0,	1,1,0,1,
				-0.5f, 0.5f, 0.5f, 	0,0,	0,1,0,1,

				-0.5f, -0.5f, -0.5f, 	0, 1, 	1,0,1,1,
				0.5f, -0.5f, -0.5f, 	1,1,	0,1,1,1,
				0.5f, 0.5f, -0.5f, 		1,0,	1,1,0,1,
				-0.5f, 0.5f, -0.5f, 	0,0,	0,1,0,1,
		});

		mesh.setIndices(new short[] {0, 1, 2, 	2, 3, 0, 	4, 5, 6,  6, 7, 4});

		int offset = 0;	// offset in the indices array, since the mesh is indexed
		int size = 12;	// nr of indices, since the mesh is indexed
		int type = GL20.GL_TRIANGLES;	// primitive type using GL constant
		return new WebGPUMeshPart("part", mesh, offset, size, type);
	}
}
