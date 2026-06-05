package net.invinciblemoebius.traumaparamedicinemod.wound;

// From shallowest to deepest.
public enum WoundDepth
{
    SUPERFICIAL,    // Epidermis only. Clots easily, no packing needed.
    DERMAL,         // Into dermis. Clots with pressure. Sutures optional.
    SUBDERMAL,      // Into fatty tissue. Slower clotting. Suturable.
    MUSCULAR,       // Into muscle. Significant bleeding. Packing optional.
    ARTERIAL,       // Into major vessel. Can't clot, always requires occlusion,
    VISCERAL        // Internal organ, no external wound. Internal bleeding. Requires chest pump.
}
