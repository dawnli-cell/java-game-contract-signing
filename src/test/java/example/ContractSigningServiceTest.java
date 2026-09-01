package example;

public final class ContractSigningServiceTest {
    public static void main(String[] args) throws Exception {
        var service = new ContractSigningService((html, id) -> "{\"ok\":true,\"data\":{\"id\":\"pdf-1\"}}");
        var asset = new ContractSigningService.PlayerAsset("p", "a", "Castle");
        var event = new ContractSigningService.LiveEvent("SUBMITTED", "a");
        var approved = service.process(asset, event, new ContractSigningService.ModerationItem("a", "APPROVED"));
        var queued = service.process(asset, event, new ContractSigningService.ModerationItem("a", "QUEUED"));
        if (!"SIGNED".equals(approved.state()) || !"MODERATION_REQUIRED".equals(queued.state())) throw new AssertionError();
        System.out.println("business decision test passed");
    }
}
