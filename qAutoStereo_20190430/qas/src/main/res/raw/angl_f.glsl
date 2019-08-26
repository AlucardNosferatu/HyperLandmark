precision highp float;

uniform vec2 u_P;

void main() {
	float v = fract(dot(u_P, gl_FragCoord.xy));
    gl_FragColor = vec4(vec3(floor(2.0*v)), 1.0);
}
