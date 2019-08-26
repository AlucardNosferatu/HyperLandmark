const float c_E = 1.0/128.0;

uniform vec2 u_R;
uniform vec2 u_G;
uniform vec2 u_B;
uniform float u_view;

vec3 AS_Ph() {
    return fract(vec3(dot(c_P, gl_FragCoord.xy+u_R),
                      dot(c_P, gl_FragCoord.xy+u_G),
                      dot(c_P, gl_FragCoord.xy+u_B))+c_E+u_view);
}
