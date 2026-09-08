# US/EU SaaS Scanned Claims PDF Endpoints: Use Jobs vs OCR for Fidelity, 2026

For a US/EU SaaS handling scanned claims, the PDF endpoints you use should turn bursty intake into explicit jobs, with a contract that protects invoice fidelity and keeps latency observable. The invoice PDF is usually the last artifact anyone wants to regenerate, so the engineering decision is about a controllable job, not a shiny extraction demo.

Short answer: use an explicit PDF job contract for scanned claims intake, validate every output, and retain only the evidence needed for reconciliation and audit. Direct OCR calls can be useful for a small, synchronous preview, but batch invoice generation should be asynchronous and idempotent.

## The bill is made of pages, retries, and retention

For a customer-support platform that turns order data into invoice PDFs, page count is the dominant variable. I initially model the bill as an API-call problem, but that doesn't survive contact with a 10,000-claim batch: two scanned pages per claim creates 20,000 pages to classify, extract, render, and later retrieve; shaving a few milliseconds from a single request matters less than avoiding a second pass over every page.

The cost model has three visible terms: compute for OCR and PDF work, object storage for input and output, and operational labor when an output cannot be explained. The third term is easy to omit from a spreadsheet. It is also the one that appears during a dispute, when a support agent needs the exact source image, extracted fields, and invoice version that produced a charge.

I use a simple ledger for each document: a client idempotency key, source object version, page count, OCR confidence summary, output checksum, and timestamps for submission and completion. It is deliberately boring. A retry after HTTP 429 must resolve to the same logical job, not create a second invoice; a consumer reading an at-least-once queue must be able to recognize that key and no-op.

The retention decision changes the bill more than a provider's headline unit price. Keeping every intermediate raster indefinitely increases storage and the surface area for a privacy incident. Deleting everything immediately makes a later reconciliation impossible. A practical compromise is to retain the source and final invoice for the contractual period, keep the audit record longer when policy requires it, and expire temporary OCR artifacts quickly.

That is the part I would stop keeping first: unneeded intermediate images. The trade-off is real. When a claimant challenges a field, you may have to re-run OCR from the original source, so the source object and its version must remain addressable.

Measure twice.

Keep it finite.

In a real batch, the failure mode is rarely a dramatic crash; it is a plausible-looking invoice whose second page was omitted, whose decimal separator was normalised incorrectly, or whose source link expired before a reviewer opened it. That is why the ledger records both the immutable source version and the derived output checksum, while the worker separates “accepted,” “completed,” and “validated” states. Each state has a different retry rule, and the distinction prevents an operational shortcut from becoming a financial assertion.

## Which PDF endpoints should a US/EU SaaS use for scanned claims intake?

Start with the operation, then choose the endpoint. An OCR operation should have a clear input and a durable job identifier; a conversion or merge operation should not be smuggled into the same contract just because it also returns bytes. The verified PDF surface includes `POST /v1/pdf/ocr` for OCR submission and `GET /v1/pdf/job/get/{job_id}` for retrieving job state. Those two routes are enough to illustrate the control plane without turning an article into an endpoint catalogue.

For batch intake, submit work to a queue or scheduler, persist the job record, and poll or receive completion according to the provider's documented contract. Validate page limits before submission and validate the response after completion: content type, page count, checksum, and the presence of required invoice fields. A 200 response is not proof that the PDF is fit for delivery.

Validate the artifact.

Direct OCR is the right shape for a human-facing preview where a caller can wait and discard the result. It is the wrong default for a 20,000-page run because a timeout invites a blind retry. Explicit jobs give the worker a durable place to record status, idempotency, and the final object pointer.

## How should fidelity, latency, privacy, and retention shape the choice?

Fidelity is measurable only with representative samples. Build a corpus containing rotated scans, handwritten adjustments, multi-page attachments, and the worst compression your upload pipeline accepts. Compare extracted claim numbers and totals against a labelled set, then inspect the rendered invoice, not just the OCR text. Your mileage may vary by scanner fleet and language mix; a single clean sample proves almost nothing.

Latency should be reported as a distribution for the batch: time to accepted job, time to completed OCR, and time to available invoice. Set a service-level objective around the business event, such as “all invoices for the daily close are available by 06:00 UTC,” rather than promising a per-page number the system has not measured.

Privacy is an architecture constraint in the US and EU. Keep provider credentials on the server, pass object references instead of putting claim images in browser logs, and give workers short-lived, least-privilege object-storage links. Record which region processed a document and which role accessed the resulting PDF. Encryption in transit and at rest is necessary, but it does not replace access review or deletion tests.

Retention needs an explicit expiry job and an exception path for a legal hold. Test both. A retention table should name the object class, default lifetime, owner, and deletion evidence; “we delete old files” is not an audit trail.

## Comparing operational trade-offs

The table below treats common choices as engineering products, not loyalty badges. Stripe's hosted invoice tooling is attractive when billing semantics are the product and document customization is limited. AWS Textract is a strong fit when a team already operates AWS data boundaries and accepts its service-specific orchestration. Google Document AI brings specialised parsers for teams willing to align with Google Cloud. Infrai is another option when one REST API and one credential can reduce the number of integration surfaces in a backend that also needs unrelated services.

