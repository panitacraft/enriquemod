package com.panita.enriquecraft.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A whole player as the inventory shows one, turning to follow the mouse: the player who is viewing the screen, with the skin the server
 * gave them, or another player with the skin in a profile the server sent. Inert, like the other decorations.
 */
final class UiPlayerModelWidget extends AbstractWidget {

    /** The size the game gives the model for each pixel of height of its box, as the inventory does. */
    private static final float SIZE_PER_HEIGHT = 0.4F;
    private static final float OFFSET_Y = 0.0625F;
    private static final float TURN_DISTANCE = 40.0F;
    private static final float TURN_DEGREES = 20.0F;

    // The same player is shown again at each redraw of the screen, so it is made once
    private static @Nullable ProfileModelPlayer lastShown;
    private static @Nullable ClientLevel lastLevel;

    private final @Nullable ResolvableProfile profile;

    /** The model of the player who is viewing the screen. */
    UiPlayerModelWidget(int width, int height) {
        this(width, height, null);
    }

    /** The model of the player whose profile this is. */
    UiPlayerModelWidget(int width, int height, @Nullable ResolvableProfile profile) {
        super(0, 0, width, height, Component.empty());
        this.profile = profile;
        active = false;
    }

    private @Nullable LivingEntity entity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (profile == null || minecraft.level == null) {
            return minecraft.player;
        }
        if (lastShown == null || lastLevel != minecraft.level || !lastShown.profile().equals(profile.partialProfile())) {
            lastShown = new ProfileModelPlayer(minecraft.level, profile);
            lastLevel = minecraft.level;
        }
        return lastShown;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        LivingEntity entity = entity();
        if (entity == null) {
            return;
        }
        // As the inventory screen draws its player, but without the name over the head that a player who is not the camera gets
        float centerX = (getX() + getX() + getWidth()) / 2.0F;
        float centerY = (getY() + getY() + getHeight()) / 2.0F;
        float xAngle = (float) Math.atan((centerX - mouseX) / TURN_DISTANCE);
        float yAngle = (float) Math.atan((centerY - mouseY) / TURN_DISTANCE);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf xRotation = new Quaternionf().rotateX(yAngle * TURN_DEGREES * (float) (Math.PI / 180.0));
        rotation.mul(xRotation);
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState state = renderer.createRenderState(entity, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        state.scoreText = null;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0F + xAngle * TURN_DEGREES;
            living.yRot = xAngle * TURN_DEGREES;
            living.xRot = living.pose != Pose.FALL_FLYING ? -yAngle * TURN_DEGREES : 0.0F;
            living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
            living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
            living.scale = 1.0F;
        }
        Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + OFFSET_Y, 0.0F);
        graphics.entity(state, Math.round(getHeight() * SIZE_PER_HEIGHT), translation, rotation, xRotation,
                getX(), getY(), getX() + getWidth(), getY() + getHeight());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
