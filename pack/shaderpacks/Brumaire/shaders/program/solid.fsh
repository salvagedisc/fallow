// Fragments de toute la géométrie éclairée. Chaque programme d'Iris inclut ce fichier avec son PROGRAM_*.
#define FSH
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/shadow.glsl"
#include "/lib/light.glsl"
#include "/lib/material.glsl"
#if (defined PROGRAM_ENTITIES || defined PROGRAM_HAND) && WET_ENTITIES > 0
uniform float rainSoak;
uniform float swimSoak;
#endif
#if defined PROGRAM_ENTITIES && SNOWY_ENTITIES > 0
uniform float snowSoak;
#endif
#ifdef PROGRAM_TERRAIN
#include "/lib/rain.glsl"
#include "/lib/cover.glsl"
#include "/lib/snow.glsl"
#include "/lib/cave.glsl"
#include "/lib/tracks.glsl"
#include "/lib/shore.glsl"
#include "/lib/alpenglow.glsl"
#include "/lib/auroraglow.glsl"
#include "/lib/groundtint.glsl"
#include "/lib/grassedge.glsl"
#endif
#ifdef PROGRAM_WATER
#include "/lib/mist.glsl"
#if WATER_STARS > 0
#include "/lib/nightsky.glsl"
#endif
uniform sampler2D depthtex1;
uniform sampler2D colortex6;
#define REFLECT_DEPTH depthtex1
#define REFLECT_COLOR colortex6
#include "/lib/reflect.glsl"
#include "/lib/voxtrace.glsl"
#include "/lib/wakes.glsl"
#include "/lib/rain.glsl"
#include "/lib/waterfoam.glsl"
#if ICE_DETAIL > 0 || WINDOW_FROST > 0
#include "/lib/cover.glsl"
#include "/lib/snow.glsl"
#endif
#endif

uniform sampler2D gtexture;
#include "/lib/glass.glsl"
uniform float alphaTestRef;
uniform vec4 entityColor;
#if (defined PROGRAM_ENTITIES || defined PROGRAM_HAND) && MATERIALS > 0
// L'objet rendu (outil, armure portée, objet posé ou tenu) et la créature, numérotés comme les blocs (lib/material.glsl).
uniform int currentRenderedItemId;
uniform int entityId;
#endif

in vec2 vUv;
in vec2 vLm;
in vec4 vTint;
in vec3 vNormal;
in vec3 vPos;
flat in int vMat;
flat in int vSurf;
#if defined PROGRAM_TERRAIN
in vec2 vBend;
#endif
#if defined PROGRAM_TERRAIN && RELIEF > 0
in vec4 vTangent;
in vec2 vSpriteMid;
in vec2 vSpriteHalf;
#include "/lib/relief.glsl"
#endif

#if defined PROGRAM_TERRAIN || defined PROGRAM_BLOCK || defined PROGRAM_ENTITIES || defined PROGRAM_CLRWL
#define WRITE_SURFACE
#endif

#ifdef WRITE_SURFACE
// Couche 1 : normale de la surface et part de lumière indirecte, lues par l'occlusion d'écran (deferred).
// Couche 4 : la couleur propre de la surface (avant la lumière), pour la lumière qu'elle reçoit de ses voisines.
/* RENDERTARGETS: 0,1,4 */
layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outSurface;
layout(location = 2) out vec4 outAlbedo;
#else
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
#endif

#ifdef PROGRAM_WATER
#include "/lib/watershade.glsl"

/** La glace : sa couleur laisse voir au travers, sa surface lisse reflète le paysage, plus à jour frisant. */
vec4 ice(vec3 albedo, float alpha, vec3 normal, vec2 lm, float ao) {
    float indirect;
    vec3 lit = shade(albedo, normal, lm, ao, vPos, 0.0, gl_FragCoord.xy, SHADOW_FILTER, indirect);
    vec3 world = vPos + cameraPosition;
    vec3 eye = normalize(-vPos);
    vec3 r = reflect(-eye, normal);
    float f = 0.03 + 0.97 * pow(1.0 - max(dot(normal, eye), 0.0), 5.0);
    vec3 mirror = skyReflection(world, r) * mix(0.12, 1.0, lm.y * lm.y);
    float dist;
    vec3 view = (gbufferModelView * vec4(vPos, 1.0)).xyz;
    vec4 hit = screenReflection(view, mat3(gbufferModelView) * r, dither(gl_FragCoord.xy), normal.y > 0.5 ? 1.0 : 0.0, WATER_REFLECTIONS, dist);
#if VOXEL_REFLECTIONS > 0
    if (hit.a < 0.99) {
        float reach;
        vec4 vox = voxelReflection(vPos + normal * 0.02, r, reach);
        if (vox.a > 0.0) mirror = mix(mirror, veiled(vox.rgb, r * reach * 0.4), vox.a);
    }
#endif
    if (hit.a > 0.0) mirror = mix(mirror, veiled(hit.rgb, r * dist * 0.4), hit.a);
    f = f * 0.85 + 0.04;
    // Mélange sur ce qui est derrière : la glace garde sa transparence, le reflet s'y ajoute.
    float a = alpha + f * (1.0 - alpha);
    return vec4((lit * alpha * (1.0 - f) + mirror * f) / max(a, 1e-4), a);
}
#endif

