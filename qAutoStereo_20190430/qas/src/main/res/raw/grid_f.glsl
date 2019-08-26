precision highp float;

uniform sampler2D u_F;

varying vec2 v_T;

$AS_F

void main() {
    gl_FragColor = AS_F();
}
