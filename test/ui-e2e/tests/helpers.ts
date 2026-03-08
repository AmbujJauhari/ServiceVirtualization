import { Page } from '@playwright/test';

/** Backend management API — stub CRUD, health, etc. */
export const BACKEND_URL    = process.env.BACKEND_URL    ?? 'http://localhost:8080';
/** WireMock runtime — clients hit stubs here directly (no proxy). */
export const WIREMOCK_URL   = process.env.WIREMOCK_URL   ?? 'http://localhost:8081';
/**
 * JMS Helper service — standalone Spring Boot app in test/jms-helper that
 * publishes/consumes JMS messages over IBM MQ and Tibco EMS via HTTP so
 * Playwright tests can drive full stub round-trips without any JMS client code.
 * Start with: cd test/jms-helper && mvn spring-boot:run
 */
export const JMS_HELPER_URL = process.env.JMS_HELPER_URL ?? 'http://localhost:9999';

export function uniqueSuffix(): number {
  return Date.now();
}

export interface RequestHeader {
  name: string;
  value: string;
  matchType?: 'exact' | 'regex' | 'contains';
}

export interface CreateStubOptions {
  name: string;
  url: string;
  method?: string;
  urlMatchType?: 'exact' | 'regex' | 'urlPath';
  responseStatus?: string;
  responseBody?: string;
  responseBodyType?: 'json' | 'xml' | 'text' | 'html';
  // Request body matching
  requestBody?: string;
  requestBodyMatchType?: 'exact' | 'json' | 'jsonpath' | 'xpath' | 'contains' | 'regex';
  // Request header matching
  requestHeaders?: RequestHeader[];
  // Callback / webhook response
  useCallback?: boolean;
  callbackUrl?: string;
}

/**
 * Navigate to /rest/stubs/new, fill in all form fields and submit.
 * After a successful submit the browser is redirected back to /rest.
 */
export async function createStubViaUI(page: Page, opts: CreateStubOptions): Promise<void> {
  await page.goto('/rest/stubs/new');

  // ── Basic information ────────────────────────────────────────────────────────
  await page.locator('#name').fill(opts.name);

  // ── Request Matching tab (active by default) ─────────────────────────────────
  await page.locator('#requestMethod').selectOption(opts.method ?? 'GET');
  await page.locator('#requestUrl').fill(opts.url);
  if (opts.urlMatchType && opts.urlMatchType !== 'exact') {
    await page.locator('#requestUrlMatchType').selectOption(opts.urlMatchType);
  }

  // Request headers — fill the pre-existing first row, add rows for extras
  if (opts.requestHeaders && opts.requestHeaders.length > 0) {
    const [first, ...extra] = opts.requestHeaders;

    await page.locator('[placeholder="Header name"]').first().fill(first.name);
    await page.locator('[placeholder="Header value"]').first().fill(first.value);
    if (first.matchType && first.matchType !== 'exact') {
      // Navigate up two levels from the input (col-span-5 → grid-cols-12) to scope the select
      const headerRow = page
        .locator('[placeholder="Header name"]')
        .first()
        .locator('xpath=../..'); // input → parent div → grandparent grid row
      await headerRow.locator('select').selectOption(first.matchType);
    }

    for (let i = 0; i < extra.length; i++) {
      await page.getByRole('button', { name: 'Add Header' }).click();
      const idx = i + 1;
      await page.locator('[placeholder="Header name"]').nth(idx).fill(extra[i].name);
      await page.locator('[placeholder="Header value"]').nth(idx).fill(extra[i].value);
      if (extra[i].matchType && extra[i].matchType !== 'exact') {
        const headerRow = page
          .locator('[placeholder="Header name"]')
          .nth(idx)
          .locator('xpath=../..');
        await headerRow.locator('select').selectOption(extra[i].matchType!);
      }
    }
  }

  // Request body matching
  if (opts.requestBody !== undefined && opts.requestBody !== '') {
    await page.locator('#requestBody').fill(opts.requestBody);
    if (opts.requestBodyMatchType) {
      await page.locator('#requestBodyMatchType').selectOption(opts.requestBodyMatchType);
    }
  }

  // ── Response Definition tab ─────────────────────────────────────────────────
  await page.getByRole('button', { name: 'Response Definition' }).click();

  if (opts.useCallback && opts.callbackUrl) {
    await page.locator('#callback-response').click();
    await page.locator('#callbackUrl').fill(opts.callbackUrl);
  } else {
    if (opts.responseStatus) {
      await page.locator('#responseStatus').selectOption(opts.responseStatus);
    }
    if (opts.responseBodyType) {
      await page.locator('#responseBodyType').selectOption(opts.responseBodyType);
    }
    await page.locator('#responseBody').fill(
      opts.responseBody ?? JSON.stringify({ id: 1, message: 'ok' }, null, 2),
    );
  }

  // ── Submit ──────────────────────────────────────────────────────────────────
  await page.getByRole('button', { name: 'Create Stub' }).click();
  await page.waitForURL('**/rest', { waitUntil: 'commit' });
}

/** Returns a row locator scoped to the stub with the given name. */
export function stubRow(page: Page, stubName: string) {
  return page.locator('tbody tr').filter({ hasText: stubName });
}
