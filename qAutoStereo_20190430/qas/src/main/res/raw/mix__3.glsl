const vec2 c_P = vec2($AS_H, $AS_V);

uniform vec2 u_O;
uniform float u_C;

$AS_Ph
vec4 AS_F() {
    vec3 i = floor(u_C*AS_Ph());
    float r = texture2D(u_F, v_T+u_O*i.r).r;
    float g = texture2D(u_F, v_T+u_O*i.g).g;
    float b = texture2D(u_F, v_T+u_O*i.b).b;
    return vec4(r, g, b, 1.0);
}
