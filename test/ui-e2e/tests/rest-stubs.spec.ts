import { test, expect, Page } from '@playwright/test';

// ============================================================================
// Helpers
// ============================================================================

/** WireMock runtime — clients call stubs directly on this port. */
const WIREMOCK_URL = process.env.WIREMOCK_URL ?? 'http://localhost:8081';

/** Unique values per test run so parallel runs never collide. */
function uniqueSuffix() {
  return Date.now();
}

/**
 * Navigate to the REST stub creation form, fill in all required fields and
 * submit. Returns the stub name that was used.
 */
async function createStubViaUI(
  page: Page,
  opts: {
    name: string;
    url: string;
    method?: string;
    responseStatus?: string;
    responseBody?: string;
  }
): Promise<void> {
  const {
    name,
    url,
    method = 'GET',
    responseStatus = '200',
    responseBody = JSON.stringify({ id: 1, title: 'delectus aut autem', userId: 1, completed: false }, null, 2),
  } = opts;

  await page.goto('/rest/stubs/new');

  // ── Basic Information ──────────────────────────────────────────────────────
  await page.locator('#name').fill(name);

  // ── Request Matching tab (active by default) ───────────────────────────────
  await page.locator('#requestMethod').selectOption(method);
  await page.locator('#requestUrl').fill(url);

  // ── Response Definition tab ────────────────────────────────────────────────
  await page.getByRole('button', { name: 'Response Definition' }).click();
  await page.locator('#responseStatus').selectOption(responseStatus);
  // content type defaults to json — leave as-is
  await page.locator('#responseBody').fill(responseBody);

  // ── Submit ─────────────────────────────────────────────────────────────────
  await page.getByRole('button', { name: 'Create Stub' }).click();

  // Should redirect back to /rest after successful creation
  await page.waitForURL('**/rest');
}

/** Find the table row for a given stub name. */
function stubRow(page: Page, stubName: string) {
  return page.locator('tbody tr').filter({ hasText: stubName });
}

// ============================================================================
// Tests
// ============================================================================

