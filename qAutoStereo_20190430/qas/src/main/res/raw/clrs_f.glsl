precision highp float;

uniform vec2 u_P;
uniform sampler2D u_D;

$AS_Ph
void main() {
    vec3 v = AS_Ph();
    float r = texture2D(u_D, vec2(v.r, 0.5)).r;
    float g = texture2D(u_D, vec2(v.g, 0.5)).g;
    float b = texture2D(u_D, vec2(v.b, 0.5)).b;
    gl_FragColor = vec4(r, g, b, 1.0);
}
