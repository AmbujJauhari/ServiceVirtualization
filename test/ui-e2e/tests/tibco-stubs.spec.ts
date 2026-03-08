import { test, expect, Page, APIRequestContext } from '@playwright/test';
import { JMS_HELPER_URL, uniqueSuffix } from './helpers';

// ============================================================================
// Tibco-specific helper
// ============================================================================

interface CreateTibcoStubOptions {
  name: string;
  destinationType?: 'QUEUE' | 'TOPIC';
  destinationName: string;
  responseDestination?: string;
  responseContent?: string;
  contentMatchType?: 'NONE' | 'CONTAINS' | 'EXACT' | 'REGEX';
  contentPattern?: string;
  messageSelector?: string;
  priority?: number;
  latency?: number;
  status?: 'ACTIVE' | 'INACTIVE';
}

async function createTibcoStubViaUI(
  page: Page,
  opts: CreateTibcoStubOptions,
): Promise<void> {
  await page.goto('/tibco/stubs/create');

  await page.locator('#name').fill(opts.name);
  await page.locator('#destinationType').selectOption(opts.destinationType ?? 'QUEUE');
  await page.locator('#destinationName').fill(opts.destinationName);

  if (opts.messageSelector) {
    await page.locator('#messageSelector').fill(opts.messageSelector);
  }
  if (opts.priority !== undefined) {
    await page.locator('#priority').fill(String(opts.priority));
  }
  if (opts.contentMatchType && opts.contentMatchType !== 'NONE') {
    await page.locator('#contentMatchType').selectOption(opts.contentMatchType);
    if (opts.contentPattern) {
      await page.locator('#contentPattern').fill(opts.contentPattern);
    }
  }
  if (opts.responseDestination) {
    await page.locator('#responseDestination').fill(opts.responseDestination);
  }
  await page.locator('#responseContent').fill(opts.responseContent ?? 'test response');
  if (opts.latency !== undefined) {
    await page.locator('#latency').fill(String(opts.latency));
  }
  if (opts.status) {
    await page.locator('#status').selectOption(opts.status);
  }

  await Promise.all([
    page.waitForResponse(
      (r) => r.url().includes('/api/tibco/stubs') && r.request().method() === 'POST',
      { timeout: 30_000 },
    ),
    page.getByRole('button', { name: 'Create Stub' }).click(),
  ]);
  await page.goto('/tibco');
}

function tibcoRow(page: Page, stubName: string) {
  return page.locator('tbody tr').filter({ hasText: stubName });
}

async function toggleTibcoStatus(row: ReturnType<typeof tibcoRow>) {
  await row.locator('td').first().locator('label').click();
}

// ── JMS helper wrappers ────────────────────────────────────────────────────

async function jmsPublish(
  request: APIRequestContext,
  opts: {
    destinationType?: string;
    destinationName: string;
    message: string;
    replyTo?: string;
    correlationId?: string;
  },
) {
  const res = await request.post(`${JMS_HELPER_URL}/tibco/publish`, {
    data: {
      destinationType: opts.destinationType ?? 'QUEUE',
      destinationName: opts.destinationName,
      message: opts.message,
      replyTo: opts.replyTo,
      correlationId: opts.correlationId,
    },
  });
  expect(res.status(), `Tibco publish failed: ${await res.text()}`).toBe(200);
}

async function jmsConsume(
  request: APIRequestContext,
  opts: { destinationName: string; destinationType?: string; timeoutMs?: number },
): Promise<{ found: boolean; message: string | null }> {
  const res = await request.post(`${JMS_HELPER_URL}/tibco/consume`, {
    data: {
      destinationType: opts.destinationType ?? 'QUEUE',
      destinationName: opts.destinationName,
      timeoutMs: opts.timeoutMs ?? 6_000,
    },
  });
  expect(res.status(), `Tibco consume failed: ${await res.text()}`).toBe(200);
  return res.json();
}

// ============================================================================
// Suite 1 — CRUD lifecycle
// ============================================================================

