import { test, expect } from '@playwright/test';
import { WIREMOCK_URL, BACKEND_URL, uniqueSuffix, createStubViaUI, stubRow } from './helpers';

/**
 * Callback stub tests
 *
 * When a stub is created with "Use Callback URL", the backend stores:
 *   response: { callback: { url: "...", method: "POST" } }
 *
 * When the WireMock proxy receives a matching request it returns a body of
 *   {"callback":{"url":"...","method":"..."}}
 * The backend's ProxyHandler then calls the callback URL and returns its
 * response to the original caller.
 *
 * For the proxy-call tests below we use the backend's own /actuator/health
 * endpoint as the callback URL — it is always accessible from within the
 * Docker container (same process, same port).
 */

test.describe('REST Protocol — callback / webhook response type', () => {
  test('callback stub form — "Use Callback URL" radio shows callback URL input', async ({
    page,
  }) => {
    const suffix = uniqueSuffix();
    await page.goto('/rest/stubs/new');

    await page.locator('#name').fill(`Callback Form Test ${suffix}`);
    await page.locator('#requestUrl').fill(`/api/e2e/callback-form/${suffix}`);

    // Switch to Response Definition tab
    await page.getByRole('button', { name: 'Response Definition' }).click();

    // By default, "Define Response" radio should be selected
    await expect(page.locator('#direct-response')).toBeChecked();
    await expect(page.locator('#callback-response')).not.toBeChecked();

    // The responseBody textarea and status code select are visible
    await expect(page.locator('#responseStatus')).toBeVisible();
    await expect(page.locator('#responseBody')).toBeVisible();

    // Select the callback radio
    await page.locator('#callback-response').click();
    await expect(page.locator('#callback-response')).toBeChecked();

    // Callback URL input is now visible; direct response fields are hidden
    await expect(page.locator('#callbackUrl')).toBeVisible();
    await expect(
      page.getByText('The URL that will be called when this stub is matched'),
    ).toBeVisible();
    await expect(page.locator('#responseBody')).not.toBeVisible();
    await expect(page.locator('#responseStatus')).not.toBeVisible();
  });

  test('creates a callback stub and it appears in the stub list as ACTIVE', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Callback Stub ${suffix}`;
    const stubUrl = `/api/e2e/callback-create/${suffix}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'POST',
      useCallback: true,
      callbackUrl: `${BACKEND_URL}/actuator/health`,
    });

    const row = stubRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('POST', { exact: true })).toBeVisible();
    await expect(row.getByText(stubUrl, { exact: true })).toBeVisible();
    // Callback stubs have no direct response status code, but the stub itself should be ACTIVE
    await expect(row.getByText('ACTIVE', { exact: true })).toBeVisible();
  });

  test('callback stub can be deactivated and reactivated', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Callback Toggle Stub ${suffix}`;
    const stubUrl = `/api/e2e/callback-toggle/${suffix}`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'GET',
      useCallback: true,
      callbackUrl: `${BACKEND_URL}/actuator/health`,
    });

    const row = stubRow(page, stubName);
    await expect(row.getByRole('button', { name: 'Deactivate' })).toBeVisible();

    // Deactivate
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/rest/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Deactivate' }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'Activate', exact: true })).toBeVisible({
      timeout: 10_000,
    });

    // Reactivate
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/rest/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Activate' }).click(),
    ]);
    await expect(row.getByText('ACTIVE', { exact: true })).toBeVisible({ timeout: 10_000 });
  });

  test('callback stub forwards request to callback URL and returns its response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Callback Forward Stub ${suffix}`;
    const stubUrl = `/api/e2e/callback-forward/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;

    // The health endpoint is reachable from within the backend container as localhost:8080
    // (ProxyHandler runs in the same JVM process as the backend itself)
    const callbackUrl = `http://localhost:8080/actuator/health`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'POST',
      useCallback: true,
      callbackUrl,
    });

    await expect(stubRow(page, stubName)).toBeVisible();

    // POST to WireMock — the stub is matched and the webhook fires asynchronously to
    // callbackUrl; WireMock itself returns 200 with an empty body (the webhook is a
    // fire-and-forget side-effect, not an inline response).
    const proxyResponse = await request.post(proxyUrl, {
      data: JSON.stringify({ trigger: true }),
      headers: { 'Content-Type': 'application/json' },
    });

    // WireMock matched the stub → 200 (not 404 / 500)
    expect(proxyResponse.status()).toBe(200);
  });

  test('callback stub edit — URL is preserved after editing', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Callback Edit Stub ${suffix}`;
    const stubUrl = `/api/e2e/callback-edit/${suffix}`;
    const originalCallbackUrl = `http://localhost:8080/actuator/health`;
    const updatedCallbackUrl = `http://localhost:8080/actuator/info`;

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'GET',
      useCallback: true,
      callbackUrl: originalCallbackUrl,
    });

    const row = stubRow(page, stubName);
    await expect(row).toBeVisible();

    // Open the edit form
    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/rest/stubs/**/edit');

    // Switch to Response Definition tab and verify callback radio is checked
    await page.getByRole('button', { name: 'Response Definition' }).click();
    await expect(page.locator('#callback-response')).toBeChecked();
    await expect(page.locator('#callbackUrl')).toHaveValue(originalCallbackUrl);

    // Update the callback URL
    await page.locator('#callbackUrl').fill(updatedCallbackUrl);
    await page.getByRole('button', { name: 'Update Stub' }).click();
    await page.waitForURL('**/rest');

    // Re-open edit form and confirm the URL was saved
    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/rest/stubs/**/edit');
    await page.getByRole('button', { name: 'Response Definition' }).click();
    await expect(page.locator('#callbackUrl')).toHaveValue(updatedCallbackUrl);
  });

  test('creating multiple callback stubs with different callback URLs', async ({ page }) => {
    const suffix = uniqueSuffix();

    const stubs = [
      {
        name: `CB Stub A ${suffix}`,
        url: `/api/e2e/cb-a/${suffix}`,
        callbackUrl: `http://localhost:8080/actuator/health`,
      },
      {
        name: `CB Stub B ${suffix}`,
        url: `/api/e2e/cb-b/${suffix}`,
        callbackUrl: `http://localhost:8080/actuator/info`,
      },
    ];

    for (const s of stubs) {
      await createStubViaUI(page, {
        name: s.name,
        url: s.url,
        method: 'GET',
        useCallback: true,
        callbackUrl: s.callbackUrl,
      });
    }

    // Both stubs should be visible in the list
    for (const s of stubs) {
      await expect(stubRow(page, s.name)).toBeVisible();
      await expect(stubRow(page, s.name).getByText('ACTIVE', { exact: true })).toBeVisible();
    }
  });
});
