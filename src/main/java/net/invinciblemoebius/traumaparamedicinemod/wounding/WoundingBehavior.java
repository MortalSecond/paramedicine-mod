package net.invinciblemoebius.traumaparamedicinemod.wounding;


import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.limbs.BoneState;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LungData;
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
        switch (ctx.category())
        {
            case MELEE_UNARMED -> handleUnarmedMelee(ctx);
            case MELEE_BLADED -> handleBladedMelee(ctx);
            case MELEE_BLUNT -> handleBluntMelee(ctx);
            case PROJECTILE_ARROW, PROJECTILE_TRIDENT -> handleProjectileArrow(ctx);
            case EXPLOSION -> handleExplosion(ctx);
            case FALL -> handleFall(ctx);

            // Until i get around to doing the rest of the wounds,
            // they'll get voided for now.
            default ->
            {
                return;
            }
        }
    }

    // === MELEE, UNARMED ===
    // As is the case IRL, the puncher takes more damage than the punchee (handled in PlayerEventEffects).
    // Still, they're mostly blunt force impacts with a chance for knockout or vagal response.

    private static void handleUnarmedMelee(WoundingContext ctx)
    {
        LimbData limb = ctx.data().getLimb(ctx.attackedNode());
        if (limb == null) return;
        float roll = RNG.nextFloat();
        boolean critical = ctx.preArmorDamage() > 1.2f; // Crit punch or Strength I+ punch.

        if (ctx.isHeadHit())
        {
            unarmedHeadHit(ctx, limb, roll, critical);
            return;
        }
        if (ctx.attackedNode() == LOWER_TORSO)
        {
            unarmedStomachHit(ctx, limb, roll, critical);
            return;
        }
        if (ctx.attackedNode() == UPPER_TORSO)
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
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
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
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
        }

        // Knockout. 6% base, 18% crit.
        if (roll < 0.06 * critMult)
        {
            new WoundingInstruction(BLUNT, DERMAL, 0.10f)
                    .consciousnessDrop(critical ? 0.40f : 0.25f)
                    .muscleHealthDamage(0.08f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
        }

        // Generic non-special wound.
        new WoundingInstruction(BLUNT, SUBDERMAL, 0.05f + RNG.nextFloat() * 0.05f)
                .muscleHealthDamage(0.03f)
                .painSpike(0.08f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
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
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // Generic wound.
        new WoundingInstruction(BLUNT, SUBDERMAL, 0.1f)
                .painSpike(0.12f)
                .muscleHealthDamage(0.03f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    public static void unarmedChestHit(WoundingContext ctx, LimbData limb, float roll, boolean critical)
    {
        float critMult = critical ? 2.5f : 1.0f;

        // ULTRA rare 0.5% - 1.25% chance of causing heart fibrillations
        if (roll < 0.005 * critMult)
        {
            new WoundingInstruction(BLUNT, VISCERAL, 0f)
                    .givesFibrillations(0.50f)
                    .applyMutationsOnly(ctx.data());
        }

        // Potential internal bleeding, 2% base to 5% crit.
        if (roll < 0.02f * critMult)
        {
            new WoundingInstruction(BLUNT, VISCERAL, 0.12f * RNG.nextFloat() * 0.10f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
        }

        // Solar plexus hit, 8% - 20% chance. Has a return to make it fair.
        if (roll < 0.08f * critMult)
        {
            new WoundingInstruction(BLUNT, SUBDERMAL, 0.15f)
                    .painSpike(0.25f)
                    .muscleHealthDamage(0.05f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // Regular body blow.
        new WoundingInstruction(BLUNT, SUBDERMAL, 0.06f + RNG.nextFloat() * 0.06f)
                .painSpike(0.08f)
                .muscleHealthDamage(0.02f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    private static void unarmedExtremity(WoundingContext ctx, LimbData limb, float roll, boolean critical)
    {
        float critMult = critical ? 2.5f : 1.0f;

        // 1% - 2.5% check for a full-on fracture.
        if (roll < 0.01 * critMult)
        {
            new WoundingInstruction(BLUNT, SUBDERMAL, 0.12f)
                    .painSpike(0.20f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

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
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            if (limb.getBoneState().ordinal() < BoneState.DISLOCATED.ordinal())
                limb.setBoneState(BoneState.DISLOCATED);

            return;
        }

        // Baseline is just a basic knock.
        new WoundingInstruction(BLUNT, SUPERFICIAL, 0.04f * RNG.nextFloat() * 0.05f)
                .muscleHealthDamage(0.01f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    // === MELEE - BLADED ===
    // Armor changes wound type. A blade stopped by hard armor becomes BABT.

    private static void handleBladedMelee(WoundingContext ctx)
    {
        LimbData limb = ctx.data().getLimb(ctx.attackedNode());
        if (limb == null) return;
        float roll = RNG.nextFloat();
        float dmg = ctx.preArmorDamage();

        switch (ctx.nodeArmorTier())
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
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            // Another roll checks if it causes temporary fibs. Has a net 0.45% chance of occurring.
            if (RNG.nextFloat() < 0.30f)
            {
                new WoundingInstruction(LACERATION, ARTERIAL, 0f)
                        .givesFibrillations(0.50f)
                        .applyMutationsOnly(ctx.data());
            }

            return;
        }

        // 8% chance of a deep muscular wound.
        if (roll < 0.08f)
        {
            new WoundingInstruction(LACERATION, MUSCULAR, 0.5f + RNG.nextFloat() * 0.3f)
                    .withContamination(0.10f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

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
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
        }

        // Regular wound is just a minor laceration.
        new WoundingInstruction(LACERATION, depthFromDamage(dmg * 0.6f), 0.15f + RNG.nextFloat() * 0.15f)
                .withContamination(0.06f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    private static void bladedLeather(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 4% chance of a deep cut through a gap, plus bruising.
        if (roll < 0.04f)
        {
            new WoundingInstruction(LACERATION, MUSCULAR, 0.4f + RNG.nextFloat() * 0.2f)
                    .withContamination(0.12f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
            new WoundingInstruction(BLUNT, SUBDERMAL, dmg / 24f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // 25% chance of partial penetration with blunt force transfer.
        if (roll < 0.25f)
        {
            new WoundingInstruction(LACERATION, depthFromDamage(ctx.postArmorDamage()), 0.3f + RNG.nextFloat() * 0.2f)
                    .withContamination(0.12f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
            new WoundingInstruction(BLUNT, SUBDERMAL, dmg / 28f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // Typical leather hit is just a glancing abrasion, leather absorbs most of the force.
        new WoundingInstruction(ABRASION, SUPERFICIAL, 0.1f + RNG.nextFloat() * 0.15f)
                .withContamination(0.10f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    private static void bladedChain(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 3% chance of a blow so forceful that rings embed into the skin.
        if (roll < 0.03f)
        {
            new WoundingInstruction(LACERATION, SUBDERMAL, 0.25f + RNG.nextFloat() * 0.15f)
                    .withContamination(0.18f)
                    .withShrapnel()
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
            new WoundingInstruction(BLUNT, SUBDERMAL, dmg / 20f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // 15% chance of the tip of the blade getting caught inside the loop of the ring.
        if (roll < 0.15f)
        {
            new WoundingInstruction(LACERATION, SUPERFICIAL, ctx.postArmorDamage() / 22f)
                    .withContamination(0.10f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // Typical wound gets deflected, only a minor abrasion happens due to
        // the rings scraping the skin.
        new WoundingInstruction(ABRASION, SUPERFICIAL, 0.05f + RNG.nextFloat() * 0.08f)
                .withContamination(0.06f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    private static void bladedHardArmor(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 4% chance of serious blunt force transfer causing internal bleeding.
        if (roll < 0.04f && ctx.isTorsoHit() && dmg >= 8f)
        {
            new WoundingInstruction(BLUNT, depthFromDamage(dmg * 0.6f), dmg / 22f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            new WoundingInstruction(BLUNT, VISCERAL, 0.2f + RNG.nextFloat() * 0.2f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // 15% chance of a hard blunt impact causing a fracture.
        if (roll < 0.19f)
        {
            new WoundingInstruction(BLUNT, depthFromDamage(dmg * 0.6f), dmg / 22f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

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
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    // === MELEE - BLUNT ===
    // Functionally the same with or without armor (broken bones, internal bleeding),
    // but hard armor helps spread out the kinetic force so it isn't as immediately deadly.

    private static void handleBluntMelee(WoundingContext ctx)
    {
        LimbData limb = ctx.data().getLimb(ctx.attackedNode());
        if (limb == null) return;
        float roll = RNG.nextFloat();
        float dmg = ctx.preArmorDamage();

        switch (ctx.nodeArmorTier())
        {
            case NONE, LEATHER, CHAIN -> bluntLightOrNoArmor(ctx, limb, roll, dmg);
            case HARD -> bluntHardArmor(ctx, limb, roll, dmg);
        }
    }

    private static void bluntLightOrNoArmor(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 3% chance of direct hit causing bone fracture.
        // IRL it's borderline guaranteed, but... feels unfair gameplay-wise.
        if (roll < 0.03f)
            if (limb.getBoneState().ordinal() < BoneState.FRACTURED.ordinal())
                limb.setBoneState(BoneState.FRACTURED);

        // 15% chance of a less brutal dislocation, as a stand-in for the commonality of fractures.
        if (roll < 0.15f)
            if (limb.getBoneState().ordinal() < BoneState.DISLOCATED.ordinal())
                limb.setBoneState(BoneState.DISLOCATED);

        // If the hit was on the head, it basically causes severe concussion.
        // Just a 15% chance to make it a bit fairer.
        if (ctx.isHeadHit() && roll < 0.15)
        {
            new WoundingInstruction(BLUNT, VISCERAL, dmg / 30f)
                    .consciousnessDrop(0.80f)
                    .muscleHealthDamage(dmg / 30f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // The actual muscle/internal bleeding, which rolls a 50/50 of reaching internals.
        if (roll < 0.50f)
            new WoundingInstruction(BLUNT, VISCERAL, dmg / 30f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
        else
            new WoundingInstruction(BLUNT, MUSCULAR, dmg / 24f)
                    .muscleHealthDamage(dmg / 20f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
    }

    private static void bluntHardArmor(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 40% chance that the armor itself cuts at the edges.
        if (roll < 0.40f)
            applyArmorEdgeTrauma(ctx);

        // The generic normal wound. Less brutal than without armor, but
        // the force is still transmitted.
        new WoundingInstruction(BLUNT, SUBDERMAL, dmg / 30f)
                .muscleHealthDamage(dmg / 20f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    // === PROJECTILE - ARROW ===
    // Using hard armor effectively negates the danger of arrows, and
    // chainmail prevents total penetration.

    private static void handleProjectileArrow(WoundingContext ctx)
    {
        LimbData limb = ctx.data().getLimb(ctx.attackedNode());
        if (limb == null) return;
        float roll = RNG.nextFloat();
        float dmg = ctx.preArmorDamage();

        switch (ctx.nodeArmorTier())
        {
            case NONE -> projectileArrowNoArmor(ctx, limb, roll, dmg);
            case LEATHER -> projectileArrowLeather(ctx, limb, roll, dmg);
            case CHAIN -> projectileArrowChain(ctx, limb, roll, dmg);
            case HARD -> new WoundingInstruction(BLUNT, DERMAL, dmg / 50f)
                                .atPosition(ctx.hitU(), ctx.hitV())
                                .apply(limb, ctx.data());
        }
    }

    private static void projectileArrowNoArmor(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // 3% that an arrow goes through-and-through, with an entry and exit wound
        // but no arrow to tamponade the bleeding.
        if (roll < 0.03f)
        {
            new WoundingInstruction(PUNCTURE, ARTERIAL, dmg / 30f)
                    .asEntry()
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
            new WoundingInstruction(PUNCTURE, MUSCULAR, dmg / 30f)
                    .asExit()
                    .atPosition(wrapU(ctx.hitU()), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // 10% that an arrow goes through but gets lodged in.
        if (roll < 0.20f)
        {
            new WoundingInstruction(PUNCTURE, ARTERIAL, dmg / 30f)
                    .asEntry()
                    .withArrow()
                    .managedBleeding(0.1f)
                    .withContamination(0.6f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
            new WoundingInstruction(PUNCTURE, MUSCULAR, dmg / 30f)
                    .asExit()
                    .managedBleeding(0.1f)
                    .atPosition(wrapU(ctx.hitU()), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // 30% that an arrow DOESN'T go through and gets lodged in.
        if (roll < 0.30f)
        {
            new WoundingInstruction(PUNCTURE, depthFromDamage(dmg * 1.3f), dmg / 30f)
                    .withArrow()
                    .managedBleeding(0.1f)
                    .withContamination(0.6f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // Generic wound, which is that the arrow cuts but glances off.
        new WoundingInstruction(LACERATION, depthFromDamage(dmg * 1.3f), dmg / 30f)
                .withContamination(0.1f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    private static void projectileArrowLeather(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // Functionally, leather armor is the same as no armor, but slightly lowered lethality.
        // 5% that an arrow goes through but gets lodged in.
        if (roll < 0.05f)
        {
            new WoundingInstruction(PUNCTURE, ARTERIAL, dmg / 30f)
                    .asEntry()
                    .withArrow()
                    .managedBleeding(0.1f)
                    .withContamination(0.6f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
            new WoundingInstruction(PUNCTURE, MUSCULAR, dmg / 30f)
                    .asExit()
                    .managedBleeding(0.1f)
                    .atPosition(wrapU(ctx.hitU()), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // 15% that an arrow DOESN'T go through and gets lodged in.
        if (roll < 0.15f)
        {
            new WoundingInstruction(PUNCTURE, depthFromDamage(dmg * 1.3f), dmg / 30f)
                    .withArrow()
                    .managedBleeding(0.1f)
                    .withContamination(0.6f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // Generic wound, which is that the arrow cuts but glances off. With this
        // multiplier, the wound basically never reaches arterial depth.
        new WoundingInstruction(LACERATION, depthFromDamage(dmg * 0.9f), dmg / 30f)
                .withContamination(0.1f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    private static void projectileArrowChain(WoundingContext ctx, LimbData limb, float roll, float dmg)
    {
        // A perfectly straight arrow doesn't bounce and actually tears through the armor.
        // 10% that an arrow DOESN'T go through and gets lodged in.
        if (roll < 0.10f)
        {
            new WoundingInstruction(PUNCTURE, depthFromDamage(dmg), dmg / 30f)
                    .withArrow()
                    .managedBleeding(0.1f)
                    .withContamination(0.6f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());

            return;
        }

        // Generic wound, which is that the arrow glances off without cutting.
        new WoundingInstruction(BLUNT, depthFromDamage(dmg), dmg / 30f)
                .withContamination(0.1f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    // === EXPLOSION ===
    // Blast is FOUR distinct mechanisms.
    //      PRIMARY - The pressure wave. Goes THROUGH armor. Wrecks air-filled organs (lungs for Paramedicine).
    //      SECONDARY - Fragmentation. Armor genuinely helps. Filthy, penetrating, the usual killer.
    //      TERTIARY - Being thrown. Blunt trauma and fractures on landing.
    //      QUATERNARY - Burns, which route through FIRE instead (don't have it set up yet).
    private static void handleExplosion(WoundingContext ctx)
    {
        LimbData limb = ctx.data().getLimb(ctx.attackedNode());
        if (limb == null)
            return;

        float dmg = ctx.preArmorDamage();
        float proximity = Math.max(0f, Math.min(1f, ctx.explosionProximity()));

        applyPrimaryBlast(ctx, proximity, dmg);
        applyFragmentation(ctx, limb, dmg, proximity);
        applyBlastThrow(ctx, limb, dmg, proximity);
    }

    // Overpressure. This NEVER checks armor tier, since it goes straight through plate.
    // Scales with the SQUARE of proximity, because blast falls off with distance.
    private static void applyPrimaryBlast(WoundingContext ctx, float proximity, float dmg)
    {
        if (proximity < ModConstants.BLAST_PRIMARY_MIN_PROXIMITY)
            return;

        float overpressure = proximity * proximity * (dmg / ModConstants.BLAST_OVERPRESSURE_DIVISOR);
        if (overpressure <= 0f)
            return;

        LungData left = ctx.data().getLeftLung();
        LungData right = ctx.data().getRightLung();

        // Blast lung: Alveoli rupture and bleed. Bilateral since the wave hits the whole chest.
        if (RNG.nextFloat() < overpressure * ModConstants.BLAST_LUNG_CHANCE)
        {
            float fluid = overpressure * ModConstants.BLAST_LUNG_FLUID_ML;
            left.addFluid(fluid);
            right.addFluid(fluid);
        }

        // CLOSED pneumothorax: No open wound to vent through, so it's a TENSION pneumo from the
        // start. This is the needle-decompression case. Usually unilateral.
        if (RNG.nextFloat() < overpressure * ModConstants.BLAST_TENSION_PNEUMO_CHANCE)
        {
            LungData collapsedLung = RNG.nextBoolean() ? left : right;
            collapsedLung.addAir(overpressure * ModConstants.BLAST_TENSION_PNEUMO_AIR_ML);
            collapsedLung.setTensionPneumothorax(true);
        }

        ctx.data().spikeAggregatedPain(overpressure * ModConstants.BLAST_PAIN_COEFF);

        // Close-range wave rattles the skull.
        if (proximity > ModConstants.BLAST_CONCUSSION_PROXIMITY)
        {
            new WoundingInstruction(BLUNT, VISCERAL, 0f)
                    .consciousnessDrop(Math.min(0.75f, overpressure * ModConstants.BLAST_CONCUSSION_COEFF))
                    .applyMutationsOnly(ctx.data());
        }
    }

    // Fragmentation. The main killer at any distance. Most blast casualties are frag casualties.
    private static void applyFragmentation(WoundingContext ctx, LimbData limb, float dmg, float proximity)
    {
        float armorStop = switch (ctx.nodeArmorTier())
        {
            case NONE -> 0f;
            case LEATHER -> 0.25f;
            case CHAIN -> 0.45f;
            case HARD -> 0.75f;
        };

        float fragChance = proximity * ModConstants.BLAST_FRAG_CHANCE * (1f - armorStop);
        int fragments = 0;
        for (int i = 0; i < ModConstants.BLAST_FRAG_MAX; i++)
            if (RNG.nextFloat() < fragChance) fragments++;

        // No penetration still means the debris scours the skin raw.
        if (fragments == 0)
        {
            new WoundingInstruction(ABRASION, SUPERFICIAL, 0.08f + RNG.nextFloat() * 0.12f)
                    .withContamination(0.25f)
                    .atPosition(ctx.hitU(), ctx.hitV())
                    .apply(limb, ctx.data());
            return;
        }

        for (int i = 0; i < fragments; i++)
        {
            // Blast debris is FILTHY. Dirt, gravel, fabric driven into the wound.
            float severity = (0.10f + RNG.nextFloat() * 0.35f) * (0.5f + proximity * 0.5f);
            WoundDepth depth = depthFromDamage(dmg * (0.4f + RNG.nextFloat() * 0.8f));

            new WoundingInstruction(PUNCTURE, depth, severity)
                    .withShrapnel()
                    .withContamination(ModConstants.BLAST_FRAG_CONTAMINATION)
                    .managedBleeding(0.15f)
                    .atPosition(scatterU(ctx.hitU()), scatterV(ctx.hitV()))
                    .apply(limb, ctx.data());

            // A fragment through the chest wall opens the pleura. This is an OPEN pneumothorax,
            // a sucking chest wound, NOT a tension one, because it vents out through the hole.
            // TODO: sealing this with an OCCLUSIVE dressing (no flutter valve) should convert it to
            // a tension pneumo. That interaction is the entire reason actual chest seals have valves.
            if (ctx.isTorsoHit() && (depth == MUSCULAR || depth == ARTERIAL) && RNG.nextFloat() < ModConstants.BLAST_OPEN_PNEUMO_CHANCE)
            {
                LungData lung = ctx.isRightSideHit() ? ctx.data().getRightLung() : ctx.data().getLeftLung();
                lung.addAir(ModConstants.BLAST_OPEN_PNEUMO_AIR_ML * severity);
            }
        }
    }

    // Tertiary: The wave throws you.
    private static void applyBlastThrow(WoundingContext ctx, LimbData limb, float dmg, float proximity)
    {
        if (proximity < ModConstants.BLAST_DISPLACEMENT_MIN_PROXIMITY || dmg < 4f)
            return;

        float impact = proximity * (dmg / ModConstants.BLAST_OVERPRESSURE_DIVISOR);

        new WoundingInstruction(BLUNT, SUBDERMAL, Math.min(0.6f, impact * 0.5f))
                .muscleHealthDamage(Math.min(0.3f, impact * 0.25f))
                .painSpike(Math.min(0.4f, impact * 0.3f))
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());

        if (RNG.nextFloat() < impact * ModConstants.BLAST_FRACTURE_CHANCE)
            if (limb.getBoneState().ordinal() < BoneState.FRACTURED.ordinal())
                limb.setBoneState(BoneState.FRACTURED);
    }

    // === ENVIRONMENTAL ===

    private static void handleFall(WoundingContext ctx)
    {
        LimbData leftFoot = ctx.data().getLimb(LEFT_FOOT);
        LimbData rightFoot = ctx.data().getLimb(RIGHT_FOOT);
        LimbData leftShin = ctx.data().getLimb(LEFT_LOWER_LEG);
        LimbData rightShin = ctx.data().getLimb(RIGHT_LOWER_LEG);
        if (leftFoot == null || rightFoot == null || leftShin == null || rightShin == null)
            return;

        float dmg = ctx.preArmorDamage();
        float severity = Math.min(ModConstants.FALL_SEVERITY_MAX, dmg / ModConstants.FALL_SEVERITY_DIVISOR);
        if (severity < ModConstants.FALL_MIN_SEVERITY)
            return;

        // Bone injury chance RAMPS WITH HEIGHT instead of being a classic roll*dmg lottery.
        float boneChance = Math.max(0f, (dmg - ModConstants.FALL_BONE_THRESHOLD) / ModConstants.FALL_BONE_RAMP);
        if (RNG.nextFloat() < boneChance)
        {
            LimbData target = randomLandingLimb(leftFoot, rightFoot, leftShin, rightShin);

            BoneState result;
            if (dmg >= ModConstants.FALL_COMPOUND_DMG) result = BoneState.COMPOUND;
            else if (dmg >= ModConstants.FALL_FRACTURE_DMG) result = BoneState.FRACTURED;
            else if (dmg >= ModConstants.FALL_HAIRLINE_DMG) result = BoneState.HAIRLINE;
            else result = BoneState.DISLOCATED;

            if (target.getBoneState().ordinal() < result.ordinal())
                target.setBoneState(result);
        }

        // Impact spreads across BOTH feet, so each takes half.
        applyFallImpact(ctx, leftFoot, severity * 0.5f, dmg);
        applyFallImpact(ctx, rightFoot, severity * 0.5f, dmg);
    }

    private static void applyFallImpact(WoundingContext ctx, LimbData limb, float severity, float dmg)
    {
        new WoundingInstruction(BLUNT, depthFromDamage(Math.min(13f, dmg)), severity)
                .muscleHealthDamage(Math.min(ModConstants.FALL_MUSCLE_MAX, dmg / 60f))
                .painSpike(Math.min(0.5f, severity))
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(limb, ctx.data());
    }

    // Feet AND shins, all four actually reachable (the old if-else chain made the shins dead code).
    private static LimbData randomLandingLimb(LimbData leftFoot, LimbData rightFoot, LimbData leftShin, LimbData rightShin)
    {
        return switch (RNG.nextInt(4))
        {
            case 0 -> leftFoot;
            case 1 -> rightFoot;
            case 2 -> leftShin;
            default -> rightShin;
        };
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

        LimbData edgeLimb = ctx.data().getLimb(edgeNode);
        if (edgeLimb == null) return;

        new WoundingInstruction(LACERATION, SUPERFICIAL,
                0.08f + (RNG.nextFloat() * 0.10f))
                .withContamination(0.05f)
                .atPosition(ctx.hitU(), ctx.hitV())
                .apply(edgeLimb, ctx.data());
    }

    private static LimbNode adjacentUnarmored(WoundingContext ctx)
    {
        LimbNode proximal = ctx.attackedNode().proximalNode;
        if (proximal != null && armorTierFor(proximal, ctx) == ArmorTier.NONE)
            return proximal;

        for (LimbNode node: LimbNode.values())
        {
            if (node.proximalNode == ctx.attackedNode() && armorTierFor(node, ctx) == ArmorTier.NONE)
                return node;
        }

        return null;
    }

    private static ArmorTier armorTierFor(LimbNode node, WoundingContext ctx)
    {
        return switch (node.category)
        {
            case HEAD -> ctx.helmetTier();
            case TRUNK -> ctx.chestTier();
            case ARM -> ctx.chestTier();
            case LEG -> node.proximalNode == LimbNode.GROIN ? ctx.leggingsTier() : ctx.bootsTier();
        };
    }

    // Scatter across the limb. Fragments don't all land on the same square inch.
    private static float scatterU(float u)
    {
        return wrapU(u + (RNG.nextFloat() - 0.5f) * 0.4f);
    }

    private static float scatterV(float v)
    {
        return Math.max(0f, Math.min(1f, v + (RNG.nextFloat() - 0.5f) * 0.4f));
    }
}
