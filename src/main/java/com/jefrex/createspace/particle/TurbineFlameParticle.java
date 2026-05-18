package com.jefrex.createspace.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class TurbineFlameParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private float initialSize;
    private boolean hasCollided = false;

    protected TurbineFlameParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz,
                                   SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = 35 + (int)(Math.random() * 20);
        this.initialSize = 0.3f + (float)(Math.random() * 0.1f);
        this.quadSize = this.initialSize;
        this.alpha = 1.0f;
        this.gravity = 0.0f;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        int frameCount = 8;
        int frame = (int)((this.age / (float) this.lifetime) * frameCount);
        frame = Math.min(frame, frameCount - 1);
        this.setSprite(sprites.get(frame, frameCount));

        float progress = this.age / (float) this.lifetime;

        if (progress < 0.05f) {
            // швидко росте до 1.5x
            this.quadSize = initialSize + (progress / 0.05f) * (initialSize * 0.5f);
        } else if (progress < 0.35f) {
            // тримає 1.5x
            this.quadSize = initialSize * 1.5f;
        } else if (progress < 0.9f) {
            // повільно росте
            float t = (progress - 0.35f) / 0.55f;
            this.quadSize = initialSize * 1.5f + t * (initialSize * 2f);
        } else {
            // зменшується і розлітається
            float t = (progress - 0.9f) / 0.1f;
            this.quadSize = (initialSize * 3.5f) * (1.0f - t);

            this.xd += (Math.random() - 0.5) * 0.05;
            this.yd += (Math.random() - 0.5) * 0.05;
            this.zd += (Math.random() - 0.5) * 0.05;
        }

        if (progress > 0.6f) {
            float scatter = (progress - 0.6f) / 0.4f; // 0.0 -> 1.0
            this.xd += (Math.random() - 0.5) * 0.05 * scatter;
            this.yd += (Math.random() - 0.5) * 0.05 * scatter;
            this.zd += (Math.random() - 0.5) * 0.05 * scatter;
        }



        double nextX = this.x + this.xd;
        double nextY = this.y + this.yd;
        double nextZ = this.z + this.zd;

        if (!hasCollided && isBlockAt(nextX, nextY, nextZ)) {
            hasCollided = true;

            Vec3 dir = new Vec3(xd, yd, zd).normalize();
            Vec3 perp1 = dir.cross(new Vec3(0, 1, 0)).normalize();
            if (perp1.lengthSqr() < 0.01) {
                perp1 = dir.cross(new Vec3(1, 0, 0)).normalize();
            }

            double speed = Math.sqrt(xd * xd + yd * yd + zd * zd);
            double angle = Math.toRadians(Math.random() * 360);
            Vec3 perp2 = dir.cross(perp1).normalize();

            this.xd = (perp1.x * Math.cos(angle) + perp2.x * Math.sin(angle)) * speed;
            this.yd = (perp1.y * Math.cos(angle) + perp2.y * Math.sin(angle)) * speed + 0.05;
            this.zd = (perp1.z * Math.cos(angle) + perp2.z * Math.sin(angle)) * speed;

            return;
        }

        this.xd *= 0.99;
        this.yd *= 0.99;
        this.zd *= 0.99;

        this.move(this.xd, this.yd, this.zd);
    }

    private boolean isBlockAt(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        return !this.level.getBlockState(pos).isAir();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880; // максимальна яскравість (15 << 20 | 15 << 4)
    }

    public static class Provider implements ParticleProvider<TurbineFlameParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(TurbineFlameParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new TurbineFlameParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}