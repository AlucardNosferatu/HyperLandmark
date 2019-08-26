precision highp float;

uniform float u_A;
uniform sampler2D u_D;

varying vec2 v_V;

void main() {
    gl_FragColor = texture2D(u_D, v_V);
    gl_FragColor.a *= u_A;
}