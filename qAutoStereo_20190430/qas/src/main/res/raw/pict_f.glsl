const vec2 c_T = vec2(0.5, 0.0);

precision highp float;

uniform vec2 u_P;
uniform sampler2D u_F;

varying vec2 v_T;

$AS_Ph
void main() {
    vec3 t = floor(2.0*AS_Ph());
    float r = texture2D(u_F, v_T+t.r*c_T).r;
    float g = texture2D(u_F, v_T+t.g*c_T).g;
    float b = texture2D(u_F, v_T+t.b*c_T).b;
    gl_FragColor = vec4(r, g, b, 1.0);
}