void main() {
    initEnv();
    // Les particules n'ont souvent pas de normale (le format du jeu n'en porte pas) : une normale nulle donnerait un
    // point noir à l'écran. Elles regardent alors l'œil.
    vec3 normal = dot(vNormal, vNormal) > 1e-4 ? normalize(vNormal) : -normalize(vPos);
#ifdef PROGRAM_PARTICLES
    // Une particule est mince et se voit des deux côtés (une feuille qui tombe, un flocon, une fumée) : sa face
    // éclairée est celle qui regarde l'astre, et elle prend aussi le ciel au-dessus. Sans quoi, vue à contre-jour,
    // une feuille qui tombe devient une croix noire ou gris-bleu sur le ciel.
    if (dot(normal, gLightDir) < 0.0) normal = -normal;
    normal = normalize(normal + vec3(0.0, 0.5, 0.0));
#endif
    vec2 lm = vLm;
#ifdef PROGRAM_PARTICLES
    // Certaines particules de mods (les feuilles qui tombent) arrivent sans lumière du jeu : noires même en plein jour.
    // Elles volent près de l'œil, elles prennent au moins la lumière qu'il reçoit.
    lm = max(lm, vec2(eyeBrightnessSmooth) / 240.0 * 0.9);
#endif
    float ao = 1.0;

#if defined PROGRAM_BASIC || defined PROGRAM_LIGHTNING
    vec4 color = vTint;
#elif defined PROGRAM_CLRWL
    // Flywheel (Create) : Colorwheel applique sa matière, sa lumière et sa découpe.
    vec4 color;
    vec4 overlay;
    vec2 light;
    clrwl_computeFragment(texture(gtexture, vUv), color, light, ao, overlay);
    color.rgb = mix(color.rgb, overlay.rgb, overlay.a);
    lm = lightCoord(light);
#elif defined PROGRAM_TERRAIN || defined PROGRAM_WATER
    // Sur le terrain, l'alpha du sommet porte l'occlusion (separateAo).
    float pbrAo = 1.0;
#if defined PROGRAM_TERRAIN && RELIEF > 0
    // Le relief des blocs (lib/relief.glsl) : sur les blocs pleins, le sable et les blocs partiels, pas sur les plantes,
    // les feuillages, l'eau, la glace, le verre ni ce qui éclaire.
    vec2 uvDx = dFdx(vUv);
    vec2 uvDy = dFdy(vUv);
    vec2 reliefUv = vUv;
    vec3 reliefN = normal;
    float reliefShade = 0.0;
    bool sprite = vSpriteHalf.x > 1e-6 && vSpriteHalf.y > 1e-6;
    // Un pack labPBR donne ses propres cartes : elles valent pour tous les blocs (pas l'eau), à toute distance ; sans
    // lui, le relief est tiré de la clarté des textures, sur les blocs pleins, le sable et les blocs partiels.
    bool pbr = false;
#if LABPBR > 0
    pbr = sprite && vMat != 6 && labPbrPresent(vUv);
    gPbrHeight = pbr;
#endif
    float reliefFade = (1.0 - smoothstep(RELIEF_RANGE * 0.6, RELIEF_RANGE, length(vPos)))
                     * float((pbr || vMat == 0 || vMat == 12 || vMat == 14) && sprite);
    float reliefLight = pbr ? 1.0 : reliefFade;
    if (reliefLight > 0.0) {
        Sprite sp = Sprite(vSpriteMid, vSpriteHalf, 1.0 / vec2(atlasSize));
        float mean = luma(textureLod(gtexture, vUv, 4.0).rgb);
        vec3 t = normalize(vTangent.xyz - normal * dot(normal, vTangent.xyz));
        vec3 b = cross(normal, t) * (vTangent.w < 0.0 ? -1.0 : 1.0);
        mat3 tbn = mat3(t, b, normal);
        float h = 1.0;
        if (reliefFade > 0.0) {
            reliefUv = reliefParallax(sp, vUv, normalize(-vPos) * tbn, mean, reliefFade, h);
            reliefN = reliefNormal(sp, reliefUv, t, b, normal, mean, reliefFade);
            reliefShade = reliefShadow(sp, reliefUv, h, gLightDir * tbn, mean, reliefFade);
        }
#if LABPBR > 0
        if (pbr) {
            vec4 nm = textureGrad(normals, reliefUv, uvDx, uvDy);
            reliefN = normalize(tbn * labPbrNormal(nm));
            pbrAo = nm.b;
        }
#endif
    }
    vec4 color = textureGrad(gtexture, reliefUv, uvDx, uvDy);
#else
    vec4 color = texture(gtexture, vUv);
#endif
    color.rgb *= vTint.rgb;
#if defined PROGRAM_TERRAIN || defined PROGRAM_WATER
    if (vMat == 13) color = connectGlass(color, vPos + cameraPosition, normal, vUv, vTint.rgb);
#if WINDOW_RAIN > 0
    // Sous la pluie, l'eau perle et coule sur les vitres exposées au ciel : un peu de lumière et d'opacité en plus.
    if (vMat == 13) {
        vec2 drops = windowRain(vPos + cameraPosition, normal, frameTimeCounter, rainStrength * rainHere * smoothstep(0.85, 0.97, vLm.y));
        color.rgb = mix(color.rgb, vec3(0.70, 0.76, 0.82) * (0.6 + 0.6 * drops.y), drops.x * 0.45);
        color.a = max(color.a, drops.x * (0.30 + 0.35 * drops.y));
    }
#endif
#if WINDOW_FROST > 0
    // Par grand froid, le givre pousse sur les vitres depuis leurs bords : des fougères blanches au pixel.
    if (vMat == 13) {
        vec2 rime = windowFrost(vPos + cameraPosition, normal, windowCold());
        color.rgb = mix(color.rgb, vec3(0.82, 0.88, 0.94) * (0.85 + 0.3 * rime.y), rime.x * 0.65);
        color.a = max(color.a, rime.x * (0.32 + 0.3 * rime.y));
    }
#endif
#endif
    ao = vTint.a * pbrAo;
#elif defined PROGRAM_PARTICLES
    // Les feuilles qui tombent de Particular portent la lumière du jeu cuite dans leur couleur : la nuit ou sous une
    // couronne, leur teinte verte tombe presque au noir et la lumière du pack s'y ajoute encore. Une teinte verte trop
    // sombre retrouve sa clarté de feuille ; la lumière vient ensuite du pack, comme pour tout le reste.
    vec3 tint = vTint.rgb;
    float top = max(tint.r, max(tint.g, tint.b));
    if (tint.g >= top - 1e-4 && tint.g > tint.b + 0.01 && top < 0.7) tint *= 0.7 / max(top, 0.02);
    vec4 color = texture(gtexture, vUv) * vec4(tint, vTint.a);
#else
    vec4 color = texture(gtexture, vUv) * vTint;
#endif

#if defined PROGRAM_DAMAGEDBLOCK
    // Les fissures multiplient ce qui est déjà dessiné : la texture part telle quelle.
    outColor = color;
    return;
#endif

#if defined PROGRAM_TERRAIN && GRASS_OVERHANG > 0
    // Le côté d'un bloc d'herbe : sa couche teinte (la bordure verte) descend un peu plus bas (lib/grassedge.glsl).
    if (vSurf == 22 && color.a < alphaTestRef && abs(normal.y) < 0.5 && min(vTint.r, min(vTint.g, vTint.b)) < 0.97) {
        vec4 edge = grassOverhang(vPos + cameraPosition, normal);
        if (edge.a > 0.0) color = vec4(edge.rgb * vTint.rgb, 1.0);
    }
#if GRASS_SLOPES > 0 && VOXEL_REFLECTIONS > 0
    // La marche d'une pente d'herbe se couvre d'herbe sur toute sa hauteur, sa couche teinte devenue pleine.
    if (vSurf == 22 && abs(normal.y) < 0.5 && min(vTint.r, min(vTint.g, vTint.b)) < 0.97 && grassSlope(vPos + cameraPosition, normal))
        color = vec4(grassRiser(vPos + cameraPosition, normal).rgb * vTint.rgb, 1.0);
#endif
#if GRASS_FAR_SIDES > 0
    // Au loin, les flancs d'herbe sous le ciel se couvrent d'herbe comme les collines lointaines.
    if (vSurf == 22 && abs(normal.y) < 0.5 && min(vTint.r, min(vTint.g, vTint.b)) < 0.97 && vLm.y > 0.5 && grassFarSide(vPos + cameraPosition, normal))
        color = vec4(grassRiser(vPos + cameraPosition, normal).rgb * vTint.rgb, 1.0);
#endif
#endif
#ifndef PROGRAM_CLRWL
    if (color.a < alphaTestRef) discard;
#endif
#if defined PROGRAM_ENTITIES
    color.rgb = mix(color.rgb, entityColor.rgb, entityColor.a);
#endif

    vec3 albedo = toLinear(color.rgb);

#if defined PROGRAM_EMISSIVE
    outColor = vec4(albedo * 2.4, color.a);
#elif defined PROGRAM_UNLIT
    outColor = vec4(albedo, color.a);
#elif defined PROGRAM_GLINT
    outColor = vec4(albedo * 0.8, color.a);
#elif defined PROGRAM_LIGHTNING
    // L'éclair : un trait blanc bleuté ; les couches du jeu s'additionnent, le cœur éblouit et le halo reste léger.
    outColor = vec4(vec3(0.80, 0.86, 1.0) * 1.8, color.a);
#elif defined PROGRAM_WEATHER
#if SHADER_RAIN > 0
    // La pluie et la neige sont dessinées par composite1 (lib/precip.glsl).
    discard;
#endif
    // Gouttes et flocons prennent la lumière du ciel : de fins traits gris clair, pas les traits bleus du jeu. Ceux
    // qui tombent tout contre l'œil s'effacent, sans quoi ils barrent l'écran.
    bool rain = color.b > color.r + 0.04;
    vec3 tone = rain ? vec3(0.92, 0.95, 1.0) * mix(0.75, 1.0, luma(color.rgb)) : vec3(luma(color.rgb)) * 1.05;
    vec3 light = gAmbient * 1.25 + gLightColor * 0.10 + vec3(0.025);
    float close = smoothstep(1.5, 6.0, length(vPos));
    outColor = vec4(tone * light, color.a * (rain ? 0.22 : 0.8) * mix(0.2, 1.0, close));
#elif defined PROGRAM_CLOUDS
#if CLOUD_STEPS > 0
    // Les nuages du jeu laissent la place à ceux du pack (composite).
    discard;
#endif
    float under = 0.75 + 0.25 * normal.y;
    outColor = vec4((gAmbient * 1.15 + gLightColor * 0.30 * max(gLightDir.y, 0.0)) * under, 0.82 * color.a);
#else
    float leaf = 0.0;
    int taps = SHADOW_FILTER;
#if defined PROGRAM_TERRAIN || defined PROGRAM_WATER
    albedo = olive(albedo);
    if ((vMat >= 1 && vMat <= 5) || vMat == 7) leaf = vMat == 4 ? 0.55 : 1.0;
#endif
#if defined PROGRAM_TERRAIN && GROUND_TINT > 0
    // Les nuances du paysage, sur ce qui n'est ni lumière ni neige.
    if (!emissive(vMat) && vMat != 9) albedo = groundTint(albedo, vPos + cameraPosition, groundGreen(vTint.rgb));
#endif
    // La matière : rugosité, métal, éclat. Les minerais et la blackstone dorée la prennent texel par texel : le
    // minéral brille, la pierre autour reste mate. Un métal garde sa couleur pour ses reflets, pas pour sa diffusion.
    Surface surf = surfaceOf(0);
    vec3 f0 = vec3(0.04);
    float texelGlow = 0.0;
    float glint = 0.0;
#if defined PROGRAM_TERRAIN && MATERIALS > 0
    surf = surfaceOf(vSurf);
    if (vMat == 9) surf = Surface(0.62, 0.0, 0.0, 0.0);
    else if (vMat == 11) surf = Surface(0.10, 0.0, 0.0, 0.0);
    else if (vMat == 12) surf = Surface(0.92, 0.0, 0.0, 0.0);
    if (vSurf == 12 || vSurf == 19) {
        vec3 mean = toLinear(textureLod(gtexture, vUv, 4.0).rgb * vTint.rgb);
        float mineral = mineralTexel(albedo, mean);
        surf.rough = mix(surf.rough, vSurf == 19 ? 0.28 : 0.22, mineral);
        surf.metal = vSurf == 19 ? mineral : mineral * 0.5;
        surf.gloss = 0.30 * mineral;
        glint = mineral;
    }
    // Les pierres précieuses et l'améthyste : toutes leurs facettes peuvent accrocher la lumière.
    if (vSurf == 10) glint = 0.7;
    else if (vSurf == 14) glint = 0.75;
    if (vSurf == 15) {
        // Le sculk : ses veines cyan luisent faiblement dans le noir.
        texelGlow = smoothstep(0.10, 0.30, (albedo.g + albedo.b) * 0.5 - albedo.r) * smoothstep(0.04, 0.16, luma(albedo));
    }
    // Un métal sombre (netherite) reste sombre dans ses reflets ; un métal clair (fer, or) reflète presque tout.
    f0 = mix(vec3(0.04), clamp(albedo * 1.7, 0.04, 1.0), surf.metal);
    if (vSurf == 10 || vSurf == 14) f0 = vec3(0.08);
#if RELIEF > 0 && LABPBR > 0
    // La carte de matière d'un pack labPBR remplace la table : lissé, réflectance ou métal, émission.
    if (pbr) {
        vec4 sm = textureGrad(specular, reliefUv, uvDx, uvDy);
        if (any(greaterThan(sm, vec4(0.002)))) {
            surf.rough = (1.0 - sm.r) * (1.0 - sm.r);
            surf.metal = sm.g * 255.0 >= 229.5 ? 1.0 : 0.0;
            surf.gloss = (1.0 - surf.rough) * 0.55;
            surf.sheen = 0.0;
            f0 = surf.metal > 0.5 ? clamp(albedo * 1.7, 0.04, 1.0) : vec3(sm.g);
            if (sm.a * 255.0 < 254.5) texelGlow = max(texelGlow, sm.a * 1.6);
        }
    }
#endif
    albedo *= 1.0 - 0.65 * surf.metal;
#endif
#if (defined PROGRAM_ENTITIES || defined PROGRAM_HAND) && MATERIALS > 0
    // Les outils, armures et lingots de métal ou de pierre précieuse, la créature de fer : leur matière, texel par
    // texel (le manche de bois d'une épée reste mat).
    {
        int kind = itemKind(currentRenderedItemId);
#if defined PROGRAM_ENTITIES
        if (kind == 0) kind = itemKind(entityId);
#endif
        if (kind > 0) {
            float part = itemTexel(kind, color.rgb);
            Surface m = surfaceOf(kind);
            // Un objet est plus clair que son bloc (une armure de fer presque blanche) : sa réflectance est tenue plus
            // bas, sans quoi il renvoie tout le ciel et blanchit.
            surf = Surface(mix(surf.rough, m.rough, part), m.metal * part, m.gloss * part * 0.6, 0.0);
            f0 = mix(vec3(0.04), clamp(albedo * 1.05, 0.04, 0.85), surf.metal);
            if (kind == 10) f0 = mix(f0, vec3(0.08), part);
            albedo *= 1.0 - 0.65 * surf.metal;
        }
    }
#endif
#if (defined PROGRAM_ENTITIES || defined PROGRAM_HAND) && WET_ENTITIES > 0
    // Sous la pluie à ciel ouvert, les êtres, leurs armures et le bras du joueur sont trempés : plus sombres, plus
    // saturés et luisants ; en sortant de l'eau, le joueur ruisselle, puis tout sèche en une demi-minute.
    {
        float soaked = rainSoak * smoothstep(0.80, 0.95, lm.y);
#if defined PROGRAM_HAND
        soaked = max(soaked, swimSoak);
#endif
        if (soaked > 0.01) {
            albedo = mix(vec3(luma(albedo)), albedo, 1.0 + 0.15 * soaked) * (1.0 - 0.28 * soaked * (1.0 - surf.metal));
            surf.gloss = max(surf.gloss, 0.40 * soaked);
            surf.rough = mix(surf.rough, min(surf.rough, 0.25), soaked);
        }
    }
#endif
#if defined PROGRAM_ENTITIES && SNOWY_ENTITIES > 0
    // Dans la neige qui tombe à ciel ouvert, les flocons se posent sur le dos des bêtes, les épaules et les casques :
    // des pixels blancs de leur propre texture (ils suivent l'être qui bouge), d'abord épars puis de plus en plus
    // nombreux, sur ce qui regarde le ciel.
    {
        float settle = snowSoak * smoothstep(0.80, 0.95, lm.y) * smoothstep(0.25, 0.75, normal.y);
        if (settle > 0.01) {
            vec2 texel = floor(vUv * vec2(textureSize(gtexture, 0)));
            float h = fract(sin(dot(texel, vec2(127.1, 311.7))) * 43758.5453);
            if (h < settle * 0.55) {
                albedo = mix(albedo, vec3(0.70, 0.73, 0.78), 0.85);
                surf.gloss = max(surf.gloss, 0.15);
            }
        }
    }
#endif
#if defined PROGRAM_TERRAIN
    // Le vent couche les herbes : un brin couché montre son revers, plus clair et plus jaune. Les rafales passent sur
    // une prairie en vagues claires.
    if (vMat >= 1 && vMat <= 4) albedo = mix(albedo, albedo * 1.35 + vec3(0.010, 0.012, 0.003), vBend.x * (vMat == 4 ? 0.25 : 0.5) * FOLIAGE_SHEEN);
#if PLANT_ROOTS > 0
    // Le pied des herbes et des fleurs : dans une touffe, les brins se font de l'ombre ; le bas reste sombre et un peu
    // plus froid, le haut prend toute la lumière. Une grande herbe s'assombrit sur sa moitié basse. Les plans couchés
    // (pétales au sol) n'ont pas de pied.
    if (vMat >= 1 && vMat <= 3 && abs(vNormal.y) < 0.5) {
        float h = vBend.y * (vMat == 1 ? 1.0 : 0.5);
        float shade = 1.0 - PLANT_ROOTS_DEPTH * (1.0 - smoothstep(0.0, 0.7, h));
        shade = mix(1.0, shade, 1.0 - smoothstep(64.0, 128.0, length(vPos)));
        albedo *= shade * mix(vec3(0.95, 1.0, 0.97), vec3(1.0), shade);
    }
#endif
#endif
#if defined PROGRAM_PARTICLES || defined PROGRAM_BASIC
    leaf = 1.0;
    taps = 1;
#endif
#ifdef PROGRAM_PARTICLES
    // Les feuilles qui tombent prennent le vert olive du feuillage dont elles viennent.
    albedo = olive(albedo);
    // Les gouttes et les éclaboussures du jeu sont d'un bleu de pictogramme : de l'eau claire n'a que la couleur de ce
    // qu'elle reflète, un gris à peine bleuté.
    float water = smoothstep(0.03, 0.20, albedo.b - max(albedo.r, albedo.g));
    albedo = mix(albedo, vec3(max(luma(albedo), albedo.b * 0.45)) * vec3(0.88, 0.95, 1.08), water * 0.8);
#endif
#ifdef PROGRAM_WATER
    if (vMat == 6) {
        outColor = water(vTint.rgb, normal, lm);
        return;
    }
    if (vMat == 10) {
#if ICE_DETAIL > 0
        // La glace claire : ses fissures et ses bulles sont blanches et couvrantes, le reste laisse voir au travers.
        vec2 iceD = iceDetail(vPos + cameraPosition, normal);
        float white = iceD.x * 0.6 + iceD.y * 0.8;
        albedo = mix(albedo, vec3(0.72, 0.80, 0.88), white);
        color.a = mix(color.a, 0.85, white);
#endif
        outColor = ice(albedo, color.a, normal, lm, ao);
        return;
    }
#endif
    // Sous la pluie, le sol fonce et luit ; dans les creux plats, des flaques (leur reflet : deferred1). La glace
    // tassée luit comme un miroir terni, la neige à peine.
    float gloss = 0.0;
    float sparkle = 0.0;
    float damp = 0.0;
#if defined PROGRAM_TERRAIN
    vec3 world = vPos + cameraPosition;
    float snow = 0.0;
    if (vMat == 0 || vMat == 4 || vMat == 9 || vMat == 14) snow = snowCover(world, normal, lm.y, vMat == 4 ? 1.0 : 0.0, luma(color.rgb / max(vTint.rgb, vec3(0.05))));
    // Le vent de la neige : celui du mod quand il est là, sinon un vent d'ouest régulier.
    vec2 snowWind = brumaireLink > 0.5 && dot(brumaireWind.xy, brumaireWind.xy) > 0.01 ? normalize(brumaireWind.xy) : vec2(0.8, 0.6);
    bool snowTop = normal.y > 0.9 && vMat != 4;
    // Pendant une chute, la neige se plaque aussi sur les faces qui font front au vent.
    if ((vMat == 0 || vMat == 14) && !snowTop) snow = max(snow, snowPlaster(world, normal, lm.y, snowWind, luma(color.rgb / max(vTint.rgb, vec3(0.05)))));
#if SNOW_SLOPES > 0 && defined VOXELS
    // Sur un coteau en paliers, la marche entre deux paliers enneigés blanchit aussi (lib/snow.glsl).
    if ((vMat == 0 || vMat == 14) && !snowTop) snow = max(snow, snowRiser(world, normal, lm.y));
#endif
#if SNOW_DETAIL > 0
    if (snowTop && snow > 0.0) {
        // Sous les arbres et autour des lumières, la neige ne fait plus un voile : elle se pose par plaques, et fond
        // en cercle près d'une torche.
        snow = snowPatch(world, snow * (1.0 - smoothstep(0.80, 0.94, lm.x)));
    }
#endif
    if (snow > 0.0) albedo = mix(albedo, snowTop ? snowAlbedo(world, snowWind) : vec3(0.64, 0.67, 0.72), snow);
    // La pente de la neige au texel : congères, rides du vent et grain, sur les dessus enneigés (proches).
    float snowBump = 0.0;
    vec3 snowN = normal;
#if SNOW_DETAIL > 0
    if (snowTop && (vMat == 9 || snow > 0.5)) {
        snowBump = 1.0 - smoothstep(20.0, 32.0, length(vPos));
        if (snowBump > 0.0) snowN = snowNormal(world, snowWind, snowBump);
    }
#endif
    // Le givre : les herbes et les feuillages blanchissent au pixel les matins froids.
    bool grass = vMat == 0 && vTint.g > vTint.b + 0.08;
    if (snow < 0.5 && (grass || (vMat >= 1 && vMat <= 4))) {
        float frost = frostCover(world, normal, lm, vMat >= 1 && vMat <= 3 ? 1.0 : 0.0, luma(color.rgb / max(vTint.rgb, vec3(0.05))));
        albedo = mix(albedo, vec3(0.56, 0.60, 0.66), frost * 0.7);
#if DEW > 0 || WET_FOLIAGE > 0
        // La rosée : les brins se couvrent de gouttes qui renvoient le soleil bas vers l'œil, une à une quand on bouge,
        // et l'herbe luit un peu.
        float dew = dewAmount(lm);
        float glintDensity = 0.06;
#if WET_FOLIAGE > 0
        // Après l'averse, feuilles et herbes restent couvertes de gouttes : quand le soleil revient, la forêt
        // scintille, et le feuillage trempé fonce un peu.
        float soaked = clamp(wetness * rainHere * WETNESS, 0.0, 1.0) * smoothstep(0.80, 0.95, lm.y);
        albedo *= 1.0 - 0.10 * soaked;
        // Les gouttes de pluie sont plus grosses et plus nombreuses que la rosée : trois fois plus d'éclats.
        if (soaked * (1.0 - 0.6 * rainStrength) > dew) { dew = soaked * (1.0 - 0.6 * rainStrength); glintDensity = 0.18; }
#endif
        dew *= 1.0 - frost;
        if (dew > 0.01 && (grass ? normal.y > 0.5 : true)) {
#if SNOW_DETAIL > 0
            sparkle = max(sparkle, snowGlints(world, normal, normalize(vPos), gLightDir, glintDensity * dew) * 0.6);
#else
            sparkle = max(sparkle, glitter(world, 0.004 * dew) * 0.5);
#endif
            gloss = max(gloss, 0.18 * dew);
            albedo *= 1.0 - 0.06 * dew;
#if DEW_HALO > 0
            // L'auréole de rosée : autour de l'ombre de la tête, chaque goutte renvoie le soleil droit vers l'œil et
            // l'herbe s'éclaire d'un halo blanc qui suit le regard.
            float fromShadow = acos(clamp(dot(normalize(vPos), -gLightDir), -1.0, 1.0));
            albedo *= 1.0 + 1.4 * dew * exp(-sq(fromShadow / 0.10)) * float(dot(gLightDir, gSunDir) > 0.5);
#endif
        }
#endif
    }
    if (vMat == 9 || snow > 0.5) {
        // La neige du jeu est d'un blanc pur : au soleil elle brûlerait l'image, sans plus rien laisser voir de son grain.
        if (vMat == 9) albedo *= snowTop ? snowAlbedo(world, snowWind) * 0.95 : vec3(0.60, 0.62, 0.66);
        gloss = 0.3;
#if SNOW_DETAIL > 0
        // Des facettes de glace qui renvoient l'astre vers l'œil : les éclats changent quand on bouge.
        sparkle = snowGlints(world, snowN, normalize(vPos), gLightDir, 0.05) * 0.45 * max(normal.y, 0.0);
#else
        sparkle = glitter(world, 0.003) * max(normal.y, 0.0);
#endif
    } else if (vMat == 11) {
        gloss = 0.75;
    } else if (vMat == 12) {
        // Le sable boit un peu de lumière et garde sa couleur, sinon il blanchit au soleil comme de la neige.
        albedo = mix(vec3(luma(albedo)), albedo, 1.25) * 0.55;
        sparkle = glitter(world, 0.0015) * max(normal.y, 0.0) * 0.6;
#if SAND_DETAIL > 0
        // Le vent ride le sable : des rides en travers du vent, dont la crête prend le soleil (proches seulement).
        if (normal.y > 0.9) {
            snowBump = (1.0 - smoothstep(20.0, 32.0, length(vPos))) * (1.0 - rainStrength * 0.7);
            if (snowBump > 0.0) {
                snowN = sandNormal(world, snowWind, snowBump);
                albedo *= mix(1.0, sandShade(world, snowWind), snowBump);
            }
        }
#endif
    }
#if ICE_DETAIL > 0
    if (vMat == 11) {
        // La glace tassée : des fissures blanches et des bulles prises dedans.
        vec2 iceD = iceDetail(world, normal);
        albedo = mix(albedo, vec3(0.70, 0.78, 0.86), iceD.x * 0.55 + iceD.y * 0.7);
    }
#endif
#if TRACKS > 0
    // Les pas creusent le sable et la neige : un creux un peu plus sombre, dont le bord prend la lumière.
    if ((vMat == 9 || vMat == 12 || snow > 0.5) && normal.y > 0.9) {
        float track = tracksAt(world, normal, frameTimeCounter, rainStrength * 3.0);
        // Au fond d'un pas, la neige tassée prend le bleu de l'ombre ; le sable remué est plus sombre.
        albedo *= mix(vec3(1.0), vMat == 12 ? vec3(0.76, 0.72, 0.66) : vec3(0.80, 0.86, 0.96), track);
        gloss *= 1.0 - 0.6 * track;
    }
#endif
    float wet = leaf > 0.0 || emissive(vMat) || (vMat >= 9 && vMat <= 13) || snow > 0.0 ? 0.0 : wetAt(lm.y);
    float rainWet = wet;
    float print = 0.0;
#if defined PROGRAM_TERRAIN && TRACKS > 0
    // Qui sort de l'eau laisse des pas humides sur le sol, sable compris : la semelle fonce et luit, puis sèche.
    if (normal.y > 0.9 && leaf == 0.0 && !emissive(vMat) && (vMat < 9 || vMat > 13 || vMat == 12) && snow < 0.5) {
        print = trackWet(world, frameTimeCounter, lm.y * lm.y * (1.0 - rainStrength)) * (1.0 - rainWet);
        wet = max(wet, 0.9 * print);
    }
#endif
#if defined PROGRAM_TERRAIN && SHORE_WET > 0 && defined VOXELS
    // Au bord de l'eau, le sol est mouillé, pluie ou pas.
    float shore = 0.0;
    if (leaf == 0.0 && !emissive(vMat) && (vMat < 9 || vMat > 13 || vMat == 12) && snow < 0.5) {
        shore = shoreWet(world, normal, frameTimeCounter) * (1.0 - rainWet);
        wet = max(wet, 0.9 * shore);
    }
#endif
    float splash = 0.0;
    if (wet > 0.0) {
        float puddle = rainWet > 0.0 ? puddleAt(vPos + cameraPosition, normal, rainWet) : 0.0;
        // Le relief du texel : sa clarté contre la moyenne de sa texture (un niveau de mipmap lointain).
        vec3 mean = toLinear(textureLod(gtexture, vUv, 4.0).rgb * vTint.rgb);
        float relief = clamp((luma(albedo) - luma(mean)) / max(luma(mean), 0.03) * 1.6, -1.0, 1.0);
        vec2 ws = rainWet > 0.0 ? wetSurface(world, normal, rainWet, relief) : vec2(0.0);
        ws = max(ws, vec2(0.9, 0.36) * print);
#if defined PROGRAM_TERRAIN && SHORE_WET > 0 && defined VOXELS
        ws = max(ws, vec2(0.85, 0.34) * shore);
        // Le sable mouillé fonce bien plus que la pierre.
        albedo *= 1.0 - (vMat == 12 ? 0.40 : 0.15) * shore;
#endif
        vec2 riv = rainWet > 0.0 ? rivuletAt(world, normal, frameTimeCounter, rainWet) : vec2(0.0);
        float soak = max(ws.x * mix(0.55, 1.0, max(normal.y, 0.0)), riv.x);
        albedo = mix(vec3(luma(albedo)), albedo, 1.0 + 0.2 * soak) * mix(1.0, 0.62, soak);
        albedo = mix(albedo, vec3(luma(albedo)) * 0.5, puddle * 0.8);
        gloss = max(max(ws.y, riv.y * 0.55), puddle);
#if PUDDLE_ICE > 0
        // Par nuit froide, la flaque gèle : une glace sombre et mate, rayée de fêlures blanches, ourlée de givre sur son
        // bord, qui ne renvoie plus qu'un reflet terne.
        if (puddle > 0.0) {
            float ice = puddleFreeze(lm) * puddle;
            if (ice > 0.01) {
                // Le bord : un texel voisin hors de la flaque.
                float inside = min(min(puddleAt(world + vec3(0.0625, 0.0, 0.0), normal, rainWet),
                                       puddleAt(world - vec3(0.0625, 0.0, 0.0), normal, rainWet)),
                                   min(puddleAt(world + vec3(0.0, 0.0, 0.0625), normal, rainWet),
                                       puddleAt(world - vec3(0.0, 0.0, 0.0625), normal, rainWet)));
                float rim = step(inside, puddle * 0.5);
                vec3 iceTint = vec3(0.30, 0.34, 0.38) * (1.0 + 0.9 * puddleCrack(world, normal));
                iceTint = mix(iceTint, vec3(0.75, 0.78, 0.82), rim);
                albedo = mix(albedo, iceTint, ice);
                gloss = mix(gloss, 0.10, ice);
            }
        }
#endif
        splash = rainWet > 0.0 ? splashAt(world, normal, frameTimeCounter, rainWet * rainStrength * rainHere) : 0.0;
        // Au soleil, une pierre claire trempée paraît encore blanche : une semelle mouillée fonce davantage.
        albedo *= 1.0 - 0.55 * print;
    }
#if CAVE_DETAIL > 0
    // Dans une grotte, la roche suinte : des plaques plus sombres et lisses, où la lumière des torches fait un reflet.
    if ((vMat == 0 || vMat == 14) && surf.metal < 0.5 && snow <= 0.0 && wet <= 0.0) {
        damp = caveDamp(world, normal, lm.y);
        albedo *= mix(1.0, 0.72, damp);
        surf.rough = mix(surf.rough, 0.16, damp);
    }
#endif
#endif
    float indirect;
    vec3 lit = shade(albedo, normal, lm, ao, vPos, leaf, gl_FragCoord.xy, taps, indirect);
#ifdef PROGRAM_PARTICLES
    // Certaines particules (les grandes feuilles de Particular qui tournent en tombant) sortaient presque noires de la
    // lumière complète : elles ne descendent jamais sous ce que la lumière du jeu leur donne, comme sans pack. Au
    // soleil, une feuille qui tombe est fine et le laisse passer : aussi claire que celles de l'arbre, pas une tache.
    vec3 plain = hemisphereLight(vec3(0.0, 1.0, 0.0), max(lm.y, 0.6)) * 1.3
        + gLightColor * max(gLightDir.y, 0.0) * 0.75 * smoothstep(0.70, 0.90, lm.y)
        // Près d'une torche, la feuille qui passe devant l'œil en prend la lueur, comme ce qui l'entoure.
        + lightTint(0) * lm.x * lm.x * 0.8;
    lit = max(lit, albedo * plain);
#endif
#if (defined PROGRAM_ENTITIES || defined PROGRAM_HAND) && MATERIALS > 0
    // Le reflet de l'astre sur un objet, et pour un métal ce qu'il reflète autour de lui, teint de sa couleur.
    if (surf.metal > 0.0 || surf.gloss > 0.0) {
        vec3 eye = -normalize(vPos);
        lit += gSunReach * sunSpecular(normal, eye, gLightDir, surf.rough, f0) * SPECULAR_STRENGTH;
        if (surf.metal > 0.0) {
            vec3 r = reflect(-eye, normal);
            vec3 env = hemisphereLight(normalize(mix(r, normal, surf.rough * surf.rough)), lm.y * lm.y) * 1.1;
            float near_ = lm.x;
            env += lightTint(0) * near_ * near_ * (0.30 + 2.3 * pow(near_, 3.0)) * 0.6;
            vec3 fm = mix(f0, vec3(1.0), pow(1.0 - max(dot(normal, eye), 0.0), 5.0) * 0.35);
#if ROUGH_REFLECTIONS > 0 && defined WRITE_SURFACE
            env *= 0.5;
#endif
            lit += env * fm * mix(1.0, 0.55, surf.rough) * surf.metal * ao * 0.6;
        }
        gloss = max(gloss, surf.gloss);
    }
#endif
#if defined PROGRAM_TERRAIN && RELIEF > 0
    if (reliefLight > 0.0) {
        // La lumière directe suit la pente du texel (rapportée à celle de la face, dont l'ombre est déjà comptée) et
        // l'ombre propre du relief ; la lumière du ciel baisse un peu dans les texels qui se détournent du dehors.
        float geoL = dot(normal, gLightDir);
        float ratio = geoL > 0.03 ? clamp(max(dot(reliefN, gLightDir), 0.0) / geoL, 0.0, 2.0) : 1.0;
        lit *= mix(1.0, ratio * (1.0 - 0.7 * reliefShade), 1.0 - indirect);
        lit *= 1.0 - 0.5 * (1.0 - dot(reliefN, normal));
    }
#endif
#if defined PROGRAM_TERRAIN && (SNOW_DETAIL > 0 || SAND_DETAIL > 0)
    if (snowBump > 0.0) {
        // La neige suit sa propre pente au soleil (la face, elle, est déjà ombrée), et ses creux à l'ombre prennent le
        // bleu du ciel, qui l'éclaire seul.
        float geoL = dot(normal, gLightDir);
        float ratio = geoL > 0.03 ? clamp(max(dot(snowN, gLightDir), 0.0) / geoL, 0.0, 2.0) : 1.0;
        lit *= mix(1.0, ratio, (1.0 - indirect) * snowBump);
    }
    if (vMat == 9 || snow > 0.5) {
        lit *= mix(vec3(1.0), vec3(0.86, 0.94, 1.12), indirect * 0.7);
        // La lumière qui traverse la couche : un bord à contre-jour s'éclaire doucement.
        float rim = pow(1.0 - max(dot(normal, -normalize(vPos)), 0.0), 3.0);
        lit += gLightColor * albedo * rim * 0.12 * (1.0 - indirect);
    }
#endif
#if defined PROGRAM_TERRAIN && ALPENGLOW > 0
    // Les cimes enneigées gardent le soleil couchant un moment après la vallée.
    if (vMat == 9 || snow > 0.3) lit += alpenglow(albedo, normal, world, lm.y * lm.y) * (vMat == 9 ? 1.0 : snow);
#endif
#if defined PROGRAM_TERRAIN && AURORA_GLOW > 0 && SKY_AURORA > 0
    // Les nuits d'aurore, le paysage à découvert prend son vert, la neige surtout.
    lit += auroraGlow(albedo, normal, world, lm.y * lm.y) * (1.0 + 0.6 * max(float(vMat == 9), snow)) * (1.0 - 0.5 * leaf);
#endif
#if defined PROGRAM_TERRAIN && MATERIALS > 0
    // Le reflet de l'astre, sur la pente du texel quand le relief ou la neige en donnent une. Un sol trempé devient
    // lisse : le soleil y fait une traînée vive.
    {
        vec3 nS = normal;
#if RELIEF > 0
        if (reliefLight > 0.0) nS = reliefN;
#endif
        if (snowBump > 0.0) nS = snowN;
        float rough = mix(surf.rough, 0.10, clamp(gloss, 0.0, 1.0) * float(wet > 0.0));
        vec3 eye = -normalize(vPos);
        lit += gSunReach * sunSpecular(nS, eye, gLightDir, rough, wet > 0.0 ? max(f0, vec3(0.02)) : f0) * SPECULAR_STRENGTH * (1.0 - leaf);
#if COLORED_LIGHT > 0 && BLOCK_LIGHT_SHAPE > 0
        // Le reflet des torches, de la lave et des lanternes, du côté d'où vient leur lumière : une lueur large sur la
        // pierre, un éclat net sur la roche mouillée et le métal.
        if (gBlockSide > 0.05 && leaf == 0.0 && !emissive(vMat)) {
            // Sur la roche mouillée, chaque texel penche un peu à sa façon : la lumière s'y accroche en points vifs,
            // sur un lustre plus large.
            vec3 nB = nS;
            if (damp > 0.0) {
                vec3 q = floor(world * 16.0 + normal * 0.5);
                vec3 jit = vec3(rainHash(q.xz + q.y * 7.3), rainHash(q.zy + 3.1), rainHash(q.yx * 1.7 + 5.9)) - 0.5;
                nB = normalize(nS + (jit - normal * dot(jit, normal)) * 0.55 * damp);
            }
            vec3 fB = max(f0, vec3(0.03));
            vec3 spec = sunSpecular(nB, eye, gBlockDir, max(rough, 0.12), fB)
                      + sunSpecular(nS, eye, gBlockDir, 0.40, fB) * 0.8 * damp;
            lit += gBlockReach * spec * gBlockSide * 3.0 * SPECULAR_STRENGTH;
        }
#endif
        // Ce que reflète un métal : le ciel dans la direction du reflet (assombri sous un toit) et la lumière des blocs
        // proches, teints de sa couleur. Une surface rugueuse mêle davantage d'hémisphère, plus terne.
        if (surf.metal > 0.0) {
            vec3 r = reflect(-eye, nS);
            vec3 env = hemisphereLight(normalize(mix(r, nS, rough * rough)), lm.y * lm.y) * 1.1;
            float near_ = lm.x;
            vec3 hue = blockLightTint(vPos, nS, near_);
            env += hue * near_ * near_ * (0.30 + 2.3 * pow(near_, 3.0)) * 0.6;
            // Au ras d'un métal, le reflet blanchit moins que sur une pierre : l'or reste doré vu de loin.
            vec3 fm = mix(f0, vec3(1.0), pow(1.0 - max(dot(nS, eye), 0.0), 5.0) * 0.35);
#if ROUGH_REFLECTIONS > 0
            // deferred1 y ajoute ce que le métal reflète vraiment (l'image, les blocs) : ici, la moitié de l'ambiance.
            env *= 0.5;
#endif
            lit += env * fm * mix(1.0, 0.55, rough) * surf.metal * ao;
        }
        // Le velours de la laine : un liseré clair au soleil rasant.
        lit += gSunReach * albedo * surf.sheen * pow(1.0 - max(dot(normal, eye), 0.0), 4.0) * 0.35;
        gloss = max(gloss, surf.gloss);
        lit += albedo * texelGlow * 1.4;
#if ORE_GLINTS > 0
        // Les paillettes des minerais et des pierres précieuses : chaque texel minéral a sa facette, qui renvoie en un
        // point vif la torche posée près de là, celle qu'on tient (la lumière vient alors des yeux) ou le soleil,
        // quand elle tombe dans le bon angle. En bougeant, d'autres facettes s'allument.
        if (glint > 0.0 && leaf == 0.0) {
            vec3 q = floor(world * 16.0 + normal * 0.5);
            vec3 jit = vec3(rainHash(q.xz + q.y * 3.7), rainHash(q.zy + 1.9), rainHash(q.yx * 2.3 + 4.1)) - 0.5;
            vec3 nG = normalize(normal + (jit - normal * dot(jit, normal)) * 0.8);
            vec3 toLight = gBlockSide > 0.05 ? normalize(mix(eye, gBlockDir, gBlockSide * 0.7)) : eye;
            vec3 reach = gBlockReach + gSunReach * max(dot(normal, gLightDir), 0.0);
            vec3 spark = gBlockReach * sunSpecular(nG, eye, toLight, 0.42, vec3(0.6))
                       + gSunReach * sunSpecular(nG, eye, gLightDir, 0.42, vec3(0.6));
            // Le scintillement : chaque facette a son angle, qu'on croise en bougeant ; à un instant, une sur douze brille.
            vec3 look = floor(eye * 7.0);
            float twinkle = step(0.92, rainHash(q.xy * 0.37 + q.z * 2.9 + look.xz * 1.7 + look.y * 5.1));
            spark += reach * twinkle * 2.5;
            // L'éclat prend un peu la couleur de la pierre.
            vec3 hue = mix(vec3(1.0), albedo / max(luma(albedo), 0.02), 0.4);
            lit += min(spark, vec3(5.0)) * hue * glint * 0.5 * float(ORE_GLINTS);
        }
#endif
    }
#endif
#ifdef WRITE_SURFACE
    // Le feuillage laisse passer la lumière : l'occlusion y est moitié moindre. L'alpha marque une surface (> 0.5) et
    // porte son éclat (1 : mat ; 0.55 : flaque, voir deferred1).
    // L'éclat au 1/255 ; un métal le marque d'un demi-pas de plus (relu par deferred1).
    float glossStep = floor(clamp(gloss, 0.0, 1.0) * 255.0 + 0.5) / 255.0;
    outSurface = vec4(packNormal(normal), indirect * (1.0 - 0.2 * leaf), 1.0 - 0.45 * (glossStep + step(0.5, surf.metal) * 0.5 / 255.0));
    outAlbedo = vec4(sqrt(clamp(albedo, 0.0, 1.0)), 1.0);
#endif
    // Les grains de neige et de sable qui scintillent, là où le soleil donne.
    lit += gLightColor * (1.0 - indirect) * sparkle * 2.5;
#if defined PROGRAM_TERRAIN && LEAF_FLUTTER > 0
    // Dans le vent, des feuilles se retournent et renvoient le soleil un instant : quelques texels du feuillage
    // s'allument et s'éteignent, plus souvent quand il souffle fort.
    if (vMat == 4) {
        float force = brumaireLink > 0.5 ? clamp(brumaireWind.z, 0.0, 2.0) : 0.3 + 0.45 * rainStrength;
        vec3 q = floor((vPos + cameraPosition) * 16.0 + normal * 0.5);
        float h = fract(sin(dot(q, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
        float beat = fract(frameTimeCounter * (0.8 + 1.6 * h) * (0.5 + force) + h * 7.0);
        float flash = step(0.86 - 0.06 * force, h) * smoothstep(0.0, 0.08, beat) * (1.0 - smoothstep(0.12, 0.3, beat));
        lit += gLightColor * albedo * flash * (1.0 - indirect) * 1.6 * min(force + 0.2, 1.0);
    }
#endif
#if defined PROGRAM_TERRAIN
    // Les gouttes qui éclatent : la lumière du ciel renvoyée par un pixel d'eau.
    lit += (gAmbient * 1.1 + gLightColor * 0.15 + vec3(0.008)) * splash;
#endif
#if defined PROGRAM_TERRAIN
    // Les blocs qui éclairent brillent par eux-mêmes.
#if CAVE_DETAIL > 0
    // La lave et le magma rougeoient selon leur chaleur ; les autres blocs qui éclairent gardent leur couleur vive.
    if (vMat == 17) lit = albedo * 0.15 + molten(color.rgb / max(vTint.rgb, vec3(0.05)), world, normal.y > 0.9, frameTimeCounter);
    else if (emissive(vMat)) {
        vec3 vivid = max(mix(vec3(luma(albedo)), albedo, 1.35), vec3(0.0));
        lit += vivid * 1.9 * smoothstep(0.22, 0.80, luma(color.rgb));
    }
#else
    if (emissive(vMat)) lit += albedo * 1.7 * smoothstep(0.25, 0.85, luma(color.rgb));
#endif
#endif
    outColor = vec4(lit, color.a);
#endif
}
