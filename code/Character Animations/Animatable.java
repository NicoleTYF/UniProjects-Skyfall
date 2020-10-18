package deco2800.skyfall.animation;

/**
 * Should only be implemented by the Abstract Entity sub classes
 */
public interface Animatable {
    /**
     * Should popualate the animations Map object in abstract entity
     * with animation linker objects. Linking direction and animation role together
     * to be used by the renderer. If the animation cannot be found the renderer
     * it defaults to the defaultDirectionTexture objectr.
     */
    void configureAnimations();

    /**
     * Default direction textures, should populate the defaultDirectionTextures
     * with all 8 cardinal directions and  texture names.
     */
    void setDirectionTextures();
}
