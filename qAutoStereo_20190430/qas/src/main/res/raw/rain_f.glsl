precision highp float;

uniform vec3 u_factor[6];
uniform vec3 u_offset[6];
uniform vec2 u_P;

$AS_Ph
void main() {
    vec3 f = 6.0*AS_Ph();
    ivec3 i = ivec3(f);
    vec3 factor = vec3(u_factor[i.r].r, u_factor[i.g].g, u_factor[i.b].b);
    vec3 offset = vec3(u_offset[i.r].r, u_offset[i.g].g, u_offset[i.b].b);
    gl_FragColor = vec4(fract(f)*factor+offset, 1.0);
}

