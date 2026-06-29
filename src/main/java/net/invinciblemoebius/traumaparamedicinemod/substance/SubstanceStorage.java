package net.invinciblemoebius.traumaparamedicinemod.substance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;

// Shared mechanism for a capacity-bounded collection of substances.
public abstract class SubstanceStorage<SELF extends SubstanceStorage<SELF>>
{
    protected final Map<SubstanceType, Float> components = new EnumMap<>(SubstanceType.class);

    // Amounts snap to this grain on write, so two equal mixtures serialize byte-identically.
    // 1/100th-unit precision.
    protected static final float GRAIN_PER_UNIT = 100f;
    // Below this, an amount counts as absent.
    protected static final float MIN = 0.5f / GRAIN_PER_UNIT;

    // Each subtype returns a fresh empty instance of its own kind, so shared ops that produce a
    // new store head back the right concrete type with no casting.
    protected abstract SELF createEmpty();

    // === MUTATION METHODS ===

    // Adds a substance, capped at `capacity`. Returns how much was actually accepted.
    public float add(SubstanceType type, float amount, float capacity)
    {
        if (amount <= 0f)
            return 0f;

        float room = Math.max(0f, capacity - total());
        float accepted = Math.min(amount, room);
        if (accepted <= 0f)
            return 0f;

        components.merge(type, accepted, Float::sum);
        return accepted;
    }

    // Removes up to X units of total, split PROPORTIONALLY across every component.
    // Returns the removed portion as its own store of the same type.
    public SELF drain(float amount)
    {
        SELF drained = createEmpty();
        float total = total();
        if (amount <= 0f || total <= 0f)
            return drained;

        float fraction = Math.min(1f, amount / total);

        // Snapshot keys; the backing map mutates as we go.
        for (SubstanceType type : new EnumMap<>(components).keySet())
        {
            float have = components.get(type);
            float take = have * fraction;
            drained.components.put(type, take);

            float left = have - take;
            if (left <= MIN)
                components.remove(type);
            else
                components.put(type, left);
        }

        return drained;
    }

    // Merges another store of the same type in, capped. Does NOT drain the source.
    public void merge(SELF other, float capacity)
    {
        if (other == null || other.isEmpty())
            return;

        float room = Math.max(0f, capacity - total());
        if (room <= 0f)
            return;

        float incoming = other.total();
        float fraction = Math.min(1f, room / incoming);
        for (Map.Entry<SubstanceType, Float> entry : other.components.entrySet())
            components.merge(entry.getKey(), entry.getValue() * fraction, Float::sum);
    }

    // Removes up to X units of a SINGLE substance (targeted, unlike drain). Returns removed.
    public float remove(SubstanceType type, float amount)
    {
        if (amount <= 0f)
            return 0f;

        float have = components.getOrDefault(type, 0f);
        if (have <= 0f)
            return 0f;

        float removed = Math.min(amount, have);
        float left = have - removed;
        if (left <= MIN)
            components.remove(type);
        else
            components.put(type, left);

        return removed;
    }

    public void clear() { components.clear(); }

    // === UI ===

    // "Morphine 2.0 <unit>, Saline 1.0 <unit>"
    // The unit label is the subtype's call.
    protected String describe(String unit)
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
            sb.append(prettyName(entry.getKey())).append(' ')
                    .append(String.format("%.1f", entry.getValue())).append(' ').append(unit);
        }
        return sb.toString();
    }

    protected static String prettyName(SubstanceType type)
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

    // === ACCESSORS ===

    // Unit-neutral total. Subtypes alias this as totalVolume() / totalMass().
    public float total()
    {
        float sum = 0f;
        for (float v : components.values())
            sum += v;
        return sum;
    }

    public boolean isEmpty() { return total() < MIN; }
    public float amountOf(SubstanceType type) { return components.getOrDefault(type, 0f); }
    public Map<SubstanceType, Float> getComponents() { return components; }

    // === SAVING STUFF ===

    // NBT QoL, not display rounding. Two values within half a grain collapse to the
    // same float, so identical contents count as equal and can get stacked as one item.
    protected static float quantize(float amount)
    {
        return Math.round(amount * GRAIN_PER_UNIT) / GRAIN_PER_UNIT;
    }

    // An empty store writes NO key (and strips a stale one),
    // keeping empty containers tag-clean so they stack.
    protected void writeComponents(CompoundTag tag, String key)
    {
        ListTag list = new ListTag();
        for (Map.Entry<SubstanceType, Float> entry : components.entrySet())
        {
            float amount = quantize(entry.getValue());
            if (amount <= 0f)
                continue;

            CompoundTag c = new CompoundTag();
            c.putString("Type", entry.getKey().name());
            c.putFloat("Amount", amount);
            list.add(c);
        }

        if (list.isEmpty())
            tag.remove(key);
        else
            tag.put(key, list);
    }

    // Fills THIS store from a list under {key}. Unknown substance ids are skipped, so that
    // an addon substance that's gone won't crash the load.
    protected void readComponents(CompoundTag tag, String key)
    {
        components.clear();
        if (tag == null || !tag.contains(key))
            return;

        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag c = list.getCompound(i);
            SubstanceType type = parse(c.getString("Type"));
            if (type != null)
                components.merge(type, c.getFloat("Amount"), Float::sum);
        }
    }

    private static SubstanceType parse(String name)
    {
        try { return SubstanceType.valueOf(name); }
        catch (IllegalArgumentException e) { return null; }
    }
}