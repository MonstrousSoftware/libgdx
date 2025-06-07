// basic ModelBatch shader

struct DirectionalLight {
    color: vec4f,
    direction: vec4f
}

struct FrameUniforms {
    projectionViewTransform: mat4x4f,
    directionalLights : array<DirectionalLight, 3>,     // todo don't use hard coded constant for array size
    ambientLight: vec4f,
    numDirectionalLights: f32,

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
    @location(3) normal: vec3f
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
   // transform model normal to world space
   out.normal = (instances[instance].modelMatrix * vec4f(in.normal, 0.0)).xyz;
#else
   out.normal = vec3f(0,1,0);
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

    let N:vec3f = normalize(in.normal);

    // for each directional light
    var radiance = uFrame.ambientLight.rgb;
    for (var i: u32 = 0; i < u32(uFrame.numDirectionalLights); i++) {
        let light = uFrame.directionalLights[i];

        let L = -normalize(light.direction.xyz);       // L is vector towards light
        let irradiance = max(dot(L, N), 0.0);
        if(irradiance > 0.0) {
            radiance += irradiance *  light.color.rgb;
        }
    }
    let litColor = vec4f(color.rgb * radiance, 1.0);

    //return vec4f(in.normal, 1.0);
    return litColor;
};