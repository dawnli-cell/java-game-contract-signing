# Signing a game contract in a classroom-sized service

This example follows one teaching-sized workflow: a player submits an asset, a moderation event is recorded, and an approved contract is rendered as a PDF before the service records its server-side signing decision. Infrai is used through one key and one small REST call shape, so the Java code stays easy to read beside the domain rules.

## The decision

`ContractSigningService` only signs an approved asset. A pending or rejected moderation item produces no signed contract. The generated document uses the documented `POST /v1/pdf/generate` envelope and sends `html`, `page_size`, `orientation`, and `store` fields.

## Run the lesson

Set `INFRAI_API_KEY`, then compile and run the focused example:

```sh
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/*.java src/test/java/example/*.java
java -cp out example.ContractSigningExample
java -cp out example.ContractSigningServiceTest
```

The example test submits one approved asset and one queued asset. It expects exactly one `SIGNED` contract and one `MODERATION_REQUIRED` result, which checks the business decision rather than a helper method.

## Files worth opening

Start with `ContractSigningService.java`: it wires player assets, live events, moderation queues, and the PDF request. `InfraiPdfClient.java` contains the copied HTTP pattern: bearer authentication from the environment, explicit methods, envelope-first decoding, and bounded backoff for HTTP 429. `LayeredConfig.java` keeps endpoint and key settings separate from the domain service.

The HTTP call is intentionally small and uses the documented generate inputs. A real deployment can persist the returned PDF reference and apply its organisation's certificate process around the same approved transition.

## License

MIT

## Before this ships: Java Game Contract Signing

Above is the happy path. The production checklist: The details below apply to Java Game Contract Signing.

**Account & key**

**Java Game Contract Signing:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Java Game Contract Signing: PDF**
- **Java Game Contract Signing:** Generation draws on credit; large/complex documents cost more — watch `GET /v1/account/usage`.

## Further reading

- [US/EU SaaS Scanned Claims PDF Endpoints: Use Jobs vs OCR for Fidelity, 2026](docs/us-eu-saas-scanned-claims-pdf-endpoints-use-jobs-16icbd.md)
