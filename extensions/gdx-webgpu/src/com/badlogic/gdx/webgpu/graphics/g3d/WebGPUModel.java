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

package com.badlogic.gdx.webgpu.graphics.g3d;


import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.webgpu.graphics.WebGPUMesh;
import com.badlogic.gdx.webgpu.graphics.g3d.model.WebGPUMeshPart;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.graphics.g3d.model.data.*;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider;
import com.badlogic.gdx.utils.*;

import java.nio.Buffer;
import java.nio.ShortBuffer;


public class WebGPUModel extends Model {


	public WebGPUModel(ModelData data, TextureProvider textureProvider) {
		super(data, textureProvider);
	}

	@Override
	protected void convertMesh (ModelMesh modelMesh) {
		int numIndices = 0;
		for (ModelMeshPart part : modelMesh.parts) {
			numIndices += part.indices.length;
		}
		boolean hasIndices = numIndices > 0;
		VertexAttributes attributes = new VertexAttributes(modelMesh.attributes);
		int numVertices = modelMesh.vertices.length / (attributes.vertexSize / 4);

		Mesh mesh = new WebGPUMesh(true, numVertices, numIndices, attributes);
		meshes.add(mesh);
		disposables.add(mesh);

		BufferUtils.copy(modelMesh.vertices, mesh.getVerticesBuffer(true), modelMesh.vertices.length, 0);
		int offset = 0;
		ShortBuffer indicesBuffer = mesh.getIndicesBuffer(true);
		((Buffer)indicesBuffer).clear();
		for (ModelMeshPart part : modelMesh.parts) {
			MeshPart meshPart = new WebGPUMeshPart();
			meshPart.id = part.id;
			meshPart.primitiveType = part.primitiveType;
			meshPart.offset = offset;
			meshPart.size = hasIndices ? part.indices.length : numVertices;
			meshPart.mesh = mesh;
			if (hasIndices) {
				indicesBuffer.put(part.indices);
			}
			offset += meshPart.size;
			meshParts.add(meshPart);
		}
		((Buffer)indicesBuffer).position(0);
		for (MeshPart part : meshParts)
			part.update();
	}

	// unchanged?
	@Override
	protected Material convertMaterial (ModelMaterial mtl, TextureProvider textureProvider) {
		Material result = new Material();
		result.id = mtl.id;
		if (mtl.ambient != null) result.set(new ColorAttribute(ColorAttribute.Ambient, mtl.ambient));
		if (mtl.diffuse != null) result.set(new ColorAttribute(ColorAttribute.Diffuse, mtl.diffuse));
		if (mtl.specular != null) result.set(new ColorAttribute(ColorAttribute.Specular, mtl.specular));
		if (mtl.emissive != null) result.set(new ColorAttribute(ColorAttribute.Emissive, mtl.emissive));
		if (mtl.reflection != null) result.set(new ColorAttribute(ColorAttribute.Reflection, mtl.reflection));
		if (mtl.shininess > 0f) result.set(new FloatAttribute(FloatAttribute.Shininess, mtl.shininess));
		if (mtl.opacity != 1.f) result.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, mtl.opacity));

		// Note: the Textures below need to be WebGPUTextures,
		// but we keep the more generic type to play nicely with existing code.
		ObjectMap<String, Texture> textures = new ObjectMap<String, Texture>();

		// FIXME uvScaling/uvTranslation totally ignored
		if (mtl.textures != null) {
			for (ModelTexture tex : mtl.textures) {
				Texture texture;
				if (textures.containsKey(tex.fileName)) {
					texture = textures.get(tex.fileName);
				} else {
					texture = textureProvider.load(tex.fileName);
					textures.put(tex.fileName, texture);
					disposables.add(texture);
				}

				TextureDescriptor<Texture> descriptor = new TextureDescriptor<>(texture);
				descriptor.minFilter = texture.getMinFilter();
				descriptor.magFilter = texture.getMagFilter();
				descriptor.uWrap = texture.getUWrap();
				descriptor.vWrap = texture.getVWrap();

				float offsetU = tex.uvTranslation == null ? 0f : tex.uvTranslation.x;
				float offsetV = tex.uvTranslation == null ? 0f : tex.uvTranslation.y;
				float scaleU = tex.uvScaling == null ? 1f : tex.uvScaling.x;
				float scaleV = tex.uvScaling == null ? 1f : tex.uvScaling.y;

				switch (tex.usage) {
					case ModelTexture.USAGE_DIFFUSE:
						result.set(new TextureAttribute(TextureAttribute.Diffuse, descriptor, offsetU, offsetV, scaleU, scaleV));
						break;
					case ModelTexture.USAGE_SPECULAR:
						result.set(new TextureAttribute(TextureAttribute.Specular, descriptor, offsetU, offsetV, scaleU, scaleV));
						break;
					case ModelTexture.USAGE_BUMP:
						result.set(new TextureAttribute(TextureAttribute.Bump, descriptor, offsetU, offsetV, scaleU, scaleV));
						break;
					case ModelTexture.USAGE_NORMAL:
						result.set(new TextureAttribute(TextureAttribute.Normal, descriptor, offsetU, offsetV, scaleU, scaleV));
						break;
					case ModelTexture.USAGE_AMBIENT:
						result.set(new TextureAttribute(TextureAttribute.Ambient, descriptor, offsetU, offsetV, scaleU, scaleV));
						break;
					case ModelTexture.USAGE_EMISSIVE:
						result.set(new TextureAttribute(TextureAttribute.Emissive, descriptor, offsetU, offsetV, scaleU, scaleV));
						break;
					case ModelTexture.USAGE_REFLECTION:
						result.set(new TextureAttribute(TextureAttribute.Reflection, descriptor, offsetU, offsetV, scaleU, scaleV));
						break;
				}
			}
		}

		return result;
	}

	@Override
	public void dispose(){
		super.dispose();
	}
}
