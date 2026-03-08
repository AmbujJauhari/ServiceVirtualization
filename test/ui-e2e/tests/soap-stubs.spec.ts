import { test, expect, Page } from '@playwright/test';
import { WIREMOCK_URL, uniqueSuffix } from './helpers';

// ============================================================================
// SOAP-specific helper
// ============================================================================

interface CreateSoapStubOptions {
  name: string;
  url: string;
  soapAction?: string;
  requestBody?: string;
  requestBodyMatchType?: 'contains' | 'equals' | 'matches';
  responseStatus?: number;
  responseBody?: string;
  webhookUrl?: string;
}

/**
 * Navigate to /soap/stubs/new, fill in all fields and submit.
 * After a successful submit the browser is redirected to /soap (the dashboard).
 */
async function createSoapStubViaUI(page: Page, opts: CreateSoapStubOptions): Promise<void> {
  await page.goto('/soap/stubs/new');

  // ── Basic Information ──────────────────────────────────────────────────────
  await page.locator('#name').fill(opts.name);

  // ── SOAP Configuration ─────────────────────────────────────────────────────
  await page.locator('#url').fill(opts.url);
  if (opts.soapAction) {
    await page.locator('#soapAction').fill(opts.soapAction);
  }

  // ── Request Matching tab (active by default) ───────────────────────────────
  if (opts.requestBody) {
    if (opts.requestBodyMatchType) {
      await page.locator('#requestBodyMatchType').selectOption(opts.requestBodyMatchType);
    }
    await page.locator('#requestBody').fill(opts.requestBody);
  }

  // ── Response Configuration tab ─────────────────────────────────────────────
  await page.getByRole('button', { name: 'Response Configuration' }).click();

  if (opts.webhookUrl) {
    await page.getByText('Webhook Response').click();
    await page.locator('#callbackUrl').fill(opts.webhookUrl);
  } else {
    if (opts.responseStatus !== undefined) {
      await page.locator('#responseStatus').fill(String(opts.responseStatus));
    }
    if (opts.responseBody) {
      await page.locator('#responseBody').fill(opts.responseBody);
    }
  }

  // ── Submit ──────────────────────────────────────────────────────────────────
  // Wait for the POST API response and the button click together.
  // React Router's navigate() is a SPA pushState that never fires a browser
  // 'load' event, making page.waitForURL unreliable here.  Instead we wait
  // for the API call then navigate to /soap ourselves.
  await Promise.all([
    page.waitForResponse(
      (r) =>
        r.url().includes('/api/soap/stubs') &&
        r.request().method() === 'POST',
      { timeout: 30_000 },
    ),
    page.getByRole('button', { name: 'Create Stub' }).click(),
  ]);
  // Navigate to the list page so the caller can immediately check rows
  await page.goto('/soap');
}

/** Returns a row locator scoped to the SOAP stub with the given name. */
function soapStubRow(page: Page, stubName: string) {
  return page.locator('tbody tr').filter({ hasText: stubName });
}

// Standard XML payloads reused across tests
const REQUEST_ENVELOPE = (operation: string, innerXml: string) =>
  `<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><${operation}>${innerXml}</${operation}></soap:Body></soap:Envelope>`;

const RESPONSE_ENVELOPE = (body: string) =>
  `<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>${body}</soap:Body></soap:Envelope>`;

// ============================================================================
// Tests
// ============================================================================

