// Lune et autres astres du jeu ou des mods : gardés, ajoutés au ciel peint plus tard.
#include "/lib/settings.glsl"
uniform sampler2D gtexture;
uniform vec3 sunPosition;
uniform int renderStage;
uniform vec3 moonPosition;
uniform mat4 gbufferModelViewInverse;
#if EARTHSHINE > 0
uniform int moonPhase;
#endif
in vec2 vUv;
in vec4 vTint;
in vec3 vView;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    // Le soleil est repeint avec le ciel (deferred). Sa texture carrée n'est pas dessinée, qu'elle vienne du
    // jeu ou d'un mod qui la déplace : tout ce qui est dessiné autour du soleil est écarté.
    if (dot(normalize(vView), normalize(sunPosition)) > 0.86) discard;
    vec4 tex = texture(gtexture, vUv);
    float gain = 2.6;
#ifdef MC_RENDER_STAGE_MOON
    if (renderStage == MC_RENDER_STAGE_MOON) {
        // La lune ressort franchement de son halo : un carré presque blanc et net, pas une tache jaunâtre dans la lueur.
        // La teinte d'une lune d'Enhanced Celestials vient du jeu (vTint) et passe toujours.
        // Le contour gris de la nouvelle lune (vers 55 sur 255 dans la texture du jeu) dessinait un cadre blanc une fois
        // la lune éclaircie : ce qui est aussi sombre ne compte plus, la lumière cendrée le remplace.
        tex.rgb = max(tex.rgb - 0.25, 0.0) / 0.75;
        tex.rgb = mix(tex.rgb, vec3(dot(tex.rgb, vec3(0.299, 0.587, 0.114))) * vec3(0.97, 0.99, 1.0), 0.8);
        // Au ras de l'horizon, l'air épais ne laisse passer que son jaune : la lune qui se lève est dorée, puis blanchit
        // en montant (Recrafted : jaune pâle à l'horizon, blanche plus haut).
        float rise = normalize(mat3(gbufferModelViewInverse) * moonPosition).y;
        tex.rgb *= mix(vec3(1.0, 0.98, 0.70), vec3(1.0), smoothstep(0.03, 0.22, rise));
        gain = 4.0;
    }
#endif
    vec4 color = tex * vTint;
    outColor = vec4(pow(color.rgb, vec3(2.2)) * gain, color.a);
#if EARTHSHINE > 0 && defined MC_RENDER_STAGE_MOON
    // La lumière cendrée : la part sombre de la lune, éclairée par la Terre, se devine en gris bleuté, d'autant plus
    // qu'elle est un mince croissant. Le disque se lit dans l'image de la pleine lune (première case de la texture,
    // quatre phases sur deux rangs), à la même place.
    if (renderStage == MC_RENDER_STAGE_MOON && moonPhase != 0) {
        vec2 local = fract(vUv * vec2(4.0, 2.0) - 1e-4);
        vec4 full = texture(gtexture, local / vec2(4.0, 2.0));
        float disc = smoothstep(0.03, 0.15, dot(full.rgb, vec3(0.299, 0.587, 0.114))) * full.a;
        float dark = disc * (1.0 - smoothstep(0.02, 0.12, dot(color.rgb, vec3(0.299, 0.587, 0.114)) * color.a));
        float crescent = float(min(moonPhase, 8 - moonPhase)) / 4.0;
        vec3 ash = pow(full.rgb, vec3(2.2)) * vec3(0.50, 0.62, 0.90) * (0.010 + 0.022 * crescent) * vTint.a;
        outColor.rgb += ash * dark;
        outColor.a = max(outColor.a, dark * vTint.a);
    }
#endif
}
