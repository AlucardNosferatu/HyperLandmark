const vec2 c_S = vec2(0.5, 1.0);

attribute vec4 a_V;

varying vec2 v_T;

void main() {
    gl_Position = vec4(a_V.xy, 0.0, 1.0);
    v_T = c_S*vec2(a_V.p, 1.0-a_V.q);//invert image Y
}