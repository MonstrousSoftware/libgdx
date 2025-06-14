
package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUBitmapFont;
import com.badlogic.gdx.webgpu.graphics.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.tests.utils.GdxTest;

// demonstrates use of WebPUBitmapFont
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
