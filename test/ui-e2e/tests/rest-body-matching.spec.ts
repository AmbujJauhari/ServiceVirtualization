import { test, expect } from '@playwright/test';
import { WIREMOCK_URL, uniqueSuffix, createStubViaUI, stubRow } from './helpers';

test.describe('REST Protocol — request body matching', () => {
  test('POST stub with "contains" body match — responds only when body contains the snippet', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Body Contains Stub ${suffix}`;
    const stubUrl = `/api/e2e/body-contains/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;
    // Use the suffix in the match snippet so it is unique and won't clash with other stubs
    const matchSnippet = `process-${suffix}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'POST',
      requestBody: matchSnippet,
      requestBodyMatchType: 'contains',
      responseStatus: '201',
      responseBody: JSON.stringify({ processed: true, ref: suffix }, null, 2),
    });

    const row = stubRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('POST', { exact: true })).toBeVisible();
    await expect(row.getByText('201', { exact: true })).toBeVisible();

    // POST with body containing the snippet → 201
    const matchResponse = await request.post(proxyUrl, {
      data: JSON.stringify({ action: matchSnippet, payload: 'hello' }),
      headers: { 'Content-Type': 'application/json' },
    });
    expect(matchResponse.status()).toBe(201);
    expect((await matchResponse.json()).processed).toBe(true);

    // POST with body NOT containing the snippet → WireMock returns 404
    const noMatchResponse = await request.post(proxyUrl, {
      data: JSON.stringify({ action: 'skip', payload: 'hello' }),
      headers: { 'Content-Type': 'application/json' },
    });
    expect(noMatchResponse.status()).toBe(404);
  });

  test('PUT stub with "exact" body match — only the exact string is accepted', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Body Exact Stub ${suffix}`;
    const stubUrl = `/api/e2e/body-exact/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;
    // WireMock exact body matching is whitespace-sensitive; use a compact single-line JSON string
    const exactBody = `{"id":${suffix},"status":"approved"}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'PUT',
      requestBody: exactBody,
      requestBodyMatchType: 'exact',
      responseStatus: '200',
      responseBody: JSON.stringify({ updated: true }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // PUT with the exact body → 200
    const matchResponse = await request.put(proxyUrl, {
      data: exactBody,
      headers: { 'Content-Type': 'application/json' },
    });
    expect(matchResponse.status()).toBe(200);
    expect((await matchResponse.json()).updated).toBe(true);

    // PUT with a different body → 404
    const noMatchResponse = await request.put(proxyUrl, {
      data: `{"id":${suffix},"status":"rejected"}`,
      headers: { 'Content-Type': 'application/json' },
    });
    expect(noMatchResponse.status()).toBe(404);
  });

  test('POST stub with "regex" body match — matches on a pattern inside the body', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Body Regex Stub ${suffix}`;
    const stubUrl = `/api/e2e/body-regex/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'POST',
      // WireMock's `matches` operator anchors against the full body string,
      // so .* prefix/suffix are required to match a substring via regex.
      requestBody: '.*"orderId":"ORD-\\d+".*',
      requestBodyMatchType: 'regex',
      responseStatus: '200',
      responseBody: JSON.stringify({ matched: 'order', type: 'regex' }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // Body with orderId matching the regex → 200
    const matchResponse = await request.post(proxyUrl, {
      data: '{"orderId":"ORD-12345","amount":100}',
      headers: { 'Content-Type': 'application/json' },
    });
    expect(matchResponse.status()).toBe(200);
    expect((await matchResponse.json()).matched).toBe('order');

    // Body with orderId NOT matching the regex (uses REF- prefix) → 404
    const noMatchResponse = await request.post(proxyUrl, {
      data: '{"orderId":"REF-12345","amount":100}',
      headers: { 'Content-Type': 'application/json' },
    });
    expect(noMatchResponse.status()).toBe(404);
  });

  test('POST stub returns 400 response body when body is unexpected', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Error Response Stub ${suffix}`;
    const stubUrl = `/api/e2e/body-error/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'POST',
      responseStatus: '400',
      responseBody: JSON.stringify({ error: 'Bad Request', code: 'INVALID_PAYLOAD' }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();
    await expect(stubRow(page, stubName).getByText('400', { exact: true })).toBeVisible();

    // Any POST → always returns 400 (no body matching condition set)
    const response = await request.post(proxyUrl, {
      data: '{"anything": true}',
      headers: { 'Content-Type': 'application/json' },
    });
    expect(response.status()).toBe(400);
    expect((await response.json()).error).toBe('Bad Request');
  });
});
