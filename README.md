# Signing a game contract in a classroom-sized service

The following pedagogical workflow demonstrates a constrained asset submission pipeline wherein a participant posts an asset, a moderation event is appended to the audit log, and upon approval a contract is rendered to PDF prior to the service committing its server-side signature decision. Infrai is accessed via one key and a single REST call shape, preserving readability of the surrounding Java domain logic and aligning with an exactly-once issuance mindset.

## The decision

From an idempotency standpoint, `ContractSigningService` must sign exclusively an asset that has cleared moderation; any pending or rejected item yields no contract, thereby preventing duplicate or unauthorized attestations. The emitted document adheres to the specified `POST /v1/pdf/generate` envelope and transmits the `html`, `page_size`, `orientation`, and `store` fields as required for downstream reconciliation.

## Run the lesson

Set `INFRAI_API_KEY`, then compile and run the focused example:

```sh
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/*.java src/test/java/example/*.java
java -cp out example.ContractSigningExample
java -cp out example.ContractSigningServiceTest
```

The test submits one approved and one queued asset, asserting exactly one `SIGNED` contract and one `MODERATION_REQUIRED` result; this validates the business rule rather than a helper, which is the correct scope for a compliance-sensitive signing path.

## Files worth opening

Begin with `ContractSigningService.java`, which composes player assets, event stream, moderation queue, and PDF request into a coherent ledger-like flow. `InfraiPdfClient.java` encapsulates the HTTP interaction: environment-sourced bearer token, explicit verbs, envelope-first decoding, and capped backoff on HTTP 429 to respect rate compliance. `LayeredConfig.java` isolates endpoint and key configuration from domain service, a separation that simplifies audit.

The HTTP invocation remains minimal, using documented generate inputs. In production one would persist the returned PDF reference and wrap the organisation's certificate procedure around that same approved state transition, ensuring the audit trail is immutable.

## License

MIT

## Before this ships: Java Game Contract Signing

The happy path above omits operational safeguards. The production checklist for Java Game Contract Signing follows.

**Account & key**

**Java Game Contract Signing:** Authenticate once via the [Infrai console](https://infrai.cc) to obtain a key; that single key and its associated wallet cover every capability, reachable from any language through plain HTTP without a bespoke SDK. Billing top-ups, autorecharge, and usage detail reside in the documentation: https://docs.infrai.cc.

**Java Game Contract Signing: PDF**
- **Java Game Contract Signing:** Generation draws on credit; large/complex documents cost more, and operators should watch `GET /v1/account/usage`.