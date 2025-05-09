
package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.g2d.WebGPUBitmapFont;
import com.badlogic.gdx.backends.webgpu.gdx.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.gdx.scene2d.WebGPUSkin;
import com.badlogic.gdx.backends.webgpu.gdx.scene2d.WebGPUStage;
import com.badlogic.gdx.backends.webgpu.gdx.utils.WebGPUScreenViewport;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.tests.utils.GdxTest;

// demonstrates the use of WebGPUSpriteBatch
// shows texture from file, texture from pixmap, texture region, sprite
//
public class FontTest extends GdxTest {
		private WebGPUSpriteBatch batch;
		private BitmapFont font;

		public void create () {
			batch = new WebGPUSpriteBatch();
			font = new WebGPUBitmapFont();

			//font = new WebGPUBitmapFont(Gdx.files.classpath("com/badlogic/gdx/utils/lsans-15.fnt"));
		}

		@Override
		public void render () {

			batch.begin();
			font.draw(batch, "Hello, world!", 200, 200);
			batch.end();
		}


		@Override
		public void resize (int width, int height) {
			Gdx.app.log("resize", "");
		}

		@Override
		public void dispose () {
			batch.dispose();
			font.dispose();
		}

}
