package net.invinciblemoebius.traumaparamedicinemod.ui;

import net.invinciblemoebius.traumaparamedicinemod.status.ConditionSeverity;

public class MoodleAnimState
{
    // Tween - Right to left.
    public static final float TWEEN_START = 20f;
    public static final float TWEEN_SPEED = 6f;
    public float xOffset = TWEEN_START;
    // Shake.
    public static final int SHAKE_DURATION = 12;
    public static final float SHAKE_AMPLITUDE = 3f;
    public int shakeTicks = 0;
    public float shakeOffset = 0f;
    // Glow pulse.
    public static final float GLOW_SPEED = 0.08f;
    public float glowPhase = 0f;
    // Fade out.
    public static final float FADE_SPEED = 0.08f;
    public float alpha = 0f;
    public boolean dying = false;
    // Severity.
    public ConditionSeverity lastSeverity = null;

    // === METHODS ===

    public boolean tick()
    {
        // Tween in.
        if (xOffset > 0f)
            xOffset = Math.max(0f, xOffset - TWEEN_SPEED);

        // Shake
        if (shakeTicks > 0)
        {
            shakeTicks--;
            float progress = (float)  shakeTicks / SHAKE_DURATION;
            shakeOffset = (float) (Math.sin(progress * Math.PI * 4) * SHAKE_AMPLITUDE * progress);
        }
        else
        {
            shakeOffset = 0f;
        }

        // Glow.
        glowPhase += GLOW_SPEED;
        if (glowPhase > Math.PI * 2)
            glowPhase -= (float) (Math.PI * 2);

        // Removal / Alpha degeneration.
        if (dying)
        {
            alpha = Math.max(0f, alpha - FADE_SPEED);
            return alpha <= 0f;
        }
        else
        {
            alpha = Math.min(1f, alpha + FADE_SPEED * 2f);
            return false;
        }
    }

    // Triggers the shake animation.
    public void triggerShake()
    {
        shakeTicks = SHAKE_DURATION;
    }

    // Computes the glow.
    public float glowAlpha()
    {
        // Goes between 0.3 and 1.0.
        return 0.3f + (float) (Math.sin(glowPhase) * 0.35f + 0.35f);
    }
}