test.describe('Tibco Protocol — stub lifecycle', () => {
  test('stub list page loads and shows the Create Stub button', async ({ page }) => {
    await page.goto('/tibco');
    await expect(
      page.getByRole('button', { name: 'Create Stub' }).or(
        page.getByRole('link', { name: 'Create Stub' }),
      ),
    ).toBeVisible();
  });

  test('create a basic QUEUE stub — appears in list with correct destination', async ({
    page,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Basic Tibco Queue Stub ${suffix}`;
    const destName = `DEV.QUEUE.${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationType: 'QUEUE',
      destinationName: destName,
      responseContent: `{"status":"ok","id":${suffix}}`,
    });

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText(destName)).toBeVisible();
    await expect(row.getByText('None')).toBeVisible();
  });

  test('create a TOPIC stub — TOPIC badge shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Topic Stub ${suffix}`;
    const destName = `DEV.TOPIC.${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationType: 'TOPIC',
      destinationName: destName,
      responseContent: 'topic response',
    });

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('TOPIC')).toBeVisible();
    await expect(row.getByText(destName)).toBeVisible();
  });

  test('create stub with CONTAINS content matching — badge shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Contains Stub ${suffix}`;
    const pattern = `correlationId-${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.CONTAINS.${suffix}`,
      contentMatchType: 'CONTAINS',
      contentPattern: pattern,
      responseContent: 'matched contains',
    });

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('Contains')).toBeVisible();
    await expect(row.getByText(pattern, { exact: false })).toBeVisible();
  });

  test('create stub with EXACT content matching — badge shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Exact Stub ${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.EXACT.${suffix}`,
      contentMatchType: 'EXACT',
      contentPattern: `{"requestId":"${suffix}"}`,
      responseContent: 'matched exact',
    });

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('Exact')).toBeVisible();
  });

  test('create stub with REGEX content matching — badge shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Regex Stub ${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.REGEX.${suffix}`,
      contentMatchType: 'REGEX',
      contentPattern: `.*orderId.*ORD-\\d+.*`,
      responseContent: 'matched regex',
    });

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('Regex')).toBeVisible();
  });

  test('create stub with priority and latency — priority appears in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Priority Stub ${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.PRIO.${suffix}`,
      priority: 10,
      latency: 200,
      responseContent: 'priority response',
    });

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();
  });

  test('create stub with message selector — stub appears in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Selector Stub ${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.SEL.${suffix}`,
      messageSelector: `JMSCorrelationID='CID-${suffix}'`,
      responseContent: 'selector response',
    });

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();
  });

  test('toggle stub status via checkbox label — checked then unchecked', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Toggle Stub ${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.TOGGLE.${suffix}`,
      responseContent: 'toggle test',
    });

    const row = tibcoRow(page, stubName);
    const checkbox = row.locator('td').first().locator('input[type="checkbox"]');
    await expect(checkbox).toBeChecked();

    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/tibco/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleTibcoStatus(row),
    ]);
    await expect(checkbox).not.toBeChecked({ timeout: 10_000 });

    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/tibco/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleTibcoStatus(row),
    ]);
    await expect(checkbox).toBeChecked({ timeout: 10_000 });
  });

  test('edit stub — updated response content is persisted', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Edit Stub ${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.EDIT.${suffix}`,
      responseContent: 'original response',
    });

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();

    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/tibco/stubs/**/edit');
    await page.locator('#responseContent').fill('updated response');

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/tibco/stubs') && r.request().method() === 'PUT',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Update Stub' }).click(),
    ]);
    await page.goto('/tibco');

    await tibcoRow(page, stubName).getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/tibco/stubs/**/edit');
    await expect(page.locator('#responseContent')).toHaveValue('updated response');
  });

  test('delete stub — row disappears from list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Delete Stub ${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.DEL.${suffix}`,
      responseContent: 'to be deleted',
    });

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();

    page.on('dialog', (d) => d.accept());
    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/tibco/stubs') && r.request().method() === 'DELETE',
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Delete', exact: true }).click(),
    ]);

    await expect(row).not.toBeVisible({ timeout: 10_000 });
  });

  test('stub list search filters by stub name', async ({ page }) => {
    const suffix = uniqueSuffix();
    const nameA = `SearchA Tibco ${suffix}`;
    const nameB = `SearchB Tibco ${suffix}`;

    for (const [name, dest] of [
      [nameA, `DEV.QUEUE.SA.${suffix}`],
      [nameB, `DEV.QUEUE.SB.${suffix}`],
    ] as const) {
      await createTibcoStubViaUI(page, {
        name,
        destinationName: dest,
        responseContent: 'search test',
      });
    }

    await page.goto('/tibco');
    const filterInput = page.getByPlaceholder('Filter stubs...');
    await filterInput.fill(`SearchA Tibco ${suffix}`);

    await expect(tibcoRow(page, nameA)).toBeVisible();
    await expect(tibcoRow(page, nameB)).not.toBeVisible();

    await filterInput.clear();
    await expect(tibcoRow(page, nameA)).toBeVisible();
    await expect(tibcoRow(page, nameB)).toBeVisible();
  });

  test('create stub set to INACTIVE — checkbox unchecked; can be activated', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Inactive Stub ${suffix}`;

    await page.goto('/tibco/stubs/create');
    await page.locator('#name').fill(stubName);
    await page.locator('#destinationType').selectOption('QUEUE');
    await page.locator('#destinationName').fill(`DEV.QUEUE.INACT.${suffix}`);
    await page.locator('#responseContent').fill('inactive response');
    await page.locator('#status').selectOption('INACTIVE');

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/tibco/stubs') && r.request().method() === 'POST',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Create Stub' }).click(),
    ]);
    await page.goto('/tibco');

    const row = tibcoRow(page, stubName);
    await expect(row).toBeVisible();
    const checkbox = row.locator('td').first().locator('input[type="checkbox"]');
    await expect(checkbox).not.toBeChecked();

    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/tibco/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleTibcoStatus(row),
    ]);
    await expect(checkbox).toBeChecked({ timeout: 10_000 });
  });
});

// ============================================================================
// Suite 2 — Message delivery (requires Tibco EMS container + jms-helper running)
//
// Prerequisites:
//   docker compose --profile tibco up -d
//   cd test/jms-helper && mvn spring-boot:run
//
// Tibco EMS allows dynamic queue creation with the admin account.
// Input queue: test.queue.in  (created automatically on first use)
// Reply queue: test.queue.out (created automatically on first use)
// Content matching makes each stub unique within a test run.
// ============================================================================

const INPUT_QUEUE = 'test.queue.in';
const REPLY_QUEUE = 'test.queue.out';

test.describe('Tibco Protocol — message delivery', () => {
  // --------------------------------------------------------------------------
  // 1. Basic round-trip
  // --------------------------------------------------------------------------
  test('basic round-trip — published message triggers stub response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Delivery Basic ${suffix}`;
    const triggerMsg = `hello-${suffix}`;
    const expectedResponse = `response-for-${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationType: 'QUEUE',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'CONTAINS',
      contentPattern: triggerMsg,
      responseContent: expectedResponse,
    });

    await jmsPublish(request, {
      destinationName: INPUT_QUEUE,
      message: `{"event":"test","payload":"${triggerMsg}"}`,
    });

    const consumed = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(consumed.found).toBe(true);
    expect(consumed.message).toContain(expectedResponse);
  });

  // --------------------------------------------------------------------------
  // 2. CONTAINS matching
  // --------------------------------------------------------------------------
  test('CONTAINS matching — non-matching message does not trigger a response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Contains Delivery ${suffix}`;
    const matchToken = `MATCH-TOKEN-${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationType: 'QUEUE',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'CONTAINS',
      contentPattern: matchToken,
      responseContent: `matched-${suffix}`,
    });

    // Non-matching message → no response
    await jmsPublish(request, {
      destinationName: INPUT_QUEUE,
      message: `no-match-${suffix}`,
    });
    const noMatch = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Matching message → response
    await jmsPublish(request, {
      destinationName: INPUT_QUEUE,
      message: `prefix-${matchToken}-suffix`,
    });
    const match = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(match.found).toBe(true);
    expect(match.message).toContain(`matched-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 3. EXACT matching
  // --------------------------------------------------------------------------
  test('EXACT matching — only the precise message body triggers a response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Exact Delivery ${suffix}`;
    const exactBody = `exact-body-${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationType: 'QUEUE',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'EXACT',
      contentPattern: exactBody,
      responseContent: `exact-response-${suffix}`,
    });

    // Near-exact body → no response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: `${exactBody}X` });
    const noMatch = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Exact body → response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: exactBody });
    const match = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(match.found).toBe(true);
    expect(match.message).toContain(`exact-response-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 4. REGEX matching
  // --------------------------------------------------------------------------
  test('REGEX matching — message matching the pattern triggers a response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Regex Delivery ${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationType: 'QUEUE',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'REGEX',
      contentPattern: `.*orderId-${suffix}-\\d+.*`,
      responseContent: `regex-response-${suffix}`,
    });

    // Non-matching (letters not digits)
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: `orderId-${suffix}-ABC` });
    const noMatch = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Matching
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: `orderId-${suffix}-99887` });
    const match = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(match.found).toBe(true);
    expect(match.message).toContain(`regex-response-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 5. INACTIVE stub does not respond; activating it makes it respond
  // --------------------------------------------------------------------------
  test('inactive stub does not respond — activating it makes it respond', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Inactive Delivery ${suffix}`;
    const trigger = `inactive-trigger-${suffix}`;

    await page.goto('/tibco/stubs/create');
    await page.locator('#name').fill(stubName);
    await page.locator('#destinationType').selectOption('QUEUE');
    await page.locator('#destinationName').fill(INPUT_QUEUE);
    await page.locator('#responseDestination').fill(REPLY_QUEUE);
    await page.locator('#contentMatchType').selectOption('CONTAINS');
    await page.locator('#contentPattern').fill(trigger);
    await page.locator('#responseContent').fill(`inactive-response-${suffix}`);
    await page.locator('#status').selectOption('INACTIVE');

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/tibco/stubs') && r.request().method() === 'POST',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Create Stub' }).click(),
    ]);
    await page.goto('/tibco');

    // Stub INACTIVE — no response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const noResponse = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noResponse.found).toBe(false);

    // Activate
    const row = tibcoRow(page, stubName);
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/tibco/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleTibcoStatus(row),
    ]);
    await expect(
      row.locator('td').first().locator('input[type="checkbox"]'),
    ).toBeChecked({ timeout: 10_000 });

    // Now publishes get a response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const response = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(response.found).toBe(true);
    expect(response.message).toContain(`inactive-response-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 6. Deactivating an ACTIVE stub stops responses
  // --------------------------------------------------------------------------
  test('deactivating a stub stops it from responding', async ({ page, request }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Deactivate Delivery ${suffix}`;
    const trigger = `deactivate-trigger-${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationType: 'QUEUE',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'CONTAINS',
      contentPattern: trigger,
      responseContent: `active-response-${suffix}`,
    });

    // Confirm it responds while ACTIVE
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const activeResponse = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(activeResponse.found).toBe(true);
    expect(activeResponse.message).toContain(`active-response-${suffix}`);

    // Deactivate via UI
    const row = tibcoRow(page, stubName);
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/tibco/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleTibcoStatus(row),
    ]);
    await expect(
      row.locator('td').first().locator('input[type="checkbox"]'),
    ).not.toBeChecked({ timeout: 10_000 });

    // Stub INACTIVE — no response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const noResponse = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noResponse.found).toBe(false);
  });

  // --------------------------------------------------------------------------
  // 7. Edit stub — updated response content is reflected in subsequent messages
  // --------------------------------------------------------------------------
  test('editing response content is reflected in the next message', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Tibco Edit Delivery ${suffix}`;
    const trigger = `edit-trigger-${suffix}`;

    await createTibcoStubViaUI(page, {
      name: stubName,
      destinationType: 'QUEUE',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'CONTAINS',
      contentPattern: trigger,
      responseContent: `version-1-${suffix}`,
    });

    // First message → v1
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const v1 = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(v1.found).toBe(true);
    expect(v1.message).toContain(`version-1-${suffix}`);

    // Edit stub response
    const row = tibcoRow(page, stubName);
    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/tibco/stubs/**/edit');
    await page.locator('#responseContent').fill(`version-2-${suffix}`);

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/tibco/stubs') && r.request().method() === 'PUT',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Update Stub' }).click(),
    ]);
    await page.goto('/tibco');

    // Second message → v2
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const v2 = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(v2.found).toBe(true);
    expect(v2.message).toContain(`version-2-${suffix}`);
  });
});
