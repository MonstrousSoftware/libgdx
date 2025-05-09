
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
		private WebGPUTexture texture;
		private BitmapFont font;
		private WebGPUSkin skin;

		public void create () {
			batch = new WebGPUSpriteBatch();
			//font = new BitmapFont();

//			skin = new WebGPUSkin(Gdx.files.internal("data/uiskin.json"));
//			font = skin.get("default-font", BitmapFont.class);

			texture = new WebGPUTexture(Gdx.files.classpath("com/badlogic/gdx/utils/lsans-15.png"));
			TextureRegion textureRegion = new TextureRegion(texture);
			font = new BitmapFont(Gdx.files.classpath("com/badlogic/gdx/utils/lsans-15.fnt"), textureRegion);




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
