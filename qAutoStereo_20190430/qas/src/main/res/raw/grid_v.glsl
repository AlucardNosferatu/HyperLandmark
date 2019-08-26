uniform mat2 u_M;
uniform vec2 u_T;

attribute vec4 a_V;

varying vec2 v_T;

void main() {
    gl_Position = vec4(a_V.xy, 0.0, 1.0);
    v_T = u_T+u_M*a_V.pq;
}
