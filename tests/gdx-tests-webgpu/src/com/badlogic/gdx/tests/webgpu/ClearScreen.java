
package com.badlogic.gdx.tests.webgpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.gdx.g2d.WebGPUSpriteBatch;
import com.badlogic.gdx.backends.webgpu.gdx.utils.WebGPUScreenUtils;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.tests.utils.GdxTest;

// demonstrates the use of WebGPUScreenUtils
//
public class ClearScreen extends GdxTest {


	@Override
	public void render () {
		WebGPUScreenUtils.clear(Color.CORAL);	// use ScreenUtils variant to clear the screen
	}



}
