#include "/lib/settings.glsl"
uniform vec3 moonPosition;
#if LUNAR_EVENTS == 0
uniform int renderStage;
#endif
out vec2 vUv;
out vec4 vTint;
out vec3 vView;
#if LUNAR_EVENTS > 0
// La couleur dont le jeu teinte la lune (les lunes d'Enhanced Celestials), rangée dans une case libre de la table du
// ciel que lib/common.glsl relit. Écrite par le sommet, pas par le pixel, pour rester à jour quand la lune est hors
// champ.
#define SKY_WRITER
#include "/lib/atmosphere.glsl"
layout(rgba16f) uniform image2D skyImg;
uniform int renderStage;
#endif
void main() {
    gl_Position = ftransform();
    vUv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    vTint = gl_Color;
    vView = (gl_ModelViewMatrix * gl_Vertex).xyz;
#ifdef MC_RENDER_STAGE_MOON
    if (renderStage == MC_RENDER_STAGE_MOON) {
        // Le centre du carré : sur la direction de la lune, à la distance où le jeu le dessine.
        vec3 f = normalize(moonPosition);
        vec3 center = f * dot(vView, f);
        vec3 d = vView - center;
        float along = dot(d, f);
        // La lune un peu plus grande que celle du jeu : un astre qu'on regarde, pas un point dans le ciel.
        vec3 flat_ = (d - f * along) * MOON_SIZE;
#if MOON_UPRIGHT > 0
        // La lune garde ses bords droits, comme le jeu la montre : l'inclinaison de la course des astres
        // (sunPathRotation) la ferait pencher. Le carré tourne sur lui-même, autour de son centre, jusqu'au bord vertical
        // ou horizontal le plus proche ; il reste à sa place dans le ciel.
        vec3 up = vec3(0.0, 1.0, 0.0);
        up = up - f * dot(up, f);
        if (dot(up, up) > 1e-6) {
            up = normalize(up);
            vec3 side = normalize(cross(up, f));
            // Chaque coin est remis à 45 degrés de la verticale, au plus près de là où il était : le carré tourne sur
            // lui-même sans changer de taille, et ses bords redeviennent droits.
            float a = atan(dot(flat_, up), dot(flat_, side));
            float q = 1.5707963;
            float target = 0.7853982 + q * floor((a - 0.7853982) / q + 0.5);
            flat_ = (side * cos(target) + up * sin(target)) * length(flat_);
        }
#endif
        vView = center + f * along + flat_;
        gl_Position = gl_ProjectionMatrix * vec4(vView, 1.0);
    }
#endif
#if LUNAR_EVENTS > 0
    if (renderStage == MC_RENDER_STAGE_MOON && gl_VertexID == 0) imageStore(skyImg, ivec2(SKY_LUNAR, SKY_H), vec4(gl_Color.rgb, 1.0));
#endif
}
