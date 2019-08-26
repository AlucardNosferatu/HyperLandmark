attribute vec4 a_V;

void main() {
    gl_Position = vec4(a_V.xy, 0.0, 1.0);
}