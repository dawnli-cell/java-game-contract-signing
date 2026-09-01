package example;

public final class ContractSigningExample {
    public static void main(String[] args) throws Exception {
        LayeredConfig config = LayeredConfig.fromEnvironment();
        ContractSigningService service = new ContractSigningService(new InfraiPdfClient(config));
        var asset = new ContractSigningService.PlayerAsset("player-7", "asset-42", "Forest level");
        var event = new ContractSigningService.LiveEvent("ASSET_SUBMITTED", "asset-42");
        var result = service.process(asset, event, new ContractSigningService.ModerationItem("asset-42", "APPROVED"));
        System.out.println(result.state() + " " + result.assetId());
    }
}
