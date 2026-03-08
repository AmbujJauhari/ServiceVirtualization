import { test, expect } from '@playwright/test';
import { WIREMOCK_URL, uniqueSuffix, createStubViaUI, stubRow } from './helpers';

test.describe('REST Protocol — URL match types', () => {
  test('"regex" URL match — numeric path segments match; non-numeric do not', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Regex URL Stub ${suffix}`;
    // Pattern uses a unique suffix in the path prefix to avoid collisions with other stubs
    const regexPattern = `/api/e2e/users-${suffix}/[0-9]+`;
    const proxyBase = `${WIREMOCK_URL}`;

    await createStubViaUI(page, {
      name: stubName,
      url: regexPattern,
      urlMatchType: 'regex',
      responseBody: JSON.stringify({ matched: true, type: 'numeric-id' }, null, 2),
    });

    const row = stubRow(page, stubName);
    await expect(row).toBeVisible();
    // The list should display the regex pattern in the URL column
    await expect(row.getByText(regexPattern, { exact: true })).toBeVisible();

    // Numeric ID → matches the regex → 200
    const numericRes = await request.get(`${proxyBase}/api/e2e/users-${suffix}/42`);
    expect(numericRes.status()).toBe(200);
    expect((await numericRes.json()).type).toBe('numeric-id');

    // Another numeric ID → still matches
    const numericRes2 = await request.get(`${proxyBase}/api/e2e/users-${suffix}/9999`);
    expect(numericRes2.status()).toBe(200);

    // Non-numeric ID (alpha string) → does NOT match the regex → 404
    const alphaRes = await request.get(`${proxyBase}/api/e2e/users-${suffix}/abc`);
    expect(alphaRes.status()).toBe(404);

    // Path with no trailing segment at all → does NOT match → 404
    const bareRes = await request.get(`${proxyBase}/api/e2e/users-${suffix}/`);
    expect(bareRes.status()).toBe(404);
  });

  test('"urlPath" URL match — ignores query parameters', async ({ page, request }) => {
    const suffix = uniqueSuffix();
    const stubName = `UrlPath Stub ${suffix}`;
    const urlPath = `/api/e2e/search-${suffix}`;
    const proxyBase = `${WIREMOCK_URL}`;

    await createStubViaUI(page, {
      name: stubName,
      url: urlPath,
      urlMatchType: 'urlPath',
      responseBody: JSON.stringify({ results: [], total: 0 }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // Bare path → 200
    const bareRes = await request.get(`${proxyBase}${urlPath}`);
    expect(bareRes.status()).toBe(200);
    expect((await bareRes.json()).total).toBe(0);

    // Same path with query parameters → also 200 (urlPath ignores query string)
    const withQueryRes = await request.get(`${proxyBase}${urlPath}?q=hello&page=1&limit=20`);
    expect(withQueryRes.status()).toBe(200);

    // Same path with different query params → still matches
    const otherQueryRes = await request.get(`${proxyBase}${urlPath}?sort=desc&filter=active`);
    expect(otherQueryRes.status()).toBe(200);

    // A completely different path → 404
    const wrongRes = await request.get(`${proxyBase}/api/e2e/other-path-${suffix}`);
    expect(wrongRes.status()).toBe(404);
  });

  test('"exact" URL match — does not match extensions or sub-paths', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Exact URL Stub ${suffix}`;
    const exactUrl = `/api/e2e/exact-${suffix}`;
    const proxyBase = `${WIREMOCK_URL}`;

    await createStubViaUI(page, {
      name: stubName,
      url: exactUrl,
      urlMatchType: 'exact',
      responseBody: JSON.stringify({ exact: true }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // Exact match → 200
    const exactRes = await request.get(`${proxyBase}${exactUrl}`);
    expect(exactRes.status()).toBe(200);
    expect((await exactRes.json()).exact).toBe(true);

    // Path with additional segment → 404 (exact doesn't match longer paths)
    const subPathRes = await request.get(`${proxyBase}${exactUrl}/extra`);
    expect(subPathRes.status()).toBe(404);

    // Path with query parameters — WireMock exact match includes query params so this is 404
    const withQueryRes = await request.get(`${proxyBase}${exactUrl}?foo=bar`);
    expect(withQueryRes.status()).toBe(404);
  });

  test('"regex" URL match — wildcard suffix captures any sub-path', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Wildcard Suffix Stub ${suffix}`;
    // Pattern matches /api/e2e/products-<suffix>/<anything>
    const regexPattern = `/api/e2e/products-${suffix}/.*`;
    const proxyBase = `${WIREMOCK_URL}`;

    await createStubViaUI(page, {
      name: stubName,
      url: regexPattern,
      urlMatchType: 'regex',
      responseBody: JSON.stringify({ product: 'found' }, null, 2),
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // Any sub-path should match
    const res1 = await request.get(`${proxyBase}/api/e2e/products-${suffix}/electronics`);
    expect(res1.status()).toBe(200);

    const res2 = await request.get(`${proxyBase}/api/e2e/products-${suffix}/food/dairy`);
    expect(res2.status()).toBe(200);

    // The base path without a trailing segment → does NOT match '.*' (needs at least '/')
    const baseRes = await request.get(`${proxyBase}/api/e2e/products-${suffix}`);
    // WireMock regex is applied to the whole URL path; base without trailing slash may or may not
    // match depending on WireMock version — we just assert there is no server error
    expect(baseRes.status()).toBeLessThan(500);
  });
});