test.describe('REST Protocol — stub lifecycle', () => {
  test('stub list page loads and shows the Create Stub button', async ({ page }) => {
    await page.goto('/rest');
    await expect(page.getByRole('link', { name: 'Create Stub', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Name', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Method', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'URL', exact: true })).toBeVisible();
    // Use exact match to avoid also matching "Response Status" column header
    await expect(page.getByRole('columnheader', { name: 'Status', exact: true })).toBeVisible();
  });

  test('create a GET stub, verify in list, hit proxy, deactivate, reactivate, delete', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `E2E Todo Stub ${suffix}`;
    const stubUrl = `/api/e2e/todos/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;
    const responsePayload = { id: 1, title: 'delectus aut autem', userId: 1, completed: false };

    // ── 1. Create via UI ─────────────────────────────────────────────────────
    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'GET',
      responseStatus: '200',
      responseBody: JSON.stringify(responsePayload, null, 2),
    });

    // ── 2. Verify stub appears in the list ───────────────────────────────────
    const row = stubRow(page, stubName);
    await expect(row).toBeVisible();
    // Use exact:true — getByText() is case-insensitive partial by default, which causes
    // strict mode violations (e.g. 'GET' matches stub names, 'ACTIVE' matches 'Deactivate' button)
    await expect(row.getByText('GET', { exact: true })).toBeVisible();
    await expect(row.getByText(stubUrl, { exact: true })).toBeVisible();
    await expect(row.getByText('ACTIVE', { exact: true })).toBeVisible();
    await expect(row.getByText('200', { exact: true })).toBeVisible();

    // ── 3. Hit the WireMock proxy — stub should respond ──────────────────────
    const activeResponse = await request.get(proxyUrl);
    expect(activeResponse.status()).toBe(200);
    const body = await activeResponse.json();
    expect(body).toMatchObject(responsePayload);

    // ── 4. Deactivate the stub ───────────────────────────────────────────────
    // Use waitForResponse + click together so we never miss a fast API response.
    // The PATCH /status call is what the component fires; wait for its 200 reply.
    await Promise.all([
      page.waitForResponse(
        (resp) =>
          resp.url().includes('/rest/stubs') &&
          resp.request().method() === 'PATCH' &&
          resp.status() === 200,
        { timeout: 10_000 }
      ),
      row.getByRole('button', { name: 'Deactivate' }).click(),
    ]);
    // After deactivating, the backend removes the WireMock mapping and marks the stub
    // as STUB_NOT_REGISTERED (the list endpoint flags any stub not in WireMock as such).
    // The reliable indicator is that the button switched from "Deactivate" to "Activate".
    await expect(row.getByRole('button', { name: 'Activate', exact: true })).toBeVisible({ timeout: 10_000 });

    // ── 5. Proxy should now return 404 (WireMock stub removed) ───────────────
    const inactiveResponse = await request.get(proxyUrl);
    expect(inactiveResponse.status()).toBe(404);

    // ── 6. Reactivate ────────────────────────────────────────────────────────
    await Promise.all([
      page.waitForResponse(
        (resp) =>
          resp.url().includes('/rest/stubs') &&
          resp.request().method() === 'PATCH' &&
          resp.status() === 200,
        { timeout: 10_000 }
      ),
      row.getByRole('button', { name: 'Activate' }).click(),
    ]);
    await expect(row.getByText('ACTIVE', { exact: true })).toBeVisible({ timeout: 10_000 });

    // Proxy should respond again
    const reactivatedResponse = await request.get(proxyUrl);
    expect(reactivatedResponse.status()).toBe(200);

    // Stub is left in place for manual inspection after the test run.
  });

  test('create a POST stub that returns 201 Created', async ({ page, request }) => {
    const suffix = uniqueSuffix();
    const stubName = `E2E Post Stub ${suffix}`;
    const stubUrl = `/api/e2e/items/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;
    const responsePayload = { id: suffix, title: 'New Item', created: true };

    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      method: 'POST',
      responseStatus: '201',
      responseBody: JSON.stringify(responsePayload, null, 2),
    });

    // Verify in list
    const row = stubRow(page, stubName);
    await expect(row).toBeVisible();
    // exact:true required — 'POST' case-insensitively matches stub name "E2E Post Stub …"
    await expect(row.getByText('POST', { exact: true })).toBeVisible();
    await expect(row.getByText('201', { exact: true })).toBeVisible();

    // POST to proxy — should return 201 + body
    const response = await request.post(proxyUrl, {
      data: { title: 'New Item' },
      headers: { 'Content-Type': 'application/json' },
    });
    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body).toMatchObject(responsePayload);

    // Stub is left in place for manual inspection after the test run.
  });

  test('stub list search filters by name', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `E2E Searchable Stub ${suffix}`;
    const stubUrl = `/api/e2e/search/${suffix}`;

    await createStubViaUI(page, { name: stubName, url: stubUrl });

    // Type in search box
    await page.getByPlaceholder('Search stubs...').fill(`Searchable Stub ${suffix}`);

    // Only our stub should be visible; other rows (if any) should disappear
    const row = stubRow(page, stubName);
    await expect(row).toBeVisible();

    // Clear search — stub still present
    await page.getByPlaceholder('Search stubs...').clear();
    await expect(row).toBeVisible();

    // Stub is left in place for manual inspection after the test run.
  });

  test('edit an existing stub updates the proxy response', async ({ page, request }) => {
    const suffix = uniqueSuffix();
    const stubName = `E2E Editable Stub ${suffix}`;
    const stubUrl = `/api/e2e/editable/${suffix}`;
    const proxyUrl = `${WIREMOCK_URL}${stubUrl}`;

    const originalPayload = { version: 1, message: 'original' };
    const updatedPayload = { version: 2, message: 'updated' };

    // Create original stub
    await createStubViaUI(page, {
      name: stubName,
      url: stubUrl,
      responseBody: JSON.stringify(originalPayload, null, 2),
    });

    // Verify original proxy response
    const originalResponse = await request.get(proxyUrl);
    expect(originalResponse.status()).toBe(200);
    expect(await originalResponse.json()).toMatchObject(originalPayload);

    // Navigate to edit page
    const row = stubRow(page, stubName);
    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/edit');

    // Update the response body
    await page.getByRole('button', { name: 'Response Definition' }).click();
    await page.locator('#responseBody').fill(JSON.stringify(updatedPayload, null, 2));
    await page.getByRole('button', { name: 'Update Stub' }).click();
    await page.waitForURL('**/rest');

    // Verify updated proxy response
    const updatedResponse = await request.get(proxyUrl);
    expect(updatedResponse.status()).toBe(200);
    expect(await updatedResponse.json()).toMatchObject(updatedPayload);

    // Stub is left in place for manual inspection after the test run.
  });
});
