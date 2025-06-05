// basic model batch shader
                
struct FrameUniforms {
    projectionViewTransform: mat4x4f,
};
struct ModelUniforms {
    modelMatrix: mat4x4f,
};
//struct MaterialUniforms {
//    diffuseColor: vec4f,
//};

@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;
//@group(1) @binding(0) var<uniform> material: MaterialUniforms;
@group(1) @binding(1) var diffuseTexture:        texture_2d<f32>;
@group(1) @binding(2) var diffuseSampler:       sampler;
@group(2) @binding(0) var<storage, read> instances: array<ModelUniforms>;


struct VertexInput {
    @location(0) position: vec3f,
#ifdef TEXTURE_COORDINATE
    @location(1) uv: vec2f,
#endif
#ifdef NORMAL
    @location(2) normal: vec3f,
#endif
#ifdef COLOR
    @location(5) color: vec4f,
#endif

};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(1) uv: vec2f,
    @location(2) color: vec4f,
    @location(3) normal: vec4f
};

@vertex
fn vs_main(in: VertexInput, @builtin(instance_index) instance: u32) -> VertexOutput {
   var out: VertexOutput;

   out.position =  uFrame.projectionViewTransform * instances[instance].modelMatrix * vec4f(in.position, 1.0);
#ifdef TEXTURE_COORDINATE
   out.uv = in.uv;
#else
   out.uv = vec2f(0);
#endif
#ifdef COLOR
   out.color = in.color;
#else
   out.color = vec4f(1);
#endif
#ifdef NORMAL
   out.normal = vec4f(in.normal, 1.0);  // to do transform
#else
   out.normal = vec4f(0,1,0,1); // hmm...
#endif

   return out;
}


@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
#ifdef TEXTURE_COORDINATE
    let color = in.color * textureSample(diffuseTexture, diffuseSampler, in.uv);
#else
   let color = in.color;
#endif
    return color;
};