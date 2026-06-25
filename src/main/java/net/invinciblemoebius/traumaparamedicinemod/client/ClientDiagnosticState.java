package net.invinciblemoebius.traumaparamedicinemod.client;

// Client-only snapshot of what the player has diagnosed by hand (no live tools).
// Persists until overwritten by a fresh check. The screen fades it visually, but the
// data stays frozen. That way the player doesn't have to re-check every 15 seconds.
public final class ClientDiagnosticState
{
    private ClientDiagnosticState() {}

    // BLOOD PRESSURE
    // Highest threshold with a felt pulse
    private static Integer lowSystolic = null;
    // Lowest threshold with NO pulse
    private static Integer highSystolic = null;
    private static long bpUpdatedAt = 0L;

    // TEXT
    private static String feedbackText = "";
    private static long feedbackAt = 0L;


    // === CHECK METHODS ===

    public static void showFeedback(String text)
    {
        feedbackText = text;
        feedbackAt = System.currentTimeMillis();
    }

    public static void applyPulseReading(int threshold, boolean present)
    {
        if (present)
        {
            // Newer wins.
            if (highSystolic != null && threshold >= highSystolic)
                highSystolic = null;
            lowSystolic = (lowSystolic == null) ? threshold : Math.max(lowSystolic, threshold);
        }
        else
        {
            if (lowSystolic != null && threshold <= lowSystolic)
                lowSystolic = null;
            highSystolic = (highSystolic == null) ? threshold : Math.min(highSystolic, threshold);
        }

        bpUpdatedAt = System.currentTimeMillis();
    }

    // === ACESSORS ===

    public static Integer getLowSystolic() { return lowSystolic; }
    public static Integer getHighSystolic() { return highSystolic; }
    public static long getBpUpdatedAt() { return bpUpdatedAt; }
    public static boolean hasBpEstimate() { return lowSystolic != null || highSystolic != null; }
    public static String getFeedbackText() { return feedbackText; }
    public static long getFeedbackAt() { return feedbackAt; }
}
