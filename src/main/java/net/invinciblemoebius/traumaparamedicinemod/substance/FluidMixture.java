package net.invinciblemoebius.traumaparamedicinemod.substance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;

// A volume of mixed substances, by component. Not an item, but it does live in
// an ItemStack's NBT (a syringe, a bowl, whichever other container i choose to add).
public class FluidMixture
{
    private final Map<SubstanceType, Float> components = new EnumMap<>(SubstanceType.class);

    // NBT volumes are snapped to this grain so equal mixtures are able to be stacked.
    // 1/100th ML resolution
    private static final float ML_GRAIN_PER_ML = 100f;
    // 0.005 mL. Anything below rounds to zero, so it counts as absent.
    private static final float ML_MIN = 0.5f / ML_GRAIN_PER_ML;

    public FluidMixture(){}

    // === MUTATION METHODS ===

    // Returns how much was actually accepted.
    public float add(SubstanceType type, float ml, float capacity)
    {
        if (ml <= 0f)
            return 0f;

        float room = Math.max(0f, capacity - totalVolume());
        float accepted = Math.min(ml, room);
        if (accepted <= 0f)
            return 0f;

        components.merge(type, accepted, Float::sum);
        return accepted;
    }

    // Removes ml of total volume, split PROPORTIONALLY across all components.
    // Returns the removed portion as its own mixture.
    public FluidMixture drain(float ml)
    {
        FluidMixture drained = new FluidMixture();
        float total = totalVolume();
        if (ml <= 0f || total <= 0f)
            return drained;

        float fraction = Math.min(1f, ml / total);

        // Snapshot keys first, the map mutates as it goes.
        for (SubstanceType type : new EnumMap<>(components).keySet())
        {
            float have = components.get(type);
            float take = have * fraction;
            drained.components.put(type, take);

            float left = have - take;
            if (left <= ML_MIN)
                components.remove(type);
            else
                components.put(type, left);
        }

        return drained;
    }

    public void merge(FluidMixture other, float capacity)
    {
        if (other == null || other.isEmpty())
            return;

        float room = Math.max(0f, capacity - totalVolume());
        if (room <= 0f)
            return;

        float incoming = other.totalVolume();
        float fraction =  Math.min(1f, room / incoming);
        for (Map.Entry<SubstanceType, Float> entry : other.components.entrySet())
            components.merge(entry.getKey(), entry.getValue() * fraction, Float::sum);
    }

    public void clear()
    {
        components.clear();
    }

    // === HELPER METHODS ===

    // Snaps a volume to the NBT grain. This is canonicalization, not rounding for display.
    // Any two values within half a grain produce the identical float, so they serialize equal.
    private static float quantize(float ml)
    {
        return Math.round(ml * ML_GRAIN_PER_ML) / ML_GRAIN_PER_ML;
    }

    // Readable rewrites for tooltips: "Morphine 2.0 mL, Saline 1.0 mL".
    public String describe()
    {
        if (isEmpty())
            return "Empty";

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<SubstanceType, Float> entry : components.entrySet())
        {
            if (entry.getValue() <= 0f)
                continue;

            if (sb.length() > 0)
                sb.append(", ");
            sb.append(prettyName(entry.getKey())).append(' ').append(String.format("%.1f", entry.getValue())).append(" mL");
        }

        return sb.toString();
    }

    private static String prettyName(SubstanceType type)
    {
        String[] parts = type.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts)
        {
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    // === SAVING STUFF ===

    public void writeToNBT(CompoundTag tag)
    {
        ListTag list = new ListTag();
        for (Map.Entry<SubstanceType, Float> entry : components.entrySet())
        {
            float amount = quantize(entry.getValue());

            // Substances below the minimal grain round away to nothing
            if (amount <= 0f)
                continue;

            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putString("Type", entry.getKey().name());
            compoundTag.putFloat("AmountML", amount);
            list.add(compoundTag);
        }

        // An empty mixture writes NO key, and clears any stale one. This is what keeps empty
        // containers tag-clean so they stack with each other (and with freshly-crafted ones).
        if (list.isEmpty())
            tag.remove("Mixture");
        else
            tag.put("Mixture", list);
    }

    public static FluidMixture readFromNBT(CompoundTag tag)
    {
        FluidMixture mix = new FluidMixture();
        if (tag == null || !tag.contains("Mixture"))
            return mix;

        ListTag list = tag.getList("Mixture", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag compoundTag = list.getCompound(i);
            SubstanceType type = SubstanceType.valueOf(compoundTag.getString("Type"));
            mix.components.merge(type, compoundTag.getFloat("AmountML"), Float::sum);
        }

        return mix;
    }

    // === ACCESSORS ===

    public float totalVolume()
    {
        float sum = 0f;
        for (float value : components.values())
            sum += value;

        return sum;
    }
    public boolean isEmpty() { return totalVolume() < ML_MIN; }
    public float amountOf(SubstanceType type) { return components.getOrDefault(type, 0f); }
    public Map<SubstanceType, Float> getComponents() { return components; }
}
