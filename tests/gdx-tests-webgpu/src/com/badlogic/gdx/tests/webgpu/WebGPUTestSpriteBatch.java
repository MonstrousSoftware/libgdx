
package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.WebGPUApplicationConfiguration;
import com.badlogic.gdx.backends.webgpu.gdx.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.backends.webgpu.wrappers.RenderPassBuilder;
import com.badlogic.gdx.backends.webgpu.wrappers.RenderPassType;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPURenderPass;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.ScreenUtils;
import jnr.ffi.Pointer;

public class WebGPUTestSpriteBatch  {

	// launcher
	public static void main (String[] argv) {

		WebGPUApplicationConfiguration config = new WebGPUApplicationConfiguration();
		config.setWindowedMode(640, 480);
		config.setTitle("WebGPUTest");

		new WebGPUApplication(new TestApp(), config);
	}

	// application
	static class TestApp extends ApplicationAdapter {
		private WebGPUSpriteBatch batch;
		private WebGPUTexture texture;
		private WebGPUTexture texture2;

		public void create () {
			batch = new WebGPUSpriteBatch();
			texture = new WebGPUTexture(Gdx.files.internal("data/badlogic.jpg"));

			// create a texture from a pixmap
			Pixmap pm = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
			pm.setColor(Color.BLUE);
			pm.fill();
			pm.setColor(Color.YELLOW);
			pm.fillCircle(64, 64, 32);
			texture2 = new WebGPUTexture(pm);
		}

		@Override
		public void render () {


			batch.begin(Color.FOREST);
			batch.draw(texture, 100, 100);
			batch.draw(texture2, 400, 300);
			batch.end();
		}


		@Override
		public void resize (int width, int height) {
			Gdx.app.log("resize", "");
		}

		@Override
		public void dispose () {
			batch.dispose();
			texture.dispose();
			texture2.dispose();
		}


	}
}
