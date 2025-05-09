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

package com.badlogic.gdx.backends.webgpu.gdx.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.webgpu.WebGPUApplication;
import com.badlogic.gdx.backends.webgpu.gdx.WebGPUVertexAttributes;
import com.badlogic.gdx.backends.webgpu.utils.JavaWebGPU;
import com.badlogic.gdx.backends.webgpu.webgpu.*;
import com.badlogic.gdx.backends.webgpu.wrappers.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import jnr.ffi.Pointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

// todo

/** Immediate mode rendering class for WebGPU. The renderer will allow you to specify vertices on the fly and provides a default
 * shader for (unlit) rendering.
 * @author mzechner */
public class WebGPUImmediateModeRenderer implements ImmediateModeRenderer {
	private int primitiveType;
	private int vertexIdx;
	private int numSetTexCoords;
	private final int maxVertices;
	private int numVertices;

	private final int vertexSize;
	private final int normalOffset;
	private final int colorOffset;
	private final int texCoordOffset;
	private final Matrix4 projModelView = new Matrix4();
	private final float[] vertices;

	private WebGPUApplication app;
	private WebGPUVertexAttributes vertexAttributes;
	private WebGPUVertexBuffer vertexBuffer;
	private WebGPUUniformBuffer uniformBuffer;
	private int uniformBufferSize;
	private WebGPUBindGroupLayout bindGroupLayout;
	private WebGPUPipelineLayout pipelineLayout;
	private PipelineCache pipelines;
	private PipelineSpecification pipelineSpec;
	private WebGPUTexture texture;
	private WebGPURenderPass renderPass;
	private Pointer vertexDataPtr;
	private FloatBuffer vertexData;
	private WebGPUPipeline prevPipeline;

	public WebGPUImmediateModeRenderer(boolean hasNormals, boolean hasColors, int numTexCoords) {
		this(5000, hasNormals, hasColors, numTexCoords, createDefaultShader(hasNormals, hasColors, numTexCoords));

	}

	public WebGPUImmediateModeRenderer(int maxVertices, boolean hasNormals, boolean hasColors, int numTexCoords) {
		this(maxVertices, hasNormals, hasColors, numTexCoords, createDefaultShader(hasNormals, hasColors, numTexCoords));

	}

	public WebGPUImmediateModeRenderer(int maxVertices, boolean hasNormals, boolean hasColors, int numTexCoords,
									   ShaderProgram shader) {

		app = (WebGPUApplication) Gdx.app;
		this.maxVertices = maxVertices;


		vertexAttributes = new WebGPUVertexAttributes(WebGPUVertexAttributes.Usage.POSITION| WebGPUVertexAttributes.Usage.NORMAL|WebGPUVertexAttributes.Usage.TEXTURE_COORDINATE| WebGPUVertexAttributes.Usage.COLOR_PACKED);

		vertexSize = vertexAttributes.getVertexSizeInBytes()/Float.BYTES;	// size in floats
		vertices = new float[maxVertices * vertexSize];

		// PPP C UU NNN
		normalOffset = 6;
		colorOffset = 3;
		texCoordOffset = 4;


		createBuffers();

		ByteBuffer vertexBB = ByteBuffer.allocateDirect(maxVertices * vertexSize*Float.BYTES);
		vertexBB.order(ByteOrder.nativeOrder());  // important
		vertexData = vertexBB.asFloatBuffer();
		vertexDataPtr = Pointer.wrap(JavaWebGPU.getRuntime(), vertexBB);


		bindGroupLayout = createBindGroupLayout();
		pipelineLayout = new WebGPUPipelineLayout("ImmediateModeRenderer pipeline layout", bindGroupLayout);

		pipelines = new PipelineCache();
		pipelineSpec = new PipelineSpecification(vertexAttributes, defaultShaderSource());
		pipelineSpec.name = "ImmediateModeRenderer pipeline";
		pipelineSpec.disableDepthTest();

		// default blending values
		pipelineSpec.enableBlending();
		pipelineSpec.setBlendFactor(WGPUBlendFactor.SrcAlpha, WGPUBlendFactor.OneMinusSrcAlpha);

		prevPipeline = null;
	}

//	private VertexAttribute[] buildVertexAttributes (boolean hasNormals, boolean hasColor, int numTexCoords) {
//		Array<VertexAttribute> attribs = new Array<VertexAttribute>();
//		attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
//		if (hasNormals) attribs.add(new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE));
//		if (hasColor) attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
//		for (int i = 0; i < numTexCoords; i++) {
//			attribs.add(new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + i));
//		}
//		VertexAttribute[] array = new VertexAttribute[attribs.size];
//		for (int i = 0; i < attribs.size; i++)
//			array[i] = attribs.get(i);
//		return array;
//	}

