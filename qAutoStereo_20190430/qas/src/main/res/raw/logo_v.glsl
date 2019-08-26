attribute vec4 a_V;

varying vec2 v_V;

void main() {
    gl_Position = vec4(a_V.xy, 0.0, 1.0);
    v_V = vec2(a_V.p, 1.0-a_V.q);
}