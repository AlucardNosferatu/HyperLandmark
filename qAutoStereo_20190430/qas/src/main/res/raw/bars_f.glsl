precision highp float;

uniform vec2 u_P;

$AS_Ph
void main() {
    gl_FragColor = vec4(abs(2.0*AS_Ph()-1.0), 1.0);
}