	public void setShader (ShaderProgram shader) {
//		if (ownsShader) this.shader.dispose();
//		this.shader = shader;
//		ownsShader = false;
	}

	public ShaderProgram getShader () {
		return null; //shader;
	}

	public void begin (Matrix4 projModelView, int primitiveType) {
		this.projModelView.set(projModelView);
		this.primitiveType = primitiveType;

		renderPass = RenderPassBuilder.create(null, app.getConfiguration().samples);
	}

	public void color (Color color) {
		vertices[vertexIdx + colorOffset] = color.toFloatBits();
	}

	public void color (float r, float g, float b, float a) {
		vertices[vertexIdx + colorOffset] = Color.toFloatBits(r, g, b, a);
	}

	public void color (float colorBits) {
		vertices[vertexIdx + colorOffset] = colorBits;
	}

	public void texCoord (float u, float v) {
		final int idx = vertexIdx + texCoordOffset;
		vertices[idx ] = u;
		vertices[idx + 1] = v;
	}

	public void normal (float x, float y, float z) {
		final int idx = vertexIdx + normalOffset;
		vertices[idx] = x;
		vertices[idx + 1] = y;
		vertices[idx + 2] = z;
	}

	public void vertex (float x, float y, float z) {
		final int idx = vertexIdx;
		vertices[idx] = x;
		vertices[idx + 1] = y;
		vertices[idx + 2] = z;

		vertexIdx += vertexSize;
		numVertices++;
	}

	public void flush () {
		if (numVertices == 0) return;

		setUniforms();	// push matrix to uniform buffer

		// bind texture
		WebGPUBindGroup bg = makeBindGroup(bindGroupLayout, uniformBuffer, texture);
		setPipeline();
		renderPass.setPipeline(prevPipeline.getHandle());

		// write number of vertices to the GPU's vertex buffer
		//
		int numBytes = numVertices * vertexSize * Float.BYTES;
		for(int i = 0; i < numVertices * vertexSize; i++)	// copy floats
			vertexData.put(i, vertices[i]);
		//vertexData.put(0, vertices, 0, numBytes/Float.BYTES);	// copy to FloatBuffer

		// copy vertex data to GPU vertex buffer
		app.getQueue().writeBuffer(vertexBuffer, 0, vertexDataPtr, numBytes);

		// Set vertex buffer while encoding the render pass
		renderPass.setVertexBuffer( 0, vertexBuffer.getHandle(), 0, numBytes);

		renderPass.setBindGroup( 0, bg.getHandle(), 0, JavaWebGPU.createNullPointer());

		renderPass.draw( numVertices);

		bg.dispose();	// done with bind group

		// reset
		// todo allow multiple batches
		vertexIdx = 0;
		numVertices = 0;
	}

	public void end () {
		flush();
		renderPass.end();
		renderPass = null;
	}

	public int getNumVertices () {
		return numVertices;
	}

	@Override
	public int getMaxVertices () {
		return maxVertices;
	}

	public void setTexture( WebGPUTexture texture ){
		this.texture = texture;
	}


//	static private String createVertexShader (boolean hasNormals, boolean hasColors, int numTexCoords) {
//		String shader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
//			+ (hasNormals ? "attribute vec3 " + ShaderProgram.NORMAL_ATTRIBUTE + ";\n" : "")
//			+ (hasColors ? "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" : "");
//
//		for (int i = 0; i < numTexCoords; i++) {
//			shader += "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + i + ";\n";
//		}
//
//		shader += "uniform mat4 u_projModelView;\n" //
//			+ (hasColors ? "varying vec4 v_col;\n" : "");
//
//		for (int i = 0; i < numTexCoords; i++) {
//			shader += "varying vec2 v_tex" + i + ";\n";
//		}
//
//		shader += "void main() {\n" + "   gl_Position = u_projModelView * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n";
//		if (hasColors) {
//			shader += "   v_col = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
//				+ "   v_col.a *= 255.0 / 254.0;\n";
//		}
//
//		for (int i = 0; i < numTexCoords; i++) {
//			shader += "   v_tex" + i + " = " + ShaderProgram.TEXCOORD_ATTRIBUTE + i + ";\n";
//		}
//		shader += "   gl_PointSize = 1.0;\n" //
//			+ "}\n";
//		return shader;
//	}
//
//	static private String createFragmentShader (boolean hasNormals, boolean hasColors, int numTexCoords) {
//		String shader = "#ifdef GL_ES\n" + "precision mediump float;\n" + "#endif\n";
//
//		if (hasColors) shader += "varying vec4 v_col;\n";
//		for (int i = 0; i < numTexCoords; i++) {
//			shader += "varying vec2 v_tex" + i + ";\n";
//			shader += "uniform sampler2D u_sampler" + i + ";\n";
//		}
//
//		shader += "void main() {\n" //
//			+ "   gl_FragColor = " + (hasColors ? "v_col" : "vec4(1, 1, 1, 1)");
//
//		if (numTexCoords > 0) shader += " * ";
//
//		for (int i = 0; i < numTexCoords; i++) {
//			if (i == numTexCoords - 1) {
//				shader += " texture2D(u_sampler" + i + ",  v_tex" + i + ")";
//			} else {
//				shader += " texture2D(u_sampler" + i + ",  v_tex" + i + ") *";
//			}
//		}
//
//		shader += ";\n}";
//		return shader;
//	}

