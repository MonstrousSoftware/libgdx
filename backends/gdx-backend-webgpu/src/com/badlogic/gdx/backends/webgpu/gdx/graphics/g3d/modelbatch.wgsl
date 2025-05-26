// basic model batch shader

struct FrameUniforms {
    projectionMatrix: mat4x4f,
};

@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;

//@group(1) @binding(1) var albedoTexture:        texture_2d<f32>;
//@group(1) @binding(2) var textureSampler:       sampler;


struct VertexInput {
    @location(0) position: vec3f,
//    @location(1) uv: vec2f,
};

struct VertexOutput {
    @builtin(position) position: vec4f,
//    @location(1) uv: vec2f,
};

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
   var out: VertexOutput;

   let worldPosition =  uFrame.projectionMatrix* vec4f(in.position, 1.0);
   out.position = worldPosition;
//   out.uv = in.uv;

   return out;
}


@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    var color: vec3f = vec3f(1,0,0);
    return vec4f(color, 1);
}