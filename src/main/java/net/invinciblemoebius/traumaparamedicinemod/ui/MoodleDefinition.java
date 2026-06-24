package net.invinciblemoebius.traumaparamedicinemod.ui;

import net.invinciblemoebius.traumaparamedicinemod.status.Condition;
import net.invinciblemoebius.traumaparamedicinemod.status.ConditionSeverity;

public class MoodleDefinition
{
    public final String displayName;
    public final String flavorText;
    // Because i don't really have icon assets yet, it'll be just. A unicode char. LMAO
    public final char icon;

    public MoodleDefinition(String displayName, String flavorText, char icon)
    {
        this.displayName = displayName;
        this.flavorText = flavorText;
        this.icon = icon;
    }

    public static MoodleDefinition get(Condition condition)
    {
        return switch (condition)
        {
            // POSITIVE
            case IMMUNOCOMPETENT ->  new MoodleDefinition("Immunocompetent", "Good nutrition and healthy habits have rewarded you with a strong immune system. Infections are less dangerous, and might even regress.", '☻');

            // PAIN
            case DISCOMFORT ->  new MoodleDefinition("Discomfort", "You feel a dull ache, like little pinpricks.", '~');
            case PAIN ->  new MoodleDefinition("Pain", "Something hurts at a concerning level. Something isn't right.", '!');
            case SEVERE_PAIN ->  new MoodleDefinition("Severe Pain", "Intense pain that spreads beyond the origin of the pain. Everything hurts. Besides making focus difficult, it's starting to increase your heart rate. Find relief soon.", '‼');
            case AGONY ->  new MoodleDefinition("Agony", "Unbearable pain is consuming your awareness. You can't think of anything other than painkillers. Untreated, your body will enter systemic shock soon.", 'X');
            case PAIN_SHOCK ->  new MoodleDefinition("Shock", "Your body has entered a state of shock from intense, prolonged pain. You have a sudden drop in blood pressure, and are dangerously close to fainting from agony.", '◯');

            // CONSCIOUSNESS
            case CONFUSED ->  new MoodleDefinition("Confused", "You feel a little sluggish.", '?');
            case VERY_CONFUSED ->  new MoodleDefinition("Very Confused", "You're struggling to think clearly and your mind is foggy.", '?');
            case FAINTING ->  new MoodleDefinition("Fainting", "The edges of your vision are darkening. Your body is close to shutting down conscious awareness.", '↓');
            case INCAPACITATED ->  new MoodleDefinition("Incapacitated", "You have been incapacitated. Someone else needs to find you and intervene.", '↓');
            case UNCONSCIOUS ->  new MoodleDefinition("Unconscious", "You are unconscious and completely unresponsive to any stimuli.", '◯');

            // CARDIOVASCULAR - HEART RATE
            case MILD_TACHYCARDIA ->  new MoodleDefinition("Elevated Heart Rate", "Your heart is beating at a higher BPM than usual, likely from physical exertion. Not dangerous, but consider taking a break.", '▲');
            case TACHYCARDIA ->  new MoodleDefinition("Tachycardia", "Your heart is working significantly harder than it should be. Your body is having a compensatory response for something; blood loss, pain, low oxygen. Find the cause.", '▲');
            case SEVERE_TACHYCARDIA ->  new MoodleDefinition("Severe Tachycardia", "It feels like your heart is beating out of your chest. This dangerous pace is straining it, risking fibrillations if left untreated.", '▲');
            case BRADYCARDIA ->  new MoodleDefinition("Bradycardia", "Your heart rate has dropped below normal, meaning your body is struggling to compensate. You need to seek medical attention immediately.", '▼');
            case SEVERE_BRADYCARDIA ->  new MoodleDefinition("Severe Bradycardia", "Dangerously low heart rate. Epinephrine may help momentarily, but at this rate, your body is undergoing cardiovascular depression.", '▼');

            // CARDIOVASCULAR - RHYTHM
            case ARRYTHMIA ->  new MoodleDefinition("Arrythmia", "It feels like your heart is skipping a beat in an irregular rhythm.", '~');
            case VENTRICULAR_TACHYCARDIA ->  new MoodleDefinition("Ventricular Tachycardia", "Concerningly bad quality of heartbeat rhythm. Will worsen into a cardiac event if not treated. Defibrillation might help restore correct rhythm momentarily until the root cause is addressed.", '~');
            case PALPITATIONS ->  new MoodleDefinition("Palpitations", "You're uncomfortably aware of your own heartbeat. It feels as though your heart were somehow moving frantically inside you. Something isn't right.", '~');
            case VENTRICULAR_FIBRILLATIONS ->  new MoodleDefinition("Ventricular Fibrillations", "Your heart is quivering, not pumping. This inability to eject blood elsewhere is depriving your body of oxygen. Defibrillation might restore the normal rhythm, but asystole is more likely to follow.", '!');
            case SENSE_OF_IMPENDING_DOOM ->  new MoodleDefinition("Sense Of Impending Doom", "Your chest feels strange and the world feels tiny. Something is terribly wrong.", '!');
            case MYOCARDIAL_EXHAUSTION -> new MoodleDefinition("Failing Heart", "Your heart muscle is running out of the energy it needs to keep beating. Without restored perfusion, it will stop.", '<');
            case PULSELESS_ELECTRICAL_ACTIVITY -> new MoodleDefinition("Pulseless Electrical Activity", "Your heart's wiring is still firing, but there's nothing left to pump. A monitor would look almost normal while you have no pulse at all. Can't be fixed by defibrillating.", '!');
            case CARDIAC_ARREST ->  new MoodleDefinition("CARDIAC ARREST", "Your heart has completely stopped moving. Even with immediate medical attention, your prognosis looks grim.", '‼');

            // CARDIOVASCULAR - BLOOD PRESSURE
            case MILD_HYPOTENSION ->  new MoodleDefinition("Clinical Hypotension", "Your blood pressure is slightly below normal. Not dangerous on its own, but your body's margin for blood loss or stress is reduced.", '↓');
            case HYPOTENSION ->  new MoodleDefinition("Hypotension", "Your blood pressure has dropped into a concerning level. You feel lightheaded, especially when standing. Your body is working harder to mantain circulation.", '↓');
            case SEVERE_HYPOTENSION ->  new MoodleDefinition("Severe Hypotension", "Blood pressure is critically low. Your body's compensatory mechanisms are failing.", '↓');
            case CIRCULATORY_COLLAPSE ->  new MoodleDefinition("Circulatory Collapse", "The heart cannot mantain output. Without immediate intervention, this is fatal.", '▼');
            case MILD_HYPERTENSION ->  new MoodleDefinition("Clinical Hypertension", "Your blood pressure is slightly elevated. Technically hypertension, but completely normal.", '↑');
            case HYPERTENSION ->  new MoodleDefinition("Hypertension", "Blood pressure is over your usual ceiling. You feel pain behind your eyes and it feels like the back of your neck is pulsating.", '↑');
            case SEVERE_HYPERTENSION ->  new MoodleDefinition("Severe Hypertension", "Dangerously high blood pressure. Your vision is weird, you hear ringing in your ears, and it's like the back of your neck is going to explode.", '↑');
            case HYPERTENSIVE_CRISIS ->  new MoodleDefinition("Hypertensive Crisis", "Your blood pressure is so high that it is causing active organ damage every second. A stroke or cardiac event are almost certain at this stage.", '▲');

            // CARDIOVASCULAR - BLOOD VOLUME
            case MILD_HYPOVOLEMIA ->  new MoodleDefinition("Low Blood Volume", "You've lost a small amount of blood. It makes you feel a little lightheaded and your heartbeat is higher than normal, but nothing critical. Consider resting and staying hydrated.", '↓');
            case HYPOVOLEMIA ->  new MoodleDefinition("Mild Hypovolemia", "You feel faint from blood loss. You're starting to feel thirsty and a little cold. While your body can replenish the missing fluids overtime, having an IV handy would be wise.", '↓');
            case SEVERE_HYPOVOLEMIA ->  new MoodleDefinition("Severe Hypovolemia", "You are losing the fight to maintain circulation. Your skin is cold and pale, pulse is rapid and weak. Without an IV, your cardiac response will fail.", '↓');
            case CRITICAL_HYPOVOLEMIA ->  new MoodleDefinition("EXSANGUINATION", "Without blood to feed oxygen to your body, your organs begin to fail, your heart can't pump in the correct rhythm, and consciousness fades to blackness. Your body enters terminal shock. The end approaches.", '▼');
            case MILD_HYPERVOLEMIA ->  new MoodleDefinition("Too Much Blood", "There is more blood coursing through your bloodstream than your body can comfortably handle. You feel bloated and your heart is working harder than it should.", '↑');
            case HYPERVOLEMIA ->  new MoodleDefinition("Hypervolemia", "Excess fluid is straining your cardiovascular system. Blood pressure is elevated and your heart is working double duty. Literally.", '↑');
            case CRITICAL_HYPERVOLEMIA ->  new MoodleDefinition("Blood-Swollen", "As it turns out, too much blood is also bad for you. The heart is overwhelmed, and, untreated, pulmonary edema is likely. How did we get here?", '▲');

            // CARDIOVASCULAR - BLOOD COMPOSITION
            case ANEMIA -> new MoodleDefinition("Anemia", "Not enough red cells to carry oxygen. You tire fast and your margin for any further blood loss is thin.", 'a');
            case MODERATE_ANEMIA -> new MoodleDefinition("Moderate Anemia", "Your blood's oxygen-carrying capacity is seriously reduced. Plasma filled the tank, but only red cells carry oxygen.", 'a');
            case SEVERE_ANEMIA -> new MoodleDefinition("Severe Anemia", "Critically few red cells. Your tissues are starving for oxygen no matter how well you breathe. You need blood, red blood.", 'a');
            case HEMODILUTION -> new MoodleDefinition("Hemodilution", "Your blood has been thinned out with fluid, so it doesn't clot as well. Bleeding will be harder to stop.", 'd');
            case HEMOCONCENTRATION -> new MoodleDefinition("Hemoconcentration", "Your blood is thick and sticky from fluid loss, raising the risk of dangerous clots.", 'c');

            // WOUNDS - HEMORRHAGE
            case MINOR_BLEEDING ->  new MoodleDefinition("Dripping Blood", "There is a small drip of blood coming out of you. Could be handled by your body's own clotting response, but consider preparing a bandage.", '↓');
            case BLEEDING ->  new MoodleDefinition("Bleeding", "You are bleeding at a rate where your body is struggling to clot the wound. Direct pressure and a proper dressing are needed before it becomes a volume problem.", '↓');
            case HEAVY_BLEEDING ->  new MoodleDefinition("Serious Bleeding", "You are losing blood at a concerning rate. Clotting won't take care of this. You need aggressive wound packing and a helping of blood IVs before you enter hypovolemic shock.", '↓');
            case CATASTROPHIC_BLEEDING ->  new MoodleDefinition("DYING OF BLOOD LOSS", "Blood is gushing out of you uncontrollably. Without immediate aggressive intervention, shock, circulatory collapse, and eventual death is extremely likely.", '▼');
            case MINOR_INTERNAL_BLEEDING ->  new MoodleDefinition("Minor Internal Bleeding", "There is a tiny amount of blood leaking into your organs. Do not be fooled by the small amount, pulmonary edema is likely if left untreated.", '▼');
            case INTERNAL_BLEEDING ->  new MoodleDefinition("Internal Bleeding", "You are bleeding from the inside, but your body can't clot it at this rate of bleed. Without being able to apply direct pressure, this will cause hypovolemia soon. Seek medical attention.", '▼');
            case MASSIVE_INTERNAL_BLEEDING ->  new MoodleDefinition("Massive Internal Bleeding", "There is more blood in your organs than in your veins. It feels like being choked from the inside out. You are in desperate need of a chest pump. Circulatory collapse is soon to follow.", '⚠');

            // WOUNDS - BONE
            case FRACTURE ->  new MoodleDefinition("Fracture", "A bone has broken. Judging by the malformed shape of your limb, you won't be able to move it without pain for a long while.", '!');
            case COMPOUND_FRACTURE ->  new MoodleDefinition("Compound Fracture", "You can see your own shattered bone tearing through your flesh and exiting into the open air. Besides the obvious, you are going to need serious antibiotics. Oh god.", 'X');
            case DISLOCATION ->  new MoodleDefinition("Dislocation", "A joint has been forced out of its place. The surrounding muscles and ligaments look damaged as well. However, with the bone intact, it can be moved back into place.", '◯');

            // WOUND - NEUROLOGICAL
            case CONCUSSION ->  new MoodleDefinition("Concussion", "You have a bad head injury that left you feeling shaken. The world sounds extremely loud, dim lights hurt, and you have trouble standing. It feels like you are going to vomit.", '⚠');
            case SEVERE_HEAD_INJURY ->  new MoodleDefinition("SEVERE HEAD INJURY", "reality ..   stops  making     sense", '.');

            // RESPIRATORY - BREATH
            case CONTROLLED_APNOEA ->  new MoodleDefinition("Apnea", "You are holding your breath.", '◯');
            case UNCONTROLLED_APNOEA ->  new MoodleDefinition("APNEA", "You cannot breathe. Whether from obstruction or injury, your body is not getting air.", '⚠');
            case TACHYPNOEA ->  new MoodleDefinition("Breathing Fast", "You sound like a heaving dog. Consider taking a break.", '!');
            case SEVERE_TACHYPNOEA ->  new MoodleDefinition("Breathing Extremely Fast", "You are breathing at a concerningly high rate. Seek immediate medical attention.", '‼');
            case HYPERVENTILATION ->  new MoodleDefinition("Hyperventilating", "You are breathing more than what your body needs. Hypoxia is soon to follow if this isn't addressed.", '!');
            case CHOKING ->  new MoodleDefinition("Choking", "You feel breathless as your body demands more respirations than what you are able to give.", '!');

            // RESPIRATORY - OXYGENATION
            case MILD_HYPOXIA ->  new MoodleDefinition("Clinical Hypoxia", "Your oxygenation levels are technically low, but not at an effectual rate.", '↓');
            case HYPOXIA ->  new MoodleDefinition("Hypoxia", "SpO2 levels are low. You feel breathless, and your body is compensating. Supplemental oxygen would be good right about now.", '↓');
            case SEVERE_HYPOXIA ->  new MoodleDefinition("Severe Hypoxia", "Critically low oxygenation. You look blue, and you are starting to feel faint. You need supplemental oxygen, bad.", '↓');
            case OXYGEN_STARVATION ->  new MoodleDefinition("Oxygen-Starved", "Your blood isn't carrying nearly enough oxygen to your brain, or really, any organ at all. Respiratory crisis and brain hypoxemia are extremely likely if you can't get a high oxygen titration.", '▼');
            case HYPEROXIA ->  new MoodleDefinition("Clinical Hyperoxia", "Too much oxygen in your blood, likely from a supplemental oxygen at too high a concentration. Not concerning yet, but serious lung damage will ensue if you don't lower your oxygen dosage soon.", '↑');
            case OXYGEN_POISONING ->  new MoodleDefinition("Oxygen Poisoning", "Dangerously excessive oxygen in your blood is causing direct damage to your organs. Breathing feels painful. You need to reduce oxygen delivery immediately.", '▲');

            // RESPIRATORY - LUNG COLLAPSE
            case HEMOTHORAX ->  new MoodleDefinition("Hemothorax", "There is blood inside your pleural cavity, making each breath less efficient than before. At this amount however, it can clear up on its own.", '◯');
            case SERIOUS_HEMOTHORAX ->  new MoodleDefinition("HEMOTHORAX", "There is a large amount of blood inside your pleural cavity, severely affecting breathing. It stings just to take a breath. You need a chest pump, now.", '⚠');
            case PNEUMOTHORAX ->  new MoodleDefinition("Pneumothorax", "There is a small amount of air inside your pleural cavity. The pleura being accustomed to gaseous exchange, it will clear up on its own, but beware of worsening the condition.", '◯');
            case SERIOUS_PNEUMOTHORAX ->  new MoodleDefinition("PNEUMOTHORAX", "There is a concerning amount of air in your pleural cavity. It makes your chest hurt. You need to get rid of the trapped air somehow, soon.", '⚠');
            case TENSION_PNEUMOTHORAX ->  new MoodleDefinition("Tension Pneumothorax", "Negative pressure has trapped air inside your pleural cavity, and it continues increasing with each breath you take. You need immediate invasive intervention before your own lungs compress your heart and kill you.", '‼');
            case LUNG_FLUID ->  new MoodleDefinition("Fluid In Lungs", "There is some liquid inside your alveoli, inflaming them and seriously hurting you.", '◯');
            case SERIOUS_LUNG_FLUID ->  new MoodleDefinition("FLUID IN LUNGS", "You have inhaled a significant amount of liquid into your alveoli, likely from drowning. You are choking despite breathing. Seek immediate medical attention.", '⚠');

            // RESPIRATORY - AIRWAY
            case COMPROMISED_AIRWAY ->  new MoodleDefinition("Compromised Airway", "Something is blocking your airways, and while you can still breathe, it takes significant effort to do so. You can audibly hear yourself grunting and 'snoring' each breath.", '~');
            case BLOCKED_AIRWAY ->  new MoodleDefinition("BLOCKED AIRWAY", "Can't... breathe...", '~');

            // SYSTEMIC - TEMPERATURE
            case MILD_HYPOTHERMIA ->  new MoodleDefinition("Very Cold", "Your internal temperate is lower than it should be. You can't form clots as well as before and you feel sluggish. Deceptively serious, you need to seek warmth as soon as possible.", '↓');
            case HYPOTHERMIA ->  new MoodleDefinition("Hypothermia", "It feels like you've never been this cold in your entire life. You're shivering frantically, and your clotting has significantly worsened.", '↓');
            case SEVERE_HYPOTHERMIA ->  new MoodleDefinition("Freezing To Death", "Your core temperature is below hypothermic. You can't even form a coherent thought... and you somehow feel uncomfortably hot at the same time. You just want to lay down...", '▼');
            case FEVER ->  new MoodleDefinition("Fever", "You feel hot and dizzy. Thirst increased, but infections will have a harder time settling in.", '↑');
            case HEAT_STROKE ->  new MoodleDefinition("HEAT STROKE", "There is a small furnace inside you. Your brain is quite literally being cooked every second. You need to dip yourself in cold water immediately to prevent further damage.", '▲');

            // SYSTEMIC - INFECTION
            case BACTEREMIA -> new MoodleDefinition("Bacteremia", "Bacteria has breached your bloodstream. Not septic yet, though it is one step closer to it.", '~');
            case INFECTION ->  new MoodleDefinition("Infection", "Bacteria has accumulated near a wound and may need further attention soon.", '~');
            case PAINFUL_INFECTION ->  new MoodleDefinition("Painful Infection", "An infection has become warm and red. It hurts to touch it. Consider antiseptic or mild antibiotics.", '!');
            case SEVERE_INFECTION ->  new MoodleDefinition("Serious Infection", "An infection has become badly swollen and smells terrible. It gives constant waves of pinprick-like stinging pain. You need serious antiobiotics before it reaches your bloodstream.", '‼');
            case LIFE_THREATENING_INFECTION ->  new MoodleDefinition("Life-Threatening Infection", "A wound has become near-necrotic, oozing pus and having a disturbing discoloration more resembling corpse flesh than anything seen on living creatures. Slowly, you are succumbing to an infection. Sepsis is likely to follow.", '⚠');
            case SEPSIS ->  new MoodleDefinition("Sepsis", "Your body is scrambling to respond to an infection, damaging your own tissue in the process. Temperature increased. You need very serious antibiotics to win this battle.", '!');
            case SEVERE_SEPSIS ->  new MoodleDefinition("Severe Sepsis", "Your body is failing to respond to an infection and increasing compensatory responses, doing as much harm to you as the bacteria is. You feel faint and you are stricken by flames of thirst unlike anything you've had before.", '‼');
            case SEPTIC_SHOCK ->  new MoodleDefinition("SEPTIC SHOCK", "The infection has entered your bloodstream. Blood pressure drops, and your body no longer responds to fluid treatment. Realistically, you are a lost cause and death is likely imminent.", '⚠');

            // SYSTEMIC - IMMUNITY
            case IMMUNOCOMPROMISED ->  new MoodleDefinition("Weak Immune System", "Your immune system isn't quite what it used to be. You may have trouble fighting off existing infections, and are more likely to gain them from wounds.", '.');
            case IMMUNE_EXHAUSTION -> new MoodleDefinition("Immune System Overwhelmed", "Your body is fighting on more fronts than it can supply. Without rest, antibiotics, or fewer open wounds, the infections you're holding back will start to win.", '!');

            // SUBSTANCES - OPIATES
            case ANALGESIA ->  new MoodleDefinition("Analgesia", "Opiates in your bloodstream. You feel relaxed and calm.", '↑');
            case DEEP_ANALGESIA ->  new MoodleDefinition("Deep Analgesia", "Opiates at full strength in your bloodstream. You can't breathe well and you feel tired, but you feel a pleasant sensation overall.", '▲');
            case ANALGESIC_TOXICITY ->  new MoodleDefinition("Analgesic Toxicity", "Your body is entering respiratory failure from opiate overdose. You desperately need naloxone before asphyxiation kills you, but your mind is in deep euphoria.", '⚠');

            // EDGE CASE
            default -> new MoodleDefinition("DEFAULT MOODLE", "You shouldn't see this.", '?');
        };
    }

    public static int severityColor(ConditionSeverity severity)
    {
        return switch (severity)
        {
            case GREEN_DARK -> 0xFF1A7A1A;
            case GREEN -> 0xFF2ECC2E;
            case GREEN_LIGHT -> 0xFF7AE87A;
            case NEUTRAL -> 0xFFAAAAAA;
            case MILD -> 0xFFFF9966;
            case MODERATE -> 0xFFFF4422;
            case SEVERE -> 0xFFCC1100;
            case CRITICAL -> 0xFFFF0000;
            case CRITICAL_GLOW -> 0xFFFF2244;
        };
    }
}