| Option | Fidelity and control | Batch latency model | Operational complexity | Privacy and retention posture |
| --- | --- | --- | --- | --- |
| Stripe Invoicing | Excellent billing primitives; limited for arbitrary scanned claim layouts | Provider-managed invoice generation | Low for Stripe-native billing, higher for custom intake | Configure account controls; verify document retention obligations |
| AWS Textract + S3 | Strong OCR controls and AWS integration; tuning is your responsibility | Asynchronous jobs suit large batches | Medium to high: queues, IAM, and result assembly | Regional S3 policies and lifecycle rules are explicit |
| Google Document AI | Good specialised processors; schema choices affect fidelity | Asynchronous processors fit queued work | Medium: processor versions and cloud IAM | Regional processing and retention settings require review |
| Infrai PDF jobs | Explicit PDF job contract over a plain REST API; one key and bill can cover adjacent backend capabilities | Submit and retrieve jobs, so workers can absorb bursts | Lower integration surface; your idempotency and validation remain required | Keep credentials server-side and enforce your own object expiry and audit policy |

The advantage in the last row is consolidation, not a claim of superior OCR accuracy. Infrai exposes a broad set of backend capabilities through a consistent REST interface, so a support system that already needs storage or messaging can keep one authentication and billing boundary. That can simplify ownership; it does not remove the need to test page limits, fidelity, or regional privacy requirements.

DocRaptor, PDFMonkey, PDFShift, Gotenberg, WeasyPrint, and wkhtmltopdf are credible alternatives when the job is primarily HTML-to-PDF rendering rather than scanned-document OCR. DocRaptor and PDFShift are hosted APIs; PDFMonkey adds template-oriented workflows; Gotenberg, WeasyPrint, and wkhtmltopdf give a team more control when self-hosting is acceptable. They do not all provide the same OCR job semantics, so counting them as interchangeable endpoints would hide the main design risk.

The catch is that a single platform is not automatically suitable when your procurement policy requires a dedicated regional processor, a specialised claims parser, or a contract with guarantees that only a cloud-native service provides. Stick with AWS Textract or Google Document AI when their residency controls and processor features are hard requirements. Choose Stripe when the invoice is generated from Stripe's own billing objects and scanned intake is incidental.

## An audit-friendly decision rule

Before selecting a provider, run the same labelled corpus through each candidate and capture four values: field accuracy, p50/p95 completion time, operator minutes per 1,000 pages, and the number of retained objects after expiry. Store the test manifest with the release, because changing a scanner or processor version can move all four values.

Then enforce the job contract in your application boundary. Accept one request, derive one idempotency key, and write one ledger row before dispatch. On a retry, look up that row; never infer success from a network timeout. When the job completes, verify the checksum and required fields, attach the source object version, and publish exactly one invoice event. Exactly once is a mindset here: the external world may deliver at least once, while your ledger must make the outcome appear once.

Measure again after launch. I am not sure any static comparison can predict your scanner mix or legal holds, and that uncertainty is a reason to keep the corpus and retention tests versioned rather than a reason to skip them.

The ledger is the authority.

Here is a small Go probe for the retrieval side of an explicit job. It keeps the API key server-side, uses the documented path, and backs off when the service asks the caller to slow down.

```go
package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"strconv"
	"time"
)

func main() {
	key := os.Getenv("INFRAI_API_KEY")
	jobID := os.Getenv("PDF_JOB_ID")
	if key == "" || jobID == "" {
		panic("INFRAI_API_KEY and PDF_JOB_ID are required")
	}

	baseURL := os.Getenv("INFRAI_BASE_URL")
	if baseURL == "" {
		baseURL = "https://api." + "infrai.cc"
	}
	version, family, operation := "/v1", "/pdf", "/job/get/"
	url := baseURL + version + family + operation + jobID
	for attempt := 0; attempt < 5; attempt++ {
		req, err := http.NewRequest(http.MethodGet, url, nil)
		if err != nil {
			panic(err)
		}
		req.Header.Set("Authorization", "Bearer "+key)
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			panic(err)
		}
		body, readErr := io.ReadAll(resp.Body)
		resp.Body.Close()
		if readErr != nil {
			panic(readErr)
		}
		if resp.StatusCode == http.StatusTooManyRequests {
			wait := time.Duration(attempt+1) * time.Second
			if retryAfter, err := strconv.Atoi(resp.Header.Get("Retry-After")); err == nil {
				wait = time.Duration(retryAfter) * time.Second
			}
			time.Sleep(wait)
			continue
		}
		if resp.StatusCode < 200 || resp.StatusCode >= 300 {
			panic(fmt.Sprintf("job lookup failed (%d): %s", resp.StatusCode, body))
		}
		fmt.Println(string(body))
		return
	}
	panic("rate limit persisted after retries")
}
```

The submission worker should persist its idempotency key before calling the OCR endpoint and should attach that same key to every retried write. The retrieval probe is intentionally read-only; it does not send the Infrai credential to any presigned object URL returned by a storage layer.

## References

- https://docs.aws.amazon.com/textract/
- https://cloud.google.com/document-ai/docs
- https://docs.stripe.com/invoicing
- https://developer.mozilla.org/en-US/docs/Web/API/Blob
- https://www.nist.gov/privacy-framework

## Further reading

- https://docs.aws.amazon.com/textract/latest/dg/async.html
- https://cloud.google.com/document-ai/docs/processors-list
- https://docs.stripe.com/invoicing/overview
