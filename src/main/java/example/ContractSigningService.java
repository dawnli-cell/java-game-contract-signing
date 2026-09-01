package example;

import java.util.*;

public final class ContractSigningService {
    public interface PdfGenerator { String generate(String html, String requestId) throws Exception; }
    public record PlayerAsset(String playerId, String assetId, String title) {}
    public record LiveEvent(String type, String assetId) {}
    public record ModerationItem(String assetId, String status) {}
    public record SignedContract(String assetId, String state, String pdfEnvelope) {}
    private final PdfGenerator pdf;
    public ContractSigningService(PdfGenerator pdf) { this.pdf = pdf; }
    public SignedContract process(PlayerAsset asset, LiveEvent event, ModerationItem moderation) throws Exception {
        if (!event.assetId().equals(asset.assetId()) || !"APPROVED".equals(moderation.status()))
            return new SignedContract(asset.assetId(), "MODERATION_REQUIRED", "");
        String html = "<h1>Game backend contract</h1><p>Player " + asset.playerId() + " grants use of " + asset.title() + ".</p>";
        String envelope = pdf.generate(html, "contract-" + asset.assetId());
        return new SignedContract(asset.assetId(), "SIGNED", envelope);
    }
}
