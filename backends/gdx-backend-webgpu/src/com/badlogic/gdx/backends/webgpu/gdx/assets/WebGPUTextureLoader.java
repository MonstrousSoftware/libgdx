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

package com.badlogic.gdx.backends.webgpu.gdx.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.backends.webgpu.wrappers.WebGPUTexture;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.Array;

// untested....

public class WebGPUTextureLoader extends AsynchronousAssetLoader<Texture, TextureLoader.TextureParameter> {
	// subclass copied from TextureLoader because members are package private
	static public class TextureLoaderInfo {
		String filename;
		TextureData data;
		Texture texture;
	};

	TextureLoaderInfo info = new TextureLoaderInfo();

	public WebGPUTextureLoader(FileHandleResolver resolver) {
		super(resolver);
	}

	@Override
	public void loadAsync (AssetManager manager, String fileName, FileHandle file, TextureLoader.TextureParameter parameter) {
		info.filename = fileName;
		if (parameter == null || parameter.textureData == null) {
			Format format = Format.RGBA8888; // force 4 byte format
			boolean genMipMaps = false;
			info.texture = null;

			if (parameter != null) {
				format = parameter.format;
				genMipMaps = parameter.genMipMaps;
				info.texture = parameter.texture;
			}

			info.data = TextureData.Factory.loadFromFile(file, format, genMipMaps);
		} else {
			info.data = parameter.textureData;
			info.texture = parameter.texture;
		}
		if (!info.data.isPrepared()) info.data.prepare();
	}

	@Override
	public Texture loadSync (AssetManager manager, String fileName, FileHandle file, TextureLoader.TextureParameter parameter) {
		if (info == null) return null;
		Texture texture = info.texture;
		if (texture != null) {
			texture.load(info.data);
		} else {
			texture = new WebGPUTexture(info.data);
		}
		if (parameter != null) {
			texture.setFilter(parameter.minFilter, parameter.magFilter);
			texture.setWrap(parameter.wrapU, parameter.wrapV);
		}
		return texture;
	}

	@Override
	public Array<AssetDescriptor> getDependencies (String fileName, FileHandle file, TextureLoader.TextureParameter parameter) {
		return null;
	}

//	static public class TextureParameter extends AssetLoaderParameters<WebGPUTexture> {
//		/** the format of the final Texture. Uses the source images format if null **/
//		public Format format = null;
//		/** whether to generate mipmaps **/
//		public boolean genMipMaps = false;
//		/** The texture to put the {@link TextureData} in, optional. **/
//		public WebGPUTexture texture = null;
//		/** TextureData for textures created on the fly, optional. When set, all format and genMipMaps are ignored */
//		public TextureData textureData = null;
//		public TextureFilter minFilter = TextureFilter.Nearest;
//		public TextureFilter magFilter = TextureFilter.Nearest;
//		public TextureWrap wrapU = TextureWrap.ClampToEdge;
//		public TextureWrap wrapV = TextureWrap.ClampToEdge;
//	}
}