test.describe('SOAP Protocol — stub lifecycle', () => {
  test('stub list page loads and shows the Create SOAP Stub button', async ({ page }) => {
    await page.goto('/soap');
    await expect(page.getByRole('link', { name: 'Create SOAP Stub' })).toBeVisible();
  });

  test('create a basic SOAP stub — appears in list and WireMock responds', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Basic SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/basic/${suffix}`;
    const responseXml = RESPONSE_ENVELOPE(`<GetBasicResponse><status>OK</status></GetBasicResponse>`);

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      responseStatus: 200,
      responseBody: responseXml,
    });

    // Verify stub appears in the list with expected fields
    const row = soapStubRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText(stubUrl, { exact: true })).toBeVisible();
    await expect(row.getByText('200')).toBeVisible();

    // Send a SOAP POST to WireMock — should return 200 with our XML body
    const res = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('GetBasic', '<id>1</id>'),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(res.status()).toBe(200);
    const body = await res.text();
    expect(body).toContain('<status>OK</status>');
  });

  test('SOAPAction header matching — only served when SOAPAction matches', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `SOAPAction Stub ${suffix}`;
    const stubUrl = `/soap/e2e/soapaction/${suffix}`;
    const soapAction = `urn:e2e:GetUser:${suffix}`;
    const responseXml = RESPONSE_ENVELOPE(`<GetUserResponse><userId>42</userId></GetUserResponse>`);

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      soapAction,
      responseStatus: 200,
      responseBody: responseXml,
    });

    await expect(soapStubRow(page, stubName)).toBeVisible();

    const postSoap = (extraHeaders: Record<string, string> = {}) =>
      request.post(`${WIREMOCK_URL}${stubUrl}`, {
        data: REQUEST_ENVELOPE('GetUser', '<userId>42</userId>'),
        headers: { 'Content-Type': 'text/xml; charset=utf-8', ...extraHeaders },
      });

    // Correct SOAPAction → 200
    const matchRes = await postSoap({ SOAPAction: soapAction });
    expect(matchRes.status()).toBe(200);
    expect(await matchRes.text()).toContain('<userId>42</userId>');

    // Wrong SOAPAction → 404 (WireMock has no matching stub)
    const wrongRes = await postSoap({ SOAPAction: 'urn:wrong:action' });
    expect(wrongRes.status()).toBe(404);

    // No SOAPAction header → 404
    const noHeaderRes = await postSoap();
    expect(noHeaderRes.status()).toBe(404);
  });

  test('XML body matching — "contains" — served only when body contains the snippet', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Body Contains SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/body-contains/${suffix}`;
    const matchSnippet = `<operationCode>LOOKUP_${suffix}</operationCode>`;
    const responseXml = RESPONSE_ENVELOPE(`<LookupResponse><result>found</result></LookupResponse>`);

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      requestBody: matchSnippet,
      requestBodyMatchType: 'contains',
      responseStatus: 200,
      responseBody: responseXml,
    });

    await expect(soapStubRow(page, stubName)).toBeVisible();

    // Body containing the snippet → 200
    const matchRes = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('Lookup', matchSnippet),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(matchRes.status()).toBe(200);
    expect(await matchRes.text()).toContain('<result>found</result>');

    // Body missing the snippet → 404
    const noMatchRes = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('Lookup', `<operationCode>OTHER</operationCode>`),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(noMatchRes.status()).toBe(404);
  });

  test('XML body matching — "equals" — served only when body exactly matches', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Body Equals SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/body-equals/${suffix}`;
    // Compact XML — WireMock equalTo is whitespace-sensitive
    const exactBody = `<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><Ping><id>${suffix}</id></Ping></soap:Body></soap:Envelope>`;
    const responseXml = RESPONSE_ENVELOPE(`<PingResponse><pong>${suffix}</pong></PingResponse>`);

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      requestBody: exactBody,
      requestBodyMatchType: 'equals',
      responseStatus: 200,
      responseBody: responseXml,
    });

    await expect(soapStubRow(page, stubName)).toBeVisible();

    // Exact body → 200
    const matchRes = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: exactBody,
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(matchRes.status()).toBe(200);
    expect(await matchRes.text()).toContain(`<pong>${suffix}</pong>`);

    // Slightly different body → 404
    const noMatchRes = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: exactBody.replace(`<id>${suffix}</id>`, `<id>DIFFERENT</id>`),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(noMatchRes.status()).toBe(404);
  });

  test('XML body matching — "regex" — matches when body satisfies the pattern', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Body Regex SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/body-regex/${suffix}`;
    // WireMock `matches` is anchored — wrap with .* to search within the body
    const regexPattern = `.*<orderId>ORD-\\d+</orderId>.*`;
    const responseXml = RESPONSE_ENVELOPE(`<OrderResponse><matched>true</matched></OrderResponse>`);

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      requestBody: regexPattern,
      requestBodyMatchType: 'matches',
      responseStatus: 200,
      responseBody: responseXml,
    });

    await expect(soapStubRow(page, stubName)).toBeVisible();

    // Body with orderId matching ORD-\d+ → 200
    const matchRes = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('PlaceOrder', `<orderId>ORD-99887</orderId>`),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(matchRes.status()).toBe(200);
    expect(await matchRes.text()).toContain('<matched>true</matched>');

    // Body with non-numeric orderId → 404
    const noMatchRes = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('PlaceOrder', `<orderId>REF-ABC</orderId>`),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(noMatchRes.status()).toBe(404);
  });

  test('toggle stub status — deactivated stub returns 404; reactivated returns 200', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Toggle SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/toggle/${suffix}`;
    const responseXml = RESPONSE_ENVELOPE(`<ToggleResponse><active>true</active></ToggleResponse>`);

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      responseStatus: 200,
      responseBody: responseXml,
    });

    const row = soapStubRow(page, stubName);

    const postSoap = () =>
      request.post(`${WIREMOCK_URL}${stubUrl}`, {
        data: REQUEST_ENVELOPE('Toggle', '<ping/>'),
        headers: { 'Content-Type': 'text/xml; charset=utf-8' },
      });

    // Initially ACTIVE — WireMock returns 200
    expect((await postSoap()).status()).toBe(200);

    // ── Deactivate ──────────────────────────────────────────────────────────
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/soap/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'ACTIVE', exact: true }).click(),
    ]);
    // Button should now read INACTIVE
    await expect(row.getByRole('button', { name: 'INACTIVE', exact: true })).toBeVisible({
      timeout: 10_000,
    });

    // WireMock stub removed → 404
    expect((await postSoap()).status()).toBe(404);

    // ── Reactivate ─────────────────────────────────────────────────────────
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/soap/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'INACTIVE', exact: true }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'ACTIVE', exact: true })).toBeVisible({
      timeout: 10_000,
    });

    // WireMock stub restored → 200
    expect((await postSoap()).status()).toBe(200);
  });

  test('edit stub — updated response body is reflected in WireMock', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Edit SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/edit/${suffix}`;
    const originalResponseXml = RESPONSE_ENVELOPE(`<EditResponse><version>1</version></EditResponse>`);
    const updatedResponseXml = RESPONSE_ENVELOPE(`<EditResponse><version>2</version></EditResponse>`);

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      responseStatus: 200,
      responseBody: originalResponseXml,
    });

    const row = soapStubRow(page, stubName);

    // Verify original response
    const origRes = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('Edit', '<op>check</op>'),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(origRes.status()).toBe(200);
    expect(await origRes.text()).toContain('<version>1</version>');

    // Open edit form
    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/soap/stubs/**/edit');

    // Switch to Response Configuration tab and update the body
    await page.getByRole('button', { name: 'Response Configuration' }).click();
    await page.locator('#responseBody').fill(updatedResponseXml);

    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/api/soap/stubs') &&
          r.request().method() === 'PUT',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Update Stub' }).click(),
    ]);
    await page.goto('/soap');

    // Verify updated response from WireMock
    const updatedRes = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('Edit', '<op>check</op>'),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(updatedRes.status()).toBe(200);
    expect(await updatedRes.text()).toContain('<version>2</version>');
  });

  test('delete stub — stub disappears from list and WireMock returns 404', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Delete SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/delete/${suffix}`;

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      responseStatus: 200,
      responseBody: RESPONSE_ENVELOPE(`<DeleteResponse><done>true</done></DeleteResponse>`),
    });

    const row = soapStubRow(page, stubName);
    await expect(row).toBeVisible();

    // Confirm stub is live in WireMock
    expect(
      (
        await request.post(`${WIREMOCK_URL}${stubUrl}`, {
          data: REQUEST_ENVELOPE('Delete', '<id>1</id>'),
          headers: { 'Content-Type': 'text/xml; charset=utf-8' },
        })
      ).status(),
    ).toBe(200);

    // Delete: click "Delete" → click "Confirm"
    await row.getByRole('button', { name: 'Delete', exact: true }).click();
    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/soap/stubs') && r.request().method() === 'DELETE',
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Confirm', exact: true }).click(),
    ]);

    // Row should no longer be visible
    await expect(row).not.toBeVisible({ timeout: 10_000 });

    // WireMock stub removed → 404
    expect(
      (
        await request.post(`${WIREMOCK_URL}${stubUrl}`, {
          data: REQUEST_ENVELOPE('Delete', '<id>1</id>'),
          headers: { 'Content-Type': 'text/xml; charset=utf-8' },
        })
      ).status(),
    ).toBe(404);
  });

  test('stub list search filters by name', async ({ page }) => {
    const suffix = uniqueSuffix();
    const nameA = `SearchA SOAP ${suffix}`;
    const nameB = `SearchB SOAP ${suffix}`;

    for (const name of [nameA, nameB]) {
      await createSoapStubViaUI(page, {
        name,
        url: `/soap/e2e/search/${name.replace(/\s+/g, '-')}`,
        responseBody: RESPONSE_ENVELOPE(`<SearchResponse/>`),
      });
    }

    await page.goto('/soap');

    const searchInput = page.getByPlaceholder(
      'Search stubs by name, description, URL, or tags...',
    );
    await searchInput.fill(`SearchA SOAP ${suffix}`);

    await expect(soapStubRow(page, nameA)).toBeVisible();
    await expect(soapStubRow(page, nameB)).not.toBeVisible();

    await searchInput.clear();
    await expect(soapStubRow(page, nameA)).toBeVisible();
    await expect(soapStubRow(page, nameB)).toBeVisible();
  });

  test('create stub set to INACTIVE — WireMock returns 404 until activated', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Inactive SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/inactive/${suffix}`;

    // Create stub with INACTIVE status
    await page.goto('/soap/stubs/new');
    await page.locator('#name').fill(stubName);
    await page.locator('#url').fill(stubUrl);
    await page.locator('#status').selectOption('INACTIVE');
    await page.getByRole('button', { name: 'Response Configuration' }).click();
    await page.locator('#responseBody').fill(
      RESPONSE_ENVELOPE(`<InactiveResponse><msg>hello</msg></InactiveResponse>`),
    );
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/api/soap/stubs') &&
          r.request().method() === 'POST',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Create Stub' }).click(),
    ]);
    await page.goto('/soap');

    const row = soapStubRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByRole('button', { name: 'INACTIVE', exact: true })).toBeVisible();

    // WireMock has no mapping for inactive stub → 404
    expect(
      (
        await request.post(`${WIREMOCK_URL}${stubUrl}`, {
          data: REQUEST_ENVELOPE('Inactive', '<test/>'),
          headers: { 'Content-Type': 'text/xml; charset=utf-8' },
        })
      ).status(),
    ).toBe(404);

    // Activate the stub
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/soap/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'INACTIVE', exact: true }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'ACTIVE', exact: true })).toBeVisible({
      timeout: 10_000,
    });

    // Now WireMock responds
    expect(
      (
        await request.post(`${WIREMOCK_URL}${stubUrl}`, {
          data: REQUEST_ENVELOPE('Inactive', '<test/>'),
          headers: { 'Content-Type': 'text/xml; charset=utf-8' },
        })
      ).status(),
    ).toBe(200);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Non-200 response status (SOAP faults / error responses)
  // ──────────────────────────────────────────────────────────────────────────

  test('stub configured with 500 status returns a SOAP fault body', async ({ page, request }) => {
    const suffix = uniqueSuffix();
    const stubName = `SOAP Fault Stub ${suffix}`;
    const stubUrl = `/soap/e2e/fault/${suffix}`;
    const faultXml = `<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><soap:Fault><faultcode>soap:Server</faultcode><faultstring>Internal error</faultstring></soap:Fault></soap:Body></soap:Envelope>`;

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      responseStatus: 500,
      responseBody: faultXml,
    });

    const row = soapStubRow(page, stubName);
    await expect(row).toBeVisible();
    // List shows the response status badge
    await expect(row.getByText('500')).toBeVisible();

    // WireMock returns 500 with the fault body
    const res = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('TriggerFault', '<op/>'),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(res.status()).toBe(500);
    expect(await res.text()).toContain('<faultstring>Internal error</faultstring>');
  });

  test('stub configured with 400 status returns client-error body', async ({ page, request }) => {
    const suffix = uniqueSuffix();
    const stubName = `SOAP 400 Stub ${suffix}`;
    const stubUrl = `/soap/e2e/badrequest/${suffix}`;
    const errorXml = RESPONSE_ENVELOPE(`<Error><code>INVALID_REQUEST</code><message>Bad input</message></Error>`);

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      responseStatus: 400,
      responseBody: errorXml,
    });

    const res = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('Submit', '<data/>'),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(res.status()).toBe(400);
    expect(await res.text()).toContain('<code>INVALID_REQUEST</code>');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Webhook / callback response type
  // ──────────────────────────────────────────────────────────────────────────

  test('webhook response form — selecting "Webhook Response" shows URL input', async ({ page }) => {
    const suffix = uniqueSuffix();
    await page.goto('/soap/stubs/new');

    await page.locator('#name').fill(`Webhook Form Test ${suffix}`);
    await page.locator('#url').fill(`/soap/e2e/webhook-form/${suffix}`);

    await page.getByRole('button', { name: 'Response Configuration' }).click();

    // By default "Static Response" radio is selected
    await expect(page.locator('input[name="responseType"][value="static"]')).toBeChecked();
    await expect(page.locator('input[name="responseType"][value="callback"]')).not.toBeChecked();
    await expect(page.locator('#responseBody')).toBeVisible();

    // Select "Webhook Response"
    await page.getByText('Webhook Response').click();
    await expect(page.locator('input[name="responseType"][value="callback"]')).toBeChecked();
    await expect(page.locator('#callbackUrl')).toBeVisible();
    await expect(page.locator('#responseBody')).not.toBeVisible();
  });

  test('webhook stub — appears in list as ACTIVE and WireMock returns 200', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Webhook SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/webhook-create/${suffix}`;

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      webhookUrl: `http://localhost:8080/actuator/health`,
    });

    const row = soapStubRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByRole('button', { name: 'ACTIVE', exact: true })).toBeVisible();

    // WireMock fires webhook async and returns 200 with empty body
    const res = await request.post(`${WIREMOCK_URL}${stubUrl}`, {
      data: REQUEST_ENVELOPE('WebhookTrigger', '<ping/>'),
      headers: { 'Content-Type': 'text/xml; charset=utf-8' },
    });
    expect(res.status()).toBe(200);
  });

  test('webhook stub — can be deactivated and reactivated', async ({ page, request }) => {
    const suffix = uniqueSuffix();
    const stubName = `Webhook Toggle Stub ${suffix}`;
    const stubUrl = `/soap/e2e/webhook-toggle/${suffix}`;

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      webhookUrl: `http://localhost:8080/actuator/health`,
    });

    const row = soapStubRow(page, stubName);

    const postSoap = () =>
      request.post(`${WIREMOCK_URL}${stubUrl}`, {
        data: REQUEST_ENVELOPE('Toggle', '<ping/>'),
        headers: { 'Content-Type': 'text/xml; charset=utf-8' },
      });

    expect((await postSoap()).status()).toBe(200);

    // Deactivate
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/soap/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'ACTIVE', exact: true }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'INACTIVE', exact: true })).toBeVisible({
      timeout: 10_000,
    });
    expect((await postSoap()).status()).toBe(404);

    // Reactivate
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/soap/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'INACTIVE', exact: true }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'ACTIVE', exact: true })).toBeVisible({
      timeout: 10_000,
    });
    expect((await postSoap()).status()).toBe(200);
  });

  test('webhook stub edit — URL is preserved after editing', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Webhook Edit Stub ${suffix}`;
    const stubUrl = `/soap/e2e/webhook-edit/${suffix}`;
    const originalWebhookUrl = `http://localhost:8080/actuator/health`;
    const updatedWebhookUrl = `http://localhost:8080/actuator/info`;

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      webhookUrl: originalWebhookUrl,
    });

    const row = soapStubRow(page, stubName);
    await expect(row).toBeVisible();

    // Open edit form
    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/soap/stubs/**/edit');

    // Verify webhook radio + URL are pre-filled
    await page.getByRole('button', { name: 'Response Configuration' }).click();
    await expect(page.locator('input[name="responseType"][value="callback"]')).toBeChecked();
    await expect(page.locator('#callbackUrl')).toHaveValue(originalWebhookUrl);

    // Update the webhook URL
    await page.locator('#callbackUrl').fill(updatedWebhookUrl);
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/api/soap/stubs') &&
          r.request().method() === 'PUT',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Update Stub' }).click(),
    ]);
    await page.goto('/soap');

    // Re-open edit form and confirm it was saved
    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/soap/stubs/**/edit');
    await page.getByRole('button', { name: 'Response Configuration' }).click();
    await expect(page.locator('#callbackUrl')).toHaveValue(updatedWebhookUrl);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // SOAPAction + body matching combined
  // ──────────────────────────────────────────────────────────────────────────

  test('SOAPAction + body matching combined — both conditions must be met', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Combined SOAP Stub ${suffix}`;
    const stubUrl = `/soap/e2e/combined/${suffix}`;
    const soapAction = `urn:e2e:CombinedOp:${suffix}`;
    const matchSnippet = `<customerId>CUST-${suffix}</customerId>`;
    const responseXml = RESPONSE_ENVELOPE(`<CombinedResponse><matched>true</matched></CombinedResponse>`);

    await createSoapStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      soapAction,
      requestBody: matchSnippet,
      requestBodyMatchType: 'contains',
      responseStatus: 200,
      responseBody: responseXml,
    });

    await expect(soapStubRow(page, stubName)).toBeVisible();

    const post = (headers: Record<string, string>, body: string) =>
      request.post(`${WIREMOCK_URL}${stubUrl}`, {
        data: body,
        headers: { 'Content-Type': 'text/xml; charset=utf-8', ...headers },
      });

    // Correct SOAPAction + matching body → 200
    expect(
      (await post({ SOAPAction: soapAction }, REQUEST_ENVELOPE('CombinedOp', matchSnippet))).status(),
    ).toBe(200);

    // Correct SOAPAction but wrong body → 404
    expect(
      (
        await post(
          { SOAPAction: soapAction },
          REQUEST_ENVELOPE('CombinedOp', `<customerId>WRONG</customerId>`),
        )
      ).status(),
    ).toBe(404);

    // Correct body but wrong SOAPAction → 404
    expect(
      (await post({ SOAPAction: 'urn:wrong' }, REQUEST_ENVELOPE('CombinedOp', matchSnippet))).status(),
    ).toBe(404);
  });
});
