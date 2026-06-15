package net.invinciblemoebius.traumaparamedicinemod.wounding;


import net.invinciblemoebius.traumaparamedicinemod.limbs.BoneState;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundDepth;
import static net.invinciblemoebius.traumaparamedicinemod.wound.WoundType.*;
import static net.invinciblemoebius.traumaparamedicinemod.wound.WoundDepth.*;
import static net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode.*;
import java.util.Random;

// All the wounds and physiological responses to attack events.
// This works through a roll-check system, to still allow serious
// realistic wounds without making it feel like CBT.
public final class WoundingBehavior
{
    private static final Random RNG = new Random();

    // === ENTRY ===
    public static void handle(WoundingContext ctx)
    {
        switch (ctx.category)
        {
            case MELEE_UNARMED -> handleUnarmedMelee(ctx);
            case MELEE_BLADED ->  handleBladedMelee(ctx);

            // Nhhh... I don't feel like doing the rest of the
            // damage types so it'll all get routed here for now.
            default -> handleBladedMelee(ctx);
        }
    }

    // === MELEE, UNARMED ===
    // As is the case IRL, the puncher takes more damage than the punchee (handled in PlayerEventEffects).
    // Still, they're mostly blunt force impacts with a chance for knockout or vagal response.

    private static void handleUnarmedMelee(WoundingContext ctx)
    {
        LimbData limb = ctx.data.getLimb(ctx.attackedNode);
        if (limb == null) return;
        float roll = RNG.nextFloat();
        boolean critical = ctx.preArmorDamage > 1.2f; // Crit punch or Strength I+ punch.

        if (ctx.isHeadHit())
        {
            unarmedHeadHit(ctx, limb, roll, critical);
            return;
        }
        if (ctx.attackedNode == LOWER_TORSO)
        {
            unarmedStomachHit(ctx, limb, roll, critical);
            return;
        }
        if (ctx.attackedNode == UPPER_TORSO)
        {
            unarmedChestHit(ctx, limb, roll, critical);
            return;
        }
        if (ctx.isExtremityHit())
        {
            unarmedExtremity(ctx, limb, roll, critical);
            return;
        }

        // Generic limb wound, just a bruise and almost nothing happens.
        new WoundingInstruction(BLUNT, SUBDERMAL, 0.05f + RNG.nextFloat() * 0.08f)
                .muscleHealthDamage(0.02f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(limb, ctx.data);
    }

    private static void unarmedHeadHit(WoundingContext ctx, LimbData limb, float roll, boolean critical)
    {
        // Crits roughly triple the special-outcome window.
        float critMult = critical ? 3.0f : 1.0f;

        // Skull hairline fracture. 0.5% base chance, 1.5% with crit.
        if (roll < 0.005 * critMult)
        {
            new WoundingInstruction(BLUNT, SUBDERMAL, 0.15f)
                    .consciousnessDrop(0.50f)
                    .muscleHealthDamage(0.15f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);
        }

        // Knockout. 6% base, 18% crit.
        if (roll < 0.06 * critMult)
        {
            new WoundingInstruction(BLUNT, DERMAL, 0.10f)
                    .consciousnessDrop(critical ? 0.40f : 0.25f)
                    .muscleHealthDamage(0.08f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);
        }

        // Generic non-special wound.
        new WoundingInstruction(BLUNT, SUBDERMAL, 0.05f + RNG.nextFloat() * 0.05f)
                .muscleHealthDamage(0.03f)
                .painSpike(0.08f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(limb, ctx.data);
    }

    private static void unarmedStomachHit(WoundingContext ctx, LimbData limb, float roll, boolean critical)
    {
        float critMult = critical ? 2.0f : 1.0f;

        // Liver shot. 5% base, 10% on crit.
        if (roll < 0.05 * critMult)
        {
            new WoundingInstruction(BLUNT, MUSCULAR, 0.2f)
                    .painSpike(critical ? 0.65f : 0.50f)
                    .consciousnessDrop(0.20f)
                    .vascularToneDelta(0.3f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            return;
        }

        // Generic wound.
        new WoundingInstruction(BLUNT, SUBDERMAL, 0.1f)
                .painSpike(0.12f)
                .muscleHealthDamage(0.03f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(limb, ctx.data);
    }

    public static void unarmedChestHit(WoundingContext ctx, LimbData limb, float roll, boolean critical)
    {
        float critMult = critical ? 2.5f : 1.0f;

        // ULTRA rare 0.5% - 1.25% chance of causing heart fibrillations
        if (roll < 0.005 * critMult)
        {
            new WoundingInstruction(BLUNT, VISCERAL, 0f)
                    .givesFibrillations(0.50f, false)
                    .applyMutationsOnly(ctx.data);
        }

        // Potential internal bleeding, 2% base to 5% crit.
        if (roll < 0.02f * critMult)
        {
            new WoundingInstruction(BLUNT, VISCERAL, 0.12f * RNG.nextFloat() * 0.10f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);
        }

        // Solar plexus hit, 8% - 20% chance. Has a return to make it fair.
        if (roll < 0.08f * critMult)
        {
            new WoundingInstruction(BLUNT, SUBDERMAL, 0.15f)
                    .painSpike(0.25f)
                    .muscleHealthDamage(0.05f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            return;
        }

        // Regular body blow.
        new WoundingInstruction(BLUNT, SUBDERMAL, 0.06f + RNG.nextFloat() * 0.06f)
                .painSpike(0.08f)
                .muscleHealthDamage(0.02f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(limb, ctx.data);
    }

    private static void unarmedExtremity(WoundingContext ctx, LimbData limb, float roll, boolean critical)
    {
        float critMult = critical ? 2.5f : 1.0f;

        // 1% - 2.5% check for a full-on fracture.
        if (roll < 0.01 * critMult)
        {
            new WoundingInstruction(BLUNT, SUBDERMAL, 0.12f)
                    .painSpike(0.20f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            if (limb.getBoneState().ordinal() < BoneState.FRACTURED.ordinal())
                limb.setBoneState(BoneState.FRACTURED);

            return;
        }

        // 5% to 12% chance of a metacarpal fracture. Modeled as just a dislocation,
        // since i don't model individual bones that have relative severity.
        if (roll < 0.05f * critMult)
        {
            new WoundingInstruction(BLUNT, DERMAL, 0.08f)
                    .painSpike(0.15f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            if (limb.getBoneState().ordinal() < BoneState.DISLOCATED.ordinal())
                limb.setBoneState(BoneState.DISLOCATED);

            return;
        }

        // Baseline is just a basic knock.
        new WoundingInstruction(BLUNT, SUPERFICIAL, 0.04f * RNG.nextFloat() * 0.05f)
                .muscleHealthDamage(0.01f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(limb, ctx.data);
    }

    // === MELEE - BLADED ===
    // Armor changes wound type. A blade stopped by hard armor becomes BABT.
    private static void handleBladedMelee(WoundingContext ctx)
    {
        LimbData limb = ctx.data.getLimb(ctx.attackedNode);
        if (limb == null) return;
        float roll = RNG.nextFloat();
        float dmg = ctx.preArmorDamage;

        switch (ctx.nodeArmorTier)
        {
            case NONE -> bladedNoArmor(ctx, limb, roll, dmg);
            case LEATHER -> bladedLeather(ctx, limb, roll, dmg);
            case CHAIN -> bladedChain(ctx, limb, roll, dmg);
            case HARD -> bladedHardArmor(ctx, limb, roll, dmg);
        }
    }

    private static void bladedNoArmor(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 1.5% chance of hitting an artery.
        if (roll < 0.015f)
        {
            new WoundingInstruction(LACERATION, ARTERIAL, 0.7f + RNG.nextFloat() * 0.3f)
                    .withContamination(0.10f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            // Another roll checks if it causes temporary fibs. Has a net 0.45% chance of occurring.
            if (RNG.nextFloat() < 0.30f)
            {
                new WoundingInstruction(LACERATION, ARTERIAL, 0f)
                        .givesFibrillations(0.50f, false)
                        .applyMutationsOnly(ctx.data);
            }

            return;
        }

        // 8% chance of a deep muscular wound.
        if (roll < 0.08f)
        {
            new WoundingInstruction(LACERATION, MUSCULAR, 0.5f + RNG.nextFloat() * 0.3f)
                    .withContamination(0.10f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            // Another roll checks if a solid hit will give a fractured bone. 3.2% chance of occurring.
            if (dmg >= 7f && RNG.nextFloat() < 0.40f)
                if (limb.getBoneState().ordinal() < BoneState.FRACTURED.ordinal())
                    limb.setBoneState(BoneState.FRACTURED);

            return;
        }

        // 25% chance of a solid laceration with strong bleeding.
        if (roll < 0.25f)
        {
            new WoundingInstruction(LACERATION, depthFromDamage(dmg), 0.35f + RNG.nextFloat() * 0.25f)
                    .withContamination(0.08f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);
        }

        // Regular wound is just a minor laceration.
        new WoundingInstruction(LACERATION, depthFromDamage(dmg * 0.6f), 0.15f + RNG.nextFloat() * 0.15f)
                .withContamination(0.06f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(limb, ctx.data);
    }

    private static void bladedLeather(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 4% chance of a deep cut through a gap, plus bruising.
        if (roll < 0.04f)
        {
            new WoundingInstruction(LACERATION, MUSCULAR, 0.4f + RNG.nextFloat() * 0.2f)
                    .withContamination(0.12f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);
            new WoundingInstruction(BLUNT, SUBDERMAL, dmg / 24f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            return;
        }

        // 25% chance of partial penetration with blunt force transfer.
        if (roll < 0.25f)
        {
            new WoundingInstruction(LACERATION, depthFromDamage(ctx.postArmorDamage), 0.3f + RNG.nextFloat() * 0.2f)
                    .withContamination(0.12f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);
            new WoundingInstruction(BLUNT, SUBDERMAL, dmg / 28f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            return;
        }

        // Typical leather hit is just a glancing abrasion, leather absorbs most of the force.
        new WoundingInstruction(ABRASION, SUPERFICIAL, 0.1f + RNG.nextFloat() * 0.15f)
                .withContamination(0.10f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(limb, ctx.data);
    }

    private static void bladedChain(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 3% chance of a blow so forceful that rings embed into the skin.
        if (roll < 0.03f)
        {
            new WoundingInstruction(LACERATION, SUBDERMAL, 0.25f + RNG.nextFloat() * 0.15f)
                    .withContamination(0.18f)
                    .withShrapnel()
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);
            new WoundingInstruction(BLUNT, SUBDERMAL, dmg / 20f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            return;
        }

        // 15% chance of the tip of the blade getting caught inside the loop of the ring.
        if (roll < 0.15f)
        {
            new WoundingInstruction(LACERATION, SUPERFICIAL, ctx.postArmorDamage / 22f)
                    .withContamination(0.10f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            return;
        }

        // Typical wound gets deflected, only a minor abrasion happens due to
        // the rings scraping the skin.
        new WoundingInstruction(ABRASION, SUPERFICIAL, 0.05f + RNG.nextFloat() * 0.08f)
                .withContamination(0.06f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(limb, ctx.data);
    }

    private static void bladedHardArmor(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 4% chance of serious blunt force transfer causing internal bleeding.
        if (roll < 0.04f && ctx.isTorsoHit() && dmg >= 8f)
        {
            new WoundingInstruction(BLUNT, depthFromDamage(dmg * 0.6f), dmg / 22f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            new WoundingInstruction(BLUNT, VISCERAL, 0.2f + RNG.nextFloat() * 0.2f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            return;
        }

        // 15% chance of a hard blunt impact causing a fracture.
        if (roll < 0.19f)
        {
            new WoundingInstruction(BLUNT, depthFromDamage(dmg * 0.6f), dmg / 22f)
                    .atPosition(ctx.hitU, ctx.hitV)
                    .apply(limb, ctx.data);

            if (dmg >= 9f)
                if (limb.getBoneState().ordinal() < BoneState.FRACTURED.ordinal())
                    limb.setBoneState(BoneState.FRACTURED);

            // Minor roll check to see if the armor's edge nicks the unarmored bits.
            if (RNG.nextFloat() < 0.15f)
                applyArmorEdgeTrauma(ctx);

            return;
        }

        // Typical wound is that the armor absorbs the impact, only blunt force damage.
        new WoundingInstruction(BLUNT, SUBDERMAL, dmg / 30f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(limb, ctx.data);
    }

    // === HELPER METHODS ===

    // Used to get the full potential of an attack, how deep it would've gone without armor.
    private static WoundDepth depthFromDamage(float damage)
    {
        if (damage < 2f) return WoundDepth.SUPERFICIAL;
        if (damage < 5f) return WoundDepth.DERMAL;
        if (damage < 9f) return WoundDepth.SUBDERMAL;
        if (damage < 14f) return WoundDepth.MUSCULAR;

        return WoundDepth.ARTERIAL;
    }

    // Wraps coords into 0.0 to 2.0. Used for entry-exit wound placement.
    private static float wrapU(float u)
    {
        return ((u % 2.0f) + 2.0f) % 2.0f;
    }

    private static void applyArmorEdgeTrauma(WoundingContext ctx)
    {
        LimbNode edgeNode = adjacentUnarmored(ctx);
        if (edgeNode == null) return;

        LimbData edgeLimb = ctx.data.getLimb(edgeNode);
        if (edgeLimb == null) return;

        new WoundingInstruction(LACERATION, SUPERFICIAL,
                0.08f + (RNG.nextFloat() * 0.10f))
                .withContamination(0.05f)
                .atPosition(ctx.hitU, ctx.hitV)
                .apply(edgeLimb, ctx.data);
    }

    private static LimbNode adjacentUnarmored(WoundingContext ctx)
    {
        LimbNode proximal = ctx.attackedNode.proximalNode;
        if (proximal != null && armorTierFor(proximal, ctx) == ArmorTier.NONE)
            return proximal;

        for (LimbNode node: LimbNode.values())
        {
            if (node.proximalNode == ctx.attackedNode && armorTierFor(node, ctx) == ArmorTier.NONE)
                return node;
        }

        return null;
    }

    private static ArmorTier armorTierFor(LimbNode node, WoundingContext ctx)
    {
        return switch (node.category)
        {
            case HEAD -> ctx.helmetTier;
            case TRUNK -> ctx.chestTier;
            case ARM -> ctx.chestTier;
            case LEG -> node.proximalNode == LimbNode.GROIN ? ctx.leggingsTier : ctx.bootsTier;
        };
    }
}
