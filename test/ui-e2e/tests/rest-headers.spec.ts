import { test, expect } from '@playwright/test';
import { WIREMOCK_URL, uniqueSuffix, createStubViaUI, stubRow } from './helpers';

test.describe('REST Protocol — request header matching', () => {
  test('"exact" header match — only served when header value matches exactly', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Exact Header Stub ${suffix}`;
    const stubUrl = `/api/e2e/header-exact/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;
    const requiredHeaderName = 'X-Client-Id';
    const requiredHeaderValue = `client-${suffix}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      requestHeaders: [{ name: requiredHeaderName, value: requiredHeaderValue }],
      responseBody: JSON.stringify({ authenticated: true, client: requiredHeaderValue }, null, 2),
    });

    const row = stubRow(page, stubName);
    await expect(row).toBeVisible();

    // Request WITH the exact required header → 200
    const withHeaderRes = await request.get(proxyUrl, {
      headers: { [requiredHeaderName]: requiredHeaderValue },
    });
    expect(withHeaderRes.status()).toBe(200);
    expect((await withHeaderRes.json()).authenticated).toBe(true);

    // Request WITHOUT the header → 404 (no WireMock mapping matches)
    const withoutHeaderRes = await request.get(proxyUrl);
    expect(withoutHeaderRes.status()).toBe(404);

    // Request with the WRONG header value → 404
    const wrongValueRes = await request.get(proxyUrl, {
      headers: { [requiredHeaderName]: 'wrong-client-id' },
    });
    expect(wrongValueRes.status()).toBe(404);
  });

  test('"contains" header match — serves when header value contains the substring', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Contains Header Stub ${suffix}`;
    const stubUrl = `/api/e2e/header-contains/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      requestHeaders: [{ name: 'Authorization', value: 'Bearer', matchType: 'contains' }],
      responseBody: JSON.stringify({ authorized: true }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // Authorization header that CONTAINS "Bearer" → 200
    const bearerRes = await request.get(proxyUrl, {
      headers: { Authorization: 'Bearer some-jwt-token-here' },
    });
    expect(bearerRes.status()).toBe(200);
    expect((await bearerRes.json()).authorized).toBe(true);

    // Different token format (Basic) that does NOT contain "Bearer" → 404
    const basicRes = await request.get(proxyUrl, {
      headers: { Authorization: 'Basic dXNlcjpwYXNz' },
    });
    expect(basicRes.status()).toBe(404);

    // No Authorization header → 404
    const noAuthRes = await request.get(proxyUrl);
    expect(noAuthRes.status()).toBe(404);
  });

  test('"regex" header match — serves when header value matches a pattern', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Regex Header Stub ${suffix}`;
    const stubUrl = `/api/e2e/header-regex/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      // Matches any X-Request-Version header of the form "v<digits>"
      requestHeaders: [{ name: 'X-Request-Version', value: 'v\\d+', matchType: 'regex' }],
      responseBody: JSON.stringify({ versioned: true }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // Header matching the pattern (v1, v2, v100 …) → 200
    const v1Res = await request.get(proxyUrl, {
      headers: { 'X-Request-Version': 'v1' },
    });
    expect(v1Res.status()).toBe(200);

    const v42Res = await request.get(proxyUrl, {
      headers: { 'X-Request-Version': 'v42' },
    });
    expect(v42Res.status()).toBe(200);

    // Header NOT matching the pattern → 404
    const noneRes = await request.get(proxyUrl, {
      headers: { 'X-Request-Version': 'latest' },
    });
    expect(noneRes.status()).toBe(404);
  });

  test('multiple required headers — all must be present for a match', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Multi Header Stub ${suffix}`;
    const stubUrl = `/api/e2e/header-multi/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;
    const tenantId = `tenant-${suffix}`;
    const apiKey = `key-${suffix}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      requestHeaders: [
        { name: 'X-Tenant-Id', value: tenantId },
        { name: 'X-Api-Key', value: apiKey },
      ],
      responseBody: JSON.stringify({ access: 'granted', tenant: tenantId }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // Both headers present with correct values → 200
    const bothRes = await request.get(proxyUrl, {
      headers: { 'X-Tenant-Id': tenantId, 'X-Api-Key': apiKey },
    });
    expect(bothRes.status()).toBe(200);

    // Only first header → 404 (second required header missing)
    const onlyFirstRes = await request.get(proxyUrl, {
      headers: { 'X-Tenant-Id': tenantId },
    });
    expect(onlyFirstRes.status()).toBe(404);

    // Only second header → 404 (first required header missing)
    const onlySecondRes = await request.get(proxyUrl, {
      headers: { 'X-Api-Key': apiKey },
    });
    expect(onlySecondRes.status()).toBe(404);

    // Neither header → 404
    const noneRes = await request.get(proxyUrl);
    expect(noneRes.status()).toBe(404);
  });

  test('combined header + URL path matching — both conditions must be satisfied', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Header+URL Stub ${suffix}`;
    const stubUrl = `/api/e2e/combined-${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;
    const contentTypeHeader = 'application/json';

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'POST',
      requestHeaders: [{ name: 'Content-Type', value: contentTypeHeader, matchType: 'contains' }],
      responseStatus: '201',
      responseBody: JSON.stringify({ created: true }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // Correct URL + correct Content-Type header → 201
    const correctRes = await request.post(proxyUrl, {
      data: '{"name":"test"}',
      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
    });
    expect(correctRes.status()).toBe(201);

    // Correct URL + wrong Content-Type → 404
    const wrongTypeRes = await request.post(proxyUrl, {
      data: '<name>test</name>',
      headers: { 'Content-Type': 'text/xml' },
    });
    expect(wrongTypeRes.status()).toBe(404);
  });
});
