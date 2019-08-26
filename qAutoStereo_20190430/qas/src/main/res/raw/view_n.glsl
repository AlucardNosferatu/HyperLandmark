const float c_E = 1.0/128.0;
const int c_N = $AS_Pg;
const vec2 c_F = vec2($AS_Pw, $AS_Ph);

uniform sampler2D u_pattern;
uniform vec2 u_base;
uniform mat2 u_pixrot;
uniform vec2 u_R[c_N];
uniform vec2 u_G[c_N];
uniform vec2 u_B[c_N];
uniform float u_view;

vec3 AS_Ph() {
    vec2 global = u_base+u_pixrot*floor(gl_FragCoord.xy);
    int t = int(float(c_N)*texture2D(u_pattern, fract(c_F*global)).r);
    return fract(vec3(dot(c_P, gl_FragCoord.xy+u_R[t]),
                      dot(c_P, gl_FragCoord.xy+u_G[t]),
                      dot(c_P, gl_FragCoord.xy+u_B[t]))+c_E+u_view);
}
