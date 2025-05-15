
package com.badlogic.gdx.tests.webgpu;


import com.badlogic.gdx.backends.webgpu.gdx.graphics.utils.WebGPUScreenUtils;
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