	static private String defaultShaderSource() {
		return "struct Uniforms {\n" +
				"    projectionMatrix: mat4x4f,\n" +
				"};\n" +
				"\n" +
				"@group(0) @binding(0) var<uniform> uniforms: Uniforms;\n" +
				"@group(0) @binding(1) var texture: texture_2d<f32>;\n" +
				"@group(0) @binding(2) var textureSampler: sampler;\n" +
				"\n" +
				"struct VertexInput {\n" +
				"    @location(0) position: vec3f,\n" +
				"    @location(5) color: vec4f,\n" +
				"    @location(1) uv: vec2f,\n" +
				"    @location(2) normal: vec3f,\n" +
				"};\n" +
				"\n" +
				"struct VertexOutput {\n" +
				"    @builtin(position) position: vec4f,\n" +
				"    @location(0) uv : vec2f,\n" +
				"    @location(1) color: vec4f,\n" +
				"};\n" +
				"\n" +
				"\n" +
				"@vertex\n" +
				"fn vs_main(in: VertexInput) -> VertexOutput {\n" +
				"   var out: VertexOutput;\n" +
				"\n" +
				"   var pos =  uniforms.projectionMatrix * vec4f(in.position, 1.0);\n" +
				"   out.position = pos;\n" +
				"   out.uv = in.uv;\n" +
				"   out.color = in.color;\n" +
				"\n" +
				"   return out;\n" +
				"}\n" +
				"\n" +
				"@fragment\n" +
				"fn fs_main(in : VertexOutput) -> @location(0) vec4f {\n" +
				"\n" +
				"    let color = in.color * textureSample(texture, textureSampler, in.uv);\n" +
				"    return vec4f(color);\n" +
				"}";
	}


	/** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified. */
	static public ShaderProgram createDefaultShader (boolean hasNormals, boolean hasColors, int numTexCoords) {
		return null;
	}


	private void createBuffers() {

		// Create vertex buffer (no index buffer)
		vertexBuffer = new WebGPUVertexBuffer(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex, (long) maxVertices * vertexSize);

		// Create uniform buffer for the projection matrix
		uniformBufferSize = 16 * Float.BYTES;
		uniformBuffer = new WebGPUUniformBuffer(uniformBufferSize,WGPUBufferUsage.CopyDst |WGPUBufferUsage.Uniform  );
	}

	private void setUniforms(){
		uniformBuffer.beginFill();
		uniformBuffer.append(projModelView);
		uniformBuffer.endFill();
	}

	private WebGPUBindGroupLayout createBindGroupLayout() {
		WebGPUBindGroupLayout layout = new WebGPUBindGroupLayout("SpriteBatch bind group layout");
		layout.begin();
		layout.addBuffer(0, WGPUShaderStage.Vertex, WGPUBufferBindingType.Uniform, uniformBufferSize, false);
		layout.addTexture(1, WGPUShaderStage.Fragment, WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
		layout.addSampler(2, WGPUShaderStage.Fragment, WGPUSamplerBindingType.Filtering );
		layout.end();
		return layout;
	}


	private WebGPUBindGroup makeBindGroup(WebGPUBindGroupLayout bindGroupLayout, WebGPUBuffer uniformBuffer, WebGPUTexture texture) {
		WebGPUBindGroup bg = new WebGPUBindGroup(bindGroupLayout);
		bg.begin();
		bg.addBuffer(0, uniformBuffer);
		bg.addTexture(1, texture.getTextureView());
		bg.addSampler(2, texture.getSampler());
		bg.end();
		return bg;
	}

	// create or reuse pipeline on demand to match the pipeline spec
	private void setPipeline() {
		WebGPUPipeline pipeline = pipelines.findPipeline( pipelineLayout.getHandle(), pipelineSpec);
		if (pipeline != prevPipeline) { // avoid unneeded switches
			//renderPass.setPipeline(pipeline.getHandle());
			prevPipeline = pipeline;
		}
	}

	//@Override
	public void dispose(){
		pipelines.dispose();
		vertexBuffer.dispose();

		uniformBuffer.dispose();
		bindGroupLayout.dispose();
		pipelineLayout.dispose();
	}
}
